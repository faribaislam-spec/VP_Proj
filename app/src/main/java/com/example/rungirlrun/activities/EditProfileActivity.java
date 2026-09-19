package com.example.rungirlrun.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.example.rungirlrun.R;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.rungirlrun.R;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;
    private String currentEmail;

    private ImageButton btnBack;
    private TextView tvAvatarInitial;
    private TextView tvUserId;
    private EditText etFullName;
    private TextView tvEmailReadOnly;
    private EditText etPhone;
    private Button btnSaveChanges;

    private View rowChangePasswordToggle;
    private TextView tvToggleArrow;
    private View layoutPasswordSection;
    private EditText etCurrentPassword;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private Button btnUpdatePassword;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bindViews();
        setupClickListeners();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        currentUserId = currentUser.getUid();
        currentEmail = currentUser.getEmail();
        tvUserId.setText(currentUserId);

        loadCurrentProfile();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        tvAvatarInitial = findViewById(R.id.tvAvatarInitial);
        tvUserId = findViewById(R.id.tvUserId);
        etFullName = findViewById(R.id.etFullName);
        tvEmailReadOnly = findViewById(R.id.tvEmailReadOnly);
        etPhone = findViewById(R.id.etPhone);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);

        rowChangePasswordToggle = findViewById(R.id.rowChangePasswordToggle);
        tvToggleArrow = findViewById(R.id.tvToggleArrow);
        layoutPasswordSection = findViewById(R.id.layoutPasswordSection);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSaveChanges.setOnClickListener(v -> saveProfileChanges());

        rowChangePasswordToggle.setOnClickListener(v -> togglePasswordSection());

        btnUpdatePassword.setOnClickListener(v -> updatePassword());
    }

    private void togglePasswordSection() {
        boolean isVisible = layoutPasswordSection.getVisibility() == View.VISIBLE;
        layoutPasswordSection.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        tvToggleArrow.setText(isVisible ? "▾" : "▴");
    }

    /** Loads fullName and phone so the fields aren't blank when the screen opens. */
    private void loadCurrentProfile() {
        tvEmailReadOnly.setText(TextUtils.isEmpty(currentEmail) ? "—" : currentEmail);

        db.collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener((DocumentSnapshot document) -> {
                    if (document != null && document.exists()) {
                        String fullName = document.getString("fullName");
                        String phone = document.getString("phone");

                        etFullName.setText(fullName);
                        etPhone.setText(phone);

                        String initial = (fullName != null && !fullName.trim().isEmpty())
                                ? String.valueOf(fullName.trim().charAt(0)).toUpperCase()
                                : "U";
                        tvAvatarInitial.setText(initial);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Couldn't load current profile: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void saveProfileChanges() {
        String fullName = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Name can't be empty");
            etFullName.requestFocus();
            return;
        }

        btnSaveChanges.setEnabled(false);
        btnSaveChanges.setText("Saving...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("phone", phone);

        db.collection("users")
                .document(currentUserId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    btnSaveChanges.setEnabled(true);
                    btnSaveChanges.setText("Save Changes");
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSaveChanges.setEnabled(true);
                    btnSaveChanges.setText("Save Changes");
                    Toast.makeText(this,
                            "Couldn't save changes: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updatePassword() {
        String currentPassword = etCurrentPassword.getText().toString();
        String newPassword = etNewPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (TextUtils.isEmpty(currentPassword)) {
            etCurrentPassword.setError("Enter your current password");
            etCurrentPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(newPassword) || newPassword.length() < 6) {
            etNewPassword.setError("New password must be at least 6 characters");
            etNewPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords don't match");
            etConfirmPassword.requestFocus();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || TextUtils.isEmpty(currentEmail)) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnUpdatePassword.setEnabled(false);
        btnUpdatePassword.setText("Updating...");

        // Firebase requires a recent sign-in before letting you change the
        // password, so re-authenticate with the current password first.
        AuthCredential credential = EmailAuthProvider.getCredential(currentEmail, currentPassword);

        android.util.Log.d("EditProfileActivity", "Starting reauthenticate for " + currentEmail);

        user.reauthenticate(credential)
                .addOnSuccessListener(unused -> {
                    android.util.Log.d("EditProfileActivity", "Reauthenticate succeeded. Calling updatePassword...");
                    user.updatePassword(newPassword)
                            .addOnSuccessListener(unused2 -> {
                                android.util.Log.d("EditProfileActivity", "updatePassword() reported success.");
                                btnUpdatePassword.setEnabled(true);
                                btnUpdatePassword.setText("Update Password");
                                etCurrentPassword.setText("");
                                etNewPassword.setText("");
                                etConfirmPassword.setText("");
                                togglePasswordSection();

                                // Force a fresh sign-in with the new password. This also
                                // guarantees the change actually took effect server-side —
                                // if it didn't, the next login attempt will simply fail.
                                Toast.makeText(this,
                                        "Password updated. Please log in again with your new password.",
                                        Toast.LENGTH_LONG).show();
                                mAuth.signOut();
                                startActivity(new Intent(EditProfileActivity.this, MainActivity.class));
                                finishAffinity();
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("EditProfileActivity", "updatePassword() failed", e);
                                btnUpdatePassword.setEnabled(true);
                                btnUpdatePassword.setText("Update Password");
                                Toast.makeText(this,
                                        "Couldn't update password: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("EditProfileActivity", "Reauthenticate failed", e);
                    btnUpdatePassword.setEnabled(true);
                    btnUpdatePassword.setText("Update Password");
                    etCurrentPassword.setError("Incorrect current password");
                    etCurrentPassword.requestFocus();
                });
    }
}