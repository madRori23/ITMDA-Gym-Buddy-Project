package com.example.gymbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MyRequests extends AppCompatActivity {

    private static final String TAG = "MyRequests";

    private RecyclerView recyclerViewRequests;
    private BookingRequestAdapter adapter;
    private LinearLayout emptyState;
    private List<BookingRequest> requestList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);
        Log.d(TAG, "MyRequests Activity Created");

        try {
            // Initialize views
            recyclerViewRequests = findViewById(R.id.recyclerViewRequests);
            emptyState = findViewById(R.id.emptyState);

            // Initialize Firestore
            db = FirebaseFirestore.getInstance();
            requestList = new ArrayList<>();

            // Setup RecyclerView with proper configuration
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            recyclerViewRequests.setLayoutManager(layoutManager);
            recyclerViewRequests.setHasFixedSize(true); // Improve performance

            adapter = new BookingRequestAdapter(this, requestList);
            recyclerViewRequests.setAdapter(adapter);

            Log.d(TAG, "RecyclerView setup complete");

            // Load booking requests from Firestore
            loadBookingRequests();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            updateEmptyState(true);
        }
    }

    private void loadBookingRequests() {
        try {
            String currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
            Log.d(TAG, "Loading booking requests for user: " + currentUserId);

            db.collection("booking_requests")
                    .whereEqualTo("userId", currentUserId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<BookingRequest> loadedRequests = new ArrayList<>();
                            int count = 0;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    BookingRequest request = document.toObject(BookingRequest.class);
                                    request.setId(document.getId());
                                    loadedRequests.add(request);
                                    count++;
                                    Log.d(TAG, "Loaded request: " + request.getTrainerName() + " - " + request.getStatus());
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing document: " + e.getMessage());
                                }
                            }
                            Log.d(TAG, "Successfully loaded " + count + " booking requests");

                            runOnUiThread(() -> {
                                requestList.clear();
                                requestList.addAll(loadedRequests);
                                adapter.updateData(loadedRequests);
                                updateEmptyState(requestList.isEmpty());
                                Log.d(TAG, "Adapter updated with " + requestList.size() + " items");
                            });
                        } else {
                            Log.e(TAG, "Error loading booking requests: " + task.getException());
                            runOnUiThread(() -> updateEmptyState(true));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to load booking requests: " + e.getMessage());
                        updateEmptyState(true);
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error in loadBookingRequests: " + e.getMessage());
            updateEmptyState(true);
        }
    }

    private void updateEmptyState(boolean isEmpty) {
        runOnUiThread(() -> {
            try {
                if (isEmpty) {
                    emptyState.setVisibility(View.VISIBLE);
                    recyclerViewRequests.setVisibility(View.GONE);
                    Log.d(TAG, "Showing empty state - no booking requests");
                } else {
                    emptyState.setVisibility(View.GONE);
                    recyclerViewRequests.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Showing RecyclerView with " + requestList.size() + " requests");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating empty state: " + e.getMessage());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MyRequests Activity Resumed");
        loadBookingRequests();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MyRequests Activity Destroyed");
        // Clean up to prevent memory leaks
        if (adapter != null) {
            // Clear the adapter data
            requestList.clear();
        }
    }

    public void onBackPressedDispatcher() {
        // Navigate back to NonPassHolder
        Intent intent = new Intent(MyRequests.this, NonPassHolder.class);
        startActivity(intent);
        finish();
    }
}