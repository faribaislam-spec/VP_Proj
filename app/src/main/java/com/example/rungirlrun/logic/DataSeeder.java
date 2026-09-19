package com.example.rungirlrun.logic;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

/**
 * ONE-TIME DATA SEEDING UTILITY.
 *
 * Writes a small, realistic road network around IUT (Board Bazar, Gazipur)
 * into Firestore, so GraphBuilder/Dijkstra/ReportIncidentActivity have
 * real-looking data to work with for a demo.
 *
 * Coordinates are approximate (general IUT/Board Bazar area, Dhaka-Mymensingh
 * Highway corridor) - close enough to look legitimate in a demo, but you may
 * want to fine-tune exact pins later by right-clicking the real spot on
 * Google Maps and copying its lat/lng.
 *
 * Layout is a small LOOP (not just a straight line), so Dijkstra actually
 * has two possible paths to compare weights on - useful for demonstrating
 * "safest route" logic later:
 *
 *   IUT Main Gate ──── Board Bazar Bus Stand
 *        │                      │
 *   IUT South Gate ──────── Kunia Bazar
 *
 * HOW TO USE:
 * Call DataSeeder.seedIUTAreaData(FirebaseFirestore.getInstance(), listener)
 * ONCE (e.g. from a temporary button, or directly in an Activity's onCreate
 * for a single run), then remove the call. Running it multiple times just
 * overwrites the same document IDs with the same data - it's safe to re-run,
 * it won't create duplicates.
 */
public class DataSeeder {

    public interface OnSeedCompleteListener {
        void onSuccess();
        void onError(Exception e);
    }

    public static void seedIUTAreaData(FirebaseFirestore db, OnSeedCompleteListener listener) {
        WriteBatch batch = db.batch();

        // ---- Locations (real-ish coordinates around IUT, Board Bazar, Gazipur) ----

        addLocation(db, batch, "loc_iut_main_gate", "IUT Main Gate", 23.9508, 90.3956);
        addLocation(db, batch, "loc_board_bazar", "Board Bazar Bus Stand", 23.9556, 90.4013);
        addLocation(db, batch, "loc_kunia_bazar", "Kunia Bazar", 23.9462, 90.3902);
        addLocation(db, batch, "loc_iut_south_gate", "IUT South Gate", 23.9498, 90.3944);

        // ---- Roads (forms a loop so Dijkstra has real route choices) ----

        addRoad(db, batch, "road_gate_boardbazar", "IUT Main Gate to Board Bazar",
                "loc_iut_main_gate", "loc_board_bazar", 1.0);

        addRoad(db, batch, "road_boardbazar_kunia", "Board Bazar to Kunia Bazar",
                "loc_board_bazar", "loc_kunia_bazar", 1.0);

        addRoad(db, batch, "road_gate_south", "IUT Main Gate to IUT South Gate",
                "loc_iut_main_gate", "loc_iut_south_gate", 1.0);

        addRoad(db, batch, "road_south_kunia", "IUT South Gate to Kunia Bazar",
                "loc_iut_south_gate", "loc_kunia_bazar", 1.0);

        batch.commit()
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    private static void addLocation(FirebaseFirestore db, WriteBatch batch,
                                    String id, String name, double lat, double lng) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("latitude", lat);
        data.put("longitude", lng);
        batch.set(db.collection("locations").document(id), data);
    }

    private static void addRoad(FirebaseFirestore db, WriteBatch batch,
                                String id, String roadName,
                                String startLocationId, String endLocationId,
                                double baseWeight) {
        Map<String, Object> data = new HashMap<>();
        data.put("roadName", roadName);
        data.put("startLocationId", startLocationId);
        data.put("endLocationId", endLocationId);
        data.put("baseWeight", baseWeight);
        data.put("dayWeight", baseWeight);
        data.put("nightWeight", baseWeight);
        batch.set(db.collection("roads").document(id), data);
    }
}