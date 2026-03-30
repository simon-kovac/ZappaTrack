package com.example.zappatrack.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.zappatrack.AuthSession;
import com.example.zappatrack.screens.HomeActivity;
import com.example.zappatrack.R;
import com.example.zappatrack.SupabaseAuthManager;

public class LoginActivity extends AppCompatActivity {

    // UI Components
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText nameEditText;
    private Button loginButton;
    private Button registerButton;
    private TextView toggleTextView;
    private ProgressBar progressBar;
    private View nameContainer;

    // Authentication manager
    private SupabaseAuthManager authManager;

    // Track current mode
    private boolean isRegistrationMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize auth manager
        authManager = SupabaseAuthManager.getInstance();
        // Initialize views
        initializeViews();

        // Set up click listeners
        setupClickListeners();

        // Check if already logged in
        if (authManager.isSignedIn()) {
            navigateToHome();
        }
    }

    private void initializeViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        nameEditText = findViewById(R.id.nameEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        toggleTextView = findViewById(R.id.toggleTextView);
        progressBar = findViewById(R.id.progressBar);
        nameContainer = findViewById(R.id.nameContainer);

        // Initially hide registration-specific views
        nameContainer.setVisibility(View.GONE);
        registerButton.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> handleLogin());
        registerButton.setOnClickListener(v -> handleRegister());
        toggleTextView.setOnClickListener(v -> toggleMode());
    }

    private void toggleMode() {
        isRegistrationMode = !isRegistrationMode;

        if (isRegistrationMode) {
            // Switch to registration mode
            nameContainer.setVisibility(View.VISIBLE);
            loginButton.setVisibility(View.GONE);
            registerButton.setVisibility(View.VISIBLE);
            toggleTextView.setText("Already have an account? Sign in");
        } else {
            // Switch to login mode
            nameContainer.setVisibility(View.GONE);
            loginButton.setVisibility(View.VISIBLE);
            registerButton.setVisibility(View.GONE);
            toggleTextView.setText("Don't have an account? Sign up");
        }

        // Clear password field when switching
        passwordEditText.setText("");
        passwordEditText.setError(null);
    }

    private void handleLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate input
        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            return;
        }

        // Show progress
        showProgress(true);

        // Perform login
        authManager.signIn(email, password, new SupabaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(AuthSession session) {
                showProgress(false);
                Toast.makeText(LoginActivity.this,
                        "Welcome back, " + (session.getDisplayName() != null ?
                                session.getDisplayName() : session.getEmail()),
                        Toast.LENGTH_SHORT).show();
                navigateToHome();
            }

            @Override
            public void onError(String error) {
                showProgress(false);

                if (error.equals("EMAIL_NOT_CONFIRMED")) {
                    // Email not verified
                    new AlertDialog.Builder(LoginActivity.this)
                            .setTitle("Email Not Verified")
                            .setMessage("Please check your email and click the verification link before signing in.\n\n" +
                                    "If you didn't receive the email, check your spam folder.")
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    // Other login errors
                    Toast.makeText(LoginActivity.this,
                            "Login failed: " + error,
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void handleRegister() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String name = nameEditText.getText().toString().trim();

        // Validate input
        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email");
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            return;
        }

        // Show progress
        showProgress(true);

        // Perform registration
        authManager.signUp(email, password, name, new SupabaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(AuthSession session) {
                showProgress(false);

                Toast.makeText(LoginActivity.this,
                        "Account created! Please sign in.",
                        Toast.LENGTH_SHORT).show();

                if (isRegistrationMode) {
                    toggleMode();
                }

                passwordEditText.setText("");
                emailEditText.setText(session.getEmail());
            }

            @Override
            public void onError(String error) {
                showProgress(false);

                // Handle specific error codes
                if (error.equals("VERIFICATION_REQUIRED")) {
                    // Show success dialog for email verification
                    new AlertDialog.Builder(LoginActivity.this)
                            .setTitle("✓ Account Created Successfully!")
                            .setMessage("Please check your email:\n" + email + "\n\n" +
                                    "Click the verification link in the email to activate your account.\n\n" +
                                    "After verifying, you can sign in here.")
                            .setPositiveButton("OK", (dialog, which) -> {
                                // Switch back to login mode
                                if (isRegistrationMode) {
                                    toggleMode();
                                }
                                // Pre-fill the email
                                emailEditText.setText(email);
                                passwordEditText.setText("");
                            })
                            .show();
                } else if (error.equals("EMAIL_ALREADY_EXISTS")) {
                    // Email is already registered
                    new AlertDialog.Builder(LoginActivity.this)
                            .setTitle("Email Already Registered")
                            .setMessage("This email is already associated with an account.\n\n" +
                                    "Please sign in with your existing account.")
                            .setPositiveButton("Sign In", (dialog, which) -> {
                                // Switch to login mode
                                if (isRegistrationMode) {
                                    toggleMode();
                                }
                                // Pre-fill the email
                                emailEditText.setText(email);
                                passwordEditText.setText("");
                                passwordEditText.requestFocus();
                            })
                            .setNegativeButton("Try Different Email", (dialog, which) -> {
                                emailEditText.setText("");
                                emailEditText.requestFocus();
                            })
                            .show();
                } else if (error.equals("WEAK_PASSWORD")) {
                    passwordEditText.setError("Password is too weak. Use at least 6 characters with mix of letters and numbers.");
                    passwordEditText.requestFocus();
                } else {
                    // Generic error
                    Toast.makeText(LoginActivity.this,
                            "Registration failed: " + error,
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }










    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!show);
        registerButton.setEnabled(!show);
        emailEditText.setEnabled(!show);
        passwordEditText.setEnabled(!show);
        nameEditText.setEnabled(!show);
        toggleTextView.setEnabled(!show);
    }

    private void clearFields() {
        emailEditText.setText("");
        passwordEditText.setText("");
        nameEditText.setText("");
        emailEditText.setError(null);
        passwordEditText.setError(null);
        nameEditText.setError(null);
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}