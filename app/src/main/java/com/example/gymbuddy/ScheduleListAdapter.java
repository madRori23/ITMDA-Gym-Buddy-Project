package com.example.gymbuddy;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ScheduleListAdapter extends RecyclerView.Adapter<ScheduleListAdapter.ViewHolder> implements Filterable {

    private final List<ScheduleModel> scheduleList;
    private List<ScheduleModel> filteredList;
    private final OnItemClickListener listener;


    public interface OnItemClickListener {
        void onItemClick(ScheduleModel schedule);
    }

    public ScheduleListAdapter(List<ScheduleModel> scheduleList, OnItemClickListener listener) {
        this.scheduleList = scheduleList;
        this.filteredList = new ArrayList<>(scheduleList);
        this.listener = listener;

    }

    @NonNull
    @Override
    public ScheduleListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleListAdapter.ViewHolder holder, int position) {
        ScheduleModel schedule = filteredList.get(position);

        // Map adapter data to your layout IDs - using actual Firestore field names
        holder.trainingType.setText(schedule.getActivityType());
        holder.location.setText(schedule.getLocation());
        holder.trainerName.setText(schedule.getTrainerName());
        holder.sessionDate.setText(schedule.getDate());
        holder.sessionTime.setText(schedule.getTime());

        // For availability/participants
        String availabilityText = schedule.getParticipants() + "/" + schedule.getMaxParticipants();
        holder.availability.setText(availabilityText);

        // Set join button text based on availability
        if (schedule.isAvailable()) {
            holder.joinButton.setText("Join");
            holder.joinButton.setEnabled(true);
        } else {
            holder.joinButton.setText("Full");
            holder.joinButton.setEnabled(false);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(schedule);
            } else {
                Toast.makeText(v.getContext(), "No click listener assigned", Toast.LENGTH_SHORT).show();
            }
        });

        // Join button click listener
        holder.joinButton.setOnClickListener(v -> {
            if (listener != null && schedule.isAvailable()) {
                listener.onItemClick(schedule);
            } else if (!schedule.isAvailable()) {
                Toast.makeText(v.getContext(), "This session is full", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<ScheduleModel> filteredResults = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    filteredResults.addAll(scheduleList);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();

                    for (ScheduleModel schedule : scheduleList) {
                        if (schedule.getActivityType().toLowerCase().contains(filterPattern)
                                || schedule.getLocation().toLowerCase().contains(filterPattern)
                                || schedule.getTrainerName().toLowerCase().contains(filterPattern)
                                || schedule.getDate().toLowerCase().contains(filterPattern)) {
                            filteredResults.add(schedule);
                        }
                    }
                }

                FilterResults results = new FilterResults();
                results.values = filteredResults;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredList.clear();

                if (results.values instanceof List) {
                    List<?> resultList = (List<?>) results.values;
                    for (Object item : resultList) {
                        if (item instanceof ScheduleModel) {
                            filteredList.add((ScheduleModel) item);
                        }
                    }
                }
                notifyDataSetChanged();
            }
        };
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView trainingType, availability, trainerName, sessionDate, sessionTime, location;
        android.widget.Button joinButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            trainingType = itemView.findViewById(R.id.trainingType);
            availability = itemView.findViewById(R.id.availability);
            trainerName = itemView.findViewById(R.id.trainerName);
            sessionDate = itemView.findViewById(R.id.sessionDate);
            sessionTime = itemView.findViewById(R.id.sessionTime);
            location = itemView.findViewById(R.id.location);
            joinButton = itemView.findViewById(R.id.joinButton);
        }
    }

    /*public void updateData(List<ScheduleModel> newScheduleList) {
        this.scheduleList.clear();
        this.scheduleList.addAll(newScheduleList);
        this.filteredList = new ArrayList<>(newScheduleList);
        notifyDataSetChanged();
    }*/
    public void updateData(List<ScheduleModel> newScheduleList) {
        Log.d("ScheduleAdapter", "updateData called with " + newScheduleList.size() + " items");

        this.scheduleList.clear();
        this.scheduleList.addAll(newScheduleList);
        this.filteredList.clear();
        this.filteredList.addAll(newScheduleList);

        Log.d("ScheduleAdapter", "After update - scheduleList: " + this.scheduleList.size() + ", filteredList: " + this.filteredList.size());
        notifyDataSetChanged();
    }
}