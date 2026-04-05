package dev.mutwakil.kotlinide;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dev.mutwakil.kotlinide.activity.EditorActivity;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 100;
    private static final int PICK_ZIP_REQUEST = 101;
    private static final int PERMISSION_REQUEST_CODE = 50;

    private EditText etProjectPath;
    private String selectedFileUriString = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ربط العناصر من الـ XML
        etProjectPath = findViewById(R.id.etProjectPath);
        Button btnPick = findViewById(R.id.btnPick);
        Button btnGo = findViewById(R.id.btnGo);
        Button btnExtractJdk = findViewById(R.id.btnExtractJdk);

        // 1. زر اختيار ملف الكود
        btnPick.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openChooser("*/*", PICK_FILE_REQUEST);
            } else {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        });

        // 2. زر الانتقال للمحرر
        btnGo.setOnClickListener(v -> {
            String projectRoot = etProjectPath.getText().toString();
            if (selectedFileUriString.isEmpty()) {
                Toast.makeText(this, "Please pick a file first!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, EditorActivity.class);
            intent.putExtra("FILE_URI", selectedFileUriString);
            intent.putExtra("PROJECT_ROOT", projectRoot);
            startActivity(intent);
        });

        // 3. زر فك ضغط الـ JDK
        btnExtractJdk.setOnClickListener(v -> openChooser("application/zip", PICK_ZIP_REQUEST));
    }

    private void openChooser(String type, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(type);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select File"), requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;

            if (requestCode == PICK_FILE_REQUEST) {
                selectedFileUriString = uri.toString();
                etProjectPath.setText(selectedFileUriString);
            } else if (requestCode == PICK_ZIP_REQUEST) {
                extractJdk(uri);
            }
        }
    }

    private void extractJdk(Uri zipUri) {
        // تشغيل العملية في خيط خلفي (Background Thread)
        new Thread(() -> {
            try {
                // المسار: /data/user/0/dev.mutwakil.kotlinide/files/jdk
                File outputDir = new File(getFilesDir(), "jdk");
                if (!outputDir.exists()) outputDir.mkdirs();

                InputStream is = getContentResolver().openInputStream(zipUri);
                ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
                ZipEntry ze;
                byte[] buffer = new byte[8192];

                runOnUiThread(() -> Toast.makeText(this, "Extracting JDK...", Toast.LENGTH_SHORT).show());

                while ((ze = zis.getNextEntry()) != null) {
                    File f = new File(outputDir, ze.getName());
                    if (ze.isDirectory()) {
                        f.mkdirs();
                    } else {
                        f.getParentFile().mkdirs();
                        try (FileOutputStream fos = new FileOutputStream(f)) {
                            int count;
                            while ((count = zis.read(buffer)) != -1) {
                                fos.write(buffer, 0, count);
                            }
                        }
                    }
                    zis.closeEntry();
                }
                zis.close();

                runOnUiThread(() -> Toast.makeText(this, "JDK Ready at: " + outputDir.getAbsolutePath(), Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                Log.e("JDK_ERROR", "Extraction failed", e);
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
