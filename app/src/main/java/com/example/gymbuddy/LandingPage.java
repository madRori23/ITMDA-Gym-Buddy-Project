package com.example.gymbuddy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class LandingPage extends AppCompatActivity {

    Button loginBtn, registerBtn;
    private boolean requestedPermissions = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_GymBuddy);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        loginBtn = findViewById(R.id.loginBtn);
        registerBtn = findViewById(R.id.registerBtn);

        // Login button - navigate to Login activity
        loginBtn.setOnClickListener(v -> {
            Intent intent = new Intent(LandingPage.this, Login.class);
            startActivity(intent);
        });

        // Register button - navigate to Registration activity
        registerBtn.setOnClickListener(v -> {
            Intent intent = new Intent(LandingPage.this, Registration.class);
            startActivity(intent);
        });
    }
    //Delayed call for permissions
    @Override
    protected void onResume() {
        super.onResume();
        if(!requestedPermissions){
            requestPermissions();
            requestedPermissions = true;
        }
    }


    //Requesting post notification permissions'
    private void requestPermissions() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            //1st-Check permissions status
            int status = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS);
            Log.d("PermissionsDEBUG", "Permissions Status: " + status + "(0=granted, -1=denied)");

            //2nd - Request permissions
            if (status == PackageManager.PERMISSION_GRANTED) {
                Log.d("PermissionsDEBUG", "Permissions already granted. Ignoring request, Proceeding with setup (Token Generation)");
            } else {
                Log.d("PermissionsDEBUG", "Permissions not granted. Requesting permissions");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        101);
            }
        }
    }
}