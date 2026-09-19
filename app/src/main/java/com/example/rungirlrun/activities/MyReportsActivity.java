package com.example.rungirlrun.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rungirlrun.R;
import com.example.rungirlrun.adapters.ReportAdapter;
import com.example.rungirlrun.enums.ReportType;
import com.example.rungirlrun.models.SafetyReport;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shows all safety reports submitted by the currently logged-in user,
 * newest first. Pulls road names separately to display something
 * human-readable instead of raw roadId values.
 */
public class MyReportsActivity extends AppCompatActivity {

    private Button backButton;
    private TextView emptyStateText;
    private RecyclerView reportsRecyclerView;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reports);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        backButton = findViewById(R.id.backButton);
        emptyStateText = findViewById(R.id.emptyStateText);
        reportsRecyclerView = findViewById(R.id.reportsRecyclerView);

        reportsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        backButton.setOnClickListener(v -> finish());

        loadReports();
    }

    private void loadReports() {
        // First load road names so we can show "IUT Main Gate to Board Bazar"
        // instead of a raw document ID like "road_gate_boardbazar".
        db.collection("roads").get()
                .addOnSuccessListener(roadSnapshots -> {
                    Map<String, String> roadNamesById = new HashMap<>();
                    for (QueryDocumentSnapshot doc : roadSnapshots) {
                        String name = doc.getString("roadName");
                        roadNamesById.put(doc.getId(), name != null ? name : doc.getId());
                    }
                    loadUserReports(roadNamesById);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("MY_REPORTS", "Failed to load roads: ", e);
                    emptyStateText.setText("Couldn't load reports. Check your connection.");
                    emptyStateText.setVisibility(View.VISIBLE);
                });
    }

    private void loadUserReports(Map<String, String> roadNamesById) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId == null) {
            emptyStateText.setText("You need to be signed in to view your reports.");
            emptyStateText.setVisibility(View.VISIBLE);
            return;
        }

        db.collection("reports")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<SafetyReport> reports = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        SafetyReport report = parseReport(doc);
                        if (report != null) {
                            reports.add(report);
                        }
                    }

                    if (reports.isEmpty()) {
                        emptyStateText.setVisibility(View.VISIBLE);
                        reportsRecyclerView.setVisibility(View.GONE);
                    } else {
                        emptyStateText.setVisibility(View.GONE);
                        reportsRecyclerView.setVisibility(View.VISIBLE);
                        reportsRecyclerView.setAdapter(new ReportAdapter(reports, roadNamesById));
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("MY_REPORTS", "Failed to load reports: ", e);
                    emptyStateText.setText("Couldn't load reports. Check your connection.");
                    emptyStateText.setVisibility(View.VISIBLE);
                });
    }

    private SafetyReport parseReport(
            QueryDocumentSnapshot doc) {

        try {

            String reportId =
                    doc.getId();

            String roadId =
                    doc.getString("roadId");

            String userId =
                    doc.getString("userId");

            String reportTypeStr =
                    doc.getString("reportType");

            Boolean isNight =
                    doc.getBoolean("isNight");

            Long timestamp =
                    doc.getLong("timestamp");

            String locationName =
                    doc.getString("locationName");

            Double latitude =
                    doc.getDouble("latitude");

            Double longitude =
                    doc.getDouble("longitude");


            if (reportTypeStr == null
                    || isNight == null
                    || timestamp == null) {

                return null;
            }


            ReportType reportType =
                    ReportType.valueOf(
                            reportTypeStr
                    );


            return new SafetyReport(
                    reportId,
                    roadId,
                    userId,
                    reportType,
                    isNight,
                    timestamp,
                    locationName,
                    latitude,
                    longitude
            );


        } catch (Exception e) {

            e.printStackTrace();

            return null;
        }
    }
}