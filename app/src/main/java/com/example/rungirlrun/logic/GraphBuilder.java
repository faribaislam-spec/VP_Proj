package com.example.rungirlrun.logic;

import com.example.rungirlrun.enums.ReportType;
import com.example.rungirlrun.models.Location;
import com.example.rungirlrun.models.Road;
import com.example.rungirlrun.models.SafetyReport;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fetches Locations, Roads, and SafetyReports from Firestore,
 * applies WeightCalculator to each Road, and builds a ready-to-use Graph.
 *
 * ASSUMED FIRESTORE SCHEMA (adjust field/collection names to match yours):
 *   locations/{docId}: name (String), latitude (Double), longitude (Double)
 *   roads/{docId}: roadName (String), startLocationId (String),
 *                  endLocationId (String), baseWeight (Double)
 *   reports/{docId}: roadId (String), userId (String), reportType (String,
 *                    matches ReportType enum name), isNight (Boolean),
 *                    timestamp (Long)
 */
public class GraphBuilder {

    private static final String LOCATIONS_COLLECTION = "locations";
    private static final String ROADS_COLLECTION = "roads";
    private static final String REPORTS_COLLECTION = "reports";

    public interface OnGraphReadyListener {
        void onGraphReady(Graph graph);
        void onError(Exception e);
    }

    public static void buildGraph(FirebaseFirestore db, OnGraphReadyListener listener) {
        Graph graph = new Graph();
        Map<String, Location> locationMap = new HashMap<>();

        db.collection(LOCATIONS_COLLECTION).get()
                .addOnSuccessListener(locationSnapshots -> {
                    for (DocumentSnapshot doc : locationSnapshots) {
                        Location location = parseLocation(doc);
                        if (location != null) {
                            locationMap.put(location.getId(), location);
                            graph.addLocation(location);
                        }
                    }
                    fetchRoads(db, graph, locationMap, listener);
                })
                .addOnFailureListener(listener::onError);
    }

    private static void fetchRoads(FirebaseFirestore db, Graph graph,
                                   Map<String, Location> locationMap,
                                   OnGraphReadyListener listener) {
        db.collection(ROADS_COLLECTION).get()
                .addOnSuccessListener(roadSnapshots -> {
                    List<Road> roads = new ArrayList<>();
                    for (DocumentSnapshot doc : roadSnapshots) {
                        Road road = parseRoad(doc, locationMap);
                        if (road != null) {
                            roads.add(road);
                        }
                    }
                    fetchReportsAndFinish(db, graph, roads, listener);
                })
                .addOnFailureListener(listener::onError);
    }

    private static void fetchReportsAndFinish(FirebaseFirestore db, Graph graph,
                                              List<Road> roads,
                                              OnGraphReadyListener listener) {
        db.collection(REPORTS_COLLECTION).get()
                .addOnSuccessListener(reportSnapshots -> {
                    List<SafetyReport> reports = new ArrayList<>();
                    for (DocumentSnapshot doc : reportSnapshots) {
                        SafetyReport report = parseReport(doc);
                        if (report != null) {
                            reports.add(report);
                        }
                    }

                    for (Road road : roads) {
                        WeightCalculator.calculateWeights(road, reports);
                        graph.addRoad(road);
                    }

                    listener.onGraphReady(graph);
                })
                .addOnFailureListener(listener::onError);
    }

    private static Location parseLocation(DocumentSnapshot doc) {
        try {
            String id = doc.getId();
            String name = doc.getString("name");
            Double lat = doc.getDouble("latitude");
            Double lng = doc.getDouble("longitude");
            if (lat == null || lng == null) {
                return null;
            }
            return new Location(id, name, lat, lng);
        } catch (Exception e) {
            return null; // skip malformed document rather than crash the whole fetch
        }
    }

    private static Road parseRoad(DocumentSnapshot doc, Map<String, Location> locationMap) {
        try {
            String id = doc.getId();
            String roadName = doc.getString("roadName");
            String startId = doc.getString("startLocationId");
            String endId = doc.getString("endLocationId");
            Double baseWeight = doc.getDouble("baseWeight");

            Location start = locationMap.get(startId);
            Location end = locationMap.get(endId);
            if (start == null || end == null || baseWeight == null) {
                return null; // referenced location missing, or bad data
            }

            return new Road(id, roadName, start, end, baseWeight);
        } catch (Exception e) {
            return null;
        }
    }

    private static SafetyReport parseReport(DocumentSnapshot doc) {
        try {
            String reportId = doc.getId();
            String roadId = doc.getString("roadId");
            String userId = doc.getString("userId");
            String reportTypeStr = doc.getString("reportType");
            Boolean isNight = doc.getBoolean("isNight");
            Long timestamp = doc.getLong("timestamp");

            if (roadId == null || reportTypeStr == null || isNight == null || timestamp == null) {
                return null;
            }

            ReportType reportType = ReportType.valueOf(reportTypeStr);
            return new SafetyReport(reportId, roadId, userId, reportType, isNight, timestamp);
        } catch (Exception e) {
            return null; // e.g. unknown reportType string, skip it
        }
    }
}