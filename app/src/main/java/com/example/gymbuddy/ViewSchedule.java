package com.example.gymbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ViewSchedule extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ViewScheduleAdapter adapter;
    private List<PassHolderSchedule> schedules;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_schedule);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.recyclerViewSchedules);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        schedules = new ArrayList<>();
        adapter = new ViewScheduleAdapter(this, schedules, new ViewScheduleAdapter.OnItemActionListener() {
            @Override
            public void onUpdate(PassHolderSchedule schedule) {
                Intent intent = new Intent(ViewSchedule.this, UpdateSchedule.class);
                intent.putExtra("scheduleId", schedule.getId());
                intent.putExtra("title", schedule.getTitle());
                intent.putExtra("date", schedule.getDate());
                intent.putExtra("time", schedule.getTime());
                intent.putExtra("location", schedule.getLocation());
                startActivityForResult(intent, 1);
            }

            @Override
            public void onDelete(PassHolderSchedule schedule) {
                deleteScheduleFromFirestore(schedule.getId());
            }
        });

        recyclerView.setAdapter(adapter);
        setupBackPressedHandler();
        loadSchedulesFromFirestore();
    }

    private void setupBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToPassHolder();
            }
        });
    }

    private void navigateToPassHolder() {
        Intent intent = new Intent(ViewSchedule.this, PassHolder.class);
        startActivity(intent);
        finish();
    }

    private void loadSchedulesFromFirestore() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to view your schedules", Toast.LENGTH_SHORT).show();
            navigateToPassHolder();
            return;
        }

        db.collection("schedules")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        schedules.clear();
                        for (var document : task.getResult()) {
                            ScheduleModel firestoreSchedule = document.toObject(ScheduleModel.class);

                            if (firestoreSchedule != null) {
                                PassHolderSchedule schedule = new PassHolderSchedule(
                                        firestoreSchedule.getActivityType(),
                                        firestoreSchedule.getDate(),
                                        firestoreSchedule.getTime(),
                                        firestoreSchedule.getLocation(),
                                        new ArrayList<>()
                                );
                                schedule.setId(document.getId());
                                schedules.add(schedule);
                            }
                        }

                        if (schedules.isEmpty()) {
                            Toast.makeText(this, "You haven't created any schedules yet", Toast.LENGTH_SHORT).show();
                        }

                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Failed to load schedules: " +
                                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteScheduleFromFirestore(String scheduleId) {
        db.collection("schedules").document(scheduleId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Remove from local list
                    for (int i = 0; i < schedules.size(); i++) {
                        if (schedules.get(i).getId().equals(scheduleId)) {
                            schedules.remove(i);
                            adapter.notifyItemRemoved(i);
                            break;
                        }
                    }
                    Toast.makeText(this, "Schedule deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete schedule: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            // Refresh the list after update
            loadSchedulesFromFirestore();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSchedulesFromFirestore();
    }
}