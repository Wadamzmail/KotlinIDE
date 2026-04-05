package dev.mutwakil.kotlinide.activity;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.editor.LspProject;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition;
import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider;
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel;
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.langs.textmate.registry.dsl.LanguageDefinitionListBuilder;

import dev.mutwakil.kotlinide.databinding.ActivityEditorBinding;

import java.io.*;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import kotlin.Unit;

public class LspTestJavaActivity extends AppCompatActivity {

    private CodeEditor editor;
    private ActivityEditorBinding binding;
    private LspProject lspProject;
    private LspEditor lspEditor;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        editor = binding.editor;

        // خط الخطوط
        Typeface font = Typeface.createFromAsset(getAssets(), "JetBrainsMono-Regular.ttf");
        editor.setTypefaceLineNumber(font);
        editor.setTypefaceText(font);

        try {
            setupTextMateTheme();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // فك الضغط وتشغيل السيرفر والاتصال به
        ForkJoinPool.commonPool().execute(() -> {
            try {
                extractServer();
                startLanguageServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void setupTextMateTheme() throws Exception {
        FileProviderRegistry.getInstance().addFileProvider(new AssetsFileResolver(getAssets()));
        ThemeRegistry themeRegistry = ThemeRegistry.getInstance();
        String path = "textmate/QuietLight.tmTheme";
        themeRegistry.loadTheme(new ThemeModel(FileProviderRegistry.getInstance().tryGetInputStream(path)),
                "quitelight"
        ));
        themeRegistry.setTheme("quietlight");
        editor.setColorScheme(TextMateColorScheme.create(themeRegistry));
    }

    private void extractServer() throws IOException {
        File serverDir = new File(getFilesDir(), "server");
        if (!serverDir.exists()) serverDir.mkdirs();

        ZipFile zipFile = new ZipFile(new File(getFilesDir(), "server.zip"));
        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            File outFile = new File(serverDir, entry.getName());

            if (entry.isDirectory()) {
                outFile.mkdirs();
                continue;
            }

            InputStream is = zipFile.getInputStream(entry);
            FileOutputStream fos = new FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            is.close();

            if (outFile.getName().equals("kotlin-language-server")) {
                outFile.setExecutable(true);
            }
        }
        zipFile.close();
    }

    private void startLanguageServer() throws IOException, InterruptedException {
        File serverBinary = new File(getFilesDir(), "server/bin/kotlin-language-server");
        ProcessBuilder pb = new ProcessBuilder(
                serverBinary.getAbsolutePath(),
                "--tcp", "127.0.0.1:2087"
        );
        pb.redirectErrorStream(true);
        pb.start();

        Thread.sleep(1500); // اعطاء وقت للسيرفر يبدأ

        connectLspClient();
    }

    private void connectLspClient() {
        runOnUiThread(() -> Toast.makeText(this, "Connecting to LSP server...", Toast.LENGTH_SHORT).show());

        String projectPath = getExternalCacheDir() + "/testProject";
        lspProject = new LspProject(projectPath);

        CustomLanguageServerDefinition kotlinServerDefinition =
                new CustomLanguageServerDefinition("kt", workingDir -> new TcpSocketConnectionProvider("127.0.0.1", 2087));

        lspProject.addServerDefinition(kotlinServerDefinition);

        lspEditor = lspProject.createEditor(projectPath + "/sample.kt");
        lspEditor.setEditor(editor);
        lspEditor.setWrapperLanguage(createTextMateLanguage());
        try {
            lspEditor.connectWithTimeoutBlocking();
        } catch (Exception e) {
            e.printStackTrace();
        }

        runOnUiThread(() -> Toast.makeText(this, "LSP Connected", Toast.LENGTH_SHORT).show());
    }

    private TextMateLanguage createTextMateLanguage() {
        LanguageDefinitionListBuilder builder = new LanguageDefinitionListBuilder();
        builder.language("kotlin", languageDefinitionBuilder -> {
            languageDefinitionBuilder.setGrammar("textmate/kotlin/syntaxes/kotlin.tmLanguage");
            languageDefinitionBuilder.setScopeName("source.kotlin");
            languageDefinitionBuilder.setLanguageConfiguration("textmate/kotlin/language-configuration.json");
            return Unit.INSTANCE;
        });
        io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry.getInstance().loadGrammars(builder.build());
        return TextMateLanguage.create("source.kotlin", false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            lspEditor.dispose();
            lspProject.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -------------------- TCP Connection Provider --------------------
    static class TcpSocketConnectionProvider extends CustomLanguageServerDefinition {
        private final String host;
        private final int port;

        public TcpSocketConnectionProvider(String host, int port) {
            this.host = host;
            this.port = port;
        }

         
        public InputStream getInputStream() throws IOException {
            return new Socket(host, port).getInputStream();
        }

         
        public OutputStream getOutputStream() throws IOException {
            return new Socket(host, port).getOutputStream();
        }

       
        public void close() throws IOException {
            // Socket سيتم اغلاقه عند streams
        }
    }
}