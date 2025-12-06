package dev.mutwakil.kotlinide.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import dev.mutwakil.kotlinide.databinding.ActivityEditorBinding;
import dev.mutwakil.kotlinide.language.kotlin.KotlinLanguage;
import dev.mutwakil.lsp.kotlin.MyKotlinClient;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora2.text.EditorUtil;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.javacs.kt.KotlinLanguageServer;
import org.eclipse.lsp4j.TextDocumentIdentifier;

public class EditorActivity extends AppCompatActivity {
  private ActivityEditorBinding binding;
  private CodeEditor editor;
  public static KotlinLanguageServer server;
   public static String currentFileUri = "";	

  PipedInputStream clientIn;
  PipedOutputStream serverOut;

  PipedInputStream serverIn;
  PipedOutputStream clientOut;
  ExecutorService executor;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityEditorBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    editor = binding.editor;
    editor.setColorScheme(EditorUtil.getDefaultColorScheme(this, false));
    editor.setEditorLanguage(new KotlinLanguage(editor));
    editor.setText("class Main{\n  fun main(){\n} \n}");

    executor = Executors.newSingleThreadExecutor();

    executor.submit(
        () -> {
          try {

            // 1) أنشئ السيرفر والكلينت
            KotlinLanguageServer server = new KotlinLanguageServer();
            MyKotlinClient client = new MyKotlinClient();

            // 2) Streams
            PipedInputStream clientIn = new PipedInputStream();
            PipedOutputStream serverOut = new PipedOutputStream(clientIn);

            PipedInputStream serverIn = new PipedInputStream();
            PipedOutputStream clientOut = new PipedOutputStream(serverIn);

            // 3) Launcher للـ server
            Launcher<LanguageClient> serverLauncher =
                LSPLauncher.createServerLauncher(server, serverIn, serverOut);

            // 4) Launcher للـ client
            Launcher<LanguageServer> clientLauncher =
                LSPLauncher.createClientLauncher(client, clientIn, clientOut);

            // 5) اربط السيرفر بالعميل
            server.connect(serverLauncher.getRemoteProxy());

            // 6) ابدأ الاثنين
            serverLauncher.startListening();
            clientLauncher.startListening();

            InitializeParams init = new InitializeParams();
            init.setRootUri("file:///storage/emulated/0/.kotlinide/test/");
            server.initialize(init);

            String path = "/storage/emulated/0/.kotlinide/test/src/main.kt";
            String uri = "file://" + path;
			currentFileUri=uri;		

            String fileContent = new String(Files.readAllBytes(Paths.get(path)));
            TextDocumentItem item = new TextDocumentItem(uri, "kotlin", 1, fileContent);

            server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(item));

          } catch (Exception e) {
            e.printStackTrace();
          }
        });
  }

  @Override
  protected void onDestroy() {
    binding = null;
    super.onDestroy();
    // TODO: Implement this method
  }
}
