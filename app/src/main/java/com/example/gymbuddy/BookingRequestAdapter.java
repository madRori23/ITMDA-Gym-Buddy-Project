package com.example.gymbuddy;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingRequestAdapter extends RecyclerView.Adapter<BookingRequestAdapter.ViewHolder> {

    private final Context context;
    private final List<BookingRequest> requestList;

    public BookingRequestAdapter(Context context, List<BookingRequest> requestList) {
        this.context = context;
        this.requestList = new ArrayList<>(requestList);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            if (position < 0 || position >= requestList.size()) {
                Log.e("BookingRequestAdapter", "Invalid position: " + position);
                return;
            }

            BookingRequest request = requestList.get(position);
            if (request == null) {
                Log.e("BookingRequestAdapter", "Null request at position: " + position);
                return;
            }

            // Set basic information with null checks
            if (holder.txtTrainerName != null && request.getTrainerName() != null) {
                holder.txtTrainerName.setText(request.getTrainerName());
            }

            if (holder.txtDate != null && request.getScheduleDate() != null) {
                holder.txtDate.setText(request.getScheduleDate());
            }

            if (holder.txtTime != null && request.getScheduleTime() != null) {
                holder.txtTime.setText(request.getScheduleTime());
            }

            if (holder.txtLocation != null && request.getScheduleLocation() != null) {
                holder.txtLocation.setText(request.getScheduleLocation());
            }

            // Set status information
            String timeAgo = getTimeAgo(request.getRequestedAt());
            String statusText = getStatusText(request.getStatus(), timeAgo);
            String statusBadge = getStatusBadge(request.getStatus());

            if (holder.txtStatus != null) {
                holder.txtStatus.setText(statusText);
            }

            if (holder.txtStatusBadge != null) {
                holder.txtStatusBadge.setText(statusBadge);
            }

            // Set status colors and visibility
            if (request.getStatus() != null) {
                switch (request.getStatus().toLowerCase()) {
                    case "accepted":
                        if (holder.txtStatus != null) {
                            holder.txtStatus.setTextColor(context.getResources().getColor(android.R.color.holo_green_light));
                        }
                        if (holder.txtStatusBadge != null) {
                            holder.txtStatusBadge.setBackgroundColor(context.getResources().getColor(android.R.color.holo_green_dark));
                        }
                        if (holder.txtConfirmation != null) {
                            holder.txtConfirmation.setVisibility(View.VISIBLE);
                        }
                        break;
                    case "pending":
                        if (holder.txtStatus != null) {
                            holder.txtStatus.setTextColor(context.getResources().getColor(android.R.color.holo_orange_light));
                        }
                        if (holder.txtStatusBadge != null) {
                            holder.txtStatusBadge.setBackgroundColor(context.getResources().getColor(android.R.color.holo_orange_dark));
                        }
                        if (holder.txtConfirmation != null) {
                            holder.txtConfirmation.setVisibility(View.GONE);
                        }
                        break;
                    case "declined":
                        if (holder.txtStatus != null) {
                            holder.txtStatus.setTextColor(context.getResources().getColor(android.R.color.holo_red_light));
                        }
                        if (holder.txtStatusBadge != null) {
                            holder.txtStatusBadge.setBackgroundColor(context.getResources().getColor(android.R.color.holo_red_dark));
                        }
                        if (holder.txtConfirmation != null) {
                            holder.txtConfirmation.setVisibility(View.GONE);
                        }
                        break;
                }
            }

        } catch (Exception e) {
            Log.e("BookingRequestAdapter", "Error in onBindViewHolder: " + e.getMessage(), e);
        }
    }


    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtStatus, txtStatusBadge, txtTrainerName, txtDate, txtTime, txtLocation, txtConfirmation;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtStatusBadge = itemView.findViewById(R.id.txtStatusBadge);
            txtTrainerName = itemView.findViewById(R.id.txtTrainerName);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtLocation = itemView.findViewById(R.id.txtLocation);
            txtConfirmation = itemView.findViewById(R.id.txtConfirmation);
        }
    }

    public void updateData(List<BookingRequest> newRequestList) {
        this.requestList.clear();
        if (newRequestList != null) {
            this.requestList.addAll(newRequestList);
        }
        notifyDataSetChanged();
        Log.d("BookingRequestAdapter", "Data updated - " + this.requestList.size() + " items");
    }

    private String getStatusText(String status, String timeAgo) {
        switch (status.toLowerCase()) {
            case "accepted":
                return "✓ Accepted " + timeAgo;
            case "pending":
                return "⏳ Pending " + timeAgo;
            case "declined":
                return "❌ Declined " + timeAgo;
            default:
                return status + " " + timeAgo;
        }
    }

    private String getStatusBadge(String status) {
        switch (status.toLowerCase()) {
            case "accepted":
                return "Confirmed";
            case "pending":
                return "Pending";
            case "declined":
                return "Declined";
            default:
                return status;
        }
    }

    private String getTimeAgo(Date requestedAt) {
        if (requestedAt == null) return "";

        long timeDiff = System.currentTimeMillis() - requestedAt.getTime();
        long seconds = timeDiff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else {
            return "Just now";
        }
    }
}