package com.example.rungirlrun.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rungirlrun.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RouteSelectionActivity extends AppCompatActivity {

    // =====================================================
    // VIEWS
    // =====================================================

    private AutoCompleteTextView sourceInput;
    private AutoCompleteTextView destinationInput;

    private Button searchSourceButton;
    private Button searchDestinationButton;
    private Button findSafeRouteButton;
    private Button backButton;


    // =====================================================
    // SOURCE SUGGESTIONS
    // =====================================================

    private final ArrayList<String> sourceSuggestionNames =
            new ArrayList<>();

    private final ArrayList<Double> sourceSuggestionLats =
            new ArrayList<>();

    private final ArrayList<Double> sourceSuggestionLons =
            new ArrayList<>();

    private ArrayAdapter<String> sourceSuggestionAdapter;


    // =====================================================
    // DESTINATION SUGGESTIONS
    // =====================================================

    private final ArrayList<String> destinationSuggestionNames =
            new ArrayList<>();

    private final ArrayList<Double> destinationSuggestionLats =
            new ArrayList<>();

    private final ArrayList<Double> destinationSuggestionLons =
            new ArrayList<>();

    private ArrayAdapter<String> destinationSuggestionAdapter;


    // =====================================================
    // SELECTED SOURCE
    // =====================================================

    private String selectedSourceName = "";

    private double sourceLat;
    private double sourceLon;

    private boolean sourceSelected = false;


    // =====================================================
    // SELECTED DESTINATION
    // =====================================================

    private String selectedDestinationName = "";

    private double destinationLat;
    private double destinationLon;

    private boolean destinationSelected = false;


    // Prevent programmatic setText() from
    // resetting the selected status
    private boolean updatingSourceText = false;
    private boolean updatingDestinationText = false;


    // =====================================================
    // AUTOCOMPLETE DELAY
    // =====================================================

    private final Handler sourceSearchHandler =
            new Handler(Looper.getMainLooper());

    private final Handler destinationSearchHandler =
            new Handler(Looper.getMainLooper());

    private Runnable sourceSearchRunnable;
    private Runnable destinationSearchRunnable;


    // =====================================================
    // BACKGROUND NETWORK THREAD
    // =====================================================

    private final ExecutorService executor =
            Executors.newFixedThreadPool(2);


    // =====================================================
    // ON CREATE
    // =====================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_route_selection
        );


        // -------------------------------------------------
        // CONNECT VIEWS
        // -------------------------------------------------

        backButton =
                findViewById(R.id.backButton);

        sourceInput =
                findViewById(R.id.sourceInput);

        destinationInput =
                findViewById(R.id.destinationInput);

        searchSourceButton =
                findViewById(R.id.searchSourceButton);

        searchDestinationButton =
                findViewById(R.id.searchDestinationButton);

        findSafeRouteButton =
                findViewById(R.id.findSafeRouteButton);


        // -------------------------------------------------
        // BACK BUTTON
        // -------------------------------------------------

        backButton.setOnClickListener(
                v -> finish()
        );


        // -------------------------------------------------
        // SOURCE ADAPTER
        // -------------------------------------------------

        sourceSuggestionAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        sourceSuggestionNames
                );

        sourceInput.setAdapter(
                sourceSuggestionAdapter
        );

        sourceInput.setThreshold(2);


        // -------------------------------------------------
        // DESTINATION ADAPTER
        // -------------------------------------------------

        destinationSuggestionAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        destinationSuggestionNames
                );

        destinationInput.setAdapter(
                destinationSuggestionAdapter
        );

        destinationInput.setThreshold(2);


        // -------------------------------------------------
        // ROUTE BUTTON DISABLED INITIALLY
        // -------------------------------------------------

        findSafeRouteButton.setEnabled(false);


        // =================================================
        // SOURCE LIVE AUTOCOMPLETE
        // =================================================

        sourceInput.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        if (updatingSourceText) {
                            return;
                        }

                        sourceSelected = false;

                        updateFindRouteButton();


                        if (sourceSearchRunnable != null) {

                            sourceSearchHandler
                                    .removeCallbacks(
                                            sourceSearchRunnable
                                    );
                        }


                        String query =
                                s.toString()
                                        .trim();


                        if (query.length() < 2) {

                            sourceInput
                                    .dismissDropDown();

                            return;
                        }


                        sourceSearchRunnable =
                                () ->
                                        searchPhoton(
                                                query,
                                                true
                                        );


                        sourceSearchHandler
                                .postDelayed(
                                        sourceSearchRunnable,
                                        500
                                );
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {
                    }
                }
        );


        // =================================================
        // DESTINATION LIVE AUTOCOMPLETE
        // =================================================

        destinationInput.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        if (updatingDestinationText) {
                            return;
                        }

                        destinationSelected = false;

                        updateFindRouteButton();


                        if (destinationSearchRunnable != null) {

                            destinationSearchHandler
                                    .removeCallbacks(
                                            destinationSearchRunnable
                                    );
                        }


                        String query =
                                s.toString()
                                        .trim();


                        if (query.length() < 2) {

                            destinationInput
                                    .dismissDropDown();

                            return;
                        }


                        destinationSearchRunnable =
                                () ->
                                        searchPhoton(
                                                query,
                                                false
                                        );


                        destinationSearchHandler
                                .postDelayed(
                                        destinationSearchRunnable,
                                        500
                                );
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {
                    }
                }
        );


        // =================================================
        // SOURCE SUGGESTION SELECTED
        // =================================================

        sourceInput.setOnItemClickListener(
                (parent, view, position, id) -> {

                    if (position < 0
                            || position
                            >= sourceSuggestionNames.size()) {

                        return;
                    }


                    selectedSourceName =
                            sourceSuggestionNames
                                    .get(position);

                    sourceLat =
                            sourceSuggestionLats
                                    .get(position);

                    sourceLon =
                            sourceSuggestionLons
                                    .get(position);


                    sourceSelected = true;


                    updatingSourceText = true;

                    sourceInput.setText(
                            selectedSourceName,
                            false
                    );

                    updatingSourceText = false;


                    sourceInput.dismissDropDown();

                    updateFindRouteButton();
                }
        );


        // =================================================
        // DESTINATION SUGGESTION SELECTED
        // =================================================

        destinationInput.setOnItemClickListener(
                (parent, view, position, id) -> {

                    if (position < 0
                            || position
                            >= destinationSuggestionNames.size()) {

                        return;
                    }


                    selectedDestinationName =
                            destinationSuggestionNames
                                    .get(position);

                    destinationLat =
                            destinationSuggestionLats
                                    .get(position);

                    destinationLon =
                            destinationSuggestionLons
                                    .get(position);


                    destinationSelected = true;


                    updatingDestinationText = true;

                    destinationInput.setText(
                            selectedDestinationName,
                            false
                    );

                    updatingDestinationText = false;


                    destinationInput.dismissDropDown();

                    updateFindRouteButton();
                }
        );


        // =================================================
        // SEARCH SOURCE BUTTON
        // =================================================

        searchSourceButton.setOnClickListener(
                v -> {

                    String query =
                            sourceInput
                                    .getText()
                                    .toString()
                                    .trim();


                    if (query.isEmpty()) {

                        Toast.makeText(
                                RouteSelectionActivity.this,
                                "Enter a source location",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }


                    searchPhoton(
                            query,
                            true
                    );
                }
        );


        // =================================================
        // SEARCH DESTINATION BUTTON
        // =================================================

        searchDestinationButton.setOnClickListener(
                v -> {

                    String query =
                            destinationInput
                                    .getText()
                                    .toString()
                                    .trim();


                    if (query.isEmpty()) {

                        Toast.makeText(
                                RouteSelectionActivity.this,
                                "Enter a destination",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }


                    searchPhoton(
                            query,
                            false
                    );
                }
        );


        // =================================================
        // FIND SAFEST ROUTE
        // =================================================

        findSafeRouteButton.setOnClickListener(
                v -> {


                    if (!sourceSelected) {

                        Toast.makeText(
                                RouteSelectionActivity.this,
                                "Please select a source from the suggestions",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }


                    if (!destinationSelected) {

                        Toast.makeText(
                                RouteSelectionActivity.this,
                                "Please select a destination from the suggestions",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }


                    // Prevent same source and destination
                    double difference =
                            Math.abs(
                                    sourceLat
                                            - destinationLat
                            )

                                    +

                                    Math.abs(
                                            sourceLon
                                                    - destinationLon
                                    );


                    if (difference < 0.000001) {

                        Toast.makeText(
                                RouteSelectionActivity.this,
                                "Source and destination cannot be the same",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }


                    Intent intent =
                            new Intent(
                                    RouteSelectionActivity.this,
                                    SafeMapActivity.class
                            );


                    // SOURCE
                    intent.putExtra(
                            "sourceName",
                            selectedSourceName
                    );

                    intent.putExtra(
                            "sourceLat",
                            sourceLat
                    );

                    intent.putExtra(
                            "sourceLon",
                            sourceLon
                    );


                    // DESTINATION
                    intent.putExtra(
                            "destinationName",
                            selectedDestinationName
                    );

                    intent.putExtra(
                            "destinationLat",
                            destinationLat
                    );

                    intent.putExtra(
                            "destinationLon",
                            destinationLon
                    );


                    startActivity(intent);
                }
        );
    }


    // =====================================================
    // PHOTON LOCATION SEARCH
    // =====================================================

    private void searchPhoton(
            String query,
            boolean searchingSource) {


        executor.execute(() -> {

            HttpURLConnection connection =
                    null;


            try {

                String encodedQuery =
                        URLEncoder.encode(
                                query,
                                "UTF-8"
                        );


                /*
                 * Dhaka coordinates bias nearby Bangladesh
                 * locations first.
                 *
                 * Photon can still return worldwide places.
                 */
                String urlString =
                        "https://photon.komoot.io/api/"
                                + "?q=" + encodedQuery
                                + "&limit=8"
                                + "&lat=23.8103"
                                + "&lon=90.4125";


                URL url =
                        new URL(
                                urlString
                        );


                connection =
                        (HttpURLConnection)
                                url.openConnection();


                connection.setRequestMethod(
                        "GET"
                );


                connection.setConnectTimeout(
                        15000
                );


                connection.setReadTimeout(
                        15000
                );


                connection.setRequestProperty(
                        "User-Agent",
                        "RunGirlRun/1.0"
                );


                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );


                int responseCode =
                        connection.getResponseCode();


                if (responseCode
                        != HttpURLConnection.HTTP_OK) {

                    throw new Exception(
                            "Photon returned "
                                    + responseCode
                    );
                }


                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection
                                                .getInputStream()
                                )
                        );


                StringBuilder response =
                        new StringBuilder();


                String line;


                while ((line =
                        reader.readLine()) != null) {

                    response.append(
                            line
                    );
                }


                reader.close();


                JSONObject root =
                        new JSONObject(
                                response.toString()
                        );


                JSONArray features =
                        root.getJSONArray(
                                "features"
                        );


                ArrayList<String> names =
                        new ArrayList<>();


                ArrayList<Double> latitudes =
                        new ArrayList<>();


                ArrayList<Double> longitudes =
                        new ArrayList<>();


                // -----------------------------------------
                // PARSE PHOTON RESULTS
                // -----------------------------------------

                for (int i = 0;
                     i < features.length();
                     i++) {


                    JSONObject feature =
                            features
                                    .getJSONObject(i);


                    JSONObject properties =
                            feature
                                    .getJSONObject(
                                            "properties"
                                    );


                    JSONObject geometry =
                            feature
                                    .getJSONObject(
                                            "geometry"
                                    );


                    JSONArray coordinates =
                            geometry
                                    .getJSONArray(
                                            "coordinates"
                                    );


                    /*
                     * GeoJSON order:
                     *
                     * longitude first
                     * latitude second
                     */
                    double longitude =
                            coordinates
                                    .getDouble(0);


                    double latitude =
                            coordinates
                                    .getDouble(1);


                    String displayName =
                            buildDisplayName(
                                    properties
                            );


                    if (displayName.isEmpty()) {
                        continue;
                    }


                    names.add(
                            displayName
                    );


                    latitudes.add(
                            latitude
                    );


                    longitudes.add(
                            longitude
                    );
                }


                // -----------------------------------------
                // UPDATE AUTOCOMPLETE UI
                // -----------------------------------------

                runOnUiThread(() -> {

                    if (names.isEmpty()) {

                        Toast.makeText(
                                RouteSelectionActivity.this,
                                "No matching locations found",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    if (searchingSource) {

                        sourceSuggestionNames.clear();
                        sourceSuggestionLats.clear();
                        sourceSuggestionLons.clear();

                        sourceSuggestionNames.addAll(names);
                        sourceSuggestionLats.addAll(latitudes);
                        sourceSuggestionLons.addAll(longitudes);

                        sourceSuggestionAdapter =
                                new ArrayAdapter<>(
                                        RouteSelectionActivity.this,
                                        android.R.layout.simple_dropdown_item_1line,
                                        new ArrayList<>(sourceSuggestionNames)
                                );

                        sourceInput.setAdapter(sourceSuggestionAdapter);

                        sourceInput.post(() -> {
                            sourceInput.showDropDown();
                        });

                    } else {

                        destinationSuggestionNames.clear();
                        destinationSuggestionLats.clear();
                        destinationSuggestionLons.clear();

                        destinationSuggestionNames.addAll(names);
                        destinationSuggestionLats.addAll(latitudes);
                        destinationSuggestionLons.addAll(longitudes);

                        destinationSuggestionAdapter =
                                new ArrayAdapter<>(
                                        RouteSelectionActivity.this,
                                        android.R.layout.simple_dropdown_item_1line,
                                        new ArrayList<>(destinationSuggestionNames)
                                );

                        destinationInput.setAdapter(
                                destinationSuggestionAdapter
                        );

                        destinationInput.post(() -> {
                            destinationInput.showDropDown();
                        });
                    }
                });

            } catch (Exception e) {

                e.printStackTrace();


                runOnUiThread(() ->

                        Toast.makeText(
                                RouteSelectionActivity.this,
                                "Search failed: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );


            } finally {

                if (connection != null) {

                    connection.disconnect();
                }
            }
        });
    }


    // =====================================================
    // CREATE NICE LOCATION NAME
    // =====================================================

    private String buildDisplayName(
            JSONObject properties) {


        String name =
                properties.optString(
                        "name",
                        ""
                );


        String street =
                properties.optString(
                        "street",
                        ""
                );


        String district =
                properties.optString(
                        "district",
                        ""
                );


        String city =
                properties.optString(
                        "city",
                        ""
                );


        String state =
                properties.optString(
                        "state",
                        ""
                );


        String country =
                properties.optString(
                        "country",
                        ""
                );


        ArrayList<String> parts =
                new ArrayList<>();


        addIfUseful(
                parts,
                name
        );


        addIfUseful(
                parts,
                street
        );


        addIfUseful(
                parts,
                district
        );


        addIfUseful(
                parts,
                city
        );


        addIfUseful(
                parts,
                state
        );


        addIfUseful(
                parts,
                country
        );


        return android.text.TextUtils.join(
                ", ",
                parts
        );
    }


    // =====================================================
    // PREVENT DUPLICATE TEXT
    // =====================================================

    private void addIfUseful(
            ArrayList<String> parts,
            String value) {


        if (value == null
                || value.trim().isEmpty()) {

            return;
        }


        String cleaned =
                value.trim();


        for (String existing :
                parts) {

            if (existing
                    .equalsIgnoreCase(
                            cleaned
                    )) {

                return;
            }
        }


        parts.add(
                cleaned
        );
    }


    // =====================================================
    // ENABLE FIND ROUTE BUTTON
    // =====================================================

    private void updateFindRouteButton() {

        findSafeRouteButton.setEnabled(
                sourceSelected
                        && destinationSelected
        );
    }


    // =====================================================
    // CLEANUP
    // =====================================================

    @Override
    protected void onDestroy() {
        super.onDestroy();


        if (sourceSearchRunnable != null) {

            sourceSearchHandler
                    .removeCallbacks(
                            sourceSearchRunnable
                    );
        }


        if (destinationSearchRunnable != null) {

            destinationSearchHandler
                    .removeCallbacks(
                            destinationSearchRunnable
                    );
        }


        executor.shutdownNow();
    }
}