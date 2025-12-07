package dev.mutwakil.kotlinide.activity;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import dev.mutwakil.kotlinide.databinding.ActivityEditorBinding;

import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora2.text.EditorUtil;
import org.eclipse.lsp4j.InitializeParams;

public class EditorActivity extends AppCompatActivity {

  private ActivityEditorBinding binding;
  private CodeEditor editor;
  private LspEditor lspEditor; // هذا هو المدير المسؤول

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityEditorBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    editor = binding.editor;

    // إعدادات المظهر
    editor.setColorScheme(EditorUtil.getDefaultColorScheme(this, false));

    editor.setText("fun main() {\n    println(\"Hello World\")\n}");

    String myProjectRoot = "file:///storage/emulated/0/.kotlinide/test/";
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (lspEditor != null) {
      // تنظيف الموارد
      // lspEditor.dispose(); // تأكد من اسم الدالة حسب إصدار مكتبتك
    }
    binding = null;
  }
}
