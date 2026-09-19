package com.example.rungirlrun.services;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.rungirlrun.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LiveLocationService extends Service {

    public static final String ACTION_START =
            "com.example.rungirlrun.START_LIVE_TRACKING";

    public static final String ACTION_STOP =
            "com.example.rungirlrun.STOP_LIVE_TRACKING";

    public static final String EXTRA_SESSION_ID =
            "sessionId";

    private static final String CHANNEL_ID =
            "SOS_LIVE_LOCATION";

    private static final int NOTIFICATION_ID = 2001;

    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore firestore;
    private LocationCallback locationCallback;

    private String sessionId;

    @Override
    public void onCreate() {
        super.onCreate();

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        firestore =
                FirebaseFirestore.getInstance();

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        if (intent == null) {
            return START_NOT_STICKY;
        }

        String action = intent.getAction();

        if (ACTION_STOP.equals(action)) {

            stopLiveTracking();
            return START_NOT_STICKY;
        }

        sessionId =
                intent.getStringExtra(EXTRA_SESSION_ID);

        if (sessionId == null ||
                sessionId.trim().isEmpty()) {

            stopSelf();
            return START_NOT_STICKY;
        }

        startAsForegroundService();

        startLocationUpdates();

        return START_NOT_STICKY;
    }

    private void startAsForegroundService() {

        Intent stopIntent =
                new Intent(this, LiveLocationService.class);

        stopIntent.setAction(ACTION_STOP);

        PendingIntent stopPendingIntent =
                PendingIntent.getService(
                        this,
                        100,
                        stopIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        this,
                        CHANNEL_ID
                )
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(
                                "RunGirlRun SOS is active"
                        )
                        .setContentText(
                                "Your live location is being shared."
                        )
                        .setPriority(
                                NotificationCompat.PRIORITY_HIGH
                        )
                        .setOngoing(true)
                        .addAction(
                                0,
                                "STOP SOS",
                                stopPendingIntent
                        );

        startForeground(
                NOTIFICATION_ID,
                builder.build()
        );
    }

    private void startLocationUpdates() {

        if (
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
                        &&
                        ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
        ) {

            stopSelf();
            return;
        }

        LocationRequest request =
                new LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        5000
                )
                        .setMinUpdateIntervalMillis(3000)
                        .build();

        locationCallback =
                new LocationCallback() {

                    @Override
                    public void onLocationResult(
                            @NonNull LocationResult result
                    ) {

                        if (
                                result.getLastLocation()
                                        == null
                        ) {
                            return;
                        }

                        android.location.Location location =
                                result.getLastLocation();

                        updateFirebaseLocation(
                                location.getLatitude(),
                                location.getLongitude(),
                                location.getAccuracy()
                        );
                    }
                };

        fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                getMainLooper()
        );
    }

    private void updateFirebaseLocation(
            double latitude,
            double longitude,
            float accuracy
    ) {

        if (sessionId == null) {
            return;
        }

        Map<String, Object> locationData =
                new HashMap<>();

        locationData.put(
                "latitude",
                latitude
        );

        locationData.put(
                "longitude",
                longitude
        );

        locationData.put(
                "accuracy",
                accuracy
        );

        locationData.put(
                "active",
                true
        );

        locationData.put(
                "updatedAt",
                FieldValue.serverTimestamp()
        );

        firestore
                .collection("liveTracking")
                .document(sessionId)
                .update(locationData)
                .addOnSuccessListener(unused ->

                        Log.d(
                                "LIVE_TRACKING",
                                "Location updated: "
                                        + latitude
                                        + ","
                                        + longitude
                        )

                )
                .addOnFailureListener(e ->

                        Log.e(
                                "LIVE_TRACKING",
                                "Firebase update failed",
                                e
                        )

                );
    }

    private void stopLiveTracking() {

        if (locationCallback != null) {

            fusedLocationClient
                    .removeLocationUpdates(
                            locationCallback
                    );
        }

        if (sessionId != null) {

            Map<String, Object> values =
                    new HashMap<>();

            values.put(
                    "active",
                    false
            );

            values.put(
                    "stoppedAt",
                    FieldValue.serverTimestamp()
            );

            firestore
                    .collection("liveTracking")
                    .document(sessionId)
                    .update(values);
        }

        stopForeground(true);

        stopSelf();
    }

    private void createNotificationChannel() {

        if (
                Build.VERSION.SDK_INT
                        >= Build.VERSION_CODES.O
        ) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "SOS Live Tracking",
                            NotificationManager.IMPORTANCE_HIGH
                    );

            channel.setDescription(
                    "Shows when SOS live location sharing is active"
            );

            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );

            manager.createNotificationChannel(
                    channel
            );
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}