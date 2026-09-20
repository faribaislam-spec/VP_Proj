package com.example.rungirlrun.logic;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;


//ONE-TIME DATA SEEDING UTILITY.

public class DataSeeder {

    public interface OnSeedCompleteListener {
        void onSuccess();
        void onError(Exception e);
    }

    public static void seedIUTAreaData(FirebaseFirestore db, OnSeedCompleteListener listener) {
        WriteBatch batch = db.batch();

        // ---- Locations (approximate coordinates around IUT, Board Bazar, Gazipur) ----

        addLocation(db, batch, "loc_iut_main_gate", "IUT Main Gate", 23.9508, 90.3956);
        addLocation(db, batch, "loc_board_bazar", "Board Bazar Bus Stand", 23.9556, 90.4013);
        addLocation(db, batch, "loc_kunia_bazar", "Kunia Bazar", 23.9462, 90.3902);
        addLocation(db, batch, "loc_iut_south_gate", "IUT South Gate", 23.9498, 90.3944);

        // ---- Roads (forms a loop so Dijkstra has real route choices) ----
        //for demo purposes and later safe route integration
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