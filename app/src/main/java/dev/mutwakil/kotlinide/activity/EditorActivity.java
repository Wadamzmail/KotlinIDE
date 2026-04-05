package dev.mutwakil.kotlinide.activity;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.Scanner;

import dev.mutwakil.kotlinide.databinding.ActivityEditorBinding;
import dev.mutwakil.kotlinide.lsp.LSPManager;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora2.text.EditorUtil;

public class EditorActivity extends AppCompatActivity {

    private ActivityEditorBinding binding;
    private CodeEditor editor;
    
    // متغيرات المسارات (ستمتلئ من Intent)
    private String projectRoot;
    private String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        editor = binding.editor;
        editor.setColorScheme(EditorUtil.getDefaultColorScheme(this, false));
        
        // 1. استقبال البيانات المرسلة من MainActivity
        String receivedFileUri = getIntent().getStringExtra("FILE_URI");
        String receivedProjectRoot = getIntent().getStringExtra("PROJECT_ROOT");

        // تنظيف المسارات (تحويل content:// أو file:// إلى مسار نظام ملفات عادي لـ File class)
        if (receivedFileUri != null) {
            this.filePath = formatToAbsolutePath(receivedFileUri);
        }
        
        if (receivedProjectRoot != null) {
            this.projectRoot = formatToAbsolutePath(receivedProjectRoot);
        }

        // 2. تشغيل الـ LSP Server والـ Client (Singleton)
        LSPManager.setupLSP();

        // 3. عمل Initialize للسيرفر باستخدام جذر المشروع المختار
        if (LSPManager.lspClient != null && projectRoot != null) {
            // السيرفر يحتاج URI يبدأ بـ file://
            LSPManager.lspClient.initServer("file://" + projectRoot);
        }

        // 4. قراءة الملف وعرضه في المحرر وإبلاغ السيرفر
        if (filePath != null) {
            loadFileAndNotifyLSP();
        }
    }

    private void loadFileAndNotifyLSP() {
        File file = new File(filePath);
        if (!file.exists()) {
            Log.e("Editor", "File not found: " + filePath);
            return;
        }

        try {
            // قراءة محتوى الملف
            Scanner scanner = new Scanner(file, "UTF-8").useDelimiter("\\A");
            String content = scanner.hasNext() ? scanner.next() : "";
            scanner.close();

            // عرض النص في Sora Editor
            editor.setText(content);

            // 5. إبلاغ السيرفر بفتح الملف (استخدام URI كامل للسيرفر)
            if (LSPManager.lspClient != null) {
                LSPManager.lspClient.onFileOpened("file://" + filePath, content);
            }

        } catch (Exception e) {
            Log.e("LSP_ERROR", "Error loading file: " + e.getMessage());
        }
    }

    /**
     * دالة مساعدة لتنظيف المسار.
     * ملاحظة: إذا كنت تستخدم Android 11+، قد تحتاج لمكتبة FileUtils 
     * لتحويل content:// URI إلى Absolute Path بشكل دقيق.
     */
    private String formatToAbsolutePath(String uri) {
        if (uri.startsWith("file://")) {
            return uri.replace("file://", "");
        }
        // معالجة بسيطة لبعض الـ URIs الشائعة في الـ Pickers
        if (uri.startsWith("content://")) {
            // هنا يفضل استدعاء دالة تحويل URI إلى Path حقيقي
            // للتبسيط في التجربة اليدوية، سنفترض أن المستخدم يدخل مساراً صحيحاً
            return uri; 
        }
        return uri;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        // يفضل عدم إغلاق السيرفر هنا إذا كنت ستفتح ملفات أخرى لاحقاً
    }
}
