package com.example.rungirlrun.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.telephony.SmsManager;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.core.content.ContextCompat;

import com.example.rungirlrun.services.LiveLocationService;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.example.rungirlrun.dao.ContactDAO;
import com.example.rungirlrun.models.Contact;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class SOSManager {

    public static final int SOS_PERMISSION_REQUEST = 500;

    private final Activity activity;
    private final FusedLocationProviderClient fusedLocationClient;
    private final ContactDAO contactDAO;

    public SOSManager(Activity activity) {
        this.activity = activity;

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(activity);

        contactDAO = new ContactDAO(activity);
    }

    public void triggerSOS() {

        FirebaseUser currentUser =
                FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    activity,
                    "Please log in first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        ArrayList<Contact> contacts =
                contactDAO.getContactsByUser(currentUser.getUid());

        if (contacts == null || contacts.isEmpty()) {

            Toast.makeText(
                    activity,
                    "Please add at least one emergency contact first.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (!hasRequiredPermissions()) {

            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{
                            Manifest.permission.SEND_SMS,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    SOS_PERMISSION_REQUEST
            );

            return;
        }

        getLocationAndSendSOS(contacts);
    }

    private boolean hasRequiredPermissions() {

        boolean smsGranted =
                ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.SEND_SMS
                ) == PackageManager.PERMISSION_GRANTED;

        boolean fineLocationGranted =
                ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseLocationGranted =
                ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        return smsGranted &&
                (fineLocationGranted || coarseLocationGranted);
    }

    private void getLocationAndSendSOS(
            ArrayList<Contact> contacts
    ) {

        if (
                ActivityCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
                        &&
                        ActivityCompat.checkSelfPermission(
                                activity,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
        ) {
            return;
        }

        fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        null
                )
                .addOnSuccessListener(location -> {

                    if (location != null) {

                        createLiveTrackingSession(
                                contacts,
                                location
                        );

                    } else {

                        Toast.makeText(
                                activity,
                                "Unable to determine your location. Make sure Location is turned on and try again.",
                                Toast.LENGTH_LONG
                        ).show();
                    }

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            activity,
                            "Location error: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            activity,
                            "Unable to get location.",
                            Toast.LENGTH_SHORT
                    ).show();

                });
    }
    private void createLiveTrackingSession(
            ArrayList<Contact> contacts,
            @NonNull Location location
    ) {

        FirebaseUser currentUser =
                FirebaseAuth.getInstance()
                        .getCurrentUser();

        if (currentUser == null) {
            return;
        }

        String sessionId =
                UUID.randomUUID().toString();

        Map<String, Object> session =
                new HashMap<>();

        session.put(
                "ownerId",
                currentUser.getUid()
        );

        session.put(
                "latitude",
                location.getLatitude()
        );

        session.put(
                "longitude",
                location.getLongitude()
        );

        session.put(
                "accuracy",
                location.getAccuracy()
        );

        session.put(
                "active",
                true
        );

        session.put(
                "startedAt",
                FieldValue.serverTimestamp()
        );

        session.put(
                "updatedAt",
                FieldValue.serverTimestamp()
        );

        FirebaseFirestore
                .getInstance()
                .collection("liveTracking")
                .document(sessionId)
                .set(session)
                .addOnSuccessListener(unused -> {

                    startLiveLocationService(
                            sessionId
                    );

                    sendLiveTrackingMessages(
                            contacts,
                            sessionId
                    );

                    launchSosStatusActivity(
                            sessionId,
                            location
                    );

                })
                .addOnFailureListener(e -> {

                    Log.e(
                            "LIVE_TRACKING",
                            "Failed to create tracking session",
                            e
                    );

                    Toast.makeText(
                            activity,
                            "Live tracking error: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                });
    }
    private void startLiveLocationService(
            String sessionId
    ) {

        Intent intent =
                new Intent(
                        activity,
                        LiveLocationService.class
                );

        intent.setAction(
                LiveLocationService.ACTION_START
        );

        intent.putExtra(
                LiveLocationService.EXTRA_SESSION_ID,
                sessionId
        );

        ContextCompat.startForegroundService(
                activity,
                intent
        );
    }
    private void launchSosStatusActivity(
            String sessionId,
            Location location
    ) {

        Intent intent =
                new Intent(
                        activity,
                        com.example.rungirlrun.activities.SosStatusActivity.class
                );

        intent.putExtra(
                com.example.rungirlrun.activities.SosStatusActivity.EXTRA_SESSION_ID,
                sessionId
        );

        intent.putExtra(
                com.example.rungirlrun.activities.SosStatusActivity.EXTRA_LATITUDE,
                location.getLatitude()
        );

        intent.putExtra(
                com.example.rungirlrun.activities.SosStatusActivity.EXTRA_LONGITUDE,
                location.getLongitude()
        );

        activity.startActivity(intent);
    }
    private void sendLiveTrackingMessages(
            ArrayList<Contact> contacts,
            String sessionId
    ) {

        String trackingLink =
                "https://rungirlrun-app.web.app/"
                        + "track.html?id="
                        + sessionId;

        String message =
                "EMERGENCY SOS!\n\n"
                        + "I may be in danger and need help.\n\n"
                        + "Track my LIVE location here:\n"
                        + trackingLink
                        + "\n\n"
                        + "My location will update automatically "
                        + "while SOS is active.\n\n"
                        + "Sent from RunGirlRun.";

        for (int i = 0; i < contacts.size(); i++) {

            Contact contact =
                    contacts.get(i);

            String phone =
                    contact.getPhone();

            if (
                    phone == null ||
                            phone.trim().isEmpty()
            ) {
                continue;
            }

            sendSmsToContact(
                    phone.trim(),
                    message,
                    i
            );
        }
    }

    private void sendMessages(
            ArrayList<Contact> contacts,
            @NonNull Location location
    ) {

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        String mapsLink =
                "https://maps.google.com/?q="
                        + latitude
                        + ","
                        + longitude;

        String message =
                "EMERGENCY SOS!\n\n"
                        + "I may be in danger and need help.\n\n"
                        + "My current location:\n"
                        + mapsLink
                        + "\n\n"
                        + "Sent from RunGirlRun.";

        for (int i = 0; i < contacts.size(); i++) {

            Contact contact = contacts.get(i);

            String phone = contact.getPhone();

            if (phone == null || phone.trim().isEmpty()) {
                continue;
            }

            phone = phone.trim();

            sendSmsToContact(
                    phone,
                    message,
                    i
            );
        }
    }

    private void sendSmsToContact(
            String phone,
            String message,
            int contactIndex
    ) {

        try {

            String SENT_ACTION =
                    "SOS_SMS_SENT_" + contactIndex;

            Intent sentIntent =
                    new Intent(SENT_ACTION);

            PendingIntent sentPendingIntent =
                    PendingIntent.getBroadcast(
                            activity,
                            contactIndex,
                            sentIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                                    | PendingIntent.FLAG_IMMUTABLE
                    );

            BroadcastReceiver sentReceiver =
                    new BroadcastReceiver() {

                        @Override
                        public void onReceive(
                                Context context,
                                Intent intent
                        ) {

                            if (getResultCode()
                                    == Activity.RESULT_OK) {

                                android.util.Log.d(
                                        "SOS_SMS",
                                        "SUCCESS: " + phone
                                );

                                Toast.makeText(
                                        activity,
                                        "SOS sent to " + phone,
                                        Toast.LENGTH_SHORT
                                ).show();

                            } else {

                                android.util.Log.e(
                                        "SOS_SMS",
                                        "FAILED: "
                                                + phone
                                                + " result="
                                                + getResultCode()
                                );

                                Toast.makeText(
                                        activity,
                                        "SMS failed for " + phone,
                                        Toast.LENGTH_LONG
                                ).show();
                            }

                            try {
                                activity.unregisterReceiver(this);
                            } catch (Exception ignored) {
                            }
                        }
                    };

            IntentFilter filter =
                    new IntentFilter(SENT_ACTION);

            if (android.os.Build.VERSION.SDK_INT >= 33) {

                activity.registerReceiver(
                        sentReceiver,
                        filter,
                        Context.RECEIVER_NOT_EXPORTED
                );

            } else {

                activity.registerReceiver(
                        sentReceiver,
                        filter
                );
            }

            SmsManager smsManager =
                    SmsManager.getDefault();

            ArrayList<String> parts =
                    smsManager.divideMessage(message);

            ArrayList<PendingIntent> sentIntents =
                    new ArrayList<>();

            for (int j = 0; j < parts.size(); j++) {
                sentIntents.add(sentPendingIntent);
            }

            smsManager.sendMultipartTextMessage(
                    phone,
                    null,
                    parts,
                    sentIntents,
                    null
            );

            android.util.Log.d(
                    "SOS_SMS",
                    "Requested SMS to: " + phone
            );

        } catch (Exception e) {

            android.util.Log.e(
                    "SOS_SMS",
                    "Exception sending to " + phone,
                    e
            );
        }
    }
}
