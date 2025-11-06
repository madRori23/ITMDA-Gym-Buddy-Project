package com.example.gymbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Registration extends AppCompatActivity {

    private EditText etEmail, etPassword, etConfirmPassword, etAge, etContact, etUsername;
    private RadioGroup rgMembership, rgGender;
    private RadioButton rbBuddyPass, rbNoBuddyPass, rbMale, rbFemale, rbOther;
    private Button btnRegister;
    private TextView tvLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etAge = findViewById(R.id.etAge);
        etContact = findViewById(R.id.etContact);

        rgMembership = findViewById(R.id.rgMembership);
        rbBuddyPass = findViewById(R.id.rbBuddyPass);
        rbNoBuddyPass = findViewById(R.id.rbNoBuddyPass);

        rgGender = findViewById(R.id.rgGender);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);
        rbOther = findViewById(R.id.rbOther);

        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> attemptRegistration());
        tvLogin.setOnClickListener(v -> navigateToLogin());
    }

    private void attemptRegistration() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();
        String ageStr = etAge.getText().toString().trim();
        String contact = etContact.getText().toString().trim();

        // Validation
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() ||
                confirmPassword.isEmpty() || ageStr.isEmpty() || contact.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageStr);
            if (age < 13 || age > 120) {
                Toast.makeText(this, "Please enter a valid age (13-120)", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid age", Toast.LENGTH_SHORT).show();
            return;
        }

        String membership = getSelectedMembership();
        String gender = getSelectedGender();

        if (gender.equals("Unspecified")) {
            Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        btnRegister.setEnabled(false);
        btnRegister.setText("Registering...");

        // Create user in Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Registration success
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Save additional user data to Firestore
                            saveUserToFirestore(firebaseUser.getUid(), username, email, age, gender, membership, contact);
                        } else {
                            handleRegistrationFailure("User creation failed - please try again");
                        }
                    } else {
                        // Registration failed
                        handleRegistrationFailure("Registration failed: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    private void saveUserToFirestore(String userId, String username, String email, int age,
                                     String gender, String membership, String contact) {
        // Create user data map
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("email", email);
        user.put("age", age);
        user.put("gender", gender);
        user.put("membershipType", membership);
        user.put("contact", contact);
        user.put("createdAt", System.currentTimeMillis());
        user.put("updatedAt", System.currentTimeMillis());
        user.put("isActive", true);

        // Save to Firestore users collection
        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Registration", "User data saved successfully to Firestore");
                    Toast.makeText(Registration.this, "Registration successful!", Toast.LENGTH_SHORT).show();

                    // Navigate to appropriate main activity based on membership type
                    navigateToMainActivity(membership);
                })
                .addOnFailureListener(e -> {
                    Log.e("Registration", "Error saving user data to Firestore", e);

                    // Even if Firestore fails, we should still navigate since Auth succeeded
                    // But we'll delete the auth user to maintain consistency
                    FirebaseUser userAuth = mAuth.getCurrentUser();
                    if (userAuth != null) {
                        userAuth.delete().addOnCompleteListener(deleteTask -> {
                            if (deleteTask.isSuccessful()) {
                                Log.d("Registration", "Auth user deleted due to Firestore failure");
                            }
                        });
                    }

                    handleRegistrationFailure("Failed to save user data: " + e.getMessage());
                });
    }

    private String getSelectedMembership() {
        int selectedId = rgMembership.getCheckedRadioButtonId();
        if (selectedId == R.id.rbBuddyPass) {
            return "Buddy Pass";
        } else if (selectedId == R.id.rbNoBuddyPass) {
            return "No Buddy Pass";
        }
        return "No Buddy Pass"; // Default to No Buddy Pass
    }

    private String getSelectedGender() {
        int selectedId = rgGender.getCheckedRadioButtonId();
        if (selectedId == R.id.rbMale) return "Male";
        if (selectedId == R.id.rbFemale) return "Female";
        if (selectedId == R.id.rbOther) return "Other";
        return "Unspecified";
    }

    private void navigateToMainActivity(String membershipType) {
        Intent intent;
        if ("Buddy Pass".equals(membershipType)) {
            // Navigate to Pass Holder activity
            intent = new Intent(Registration.this, PassHolder.class);
            Toast.makeText(this, "Welcome! You can create workout sessions.", Toast.LENGTH_LONG).show();
        } else {
            // Navigate to Non-Pass Holder activity
            intent = new Intent(Registration.this, NonPassHolder.class);
            Toast.makeText(this, "Welcome! You can join workout sessions.", Toast.LENGTH_LONG).show();
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(Registration.this, Login.class);
        startActivity(intent);
        finish();
    }

    private void handleRegistrationFailure(String errorMessage) {
        btnRegister.setEnabled(true);
        btnRegister.setText("Register Account");
        Toast.makeText(Registration.this, errorMessage, Toast.LENGTH_LONG).show();
        Log.e("Registration", errorMessage);
    }

    public void onBackPressedDispatcher() {
        // Navigate back to LandingPage when back button is pressed
        Intent intent = new Intent(Registration.this, LandingPage.class);
        startActivity(intent);
        finish();
    }
}