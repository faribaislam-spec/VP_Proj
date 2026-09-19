package com.example.rungirlrun.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import com.example.rungirlrun.R;
import androidx.appcompat.app.AppCompatActivity;
import com.example.rungirlrun.utils.SOSManager;
import com.example.rungirlrun.dao.ContactDAO;
import com.example.rungirlrun.logic.DataSeeder;
import com.google.firebase.auth.FirebaseAuth;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {

    private TextView welcomeText;
    private TextView emailText;

    private Button menuButton;
    private Button sosButton;
    private Button contactsButton;
    private Button mapButton;
    private Button safetyTipsButton;
    private Button reportButton;
    private Button cameraButton;

    private FirebaseAuth auth;
    private String userId;
    private SOSManager sosManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth = FirebaseAuth.getInstance();

        welcomeText = findViewById(R.id.welcomeText);
        emailText = findViewById(R.id.emailText);

        menuButton = findViewById(R.id.menuButton);
        sosButton = findViewById(R.id.sosButton);
        contactsButton = findViewById(R.id.contactsButton);
        mapButton = findViewById(R.id.mapButton);
        safetyTipsButton = findViewById(R.id.safetyTipsButton);
        reportButton = findViewById(R.id.reportButton);
        cameraButton = findViewById(R.id.cameraButton);
        sosManager = new SOSManager(this);

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            returnToLogin();
            return;
        }

        userId = getIntent().getStringExtra("userId");

        if (userId == null || userId.isEmpty()) {
            userId = currentUser.getUid();
        }

        String email = currentUser.getEmail();

        welcomeText.setText("Welcome");

        if (email != null) {
            emailText.setText(email);
        }

        menuButton.setOnClickListener(v -> showProfileMenu());

        sosButton.setOnClickListener(v -> {

            sosManager.triggerSOS();

        });

        contactsButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ContactsActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        mapButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            HomeActivity.this,
                            RouteSelectionActivity.class
                    );

            startActivity(intent);
        });

        safetyTipsButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SafetyTipsActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        reportButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ReportIncidentActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        cameraButton.setOnClickListener(v -> {

            Intent intent = new Intent(
                    HomeActivity.this,
                    MagnetometerActivity.class
            );

            startActivity(intent);
        });

        // ==== TEMPORARY: seeds IUT-area locations/roads into Firestore ====
        // Run once, confirm success in Logcat (tag "SEED"), then DELETE this block.
        /*DataSeeder.seedIUTAreaData(FirebaseFirestore.getInstance(), new DataSeeder.OnSeedCompleteListener() {
            @Override
            public void onSuccess() {
                Log.d("SEED", "IUT area data seeded successfully");
            }

            @Override
            public void onError(Exception e) {
                Log.e("SEED", "Seeding failed: ", e);
            }
        });*/
        // ==== END TEMPORARY ====

    }
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (
                requestCode ==
                        SOSManager.SOS_PERMISSION_REQUEST
        ) {

            sosManager.triggerSOS();
        }
    }

    private void showProfileMenu() {
        PopupMenu popupMenu = new PopupMenu(this, menuButton);

        popupMenu.getMenu().add("My Profile");
        popupMenu.getMenu().add("Settings");
        popupMenu.getMenu().add("Logout");

        popupMenu.setOnMenuItemClickListener(item -> {

            String selectedItem = item.getTitle().toString();

            if (selectedItem.equals("My Profile")) {

                Intent intent = new Intent(HomeActivity.this,ProfileActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);

                return true;
            }

            if (selectedItem.equals("Settings")) {

                Toast.makeText(
                        this,
                        "Settings page will be added next",
                        Toast.LENGTH_SHORT
                ).show();

                return true;
            }

            if (selectedItem.equals("Logout")) {

                auth.signOut();
                returnToLogin();

                return true;
            }

            return false;
        });

        popupMenu.show();
    }

    private void returnToLogin() {
        Intent intent = new Intent(
                HomeActivity.this,
                MainActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}