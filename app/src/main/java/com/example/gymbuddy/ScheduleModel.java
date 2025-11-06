package com.example.gymbuddy;

public class ScheduleModel {
    private String activityType;
    private String id;
    private String location;
    private String userEmail;
    private String date;
    private String time;
    private int participants;
    private int maxParticipants;
    private String userId;
    private long createdAt;

    // Default constructor required for Firebase
    public ScheduleModel() {
    }

    public ScheduleModel(String activityType, String location, String userEmail,
                         String date, String time, int participants, int maxParticipants,
                         String userId) {
        this.activityType = activityType;
        this.location = location;
        this.userEmail = userEmail;
        this.date = date;
        this.time = time;
        this.participants = participants;
        this.maxParticipants = maxParticipants;
        this.userId = userId;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and setters for ALL fields
    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public int getParticipants() { return participants; }
    public void setParticipants(int participants) { this.participants = participants; }

    public int getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(int maxParticipants) { this.maxParticipants = maxParticipants; }

    public String getTrainerName() {
        // Use userEmail as trainer name if no specific trainer name field exists
        return userEmail != null ? userEmail : "Unknown Trainer";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isAvailable() {
        return participants < maxParticipants;
    }
}