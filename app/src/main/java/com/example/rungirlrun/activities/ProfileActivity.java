package com.example.rungirlrun.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.rungirlrun.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Source;

public class ProfileActivity extends AppCompatActivity {

    // ---- Firebase ----
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // ---- Views ----
    private ImageButton btnBack;
    private TextView tvAvatarInitial;
    private TextView tvFullName;
    private TextView tvEmailSubtitle;
    private TextView btnRetry;
    private TextView tvUserId; // hidden, kept only for internal reference
    private TextView tvFullNameValue;
    private TextView tvEmailValue;
    private TextView tvPhoneValue;
    private Button btnEditProfile;
    private Button btnLogout;

    // ---- State ----
    private String currentUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bindViews();
        setupClickListeners();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // No one is signed in — bounce back to the login screen.
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        currentUserId = currentUser.getUid();
        tvUserId.setText(currentUserId); // stored, never shown in the UI
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload every time this screen becomes visible — including when
        // returning from EditProfileActivity — so edits show up immediately.
        if (currentUserId != null) {
            loadUserProfile();
        }
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        tvAvatarInitial = findViewById(R.id.tvAvatarInitial);
        tvFullName = findViewById(R.id.tvFullName);
        tvEmailSubtitle = findViewById(R.id.tvEmailSubtitle);
        btnRetry = findViewById(R.id.btnRetry);
        tvUserId = findViewById(R.id.tvUserId);
        tvFullNameValue = findViewById(R.id.tvFullNameValue);
        tvEmailValue = findViewById(R.id.tvEmailValue);
        tvPhoneValue = findViewById(R.id.tvPhoneValue);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Edit Profile also covers changing the password — no separate button for it.
        btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class))
        );



        btnLogout.setOnClickListener(v -> confirmLogout());

        btnRetry.setOnClickListener(v -> {
            btnRetry.setVisibility(android.view.View.GONE);
            tvFullName.setText("Loading...");
            loadUserProfile();
        });
    }

    /** Loads fullName, email, and phone from the "users" collection for the signed-in user. */
    private void loadUserProfile() {
        db.collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot document) {
                        if (document != null && document.exists()) {
                            String fullName = document.getString("fullName");
                            String email = document.getString("email");
                            String phone = document.getString("phone");
                            populateUI(fullName, email, phone);
                        } else {
                            showLoadError("Profile not found.");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        // Log the REAL error so it's easy to find in Logcat
                        // (filter by tag "ProfileActivity").
                        android.util.Log.e("ProfileActivity", "Firestore load failed", e);

                        if (e instanceof FirebaseFirestoreException) {
                            FirebaseFirestoreException.Code code =
                                    ((FirebaseFirestoreException) e).getCode();

                            if (code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                                // This one really is a connectivity problem — try cache.
                                tryLoadFromCache();
                                return;
                            }

                            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                showLoadError("Permission denied — check your Firestore security rules.");
                                return;
                            }

                            showLoadError("Firestore error (" + code + "): " + e.getMessage());
                            return;
                        }

                        showLoadError("Couldn't load profile: " + e.getMessage());
                    }
                });
    }

    /** Only called for genuine connectivity failures (UNAVAILABLE) — falls back to cache. */
    private void tryLoadFromCache() {
        db.collection("users")
                .document(currentUserId)
                .get(Source.CACHE)
                .addOnSuccessListener(cachedDoc -> {
                    if (cachedDoc != null && cachedDoc.exists()) {
                        String fullName = cachedDoc.getString("fullName");
                        String email = cachedDoc.getString("email");
                        String phone = cachedDoc.getString("phone");
                        populateUI(fullName, email, phone);
                        Toast.makeText(ProfileActivity.this,
                                "Showing offline data. Pull to refresh once you're back online.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        showLoadError("You're offline and no saved profile was found.");
                    }
                })
                .addOnFailureListener(cacheError ->
                        showLoadError("You're offline. Check your connection."));
    }

    private void showLoadError(String message) {
        tvFullName.setText("Unable to load profile");
        tvEmailSubtitle.setText(message);
        btnRetry.setVisibility(android.view.View.VISIBLE);
    }

    private void populateUI(String fullName, String email, String phone) {
        String displayName = TextUtils.isEmpty(fullName) ? "RunGirlRun User" : fullName;
        String displayEmail = TextUtils.isEmpty(email) ? "" : email;
        String displayPhone = TextUtils.isEmpty(phone) ? "Not added" : phone;

        btnRetry.setVisibility(android.view.View.GONE);
        tvFullName.setText(displayName);
        tvEmailSubtitle.setText(displayEmail);
        tvFullNameValue.setText(displayName);
        tvEmailValue.setText(TextUtils.isEmpty(email) ? "Not added" : email);
        tvPhoneValue.setText(displayPhone);

        // Avatar initial from the first letter of the name
        String initial = displayName.trim().isEmpty()
                ? "U"
                : String.valueOf(displayName.trim().charAt(0)).toUpperCase();
        tvAvatarInitial.setText(initial);
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Log out")
                .setMessage("Are you sure you want to log out of RunGirlRun?")
                .setPositiveButton("Log out", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        mAuth.signOut();
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}