package br.pucminas.castro.boxbox;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SplashActivity extends AppCompatActivity {

    static {
        if (OpenCVLoader.initDebug()) {
            Log.i("SPLASH-OPENCV: ", "OpenCV initialize success");
        } else {
            Log.i("SPLASH-OPENCV: ", "OpenCV initialize failed");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        if(checkAllPermissions()){
            File folder = new File(Environment.getExternalStorageDirectory() + File.separator + "BoxBox");
            boolean success = true;
            if (!folder.exists())
                success = folder.mkdirs();
            if(success)
                Log.d("SPLASH", "Folder Created");
            else
                Log.d("SPLASH", "Folder not Created!");

            startBox();
        }
    }

    private void startBox() {
        Intent intent = new Intent(this,MainActivity.class);
        startActivity(intent);
        this.finish();
    }

    private boolean checkAllPermissions(){
        int camera = ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA);
        int readStorage = ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE);
        int writeStorage = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE);
        List<String> neededPermissions = new ArrayList<>();
        if(camera != PackageManager.PERMISSION_GRANTED)
            neededPermissions.add(Manifest.permission.CAMERA);
        if(readStorage != PackageManager.PERMISSION_GRANTED)
            neededPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        if(writeStorage != PackageManager.PERMISSION_GRANTED)
            neededPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if(!neededPermissions.isEmpty()){
            ActivityCompat.requestPermissions(this, neededPermissions.toArray(new String[neededPermissions.size()]),1);
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 1){
            for(int i = 0; i < grantResults.length; i++){
                if(grantResults[i] != PackageManager.PERMISSION_GRANTED)
                    checkAllPermissions();
            }
            startBox();
        }
    }
}
