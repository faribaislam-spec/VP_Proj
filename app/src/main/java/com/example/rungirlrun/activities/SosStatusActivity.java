package com.example.rungirlrun.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rungirlrun.R;
import com.example.rungirlrun.adapters.PoliceStationAdapter;
import com.example.rungirlrun.models.PoliceStation;
import com.example.rungirlrun.utils.PoliceStationFinder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SosStatusActivity extends AppCompatActivity {

    public static final String EXTRA_SESSION_ID = "session_id";
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";

    private static final String TAG = "SosStatusActivity";

    private String sessionId;
    private final List<PoliceStation> stations = new ArrayList<>();
    private PoliceStationAdapter adapter;
    private List<Map<String, Object>> stationMaps = new ArrayList<>();
    private ListenerRegistration sessionListener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos_status);

        sessionId = getIntent().getStringExtra(EXTRA_SESSION_ID);
        double latitude = getIntent().getDoubleExtra(EXTRA_LATITUDE, 0);
        double longitude = getIntent().getDoubleExtra(EXTRA_LONGITUDE, 0);

        if (sessionId == null) {
            Toast.makeText(this, "Missing SOS session.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        RecyclerView recyclerView = findViewById(R.id.stationsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PoliceStationAdapter(stations);
        recyclerView.setAdapter(adapter);

        Button imSafeButton = findViewById(R.id.imSafeButton);
        imSafeButton.setOnClickListener(v -> markSafeAndFinish());

        listenToSession();
        fetchNearbyStations(latitude, longitude);
    }

    private void fetchNearbyStations(double latitude, double longitude) {
        new PoliceStationFinder().findNearby(latitude, longitude, new PoliceStationFinder.Callback() {
            @Override
            public void onResult(List<PoliceStation> found) {
                if (found.isEmpty()) {
                    Toast.makeText(SosStatusActivity.this,
                            "No nearby police stations found in OSM data.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                writeStationsToSession(found);
                simulateNotifications(found);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to fetch stations", e);
                Toast.makeText(SosStatusActivity.this,
                        "Couldn't reach station data \u2014 check your connection.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void writeStationsToSession(List<PoliceStation> found) {

        stationMaps = new ArrayList<>();

        for (PoliceStation station : found) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", station.getName());
            map.put("latitude", station.getLatitude());
            map.put("longitude", station.getLongitude());
            map.put("distanceMeters", station.getDistanceMeters());
            map.put("status", "pending");
            stationMaps.add(map);
        }

        FirebaseFirestore.getInstance()
                .collection("liveTracking")
                .document(sessionId)
                .update("nearbyStations", stationMaps)
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to write nearbyStations", e));
    }

    private void simulateNotifications(List<PoliceStation> found) {

        // Stagger each station's status flip from "pending" to "notified"
        // so it reads as messages going out one by one.
        for (int i = 0; i < found.size(); i++) {

            int index = i;
            long delay = 900L * (i + 1);

            handler.postDelayed(() -> updateStationStatus(index, "notified"), delay);
        }
    }

    private void updateStationStatus(int index, String status) {

        if (index < 0 || index >= stationMaps.size()) {
            return;
        }

        stationMaps.get(index).put("status", status);

        FirebaseFirestore.getInstance()
                .collection("liveTracking")
                .document(sessionId)
                .update("nearbyStations", stationMaps)
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to update station status", e));
    }

    @SuppressWarnings("unchecked")
    private void listenToSession() {

        sessionListener = FirebaseFirestore.getInstance()
                .collection("liveTracking")
                .document(sessionId)
                .addSnapshotListener((DocumentSnapshot snapshot, com.google.firebase.firestore.FirebaseFirestoreException error) -> {

                    if (error != null) {
                        Log.e(TAG, "Session listener error", error);
                        return;
                    }

                    if (snapshot == null || !snapshot.exists()) {
                        return;
                    }

                    List<Map<String, Object>> rawStations =
                            (List<Map<String, Object>>) snapshot.get("nearbyStations");

                    if (rawStations == null) {
                        return;
                    }

                    List<PoliceStation> updated = new ArrayList<>();

                    for (Map<String, Object> raw : rawStations) {

                        PoliceStation station = new PoliceStation(
                                (String) raw.get("name"),
                                (Double) raw.get("latitude"),
                                (Double) raw.get("longitude"),
                                ((Number) raw.get("distanceMeters")).floatValue()
                        );

                        station.setStatus((String) raw.get("status"));
                        updated.add(station);
                    }

                    adapter.updateStations(updated);
                });
    }

    private void markSafeAndFinish() {

        FirebaseFirestore.getInstance()
                .collection("liveTracking")
                .document(sessionId)
                .update("active", false)
                .addOnCompleteListener(task -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sessionListener != null) {
            sessionListener.remove();
        }
        handler.removeCallbacksAndMessages(null);
    }
}