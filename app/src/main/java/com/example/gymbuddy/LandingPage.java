package com.example.gymbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class LandingPage extends AppCompatActivity {

    Button loginBtn, registerBtn;

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
}