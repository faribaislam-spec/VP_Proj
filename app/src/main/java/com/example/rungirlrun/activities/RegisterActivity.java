package com.example.rungirlrun.activities;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.rungirlrun.R;
import androidx.appcompat.app.AppCompatActivity;
import com.example.rungirlrun.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button registerButton;
    private TextView backToLoginText;
    private FirebaseAuth auth;
    private EditText phoneEditText;
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();

        phoneEditText = findViewById(R.id.phoneEditText);

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.registerEmailEditText);
        passwordEditText = findViewById(R.id.registerPasswordEditText);
        confirmPasswordEditText =
                findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
        backToLoginText = findViewById(R.id.backToLoginText);

        registerButton.setOnClickListener(v -> registerUser());

        backToLoginText.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword =
                confirmPasswordEditText.getText().toString().trim();

        if (name.isEmpty()) {
            nameEditText.setError("Name is required");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email");
            return;
        }

        if (phone.isEmpty()) {
            phoneEditText.setError("Phone number is required");
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError(
                    "Password must contain at least 6 characters"
            );
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError(
                    "Passwords do not match"
            );
            return;
        }

        registerButton.setEnabled(false);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();

                    if (firebaseUser == null) {
                        registerButton.setEnabled(true);
                        Toast.makeText(
                                this,
                                "Could not create user",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    String userId = firebaseUser.getUid();

                    User user = new User(
                            userId,
                            name,
                            email,
                            phone
                    );

                    database.collection("users")
                            .document(userId)
                            .set(user)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(
                                        this,
                                        "Account created successfully",
                                        Toast.LENGTH_SHORT
                                ).show();

                                finish();
                            })
                            .addOnFailureListener(e -> {
                                registerButton.setEnabled(true);

                                Toast.makeText(
                                        this,
                                        "Profile could not be saved: "
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                })
                .addOnFailureListener(e -> {
                    registerButton.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Registration failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

}