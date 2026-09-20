package com.example.rungirlrun.utils;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class TokenManager {

    private static final String TAG = "TokenManager";

    /**
     * Call this after a successful login, and also whenever onNewToken()
     * fires in your messaging service. It fetches the current device's
     * FCM token and saves it onto the logged-in user's Firestore document.
     */
    public static void saveTokenForCurrentUser() {

        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null) {
            Log.w(TAG, "No logged-in user; skipping token save.");
            return;
        }

        FirebaseMessaging.getInstance()
                .getToken()
                .addOnSuccessListener(token -> saveToken(uid, token))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to fetch FCM token", e)
                );
    }

    /**
     * Call this directly from onNewToken(String token) in your
     * FirebaseMessagingService, since that callback already gives you
     * the token without needing to fetch it again.
     */
    public static void saveToken(String uid, String token) {

        if (uid == null || token == null) {
            return;
        }

        Map<String, Object> update = new HashMap<>();
        update.put("fcmToken", token);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update(update)
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "FCM token saved for user " + uid)
                )
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to save FCM token", e)
                );
    }

    /**
     * Convenience overload for onNewToken(), which doesn't know the uid
     * itself — pulls it from FirebaseAuth.
     */
    public static void saveToken(String token) {
        String uid = FirebaseAuth.getInstance().getUid();
        saveToken(uid, token);
    }
}
