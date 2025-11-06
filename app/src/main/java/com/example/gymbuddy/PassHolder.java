package com.example.gymbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class PassHolder extends AppCompatActivity {

    // UI components, adapter and list for displaying schedules
    RecyclerView recyclerSchedules;
    ScheduleAdapter adapter;
    List<PassHolderSchedule> scheduleList;
    Button btnViewSchedules;
    Button btnCreateSchedule;
    Button logoutButton;

    // firebase instances
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.passholder_activity);

        // Firebase setup
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        btnCreateSchedule = findViewById(R.id.btnCreateSchedule);
        btnViewSchedules = findViewById(R.id.btnViewSchedules);
        logoutButton = findViewById(R.id.logoutButton);
        recyclerSchedules = findViewById(R.id.recyclerSchedules);

        // RecyclerView setup
        recyclerSchedules.setLayoutManager(new LinearLayoutManager(this));
        scheduleList = new ArrayList<>();
        adapter = new ScheduleAdapter(this, scheduleList);
        recyclerSchedules.setAdapter(adapter);

        // Button click listeners
        btnCreateSchedule.setOnClickListener(v -> startActivity(new Intent(PassHolder.this, Schedule.class))); // navigate to schedule creation page

        btnViewSchedules.setOnClickListener(v -> startActivity(new Intent(PassHolder.this, ViewSchedule.class))); //navigate to the schedule editing page

        // logout and redirect to the landing page
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(PassHolder.this, LandingPage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // load schedules for the database in real-time
        loadSchedulesFromFirestore();

        // Handle back button
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(PassHolder.this, LandingPage.class);
                startActivity(intent);
                finish();
            }
        });
    }

    // loads data and updates RecyclerView in real-time automatically
    private void loadSchedulesFromFirestore() {
        db.collection("schedules")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        //handles database errors
                        if (error != null) {
                            Toast.makeText(PassHolder.this, "Failed to load schedules: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        scheduleList.clear();
                        if (value != null) {
                            for (var document : value.getDocuments()) {
                                PassHolderSchedule schedule = document.toObject(PassHolderSchedule.class);
                                if (schedule != null) {
                                    schedule.setId(document.getId()); // store firestore document id
                                    scheduleList.add(schedule); // adds schedules to the list
                                }
                            }
                        }

                        // update adapter and refreshes RecyclerView
                        adapter.notifyDataSetChanged();

                        // no schedules are found
                        if (scheduleList.isEmpty()) {
                            Toast.makeText(PassHolder.this, "No schedules available.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
