package dev.mutwakil.kotlinide;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import dev.mutwakil.kotlinide.activity.EditorActivity;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 100;
    private EditText etProjectPath;
    private String selectedFileUriString = ""; // لتخزين مسار الملف المختار

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etProjectPath = findViewById(R.id.etProjectPath);
        Button btnPick = findViewById(R.id.btnPick);
        Button btnGo = findViewById(R.id.btnGo);

        // عند الضغط على Pick
        btnPick.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openFilePicker();
            } else {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 50);
            }
        });

        // عند الضغط على Go
        btnGo.setOnClickListener(v -> {
            String projectRoot = etProjectPath.getText().toString();
            
            if (selectedFileUriString.isEmpty()) {
                Toast.makeText(this, "Please pick a file first!", Toast.LENGTH_SHORT).show();
                return;
            }

            // إرسال البيانات إلى EditorActivity
            Intent intent = new Intent(this, EditorActivity.class);
            intent.putExtra("FILE_URI", selectedFileUriString);
            intent.putExtra("PROJECT_ROOT", projectRoot);
            startActivity(intent);
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                selectedFileUriString = uri.toString();
                // وضع المسار في الـ EditText لكي يعدله المستخدم (Project Root)
                etProjectPath.setText(selectedFileUriString);
            }
        }
    }
}
