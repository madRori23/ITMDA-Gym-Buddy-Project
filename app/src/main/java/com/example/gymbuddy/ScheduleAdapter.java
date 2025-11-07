package com.example.gymbuddy;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private final Context context;
    private final List<PassHolderSchedule> scheduleList;
    private final FirebaseFirestore db;

    public ScheduleAdapter(Context context, List<PassHolderSchedule> scheduleList) {
        this.context = context;
        this.scheduleList = scheduleList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.card_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PassHolderSchedule schedule = scheduleList.get(position);
        String scheduleId = schedule.getId();

        holder.txtTitle.setText(schedule.getTitle());
        holder.txtDate.setText(schedule.getDate());
        holder.txtTime.setText(schedule.getTime());
        holder.txtLocation.setText(schedule.getLocation());

        // Clear previous requests and show loading state
        holder.requestContainer.removeAllViews();
        holder.requestContainer.setVisibility(View.GONE);

        // Load requests from booking_requests collection
        loadRequestsForSchedule(scheduleId, holder, position);

        // Cancel schedule button
        holder.btnCancelSchedule.setOnClickListener(v -> {
            db.collection("schedules").document(scheduleId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        scheduleList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, scheduleList.size());
                        Toast.makeText(context, schedule.getTitle() + " canceled", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to cancel schedule: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void loadRequestsForSchedule(String scheduleId, ViewHolder holder, int position) {
        db.collection("booking_requests")
                .whereEqualTo("scheduleId", scheduleId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("ScheduleAdapter", "Error loading requests: " + error.getMessage());
                        return;
                    }

                    // Clear previous requests
                    holder.requestContainer.removeAllViews();

                    if (value != null && !value.isEmpty()) {
                        holder.requestContainer.setVisibility(View.VISIBLE);
                        Log.d("ScheduleAdapter", "Found " + value.size() + " pending requests for schedule: " + scheduleId);

                        for (var document : value.getDocuments()) {
                            // Use the correct field names from your Firestore
                            String userEmail = document.getString("userEmail");
                            String userName = document.getString("trainerName"); // Using trainerName as display name
                            String userId = document.getString("userId");
                            String requestId = document.getId();

                            // Determine what to display as the name
                            String displayName;
                            if (userName != null && !userName.isEmpty() && !userName.equals("account1@gmail.com")) {
                                displayName = userName;
                            } else if (userEmail != null && !userEmail.isEmpty()) {
                                displayName = userEmail;
                            } else {
                                displayName = "Unknown User";
                            }

                            // Inflate and add request row
                            View requestRow = LayoutInflater.from(context).inflate(R.layout.request_row_layout, holder.requestContainer, false);
                            TextView requestName = requestRow.findViewById(R.id.requestName);
                            Button btnAccept = requestRow.findViewById(R.id.btnAccept);
                            Button btnDecline = requestRow.findViewById(R.id.btnDecline);

                            requestName.setText(displayName);

                            // Debug log
                            Log.d("ScheduleAdapter", "Displaying request: " + displayName + " (userEmail: " + userEmail + ", userName: " + userName + ")");

                            // Set button click listeners
                            btnAccept.setOnClickListener(v -> {
                                updateBookingRequestStatus(requestId, "accepted", displayName, holder, position);
                                //Response message
                                sendMessage(userEmail, "accepted", userName);
                                Toast.makeText(context, displayName + " accepted", Toast.LENGTH_SHORT).show();
                            });

                            btnDecline.setOnClickListener(v -> {
                                updateBookingRequestStatus(requestId, "declined", displayName, holder, position);
                                sendMessage(userEmail, "declined", userName);
                                Toast.makeText(context, displayName + " declined", Toast.LENGTH_SHORT).show();
                            });

                            holder.requestContainer.addView(requestRow);
                            Log.d("ScheduleAdapter", "Added request row for: " + displayName);
                        }
                    } else {
                        holder.requestContainer.setVisibility(View.GONE);
                        Log.d("ScheduleAdapter", "No pending requests found for schedule: " + scheduleId);
                    }
                });
    }

    private void updateBookingRequestStatus(String requestId, String status, String userName, ViewHolder holder, int position) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("respondedAt", System.currentTimeMillis());

        db.collection("booking_requests").document(requestId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("ScheduleAdapter", "Successfully updated request status to: " + status);

                    // Refresh the requests for this schedule
                    PassHolderSchedule schedule = scheduleList.get(position);
                    loadRequestsForSchedule(schedule.getId(), holder, position);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to update request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ScheduleAdapter", "Error updating request: " + e.getMessage());
                });
    }

    //send reponse message to trainer
    private void sendMessage(String targetUser, String status, String creatorId) {
        Log.d("ScheduleAdapter Message", "Sending message to: " + targetUser + " Status: " + status);

        //Find target user in database
        db.collection("users")
                .whereEqualTo("email", targetUser)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String targetUserId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        Log.d("ScheduleAdapter Message ", "Found user ID: " + targetUserId + " for email: " + targetUser);

                        //Send Notification using targetUserId (Backend checks for FCMToken)

                        //Create notification
                        String title = "Request to join session has been " + status;
                        String body =  creatorId +" has " + status + " your request to join their session";

                        FCMApiService.getInstance().sendMessage(targetUserId, title, body, new FCMApiService.ApiCallBack() {
                            @Override
                            public void onSuccess(String response) {
                                Log.d("ScheduleAdapter Message", "Message has been delivered to:" + targetUser);
                            }

                            @Override
                            public void onFailure(String error) {
                                Log.e("ScheduleAdapter Message", "Message failed to deliver to: " + targetUser + "Error: " + error);
                            }
                        });
                    }else{
                        Log.d("ScheduleAdapter Message", "No user id found with email: " + targetUser);
                    }
                }).addOnFailureListener(e ->{
                    Log.e("ScheduleAdapter Message", "Error searching for user ID" + e.getMessage());
                });
    }

    @Override
    public int getItemCount() {
        return scheduleList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtDate, txtTime, txtLocation;
        Button btnCancelSchedule;
        LinearLayout requestContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtLocation = itemView.findViewById(R.id.txtLocation);
            btnCancelSchedule = itemView.findViewById(R.id.btnCancelSchedule);
            requestContainer = itemView.findViewById(R.id.requestContainer);
        }
    }
}