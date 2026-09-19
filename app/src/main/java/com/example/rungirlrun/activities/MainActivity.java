package com.example.rungirlrun.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import com.example.rungirlrun.R;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView createAccount, forgotPassword;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        createAccount = findViewById(R.id.createAccount);
        forgotPassword = findViewById(R.id.forgotPassword);

        loginButton.setOnClickListener(v -> loginUser());

        createAccount.setOnClickListener(v -> {
            Intent intent =
                    new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        forgotPassword.setOnClickListener(v ->
                Toast.makeText(this,
                        "Password reset page will be added next",
                        Toast.LENGTH_SHORT).show()
        );
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {

                    if (authResult.getUser() == null) {
                        Toast.makeText(
                                this,
                                "User information not found",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    String userId = authResult.getUser().getUid();

                    Intent intent = new Intent(
                            MainActivity.this,
                            HomeActivity.class
                    );

                    intent.putExtra("userId", userId);

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Login failed: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }
}