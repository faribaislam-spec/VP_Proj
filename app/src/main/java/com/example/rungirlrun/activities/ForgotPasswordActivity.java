package com.example.rungirlrun.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rungirlrun.R;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnReset;
    private TextView backToLogin;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        auth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        btnReset = findViewById(R.id.btnReset);
        backToLogin = findViewById(R.id.backToLogin);

        btnReset.setOnClickListener(v -> sendResetEmail());
        backToLogin.setOnClickListener(v -> finish());
    }

    private void sendResetEmail() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            return;
        }

        btnReset.setEnabled(false);

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    btnReset.setEnabled(true);

                    // Same message regardless of outcome — avoids leaking
                    // which emails are registered, which matters for a
                    // safety app like this one
                    Toast.makeText(
                            ForgotPasswordActivity.this,
                            "If an account exists for this email, a reset link has been sent.",
                            Toast.LENGTH_LONG
                    ).show();

                    finish();
                });
    }
}