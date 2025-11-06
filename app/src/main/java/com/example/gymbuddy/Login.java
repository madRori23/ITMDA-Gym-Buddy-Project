package com.example.gymbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class Login extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupClickListeners();
        checkCurrentUser();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.etUsername); // Using username field for email
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        tvRegister.setOnClickListener(v -> navigateToRegistration());
    }

    private void checkCurrentUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            determineUserTypeAndNavigate(currentUser.getUid());
        }
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Login");

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Toast.makeText(Login.this, "Login successful!", Toast.LENGTH_SHORT).show();
                            determineUserTypeAndNavigate(user.getUid());
                            //Register FCM token on successful login.
                            Log.d("Login Token", "Generating a token for user " + user.getUid());
                            registerFCMToken(user);
                            determineUserTypeAndNavigate(user.getUid());
                        }
                    } else {
                        Toast.makeText(Login.this, "Authentication failed: " +
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void determineUserTypeAndNavigate(String userId) {
        Log.d("Login", "Checking user type for user: " + userId);

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Intent intent;
                    if (documentSnapshot.exists()) {
                        // Debug: Log all document data
                        Log.d("Login", "User document data: " + documentSnapshot.getData());

                        // Try both possible field names for membership type
                        String membershipType = documentSnapshot.getString("membershipType");
                        if (membershipType == null) {
                            membershipType = documentSnapshot.getString("membership");
                        }

                        Log.d("Login", "Membership type found: " + membershipType);

                        if ("Buddy Pass".equals(membershipType)) {
                            intent = new Intent(Login.this, PassHolder.class);
                            Toast.makeText(Login.this, "Welcome back! (Buddy Pass Holder)", Toast.LENGTH_SHORT).show();
                        } else {
                            intent = new Intent(Login.this, NonPassHolder.class);
                            Toast.makeText(Login.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d("Login", "User document does not exist in Firestore");
                        // Default to NonPassHolder if user data not found
                        intent = new Intent(Login.this, NonPassHolder.class);
                        Toast.makeText(Login.this, "Welcome! Default access granted.", Toast.LENGTH_SHORT).show();
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("Login", "Error fetching user data from Firestore", e);
                    // On failure, navigate to NonPassHolder as default
                    Intent intent = new Intent(Login.this, NonPassHolder.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                    Toast.makeText(Login.this, "Welcome! You can browse sessions.", Toast.LENGTH_SHORT).show();
                });
    }

    //Token registration code
    private void registerFCMToken(FirebaseUser user) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task->{
                    if(task.isSuccessful()){
                        //Check if token generated is empty
                        if(task.getResult()==null){
                            Log.e("Login Token", "FCM token is null, but task was successful");
                        }

                        //get token
                        String fcmToken = task.getResult();
                        Log.d("Login Token", "FCM token has been generated: " + fcmToken);

                        //Register and Store token with backend (Firestore)
                        FCMApiService.getInstance().registerDeviceWithFirestore(user.getUid(),
                                fcmToken,
                                new FCMApiService.ApiCallBack() {
                                    @Override
                                    public void onSuccess(String response) {
                                        //Console output
                                        Log.d("Login Token", "FCM token has successfully been registered");
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        Log.e("Login Token", "FCM token registration failed");
                                    }
                                });
                    }else{
                        Log.e("Login Token", "FCM token generation failed");
                    }
                });
    }

    private void navigateToRegistration() {
        Intent intent = new Intent(Login.this, Registration.class);
        startActivity(intent);
        finish();
    }

    public void onBackPressedDispatcher() {
        // Navigate back to LandingPage when back button is pressed
        Intent intent = new Intent(Login.this, LandingPage.class);
        startActivity(intent);
        finish();
    }
}