package com.example.rungirlrun.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.rungirlrun.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Foreground service for "Available to help nearby" mode. Keeps
 * running independent of which screen is on top, or even if the
 * screen is locked, so long as the app hasn't been force-stopped.
 * Publishes this device's location to activeUsers/{uid}, and listens
 * for nearby SOS triggers to show a local notification — no Cloud
 * Functions required.
 */
public class HelperModeService extends Service {

    public static final String ACTION_START =
            "com.example.rungirlrun.START_HELPER_MODE";

    public static final String ACTION_STOP =
            "com.example.rungirlrun.STOP_HELPER_MODE";

    private static final String TAG = "HelperModeService";

    private static final String FOREGROUND_CHANNEL_ID = "HELPER_MODE_ACTIVE";
    private static final int FOREGROUND_NOTIFICATION_ID = 5000;

    private static final String ALERT_CHANNEL_ID = "SOS_NEARBY_ALERTS_LOCAL";
    private static final double RADIUS_METERS = 1000;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private ListenerRegistration sosListenerRegistration;

    private String cachedFcmToken;
    private Double lastLatitude;
    private Double lastLongitude;
    private final Set<String> alreadyNotifiedSessionIds = new HashSet<>();
    private int alertNotificationIdCounter = 6000;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        createNotificationChannels();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null) {
            return START_NOT_STICKY;
        }

        String action = intent.getAction();

        if (ACTION_STOP.equals(action)) {
            stopHelperMode();
            return START_NOT_STICKY;
        }

        startAsForeground();
        beginPublishingLocation();
        beginListeningForSos();

        return START_STICKY;
    }

    private void startAsForeground() {

        Intent stopIntent = new Intent(this, HelperModeService.class);
        stopIntent.setAction(ACTION_STOP);

        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                200,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Helping nearby is active")
                        .setContentText(
                                "Sharing your location so you can be alerted to nearby SOS."
                        )
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setOngoing(true)
                        .addAction(0, "Turn off", stopPendingIntent);

        startForeground(FOREGROUND_NOTIFICATION_ID, builder.build());
    }

    private void beginPublishingLocation() {

        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null) {
            Log.w(TAG, "No logged-in user; stopping helper mode.");
            stopSelf();
            return;
        }

        FirebaseMessaging.getInstance()
                .getToken()
                .addOnSuccessListener(token -> {
                    cachedFcmToken = token;
                    fetchImmediateLocation(uid);
                    startLocationUpdates(uid);
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to get FCM token", e)
                );
    }

    private void fetchImmediateLocation(String uid) {

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {

                        if (location == null) {
                            Log.w(TAG, "No cached location available yet; waiting for first update.");
                            return;
                        }

                        lastLatitude = location.getLatitude();
                        lastLongitude = location.getLongitude();

                        publishLocation(uid, lastLatitude, lastLongitude);

                        Log.d(TAG, "Published immediate cached location.");
                    })
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to get last location", e)
                    );
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission missing", e);
        }
    }

    private void startLocationUpdates(String uid) {

        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                45000
        )
                .setMinUpdateIntervalMillis(30000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {

                if (result.getLastLocation() == null) {
                    return;
                }

                lastLatitude = result.getLastLocation().getLatitude();
                lastLongitude = result.getLastLocation().getLongitude();

                publishLocation(uid, lastLatitude, lastLongitude);
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(
                    request,
                    locationCallback,
                    getMainLooper()
            );
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission missing", e);
            stopSelf();
        }
    }

    private void publishLocation(
            String uid,
            double latitude,
            double longitude
    ) {

        Map<String, Object> data = new HashMap<>();
        data.put("latitude", latitude);
        data.put("longitude", longitude);
        data.put("fcmToken", cachedFcmToken);
        data.put("active", true);
        data.put("updatedAt", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance()
                .collection("activeUsers")
                .document(uid)
                .set(data)
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to publish location", e)
                );
    }

    private void beginListeningForSos() {

        if (sosListenerRegistration != null) {
            return;
        }

        String currentUid = FirebaseAuth.getInstance().getUid();

        sosListenerRegistration = FirebaseFirestore.getInstance()
                .collection("liveTracking")
                .whereEqualTo("active", true)
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null || snapshots == null) {
                        Log.e(TAG, "SOS listen failed", error);
                        return;
                    }

                    for (DocumentChange change : snapshots.getDocumentChanges()) {

                        if (change.getType() != DocumentChange.Type.ADDED) {
                            continue;
                        }

                        handlePossibleSos(change.getDocument(), currentUid);
                    }
                });
    }

    private void handlePossibleSos(
            DocumentSnapshot doc,
            String currentUid
    ) {

        String ownerId = doc.getString("ownerId");
        Double sosLat = doc.getDouble("latitude");
        Double sosLng = doc.getDouble("longitude");
        String sessionId = doc.getId();

        if (
                ownerId == null ||
                        ownerId.equals(currentUid) ||
                        sosLat == null ||
                        sosLng == null
        ) {
            return;
        }

        if (alreadyNotifiedSessionIds.contains(sessionId)) {
            return;
        }

        if (lastLatitude == null || lastLongitude == null) {
            Log.w(TAG, "SOS received but no helper location yet; skipping.");
            return;
        }

        double distance = haversineDistanceMeters(
                lastLatitude,
                lastLongitude,
                sosLat,
                sosLng
        );

        Log.d(TAG, "Distance to SOS: " + distance + " meters (radius is " + RADIUS_METERS + ")");

        if (distance <= RADIUS_METERS) {
            alreadyNotifiedSessionIds.add(sessionId);
            fetchOwnerNameAndNotify(ownerId, sessionId);
        }
    }

    private void fetchOwnerNameAndNotify(
            String ownerId,
            String sessionId
    ) {

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(ownerId)
                .get()
                .addOnSuccessListener(userDoc -> {

                    String name = "Someone nearby";

                    if (
                            userDoc.exists() &&
                                    userDoc.getString("fullName") != null
                    ) {
                        name = userDoc.getString("fullName");
                    }

                    showAlertNotification(name, sessionId);
                })
                .addOnFailureListener(e ->
                        showAlertNotification("Someone nearby", sessionId)
                );
    }

    private void showAlertNotification(String ownerName, String sessionId) {

        String trackingLink =
                "https://rungirlrun-app.web.app/track.html?id="
                        + sessionId;

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(trackingLink));

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                alertNotificationIdCounter,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("SOS nearby: " + ownerName)
                        .setContentText(
                                ownerName
                                        + " may need help nearby. Tap to view their location."
                        )
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        NotificationManager manager =
                (NotificationManager) getSystemService(
                        Context.NOTIFICATION_SERVICE
                );

        manager.notify(alertNotificationIdCounter, builder.build());
        alertNotificationIdCounter++;
    }

    private void stopHelperMode() {

        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        if (sosListenerRegistration != null) {
            sosListenerRegistration.remove();
            sosListenerRegistration = null;
        }

        String uid = FirebaseAuth.getInstance().getUid();

        if (uid != null) {

            Map<String, Object> update = new HashMap<>();
            update.put("active", false);

            FirebaseFirestore.getInstance()
                    .collection("activeUsers")
                    .document(uid)
                    .update(update)
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to mark inactive", e)
                    );
        }

        stopForeground(true);
        stopSelf();
    }

    private void createNotificationChannels() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationManager manager =
                    (NotificationManager) getSystemService(
                            Context.NOTIFICATION_SERVICE
                    );

            NotificationChannel foregroundChannel = new NotificationChannel(
                    FOREGROUND_CHANNEL_ID,
                    "Helper mode active",
                    NotificationManager.IMPORTANCE_LOW
            );

            foregroundChannel.setDescription(
                    "Shows while you're available to help nearby"
            );

            manager.createNotificationChannel(foregroundChannel);

            NotificationChannel alertChannel = new NotificationChannel(
                    ALERT_CHANNEL_ID,
                    "SOS Nearby Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );

            alertChannel.setDescription(
                    "Alerts when someone nearby triggers SOS"
            );

            manager.createNotificationChannel(alertChannel);
        }
    }

    private double haversineDistanceMeters(
            double lat1,
            double lon1,
            double lat2,
            double lon2
    ) {

        double R = 6371000;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}