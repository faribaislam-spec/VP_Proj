package com.example.rungirlrun.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.rungirlrun.R;
import com.example.rungirlrun.utils.TokenManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class RunGirlRunMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "SOS_NEARBY_ALERTS";
    private static final int NOTIFICATION_ID = 3001;

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        TokenManager.saveToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> data = remoteMessage.getData();

        if (data.isEmpty()) {
            return;
        }

        String title = data.get("title");
        String body = data.get("body");
        String trackingLink = data.get("trackingLink");

        if (title == null) {
            title = "SOS Alert";
        }

        if (body == null) {
            body = "Someone nearby may need help.";
        }

        showNotification(title, body, trackingLink);
    }

    private void showNotification(
            String title,
            String body,
            String trackingLink
    ) {

        createNotificationChannel();

        Intent intent;

        if (trackingLink != null && !trackingLink.isEmpty()) {
            intent = new Intent(
                    Intent.ACTION_VIEW,
                    android.net.Uri.parse(trackingLink)
            );
        } else {
            intent = getPackageManager()
                    .getLaunchIntentForPackage(getPackageName());
        }

        if (intent != null) {
            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        NotificationManager manager =
                (NotificationManager) getSystemService(
                        Context.NOTIFICATION_SERVICE
                );

        manager.notify(NOTIFICATION_ID, builder.build());
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SOS Nearby Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription(
                    "Alerts when someone nearby triggers SOS"
            );

            NotificationManager manager =
                    (NotificationManager) getSystemService(
                            Context.NOTIFICATION_SERVICE
                    );

            manager.createNotificationChannel(channel);
        }
    }
}
