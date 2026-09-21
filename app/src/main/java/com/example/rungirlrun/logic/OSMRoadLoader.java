package com.example.rungirlrun.logic;

import com.example.rungirlrun.models.Location;
import com.example.rungirlrun.models.Road;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OSMRoadLoader {

    private static final String[] OVERPASS_URLS = {
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
            "https://overpass.nchc.org.tw/api/interpreter"
    };;

    private static final ExecutorService executor =
            Executors.newSingleThreadExecutor();


    public interface OnGraphLoadedListener {

        void onSuccess(Graph graph);

        void onError(Exception e);
    }


    public static void loadGraph(
            double sourceLat,
            double sourceLon,
            double destinationLat,
            double destinationLon,
            OnGraphLoadedListener listener) {

        executor.execute(() -> {

            HttpURLConnection connection = null;

            try {

                // --------------------------------------------
                // 1. CREATE BOUNDING BOX
                // --------------------------------------------

                double padding = 0.005;

                double south =
                        Math.min(sourceLat, destinationLat)
                                - padding;

                double north =
                        Math.max(sourceLat, destinationLat)
                                + padding;

                double west =
                        Math.min(sourceLon, destinationLon)
                                - padding;

                double east =
                        Math.max(sourceLon, destinationLon)
                                + padding;


                // --------------------------------------------
                // 2. REQUEST REAL ROAD DATA
                // --------------------------------------------

                String query =
                        "[out:json][timeout:20];"
                                + "("
                                + "way[\"highway\"~\"^(motorway|trunk|primary|secondary|tertiary|residential|unclassified|service)$\"]("
                                + south + ","
                                + west + ","
                                + north + ","
                                + east
                                + ");"
                                + ");"
                                + "out body;"
                                + ">;"
                                + "out skel qt;";


                URL url =
                        new URL(OVERPASS_URLS[0]);

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod("POST");

                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);

                connection.setDoOutput(true);

                connection.setRequestProperty(
                        "User-Agent",
                        "RunGirlRun/1.0"
                );

                connection.setRequestProperty(
                        "Content-Type",
                        "application/x-www-form-urlencoded"
                );


                String postData =
                        "data="
                                + URLEncoder.encode(
                                query,
                                StandardCharsets.UTF_8.name()
                        );


                try (OutputStream outputStream =
                             connection.getOutputStream()) {

                    outputStream.write(
                            postData.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );
                }


                int responseCode =
                        connection.getResponseCode();

                if (responseCode != 200) {

                    throw new Exception(
                            "Overpass server returned "
                                    + responseCode
                    );
                }


                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection.getInputStream()
                                )
                        );


                StringBuilder response =
                        new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();


                JSONObject json =
                        new JSONObject(
                                response.toString()
                        );


                // --------------------------------------------
                // 3. BUILD GRAPH
                // --------------------------------------------

                Graph graph =
                        buildGraphFromJson(json);


                listener.onSuccess(graph);


            } catch (Exception e) {

                listener.onError(e);

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }


    // =========================================================
    // CONVERT OVERPASS JSON INTO YOUR GRAPH
    // =========================================================

    private static Graph buildGraphFromJson(
            JSONObject json) throws Exception {

        Graph graph =
                new Graph();


        /*
         * OSM node ID -> your Location object
         */
        Map<Long, Location> nodeMap =
                new HashMap<>();


        JSONArray elements =
                json.getJSONArray("elements");


        // --------------------------------------------------
        // FIRST PASS:
        // CREATE ALL LOCATION NODES
        // --------------------------------------------------

        for (int i = 0;
             i < elements.length();
             i++) {

            JSONObject element =
                    elements.getJSONObject(i);


            String type =
                    element.optString("type");


            if (!type.equals("node")) {
                continue;
            }


            long osmNodeId =
                    element.getLong("id");


            double latitude =
                    element.getDouble("lat");


            double longitude =
                    element.getDouble("lon");


            String locationId =
                    "osm_node_" + osmNodeId;


            Location location =
                    new Location(
                            locationId,
                            "Road point",
                            latitude,
                            longitude
                    );


            nodeMap.put(
                    osmNodeId,
                    location
            );


            graph.addLocation(
                    location
            );
        }


        // --------------------------------------------------
        // SECOND PASS:
        // CREATE ROAD SEGMENTS
        // --------------------------------------------------

        for (int i = 0;
             i < elements.length();
             i++) {

            JSONObject element =
                    elements.getJSONObject(i);


            String type =
                    element.optString("type");


            if (!type.equals("way")) {
                continue;
            }


            long wayId =
                    element.getLong("id");


            JSONArray nodeIds =
                    element.getJSONArray("nodes");


            // Road name if OSM has one
            String roadName =
                    "Unnamed Road";


            if (element.has("tags")) {

                JSONObject tags =
                        element.getJSONObject(
                                "tags"
                        );


                roadName =
                        tags.optString(
                                "name",
                                "Unnamed Road"
                        );
            }


            /*
             * An OSM "way" contains a sequence such as:
             *
             * 101 → 102 → 103 → 104
             *
             * We convert it into separate Road edges:
             *
             * 101-102
             * 102-103
             * 103-104
             */
            for (int n = 0;
                 n < nodeIds.length() - 1;
                 n++) {


                long startNodeId =
                        nodeIds.getLong(n);


                long endNodeId =
                        nodeIds.getLong(n + 1);


                Location start =
                        nodeMap.get(startNodeId);


                Location end =
                        nodeMap.get(endNodeId);


                if (start == null
                        || end == null) {

                    continue;
                }


                // Real geographic distance
                double distance =
                        calculateDistanceMeters(
                                start.getLatitude(),
                                start.getLongitude(),
                                end.getLatitude(),
                                end.getLongitude()
                        );


                String roadId =
                        "osm_way_"
                                + wayId
                                + "_"
                                + n;


                Road road =
                        new Road(
                                roadId,
                                roadName,
                                start,
                                end,
                                distance
                        );


                graph.addRoad(
                        road
                );
            }
        }


        return graph;
    }


    // =========================================================
    // CALCULATE DISTANCE BETWEEN TWO GPS COORDINATES
    // =========================================================

    private static double calculateDistanceMeters(
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
                Math.sin(
                        deltaLatitude / 2
                )
                        * Math.sin(
                        deltaLatitude / 2
                )

                        +

                        Math.cos(latitude1)
                                * Math.cos(latitude2)

                                *

                                Math.sin(
                                        deltaLongitude / 2
                                )

                                * Math.sin(
                                deltaLongitude / 2
                        );


        double c =
                2
                        * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a)
                );


        return earthRadius * c;
    }
}
