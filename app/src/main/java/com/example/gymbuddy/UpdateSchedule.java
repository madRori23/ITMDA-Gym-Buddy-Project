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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class UpdateSchedule extends AppCompatActivity {

    private EditText etDate, etTime, etTitle;
    private Spinner spinnerLocation;
    private Button btnUpdate, btnCancel;
    private Calendar calendar;
    private int scheduleIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_schedule);

        initializeViews();
        setupSpinners();
        setupClickListeners();
        loadScheduleData();
    }

    private void initializeViews() {
        etTitle = findViewById(R.id.etTitle);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        spinnerLocation = findViewById(R.id.spinnerLocation);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnCancel = findViewById(R.id.btnCancel);

        calendar = Calendar.getInstance();
    }

    private void setupSpinners() {
        String[] locations = {"Select Location", "Centurion Gate", "Pretoria Central", "Johannesburg North", "Sandton City", "Rosebank"};
        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locations);
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocation.setAdapter(locationAdapter);
    }

    private void loadScheduleData() {
        Intent intent = getIntent();
        scheduleIndex = intent.getIntExtra("index", -1);
        etTitle.setText(intent.getStringExtra("title"));
        etDate.setText(intent.getStringExtra("date"));
        etTime.setText(intent.getStringExtra("time"));

        String location = intent.getStringExtra("location");
        if (location != null) {
            for (int i = 0; i < spinnerLocation.getCount(); i++) {
                if (spinnerLocation.getItemAtPosition(i).toString().equals(location)) {
                    spinnerLocation.setSelection(i);
                    break;
                }
            }
        }
    }

    private void setupClickListeners() {
        etDate.setOnClickListener(v -> showDatePicker());
        etTime.setOnClickListener(v -> showTimePicker());
        btnCancel.setOnClickListener(v -> finish());
        btnUpdate.setOnClickListener(v -> updateSchedule());
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
                true
        );
        timePickerDialog.show();
    }

    private void updateSchedule() {
        String title = etTitle.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String location = spinnerLocation.getSelectedItem().toString();

        if (title.isEmpty() || date.isEmpty() || time.isEmpty() || location.equals("Select Location")) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra("index", scheduleIndex);
        resultIntent.putExtra("title", title);
        resultIntent.putExtra("date", date);
        resultIntent.putExtra("time", time);
        resultIntent.putExtra("location", location);

        setResult(RESULT_OK, resultIntent);
        finish();

        Toast.makeText(this, "Schedule updated successfully", Toast.LENGTH_SHORT).show();
    }

    public void onBackPressedDispatcher() {
        super.onBackPressed();
        // Navigate back to ViewSchedule
        Intent intent = new Intent(UpdateSchedule.this, ViewSchedule.class);
        startActivity(intent);
        finish();
    }
}