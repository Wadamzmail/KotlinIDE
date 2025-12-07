package dev.mutwakil.kotlinide;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import dev.mutwakil.kotlinide.activity.LspTestJavaActivity;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED){
			startActivity(new Intent(this,LspTestJavaActivity.class));
			finish();
		}else{
			requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},50);
		}
    }
}