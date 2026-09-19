package com.example.rungirlrun.models;

public class Road {

    private String id;
    private String roadName;

    private Location start;
    private Location end;

    // Default safety value
    private double baseWeight;

    // Calculated weights
    private double dayWeight;
    private double nightWeight;

    public Road(String id, String roadName, Location start, Location end, double baseWeight) {
        this.id = id;
        this.roadName = roadName;
        this.start = start;
        this.end = end;
        this.baseWeight = baseWeight;

        // Initially both are equal to the base weight
        this.dayWeight = baseWeight;
        this.nightWeight = baseWeight;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getRoadName() {
        return roadName;
    }

    public Location getStart() {
        return start;
    }

    public Location getEnd() {
        return end;
    }

    public double getBaseWeight() {
        return baseWeight;
    }

    public double getDayWeight() {
        return dayWeight;
    }

    public double getNightWeight() {
        return nightWeight;
    }

    // Setters

    public void setRoadName(String roadName) {
        this.roadName = roadName;
    }

    public void setDayWeight(double dayWeight) {
        this.dayWeight = dayWeight;
    }

    public void setNightWeight(double nightWeight) {
        this.nightWeight = nightWeight;
    }
}