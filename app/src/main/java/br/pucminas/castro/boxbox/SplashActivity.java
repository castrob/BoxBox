package br.pucminas.castro.boxbox;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        if(checkAllPermissions()){
            startBox();
        }
    }

    private void startBox() {
        Intent intent = new Intent(this,MainActivity.class);
        startActivity(intent);
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