package com.example.rungirlrun.models;

public class PoliceStation {

    private String name;
    private double latitude;
    private double longitude;
    private float distanceMeters;
    private String status; // "pending" or "notified"

    public PoliceStation() {
        // required for Firestore deserialization
    }

    public PoliceStation(String name, double latitude, double longitude, float distanceMeters) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceMeters = distanceMeters;
        this.status = "pending";
    }

    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public float getDistanceMeters() { return distanceMeters; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}