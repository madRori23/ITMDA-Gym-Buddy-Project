package com.example.gymbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NonPassHolder extends AppCompatActivity {

    private ImageButton searchIcon;
    private SearchView searchBar;
    private Button pendingRequestsButton;
    private RecyclerView schedulesRecyclerView;
    private View emptyState;
    private TextView lastUpdatedText;
    private Button logoutButton;
    private TextView welcomeText, userRole;

    private ScheduleListAdapter scheduleAdapter;
    private List<ScheduleModel> scheduleList;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration scheduleListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_non_pass_holder);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        setupBackPressedHandler();
        loadUserData();
        //listenForSchedules(); // Use real-time listener from your new code
        loadSchedulesFromFirestore();
    }

    private void setupBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToLandingPage();
            }
        });
    }

    private void navigateToLandingPage() {
        Intent intent = new Intent(NonPassHolder.this, LandingPage.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void initializeViews() {
        searchIcon = findViewById(R.id.searchIcon);
        searchBar = findViewById(R.id.searchBar);
        pendingRequestsButton = findViewById(R.id.pendingRequestsButton);
        schedulesRecyclerView = findViewById(R.id.schedulesRecyclerView);
        emptyState = findViewById(R.id.emptyState);
        lastUpdatedText = findViewById(R.id.lastUpdatedText);
        logoutButton = findViewById(R.id.logoutButton);
        welcomeText = findViewById(R.id.welcomeText);
        userRole = findViewById(R.id.userRole);

        // Initially hide search bar
        if (searchBar != null) {
            searchBar.setVisibility(View.GONE);
        }
    }

    private void setupRecyclerView() {
        scheduleList = new ArrayList<>();
        scheduleAdapter = new ScheduleListAdapter(scheduleList, new ScheduleListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ScheduleModel schedule) {
                requestToJoinSession(schedule);
            }
        });

        schedulesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        schedulesRecyclerView.setAdapter(scheduleAdapter);

        Log.d("NonPassHolder", "RecyclerView setup complete - adapter: " + scheduleAdapter);
    }

    private void setupClickListeners() {
        // Search icon click - toggle search bar visibility
        if (searchIcon != null) {
            searchIcon.setOnClickListener(v -> toggleSearchBar());
        }

        // Search functionality
        if (searchBar != null) {
            searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    filterSchedules(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    filterSchedules(newText);
                    return true;
                }
            });
        }

        // Pending Requests button click
        if (pendingRequestsButton != null) {
            pendingRequestsButton.setOnClickListener(v -> {
                Intent intent = new Intent(NonPassHolder.this, MyRequests.class);
                startActivity(intent);
            });
        }

        // Logout button
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> logoutUser());
        }
    }

    private void toggleSearchBar() {
        if (searchBar != null) {
            if (searchBar.getVisibility() == View.VISIBLE) {
                // Hide search bar
                searchBar.setVisibility(View.GONE);
                searchBar.setQuery("", false);
                searchBar.clearFocus();
                filterSchedules("");
            } else {
                // Show search bar
                searchBar.setVisibility(View.VISIBLE);
                searchBar.setIconified(false);
                searchBar.requestFocus();
            }
        }
    }

    private void filterSchedules(String query) {
        if (scheduleAdapter != null) {
            scheduleAdapter.getFilter().filter(query);
        }
    }


    private void loadSchedulesFromFirestore() {
        // Get current date in the correct format for comparison
        String currentDate = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(new Date());
        Log.d("NonPassHolder", "Current date: " + currentDate);

        db.collection("schedules")
                .whereGreaterThanOrEqualTo("date", currentDate)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        scheduleList.clear();
                        Log.d("NonPassHolder", "Found " + task.getResult().size() + " documents");

                        for (var document : task.getResult().getDocuments()) {
                            Log.d("NonPassHolder", "Document ID: " + document.getId());
                            Log.d("NonPassHolder", "Document data: " + document.getData());

                            ScheduleModel schedule = document.toObject(ScheduleModel.class);
                            if (schedule != null) {
                                schedule.setId(document.getId());
                                scheduleList.add(schedule);
                                Log.d("NonPassHolder", "Added schedule: " + schedule.getActivityType());
                            } else {
                                Log.e("NonPassHolder", "Failed to convert document to ScheduleModel");
                            }
                        }

                        Log.d("NonPassHolder", "Total schedules loaded: " + scheduleList.size());
                        updateUI();
                    } else {
                        Log.e("NonPassHolder", "Error loading schedules: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                        showEmptyState();
                        Toast.makeText(this, "Failed to load schedules", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void updateUI() {
        Log.d("NonPassHolder", "updateUI called - " + scheduleList.size() + " schedules");

        if (scheduleList.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
            // Force the adapter to update with the actual list
            if (scheduleAdapter != null) {
                // Create a new list to avoid reference issues
                List<ScheduleModel> schedulesToShow = new ArrayList<>(scheduleList);
                scheduleAdapter.updateData(schedulesToShow);
                Log.d("NonPassHolder", "Adapter updated with " + schedulesToShow.size() + " items");
            }
        }
        updateLastUpdatedTime();
    }



    private void showEmptyState() {
        Log.d("NonPassHolder", "Showing empty state");
        if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
        if (schedulesRecyclerView != null) schedulesRecyclerView.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        Log.d("NonPassHolder", "Hiding empty state, showing RecyclerView");
        if (emptyState != null) emptyState.setVisibility(View.GONE);
        if (schedulesRecyclerView != null) schedulesRecyclerView.setVisibility(View.VISIBLE);
        Log.d("NonPassHolder", "RecyclerView visibility: " + schedulesRecyclerView.getVisibility());
    }

    private void updateLastUpdatedTime() {
        if (lastUpdatedText != null) {
            String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            lastUpdatedText.setText("Last updated: " + currentTime);
        }
    }

    private void requestToJoinSession(ScheduleModel schedule) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to join sessions", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if session is full
        if (schedule.getParticipants() >= schedule.getMaxParticipants()) {
            Toast.makeText(this, "This session is already full", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if user already has a pending request for this schedule
        db.collection("booking_requests")
                .whereEqualTo("userId", currentUser.getUid())
                .whereEqualTo("scheduleId", schedule.getId())
                .whereEqualTo("status", "pending")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        Toast.makeText(this, "You already have a pending request for this session", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Create booking request in Firestore
                    db.collection("booking_requests")
                            .add(createBookingRequest(currentUser, schedule))
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(this,
                                        "Request sent to join " + schedule.getTrainerName() + "'s session",
                                        Toast.LENGTH_SHORT).show();
                                //Create notification to send to trainer
                                Log.d("Request sent to join", "Calling the send message function" );
                                sendRequest(currentUser, schedule);

                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to send request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private java.util.Map<String, Object> createBookingRequest(FirebaseUser user, ScheduleModel schedule) {
        java.util.Map<String, Object> request = new java.util.HashMap<>();
        request.put("userId", user.getUid());
        request.put("userEmail", user.getEmail());
        request.put("scheduleId", schedule.getId());
        request.put("scheduleTitle", schedule.getActivityType());
        request.put("trainerEmail", schedule.getUserEmail());
        request.put("trainerName", schedule.getTrainerName());
        request.put("scheduleDate", schedule.getDate());
        request.put("scheduleTime", schedule.getTime());
        request.put("scheduleLocation", schedule.getLocation());
        request.put("currentParticipants", schedule.getParticipants());
        request.put("maxParticipants", schedule.getMaxParticipants());
        request.put("status", "pending");
        request.put("requestedAt", new Date());
        return request;
    }

    //Send message to owner
    private void sendRequest(FirebaseUser user, ScheduleModel schedule){
        Log.d("Request Notification", "Sender ID: " + user.getUid());
        Log.d("Request Notification", "Trainer ID: " + schedule.getUserId());

        String trainerId = schedule.getUserId();

        //Find FCM token in user database
        db.collection("users")
                .document(trainerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d("Request Notification" , "Document found: " + documentSnapshot.exists());

                    if(documentSnapshot.exists()){
                        Log.d("Request Notification" , "Found token for user " + trainerId);
                        String fcmToken = documentSnapshot.getString("fcmToken");
                        Log.d("Request Notification" , "Token: " + fcmToken);

                        if (fcmToken != null) {
                            //Create notification
                            String title = "New request to join session";
                            String body = user.getEmail() + " has requested to join your " + schedule.getActivityType() + " session on " + schedule.getDate() + " at " + schedule.getTime();
                            Log.d("Request Notification", "Calling FCMApiService.sendMessage with title: " + title + ". Message: " + body );

                            //Send to trainer
                            FCMApiService.getInstance().sendMessage(trainerId, title, body, new FCMApiService.ApiCallBack() {
                                @Override
                                public void onSuccess(String response) {
                                    Log.d("Request Notification", "Message has been delivered to:" + user.getEmail());
                                }
                                @Override
                                public void onFailure(String error) {
                                    Log.e("Request Notification", "Request failed to send");
                                }
                            });
                        }else{
                            Log.e("Request Notification", "Failed to send notification, no token found");
                        }
                    }else{
                        Log.e("Request Notification", "Failed to find token for user " + trainerId + " in users collection");
                    }
                }).addOnFailureListener(e->{
                    Log.e("Request Notification", "Failed to find user. Message failed to send message. " + e.getMessage());
                });
    }


    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            if (welcomeText != null) {
                welcomeText.setText("Welcome back, " + currentUser.getEmail() + "!");
            }
            if (userRole != null) {
                userRole.setText("Non-Pass Holder");
            }

            // Optional: Load username from Firestore if available
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && documentSnapshot.contains("username")) {
                            String username = documentSnapshot.getString("username");
                            if (welcomeText != null) {
                                welcomeText.setText("Welcome back, " + username + "!");
                            }
                        }
                    });
        }
    }

    private void logoutUser() {
        mAuth.signOut();
        navigateToLandingPage();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Prevent memory leaks by removing Firestore listener
        if (scheduleListener != null) scheduleListener.remove();
    }
}