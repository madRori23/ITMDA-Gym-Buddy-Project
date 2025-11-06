package com.example.gymbuddy;

import java.util.List;

public class PassHolderSchedule {
    private String id;
    private String title;
    private String date;
    private String time;
    private String location;
    private List<ScheduleRequest> requests;

    public PassHolderSchedule() {
        // Default constructor required for Firestore
    }

    public PassHolderSchedule(String title, String date, String time, String location, List<ScheduleRequest> requests) {
        this.id = title + "_" + date;
        this.title = title;
        this.date = date;
        this.time = time;
        this.location = location;
        this.requests = requests;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getLocation() { return location; }
    public List<ScheduleRequest> getRequests() { return requests; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setLocation(String location) { this.location = location; }
    public void setRequests(List<ScheduleRequest> requests) { this.requests = requests; }
}
