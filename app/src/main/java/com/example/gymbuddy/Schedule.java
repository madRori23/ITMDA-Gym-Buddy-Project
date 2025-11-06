package com.example.gymbuddy;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Schedule extends AppCompatActivity {

    private EditText etDate, etTime;
    private Spinner spinnerLocation, spinnerActivityType;
    private Button btnAddSchedule, btnCancel;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        calendar = Calendar.getInstance();

        initializeViews();
        setupSpinners();
        setupClickListeners();
    }

    private void initializeViews() {
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        spinnerLocation = findViewById(R.id.spinnerLocation);
        spinnerActivityType = findViewById(R.id.spinnerActivityType);
        btnAddSchedule = findViewById(R.id.btnAddSchedule);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void setupSpinners() {
        // Location options
        String[] locations = {"Select Location", "Centurion Gate", "Pretoria Central", "Johannesburg North", "Sandton City", "Rosebank"};
        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locations);
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocation.setAdapter(locationAdapter);

        // Activity type options
        String[] activityTypes = {"Select Activity", "Weight Training", "Cardio", "Yoga", "CrossFit", "Swimming", "Boxing", "Martial Arts"};
        ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, activityTypes);
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerActivityType.setAdapter(activityAdapter);
    }

    private void setupClickListeners() {
        // Date picker
        etDate.setOnClickListener(v -> showDatePicker());

        // Time picker
        etTime.setOnClickListener(v -> showTimePicker());

        // Cancel button
        btnCancel.setOnClickListener(v -> finish());

        // Add to Schedule button
        btnAddSchedule.setOnClickListener(v -> createSchedule());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
                    etDate.setText(dateFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);

                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    etTime.setText(timeFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true // 24-hour format
        );
        timePickerDialog.show();
    }

    private void createSchedule() {
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String location = spinnerLocation.getSelectedItem().toString();
        String activityType = spinnerActivityType.getSelectedItem().toString();

        // Validation
        if (date.isEmpty() || date.equals("yyyy/mm/dd")) {
            showError("Please select a date");
            return;
        }

        if (time.isEmpty() || time.equals("--:--")) {
            showError("Please select a time");
            return;
        }

        if (location.equals("Select Location")) {
            showError("Please select a location");
            return;
        }

        if (activityType.equals("Select Activity")) {
            showError("Please select an activity type");
            return;
        }

        // Check if user is logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            showError("Please log in to create a schedule");
            return;
        }

        // Create schedule object
        Map<String, Object> schedule = new HashMap<>();
        schedule.put("userId", currentUser.getUid());
        schedule.put("userEmail", currentUser.getEmail());
        schedule.put("date", date);
        schedule.put("time", time);
        schedule.put("location", location);
        schedule.put("activityType", activityType);
        schedule.put("createdAt", System.currentTimeMillis());
        schedule.put("participants", 1); // Creator is the first participant
        schedule.put("maxParticipants", 10); // Default max participants

        // Show loading
        btnAddSchedule.setEnabled(false);
        btnAddSchedule.setText("Creating...");

        // Save to Firestore
        db.collection("schedules")
                .add(schedule)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(Schedule.this,
                            "Schedule created successfully!", Toast.LENGTH_SHORT).show();

                    // Navigate back to main activity or schedules list
                    Intent intent = new Intent(Schedule.this, PassHolder.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnAddSchedule.setEnabled(true);
                    btnAddSchedule.setText("Add to Schedule");
                    showError("Failed to create schedule: " + e.getMessage());
                });
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


    public void onBackPressedDispatcher() {
        super.onBackPressed();
        // Navigate back to PassHolder
        Intent intent = new Intent(Schedule.this, PassHolder.class);
        startActivity(intent);
        finish();
    }
}
