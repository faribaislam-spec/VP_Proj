package com.example.rungirlrun.utils;

import android.location.Location;
import android.util.Log;

import com.example.rungirlrun.models.PoliceStation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.Handler;
import android.os.Looper;

public class PoliceStationFinder {

    private static final String TAG = "PoliceStationFinder";
    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";
    private static final int SEARCH_RADIUS_METERS = 5000;

    public interface Callback {
        void onResult(List<PoliceStation> stations);
        void onError(Exception e);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void findNearby(double lat, double lon, Callback callback) {
        executor.execute(() -> {
            try {
                List<PoliceStation> stations = fetchFromOverpass(lat, lon);
                mainHandler.post(() -> callback.onResult(stations));
            } catch (Exception e) {
                Log.e(TAG, "Overpass fetch failed", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    private List<PoliceStation> fetchFromOverpass(double lat, double lon) throws Exception {

        String query =
                "[out:json][timeout:25];" +
                        "(" +
                        "  node[\"amenity\"=\"police\"](around:" + SEARCH_RADIUS_METERS + "," + lat + "," + lon + ");" +
                        "  way[\"amenity\"=\"police\"](around:" + SEARCH_RADIUS_METERS + "," + lat + "," + lon + ");" +
                        ");" +
                        "out center;";

        URL url = new URL(OVERPASS_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);

        byte[] body = ("data=" + java.net.URLEncoder.encode(query, "UTF-8"))
                .getBytes(StandardCharsets.UTF_8);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Overpass returned HTTP " + responseCode);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        return parseResponse(sb.toString(), lat, lon);
    }

    private List<PoliceStation> parseResponse(String json, double originLat, double originLon) throws Exception {

        List<PoliceStation> result = new ArrayList<>();
        JSONObject root = new JSONObject(json);
        JSONArray elements = root.getJSONArray("elements");

        for (int i = 0; i < elements.length(); i++) {
            JSONObject el = elements.getJSONObject(i);

            double elLat;
            double elLon;

            if (el.has("lat") && el.has("lon")) {
                elLat = el.getDouble("lat");
                elLon = el.getDouble("lon");
            } else if (el.has("center")) {
                JSONObject center = el.getJSONObject("center");
                elLat = center.getDouble("lat");
                elLon = center.getDouble("lon");
            } else {
                continue; // no usable coordinates
            }

            String name = "Police Station";
            if (el.has("tags")) {
                JSONObject tags = el.getJSONObject("tags");
                if (tags.has("name")) {
                    name = tags.getString("name");
                }
            }

            float[] distanceResult = new float[1];
            Location.distanceBetween(originLat, originLon, elLat, elLon, distanceResult);

            result.add(new PoliceStation(name, elLat, elLon, distanceResult[0]));
        }

        Collections.sort(result, Comparator.comparingDouble(PoliceStation::getDistanceMeters));

        // cap to nearest 5 so the "notifying" list stays readable
        if (result.size() > 5) {
            result = result.subList(0, 5);
        }

        return result;
    }
}