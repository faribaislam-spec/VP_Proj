package com.example.rungirlrun.dao;

import com.example.rungirlrun.models.SafetyReport;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ReportDAO {

    private static final String REPORTS_COLLECTION = "reports";

    public interface OnReportSubmittedListener {
        void onSuccess();
        void onError(Exception e);
    }

    public static void submitReport(
            FirebaseFirestore db,
            SafetyReport report,
            OnReportSubmittedListener listener) {

        try {

            Map<String, Object> reportData =
                    new HashMap<>();

            reportData.put(
                    "roadId",
                    report.getRoadId()
            );

            reportData.put(
                    "userId",
                    report.getUserId()
            );

            reportData.put(
                    "reportType",
                    report.getReportType().name()
            );

            reportData.put(
                    "isNight",
                    report.isNight()
            );

            reportData.put(
                    "timestamp",
                    report.getTimestamp()
            );

            reportData.put(
                    "locationName",
                    report.getLocationName()
            );

            reportData.put(
                    "latitude",
                    report.getLatitude()
            );

            reportData.put(
                    "longitude",
                    report.getLongitude()
            );

            db.collection(REPORTS_COLLECTION)
                    .add(reportData)
                    .addOnSuccessListener(
                            documentReference ->
                                    listener.onSuccess()
                    )
                    .addOnFailureListener(
                            listener::onError
                    );

        } catch (Exception e) {
            listener.onError(e);
        }
    }
}