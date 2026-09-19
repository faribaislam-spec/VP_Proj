package com.example.rungirlrun.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rungirlrun.R;
import com.example.rungirlrun.dao.ReportDAO;
import com.example.rungirlrun.enums.ReportType;
import com.example.rungirlrun.logic.TimeUtils;
import com.example.rungirlrun.models.SafetyReport;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportIncidentActivity extends AppCompatActivity {

    private Button backButton;
    private Button searchLocationButton;
    private Button submitReportButton;
    private Button viewMyReportsButton;

    private EditText locationInput;

    private Spinner reportTypeSpinner;

    private CheckBox isNightCheckbox;

    private TextView statusText;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Selected real-world location
    private String selectedLocationName = "";

    private Double selectedLatitude = null;
    private Double selectedLongitude = null;

    private boolean locationSelected = false;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_report_incident);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();


        // --------------------------------------------------
        // CONNECT XML VIEWS
        // --------------------------------------------------

        backButton =
                findViewById(R.id.backButton);

        locationInput =
                findViewById(R.id.locationInput);

        searchLocationButton =
                findViewById(R.id.searchLocationButton);

        reportTypeSpinner =
                findViewById(R.id.reportTypeSpinner);

        isNightCheckbox =
                findViewById(R.id.isNightCheckbox);

        submitReportButton =
                findViewById(R.id.submitReportButton);

        viewMyReportsButton =
                findViewById(R.id.viewMyReportsButton);

        statusText =
                findViewById(R.id.statusText);


        // --------------------------------------------------
        // BACK BUTTON
        // --------------------------------------------------

        backButton.setOnClickListener(v ->
                finish()
        );


        // --------------------------------------------------
        // VIEW MY REPORTS
        // --------------------------------------------------

        viewMyReportsButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            ReportIncidentActivity.this,
                            MyReportsActivity.class
                    );

            startActivity(intent);
        });


        // --------------------------------------------------
        // AUTO-DETECT DAY / NIGHT
        // --------------------------------------------------

        isNightCheckbox.setChecked(
                TimeUtils.isNightTime()
        );


        // --------------------------------------------------
        // REPORT TYPE DROPDOWN
        // --------------------------------------------------

        setupReportTypeSpinner();


        // --------------------------------------------------
        // SEARCH REAL LOCATION
        // --------------------------------------------------

        searchLocationButton.setOnClickListener(v -> {

            String query =
                    locationInput.getText()
                            .toString()
                            .trim();

            if (query.isEmpty()) {

                Toast.makeText(
                        this,
                        "Enter a location first",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            locationSelected = false;

            searchLocation(query);
        });


        // --------------------------------------------------
        // SUBMIT REPORT
        // --------------------------------------------------

        submitReportButton.setOnClickListener(v ->
                submitReport()
        );
    }


    // ======================================================
    // REPORT TYPE SPINNER
    // ======================================================

    private void setupReportTypeSpinner() {

        List<String> typeNames =
                new ArrayList<>();

        for (ReportType type :
                ReportType.values()) {

            typeNames.add(
                    formatReportType(type)
            );
        }


        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        typeNames
                );

        reportTypeSpinner.setAdapter(
                adapter
        );
    }


    private String formatReportType(
            ReportType type) {

        String[] words =
                type.name().split("_");

        StringBuilder result =
                new StringBuilder();

        for (String word : words) {

            result.append(
                            word.charAt(0)
                    )
                    .append(
                            word.substring(1)
                                    .toLowerCase()
                    )
                    .append(" ");
        }

        return result
                .toString()
                .trim();
    }


    // ======================================================
    // SEARCH REAL LOCATION USING NOMINATIM
    // ======================================================

    private void searchLocation(
            String query) {

        statusText.setText(
                "Searching location..."
        );


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
                 * Bangladesh is preferred using viewbox,
                 * but worldwide results are still allowed.
                 */
                String urlString =
                        "https://nominatim.openstreetmap.org/search"
                                + "?q=" + encodedQuery
                                + "&format=jsonv2"
                                + "&limit=5"
                                + "&addressdetails=1"
                                + "&viewbox=88.0,26.7,92.7,20.6";


                URL url =
                        new URL(urlString);


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
                        "RunGirlRun/1.0 (contact: afrinafroseee@gmail.com)"
                );


                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );


                int responseCode =
                        connection.getResponseCode();


                if (responseCode != 200) {

                    throw new Exception(
                            "Search server returned "
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

                while ((line =
                        reader.readLine()) != null) {

                    response.append(line);
                }


                reader.close();


                JSONArray results =
                        new JSONArray(
                                response.toString()
                        );


                ArrayList<String> names =
                        new ArrayList<>();

                ArrayList<Double> latitudes =
                        new ArrayList<>();

                ArrayList<Double> longitudes =
                        new ArrayList<>();


                for (int i = 0;
                     i < results.length();
                     i++) {

                    JSONObject object =
                            results.getJSONObject(i);


                    names.add(
                            object.getString(
                                    "display_name"
                            )
                    );


                    latitudes.add(
                            Double.parseDouble(
                                    object.getString(
                                            "lat"
                                    )
                            )
                    );


                    longitudes.add(
                            Double.parseDouble(
                                    object.getString(
                                            "lon"
                                    )
                            )
                    );
                }


                runOnUiThread(() -> {

                    statusText.setText("");

                    if (names.isEmpty()) {

                        Toast.makeText(
                                ReportIncidentActivity.this,
                                "No matching locations found",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }


                    showLocationResults(
                            names,
                            latitudes,
                            longitudes
                    );
                });


            } catch (Exception e) {

                e.printStackTrace();


                runOnUiThread(() -> {

                    statusText.setText(
                            "Could not search location."
                    );

                    Toast.makeText(
                            ReportIncidentActivity.this,
                            "Location search failed",
                            Toast.LENGTH_SHORT
                    ).show();
                });


            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }


    // ======================================================
    // SHOW LOCATION SEARCH RESULTS
    // ======================================================

    private void showLocationResults(
            ArrayList<String> names,
            ArrayList<Double> latitudes,
            ArrayList<Double> longitudes) {


        String[] items =
                names.toArray(
                        new String[0]
                );


        new AlertDialog.Builder(this)

                .setTitle(
                        "Choose Unsafe Location"
                )

                .setItems(
                        items,
                        (dialog, which) -> {

                            selectedLocationName =
                                    names.get(which);

                            selectedLatitude =
                                    latitudes.get(which);

                            selectedLongitude =
                                    longitudes.get(which);

                            locationSelected =
                                    true;


                            locationInput.setText(
                                    selectedLocationName
                            );


                            statusText.setText(
                                    "Location selected"
                            );
                        }
                )

                .setNegativeButton(
                        "Cancel",
                        null
                )

                .show();
    }


    // ======================================================
    // SUBMIT REPORT
    // ======================================================

    private void submitReport() {

        if (!locationSelected
                || selectedLatitude == null
                || selectedLongitude == null) {

            Toast.makeText(
                    this,
                    "Please search and select a location",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        int typeIndex =
                reportTypeSpinner
                        .getSelectedItemPosition();


        if (typeIndex < 0) {

            Toast.makeText(
                    this,
                    "Please select an incident type",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        ReportType reportType =
                ReportType.values()[
                        typeIndex
                        ];


        boolean isNight =
                isNightCheckbox
                        .isChecked();


        String userId =
                auth.getCurrentUser()
                        != null

                        ? auth
                          .getCurrentUser()
                          .getUid()

                        : "anonymous";


        /*
         * roadId is null for now.
         *
         * Later, while calculating a route,
         * we will match this latitude/longitude
         * to the nearest real OSM road.
         */
        SafetyReport report =
                new SafetyReport(
                        null,
                        null,
                        userId,
                        reportType,
                        isNight,
                        System.currentTimeMillis(),
                        selectedLocationName,
                        selectedLatitude,
                        selectedLongitude
                );


        submitReportButton.setEnabled(
                false
        );


        statusText.setText(
                "Submitting your report..."
        );


        ReportDAO.submitReport(
                db,
                report,
                new ReportDAO.OnReportSubmittedListener() {

                    @Override
                    public void onSuccess() {

                        submitReportButton
                                .setEnabled(true);


                        statusText.setText(
                                "Thank you. Your report has been submitted."
                        );


                        Toast.makeText(
                                ReportIncidentActivity.this,
                                "Unsafe location reported",
                                Toast.LENGTH_SHORT
                        ).show();


                        clearForm();
                    }


                    @Override
                    public void onError(Exception e) {

                        submitReportButton
                                .setEnabled(true);


                        statusText.setText(
                                "Something went wrong. Please try again."
                        );


                        e.printStackTrace();
                    }
                }
        );
    }


    // ======================================================
    // CLEAR FORM AFTER SUCCESS
    // ======================================================

    private void clearForm() {

        locationInput.setText("");

        selectedLocationName = "";

        selectedLatitude = null;
        selectedLongitude = null;

        locationSelected = false;

        reportTypeSpinner.setSelection(0);

        isNightCheckbox.setChecked(
                TimeUtils.isNightTime()
        );
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        executor.shutdownNow();
    }
}