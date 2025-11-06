package com.example.gymbuddy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ViewScheduleAdapter extends RecyclerView.Adapter<ViewScheduleAdapter.ViewHolder> {

    private final Context context;
    private final List<PassHolderSchedule> schedules;
    private final OnItemActionListener listener;

    public interface OnItemActionListener {
        void onUpdate(PassHolderSchedule schedule);
        void onDelete(PassHolderSchedule schedule);
    }

    // Constructor
    public ViewScheduleAdapter(Context context, List<PassHolderSchedule> schedules, OnItemActionListener listener) {
        this.context = context;
        this.schedules = schedules;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_schedule_cards, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PassHolderSchedule schedule = schedules.get(position);

        // Bind data to TextViews
        holder.txtTitle.setText(schedule.getTitle());
        holder.txtDate.setText("Date: " + schedule.getDate());
        holder.txtTime.setText("Time: " + schedule.getTime());
        holder.txtLocation.setText("Location: " + schedule.getLocation());

        // Set click listeners for buttons
        holder.btnUpdate.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUpdate(schedule);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(schedule);
            }
        });
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtDate, txtTime, txtLocation;
        Button btnUpdate, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtScheduleTitle);
            txtDate = itemView.findViewById(R.id.txtScheduleDate);
            txtTime = itemView.findViewById(R.id.txtScheduleTime);
            txtLocation = itemView.findViewById(R.id.txtScheduleLocation);
            btnUpdate = itemView.findViewById(R.id.btnUpdate);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    // Method to update data
    public void updateData(List<PassHolderSchedule> newSchedules) {
        this.schedules.clear();
        this.schedules.addAll(newSchedules);
        notifyDataSetChanged();
    }

    // Method to remove a specific schedule
    public void removeSchedule(int position) {
        if (position >= 0 && position < schedules.size()) {
            schedules.remove(position);
            notifyItemRemoved(position);
        }
    }

    // Method to update a specific schedule
    public void updateSchedule(int position, PassHolderSchedule updatedSchedule) {
        if (position >= 0 && position < schedules.size()) {
            schedules.set(position, updatedSchedule);
            notifyItemChanged(position);
        }
    }
}