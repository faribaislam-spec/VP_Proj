package com.example.rungirlrun.models;

import com.example.rungirlrun.enums.ReportType;

public class SafetyReport {

    private String reportId;
    private String roadId;
    private String userId;

    private ReportType reportType;

    // true = Night report, false = Day report
    private boolean isNight;

    private long timestamp;

    // Real-world location info
    private String locationName;
    private Double latitude;
    private Double longitude;

    public SafetyReport() {
        // Required for Firebase
    }

    // Old constructor kept for compatibility
    public SafetyReport(
            String reportId,
            String roadId,
            String userId,
            ReportType reportType,
            boolean isNight,
            long timestamp) {

        this.reportId = reportId;
        this.roadId = roadId;
        this.userId = userId;
        this.reportType = reportType;
        this.isNight = isNight;
        this.timestamp = timestamp;
    }

    // New constructor for real-world reports
    public SafetyReport(
            String reportId,
            String roadId,
            String userId,
            ReportType reportType,
            boolean isNight,
            long timestamp,
            String locationName,
            Double latitude,
            Double longitude) {

        this.reportId = reportId;
        this.roadId = roadId;
        this.userId = userId;
        this.reportType = reportType;
        this.isNight = isNight;
        this.timestamp = timestamp;

        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getReportId() {
        return reportId;
    }

    public String getRoadId() {
        return roadId;
    }

    public String getUserId() {
        return userId;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public boolean isNight() {
        return isNight;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getLocationName() {
        return locationName;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public void setRoadId(String roadId) {
        this.roadId = roadId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setReportType(ReportType reportType) {
        this.reportType = reportType;
    }

    public void setNight(boolean night) {
        isNight = night;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
}