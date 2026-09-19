package com.example.rungirlrun.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rungirlrun.R;
import com.example.rungirlrun.logic.DijkstraPathFinder;
import com.example.rungirlrun.logic.Graph;
import com.example.rungirlrun.logic.OSMRoadLoader;
import com.example.rungirlrun.models.Location;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import com.example.rungirlrun.logic.WeightCalculator;
import com.example.rungirlrun.models.Road;
import com.example.rungirlrun.models.SafetyReport;
import com.example.rungirlrun.enums.ReportType;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.rungirlrun.logic.TimeUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Comparator;
import android.widget.Button;

import java.util.Arrays;
import java.util.List;

public class SafeMapActivity extends AppCompatActivity {

    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance()
                .setUserAgentValue(
                        "RunGirlRun/1.0 (contact: afrinafroseee@gmail.com)"
                );

        setContentView(R.layout.activity_safe_map);
        Button backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish());


        // =====================================================
        // 1. RECEIVE SOURCE AND DESTINATION
        // =====================================================

        String sourceName =
                getIntent().getStringExtra("sourceName");

        double sourceLat =
                getIntent().getDoubleExtra("sourceLat", 0);

        double sourceLon =
                getIntent().getDoubleExtra("sourceLon", 0);


        String destinationName =
                getIntent().getStringExtra("destinationName");

        double destinationLat =
                getIntent().getDoubleExtra("destinationLat", 0);

        double destinationLon =
                getIntent().getDoubleExtra("destinationLon", 0);


        // =====================================================
        // 2. INITIALIZE MAP
        // =====================================================

        mapView =
                findViewById(R.id.mapView);

        mapView.setTileSource(
                new XYTileSource(
                        "OpenStreetMap",
                        0,
                        19,
                        256,
                        ".png",
                        new String[]{
                                "https://a.tile.openstreetmap.fr/osmfr/"
                        }
                )
        );

        mapView.setMultiTouchControls(true);


        // =====================================================
        // 3. CREATE SOURCE / DESTINATION GEOPOINTS
        // =====================================================

        GeoPoint sourcePoint =
                new GeoPoint(
                        sourceLat,
                        sourceLon
                );

        GeoPoint destinationPoint =
                new GeoPoint(
                        destinationLat,
                        destinationLon
                );


        // =====================================================
        // 4. SOURCE MARKER
        // =====================================================

        Marker sourceMarker =
                new Marker(mapView);

        sourceMarker.setPosition(
                sourcePoint
        );

        sourceMarker.setTitle(
                "Source: " + sourceName
        );

        sourceMarker.setAnchor(
                Marker.ANCHOR_CENTER,
                Marker.ANCHOR_BOTTOM
        );

        mapView.getOverlays()
                .add(sourceMarker);


        // =====================================================
        // 5. DESTINATION MARKER
        // =====================================================

        Marker destinationMarker =
                new Marker(mapView);

        destinationMarker.setPosition(
                destinationPoint
        );

        destinationMarker.setTitle(
                "Destination: " + destinationName
        );

        destinationMarker.setAnchor(
                Marker.ANCHOR_CENTER,
                Marker.ANCHOR_BOTTOM
        );

        mapView.getOverlays()
                .add(destinationMarker);


        // =====================================================
        // 6. FIT SOURCE + DESTINATION ON SCREEN
        // =====================================================

        BoundingBox boundingBox =
                BoundingBox.fromGeoPoints(
                        Arrays.asList(
                                sourcePoint,
                                destinationPoint
                        )
                );

        mapView.post(() ->
                mapView.zoomToBoundingBox(
                        boundingBox,
                        true,
                        120
                )
        );


        // =====================================================
        // 7. LOAD REAL OSM ROAD GRAPH
        // =====================================================

        Toast.makeText(
                this,
                "Loading road network...",
                Toast.LENGTH_SHORT
        ).show();


        OSMRoadLoader.loadGraph(
                sourceLat,
                sourceLon,
                destinationLat,
                destinationLon,

                new OSMRoadLoader.OnGraphLoadedListener() {

                    @Override
                    public void onSuccess(Graph graph) {

                        loadAndApplySafetyReports(
                                graph,
                                () -> {

                                    boolean isNight = TimeUtils.isNightTime();

                                    // Find several nearby OSM nodes for source
                                    List<Location> startCandidates =
                                            findNearestLocations(
                                                    graph,
                                                    sourceLat,
                                                    sourceLon,
                                                    10
                                            );

                                    // Find several nearby OSM nodes for destination
                                    List<Location> endCandidates =
                                            findNearestLocations(
                                                    graph,
                                                    destinationLat,
                                                    destinationLon,
                                                    10
                                            );


                                    DijkstraPathFinder.PathResult bestResult = null;

                                    double bestTotal =
                                            Double.MAX_VALUE;


                                    // Try different nearby source/destination node pairs
                                    for (Location startCandidate : startCandidates) {

                                        for (Location endCandidate : endCandidates) {

                                            DijkstraPathFinder.PathResult result =
                                                    DijkstraPathFinder.findShortestPath(
                                                            graph,
                                                            startCandidate.getId(),
                                                            endCandidate.getId(),
                                                            isNight
                                                    );


                                            if (!result.isPathFound()) {
                                                continue;
                                            }


                                            double sourceSnapDistance =
                                                    calculateDistanceMeters(
                                                            sourceLat,
                                                            sourceLon,
                                                            startCandidate.getLatitude(),
                                                            startCandidate.getLongitude()
                                                    );


                                            double destinationSnapDistance =
                                                    calculateDistanceMeters(
                                                            destinationLat,
                                                            destinationLon,
                                                            endCandidate.getLatitude(),
                                                            endCandidate.getLongitude()
                                                    );


                                            double total =
                                                    result.getTotalWeight()
                                                            + sourceSnapDistance
                                                            + destinationSnapDistance;


                                            if (total < bestTotal) {

                                                bestTotal =
                                                        total;

                                                bestResult =
                                                        result;
                                            }
                                        }
                                    }


                                    DijkstraPathFinder.PathResult finalResult =
                                            bestResult;


                                    runOnUiThread(() -> {

                                        if (finalResult == null
                                                || !finalResult.isPathFound()) {

                                            Toast.makeText(
                                                    SafeMapActivity.this,
                                                    "No connected road route found",
                                                    Toast.LENGTH_LONG
                                            ).show();

                                            return;
                                        }


                                        drawRoute(
                                                finalResult.getPath()
                                        );


                                        Toast.makeText(
                                                SafeMapActivity.this,
                                                "Safest route found",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    });
                                }
                        );
                    }


                    @Override
                    public void onError(Exception e) {

                        e.printStackTrace();

                        runOnUiThread(() ->
                                Toast.makeText(
                                        SafeMapActivity.this,
                                        "Could not load road network: "
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show()
                        );
                    }
                }
        );


        mapView.invalidate();
    }


    // =========================================================
    // FIND NEAREST GRAPH LOCATION TO A REAL GPS POINT
    // =========================================================

    private Location findNearestLocation(
            Graph graph,
            double latitude,
            double longitude) {

        Location nearest =
                null;

        double smallestDistance =
                Double.MAX_VALUE;


        for (Location location :
                graph.getAllLocations()) {


            double distance =
                    calculateDistanceMeters(
                            latitude,
                            longitude,
                            location.getLatitude(),
                            location.getLongitude()
                    );


            if (distance < smallestDistance) {

                smallestDistance =
                        distance;

                nearest =
                        location;
            }
        }


        return nearest;
    }
    private List<Location> findNearestLocations(
            Graph graph,
            double latitude,
            double longitude,
            int limit) {

        List<Location> locations =
                new ArrayList<>(
                        graph.getAllLocations()
                );

        locations.sort(
                Comparator.comparingDouble(
                        location ->
                                calculateDistanceMeters(
                                        latitude,
                                        longitude,
                                        location.getLatitude(),
                                        location.getLongitude()
                                )
                )
        );

        if (locations.size() > limit) {
            return new ArrayList<>(
                    locations.subList(0, limit)
            );
        }

        return locations;
    }
    private void loadAndApplySafetyReports(
            Graph graph,
            Runnable onFinished) {

        FirebaseFirestore db =
                FirebaseFirestore.getInstance();

        db.collection("reports")
                .get()
                .addOnSuccessListener(snapshot -> {

                    List<SafetyReport> reports =
                            new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snapshot) {

                        try {

                            String reportId =
                                    doc.getId();

                            String roadId =
                                    doc.getString("roadId");

                            String userId =
                                    doc.getString("userId");

                            String reportTypeString =
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


                            if (reportTypeString == null
                                    || isNight == null
                                    || timestamp == null) {

                                continue;
                            }


                            ReportType reportType =
                                    ReportType.valueOf(
                                            reportTypeString
                                    );


                            SafetyReport report =
                                    new SafetyReport(
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


                            /*
                             * New real-world reports have coordinates
                             * but usually no roadId yet.
                             */
                            if (report.hasCoordinates()) {

                                List<Road> nearbyRoads =
                                        findNearbyRoads(
                                                graph,
                                                report.getLatitude(),
                                                report.getLongitude(),
                                                200.0
                                        );

                                for (Road road : nearbyRoads) {

                                    SafetyReport matchedReport =
                                            new SafetyReport(
                                                    report.getReportId(),
                                                    road.getId(),
                                                    report.getUserId(),
                                                    report.getReportType(),
                                                    report.isNight(),
                                                    report.getTimestamp(),
                                                    report.getLocationName(),
                                                    report.getLatitude(),
                                                    report.getLongitude()
                                            );

                                    reports.add(matchedReport);
                                }

                            } else {

                                reports.add(report);
                            }

                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    }


                    // Apply report penalties to all roads
                    for (Road road : getAllRoads(graph)) {

                        WeightCalculator.calculateWeights(
                                road,
                                reports
                        );
                    }


                    onFinished.run();
                })

                .addOnFailureListener(e -> {

                    e.printStackTrace();

                    /*
                     * If Firestore fails, still allow routing
                     * using normal road distance.
                     */
                    onFinished.run();
                });
    }
    private List<Road> getAllRoads(Graph graph) {

        List<Road> roads =
                new ArrayList<>();

        Set<String> seenRoadIds =
                new HashSet<>();

        for (Location location :
                graph.getAllLocations()) {

            for (Road road :
                    graph.getConnectedRoads(
                            location.getId()
                    )) {

                if (!seenRoadIds.contains(
                        road.getId()
                )) {

                    seenRoadIds.add(
                            road.getId()
                    );

                    roads.add(
                            road
                    );
                }
            }
        }

        return roads;
    }
    private List<Road> findNearbyRoads(
            Graph graph,
            double latitude,
            double longitude,
            double radiusMeters) {

        List<Road> nearbyRoads =
                new ArrayList<>();

        for (Road road : getAllRoads(graph)) {

            double distance =
                    distancePointToRoadSegmentMeters(
                            latitude,
                            longitude,
                            road.getStart().getLatitude(),
                            road.getStart().getLongitude(),
                            road.getEnd().getLatitude(),
                            road.getEnd().getLongitude()
                    );

            if (distance <= radiusMeters) {

                nearbyRoads.add(road);
            }
        }

        return nearbyRoads;
    }
    private double distancePointToRoadSegmentMeters(
            double pointLat,
            double pointLon,
            double startLat,
            double startLon,
            double endLat,
            double endLon) {

        double earthRadius = 6371000.0;

        double referenceLat =
                Math.toRadians(pointLat);

        double px =
                Math.toRadians(pointLon)
                        * Math.cos(referenceLat)
                        * earthRadius;

        double py =
                Math.toRadians(pointLat)
                        * earthRadius;

        double ax =
                Math.toRadians(startLon)
                        * Math.cos(referenceLat)
                        * earthRadius;

        double ay =
                Math.toRadians(startLat)
                        * earthRadius;

        double bx =
                Math.toRadians(endLon)
                        * Math.cos(referenceLat)
                        * earthRadius;

        double by =
                Math.toRadians(endLat)
                        * earthRadius;

        double abX = bx - ax;
        double abY = by - ay;

        double apX = px - ax;
        double apY = py - ay;

        double abSquared =
                abX * abX + abY * abY;

        if (abSquared == 0) {

            return Math.sqrt(
                    apX * apX + apY * apY
            );
        }

        double t =
                (apX * abX + apY * abY)
                        / abSquared;

        t = Math.max(
                0.0,
                Math.min(1.0, t)
        );

        double closestX =
                ax + t * abX;

        double closestY =
                ay + t * abY;

        double dx =
                px - closestX;

        double dy =
                py - closestY;

        return Math.sqrt(
                dx * dx + dy * dy
        );
    }
    private Road findNearestRoad(
            Graph graph,
            double latitude,
            double longitude) {

        Road nearestRoad =
                null;

        double smallestDistance =
                Double.MAX_VALUE;


        for (Road road :
                getAllRoads(graph)) {

            Location start =
                    road.getStart();

            Location end =
                    road.getEnd();


            /*
             * Simple first version:
             * compare the report to the midpoint
             * of each road segment.
             */
            double midpointLat =
                    (
                            start.getLatitude()
                                    + end.getLatitude()
                    ) / 2.0;

            double midpointLon =
                    (
                            start.getLongitude()
                                    + end.getLongitude()
                    ) / 2.0;


            double distance =
                    calculateDistanceMeters(
                            latitude,
                            longitude,
                            midpointLat,
                            midpointLon
                    );


            if (distance < smallestDistance) {

                smallestDistance =
                        distance;

                nearestRoad =
                        road;
            }
        }


        /*
         * Ignore reports that are too far
         * from this route's road network.
         *
         * 150 meters is a reasonable starting point.
         */
        if (smallestDistance > 150) {
            return null;
        }


        return nearestRoad;
    }


    // =========================================================
    // DRAW DIJKSTRA PATH
    // =========================================================

    private void drawRoute(List<Location> path) {

        if (path == null || path.size() < 2) {
            return;
        }

        // -----------------------------------------
        // WHITE OUTLINE UNDER THE ROUTE
        // -----------------------------------------

        Polyline routeOutline = new Polyline();

        for (Location location : path) {

            routeOutline.addPoint(
                    new GeoPoint(
                            location.getLatitude(),
                            location.getLongitude()
                    )
            );
        }

        routeOutline.setWidth(16.0f);
        routeOutline.setColor(
                android.graphics.Color.WHITE
        );

        mapView.getOverlays().add(routeOutline);


        // -----------------------------------------
        // BLUE SAFEST ROUTE
        // -----------------------------------------

        Polyline routeLine = new Polyline();

        for (Location location : path) {

            routeLine.addPoint(
                    new GeoPoint(
                            location.getLatitude(),
                            location.getLongitude()
                    )
            );
        }

        routeLine.setWidth(10.0f);

        routeLine.setColor(
                android.graphics.Color.rgb(
                        66,
                        133,
                        244
                )
        );

        mapView.getOverlays().add(routeLine);

        mapView.invalidate();
    }


    // =========================================================
    // DISTANCE BETWEEN TWO GPS POINTS
    // =========================================================

    private double calculateDistanceMeters(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {


        double earthRadius =
                6371000.0;


        double latitude1 =
                Math.toRadians(lat1);

        double latitude2 =
                Math.toRadians(lat2);


        double deltaLatitude =
                Math.toRadians(
                        lat2 - lat1
                );

        double deltaLongitude =
                Math.toRadians(
                        lon2 - lon1
                );


        double a =
                Math.sin(deltaLatitude / 2)
                        * Math.sin(deltaLatitude / 2)

                        +

                        Math.cos(latitude1)
                                * Math.cos(latitude2)

                                *

                                Math.sin(deltaLongitude / 2)
                                * Math.sin(deltaLongitude / 2);


        double c =
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a)
                );


        return earthRadius * c;
    }


    // =========================================================
    // MAP LIFECYCLE
    // =========================================================

    @Override
    protected void onResume() {
        super.onResume();

        if (mapView != null) {
            mapView.onResume();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();

        if (mapView != null) {
            mapView.onPause();
        }
    }
}