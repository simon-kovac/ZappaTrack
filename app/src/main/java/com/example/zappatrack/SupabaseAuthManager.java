package com.example.zappatrack;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupabaseAuthManager {
    private static final String TAG = "SupabaseAuth";

    // SINGLETON INSTANCE - This ensures only one instance exists
    private static SupabaseAuthManager instance = null;

    // Your Supabase credentials
    private static final String SUPABASE_URL = "https://sdzcakkewwwtpwiobrhi.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNkemNha2tld3d3dHB3aW9icmhpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA2MTAxMTMsImV4cCI6MjA3NjE4NjExM30.Mn53rQ8aDKsG_ctEb4sGQX7CdVlGU6mKKoOzDawz28I";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();

    private AuthSession currentSession = null;

    // PRIVATE CONSTRUCTOR - Prevents direct instantiation
    private SupabaseAuthManager() {
        // Private constructor for singleton pattern
    }

    /**
     * Get the singleton instance of SupabaseAuthManager
     * This ensures all activities use the same instance
     */
    public static synchronized SupabaseAuthManager getInstance() {
        if (instance == null) {
            instance = new SupabaseAuthManager();
        }
        return instance;
    }

    /**
     * Callback interface for auth operations
     */
    public interface AuthCallback {
        void onSuccess(AuthSession session);
        void onError(String error);
    }

    /**
     * Check if user is signed in
     */
    public boolean isSignedIn() {
        boolean signedIn = currentSession != null && currentSession.getAccessToken() != null;
        Log.d(TAG, "isSignedIn check: " + signedIn + " (session: " + (currentSession != null ? "exists" : "null") + ")");
        return signedIn;
    }

    /**
     * Get current session
     */
    public AuthSession getCurrentSession() {
        return currentSession;
    }

    /**
     * Get current user's profile ID
     */
    public long getCurrentUserProfileId() {
        return currentSession != null ? currentSession.getProfileId() : -1;
    }

    /**
     * Sign in an existing user - now includes role fetching
     */
    public void signIn(String email, String password, AuthCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/auth/v1/token?grant_type=password");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("email", email);
                requestBody.addProperty("password", password);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = gson.toJson(requestBody).getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                BufferedReader reader;

                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == 200) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == 200) {
                    AuthResponse authResponse = gson.fromJson(response.toString(), AuthResponse.class);

                    if (authResponse != null && authResponse.access_token != null) {
                        currentSession = new AuthSession(
                                authResponse.user.id,
                                authResponse.user.email,
                                authResponse.access_token,
                                authResponse.refresh_token
                        );

                        // Get user profile including role
                        getUserProfile(currentSession.getUserId(), new ProfileDataCallback() {
                            @Override
                            public void onSuccess(UserProfileData profileData) {
                                currentSession.setDisplayName(profileData.name);
                                currentSession.setProfileId(profileData.id);
                                currentSession.setRole(profileData.role);  // Set the role

                                Log.d(TAG, "User logged in with role: " + profileData.role);
                                mainHandler.post(() -> callback.onSuccess(currentSession));
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "Profile fetch failed: " + error);
                                mainHandler.post(() -> callback.onSuccess(currentSession));
                            }
                        });
                    } else {
                        mainHandler.post(() -> callback.onError("Invalid credentials"));
                    }
                } else {
                    mainHandler.post(() -> callback.onError("Invalid email or password"));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Sign in exception", e);
                mainHandler.post(() -> callback.onError("Exception: " + e.getMessage()));
            }
        });
    }

    /**
     * Sign out the current user
     */
    public void signOut(AuthCallback callback) {
        executor.execute(() -> {
            try {
                if (currentSession == null || currentSession.getAccessToken() == null) {
                    mainHandler.post(() -> callback.onError("No active session"));
                    return;
                }

                URL url = new URL(SUPABASE_URL + "/auth/v1/logout");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + currentSession.getAccessToken());
                conn.setRequestProperty("Content-Type", "application/json");

                int responseCode = conn.getResponseCode();

                currentSession = null;

                if (responseCode == HttpURLConnection.HTTP_NO_CONTENT ||
                        responseCode == HttpURLConnection.HTTP_OK ||
                        responseCode == 204) {
                    mainHandler.post(() -> callback.onSuccess(null));
                } else {
                    mainHandler.post(() -> callback.onError("Sign out failed with code: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                currentSession = null;
                Log.e(TAG, "Sign out exception", e);
                mainHandler.post(() -> callback.onSuccess(null));
            }
        });
    }

    /**
     * Sign up a new user
     */
    public void signUp(String email, String password, String displayName, AuthCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/auth/v1/signup");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("email", email);
                requestBody.addProperty("password", password);

                JsonObject userData = new JsonObject();
                userData.addProperty("name", displayName);
                requestBody.add("data", userData);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = gson.toJson(requestBody).getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                BufferedReader reader;

                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == 200) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Log.d(TAG, "SignUp Response Code: " + responseCode);
                Log.d(TAG, "SignUp Response Body: " + response.toString());

                // Check for error responses first
                if (responseCode == 400 || responseCode == 422) {
                    // Bad request - usually means validation error
                    String responseStr = response.toString().toLowerCase();

                    if (responseStr.contains("already registered") ||
                            responseStr.contains("already exists") ||
                            responseStr.contains("duplicate") ||
                            responseStr.contains("user already registered")) {
                        // Email already exists
                        mainHandler.post(() -> callback.onError("EMAIL_ALREADY_EXISTS"));
                    } else if (responseStr.contains("weak password") ||
                            responseStr.contains("password")) {
                        // Weak password
                        mainHandler.post(() -> callback.onError("WEAK_PASSWORD"));
                    } else {
                        // Other validation error
                        mainHandler.post(() -> callback.onError("Sign up failed: Invalid input"));
                    }
                    return;
                }

                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == 200) {
                    AuthResponse authResponse = gson.fromJson(response.toString(), AuthResponse.class);

                    if (authResponse != null) {
                        // Check if we got a user object
                        if (authResponse.user != null) {
                            // Check if email verification is required
                            if (authResponse.access_token == null) {
                                // User created but needs email verification
                                mainHandler.post(() -> callback.onError("VERIFICATION_REQUIRED"));
                            } else {
                                // Email verification disabled, user created and logged in
                                currentSession = new AuthSession(
                                        authResponse.user.id,
                                        authResponse.user.email,
                                        authResponse.access_token,
                                        authResponse.refresh_token
                                );

                                // Create user profile
                                createUserProfile(currentSession.getUserId(), displayName, email,
                                        new ProfileCallback() {
                                            @Override
                                            public void onSuccess(long profileId) {
                                                currentSession.setDisplayName(displayName);
                                                currentSession.setProfileId(profileId);
                                                currentSession.setRole("user"); // New users are always regular users
                                                mainHandler.post(() -> callback.onSuccess(currentSession));
                                            }

                                            @Override
                                            public void onError(String error) {
                                                Log.e(TAG, "Profile creation failed: " + error);
                                                // Still treat as success since auth user was created
                                                mainHandler.post(() -> callback.onSuccess(currentSession));
                                            }
                                        });
                            }
                        } else {
                            // No user object returned - something went wrong
                            mainHandler.post(() -> callback.onError("Sign up failed: No user created"));
                        }
                    } else {
                        mainHandler.post(() -> callback.onError("Invalid response format"));
                    }
                } else {
                    // Other error codes
                    mainHandler.post(() -> callback.onError("Sign up failed with code: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Sign up exception", e);
                mainHandler.post(() -> callback.onError("Exception: " + e.getMessage()));
            }
        });
    }

    /**
     * Promote a user to admin (only admins can do this)
     */
    public void promoteToAdmin(String userEmail, SimpleCallback callback) {
        if (currentSession == null || !currentSession.isAdmin()) {
            callback.onError("Only admins can promote users");
            return;
        }

        executor.execute(() -> {
            try {
                // Call the promote_to_admin function
                URL url = new URL(SUPABASE_URL + "/rest/v1/rpc/promote_to_admin");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + currentSession.getAccessToken());
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("user_email", userEmail);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = gson.toJson(requestBody).getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == 200) {
                    mainHandler.post(() -> callback.onSuccess());
                } else {
                    mainHandler.post(() -> callback.onError("Failed to promote user"));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Promote exception", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Demote an admin to regular user (only admins can do this)
     */
    public void demoteToUser(String userEmail, SimpleCallback callback) {
        if (currentSession == null || !currentSession.isAdmin()) {
            callback.onError("Only admins can demote users");
            return;
        }

        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/rpc/demote_to_user");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + currentSession.getAccessToken());
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("user_email", userEmail);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = gson.toJson(requestBody).getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == 200) {
                    mainHandler.post(() -> callback.onSuccess());
                } else {
                    mainHandler.post(() -> callback.onError("Failed to demote user"));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Demote exception", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Get user profile from database - now includes role
     */
    private void getUserProfile(String userId, ProfileDataCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/user_profiles?auth_id=eq." + userId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + currentSession.getAccessToken());

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    UserProfileData[] profiles = gson.fromJson(response.toString(), UserProfileData[].class);
                    if (profiles != null && profiles.length > 0) {
                        callback.onSuccess(profiles[0]);
                    } else {
                        callback.onError("Profile not found");
                    }
                } else {
                    callback.onError("Failed to get profile: " + responseCode);
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Get profile exception", e);
                callback.onError(e.getMessage());
            }
        });
    }

    /**
     * Create user profile in database
     */
    private void createUserProfile(String userId, String name, String email, ProfileCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/user_profiles");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + (currentSession != null ?
                        currentSession.getAccessToken() : SUPABASE_KEY));
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Prefer", "return=representation");
                conn.setDoOutput(true);

                JsonObject profile = new JsonObject();
                profile.addProperty("auth_id", userId);
                profile.addProperty("name", name);
                profile.addProperty("email", email);
                profile.addProperty("role", "user"); // Default role

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = gson.toJson(profile).getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == 201) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    try {
                        UserProfileData[] profiles = gson.fromJson(response.toString(), UserProfileData[].class);
                        if (profiles != null && profiles.length > 0) {
                            callback.onSuccess(profiles[0].id);
                        } else {
                            callback.onSuccess(1);
                        }
                    } catch (Exception e) {
                        callback.onSuccess(1);
                    }
                } else {
                    callback.onError("Failed to create profile: " + responseCode);
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Create profile exception", e);
                callback.onError(e.getMessage());
            }
        });
    }

    // Callback interfaces
    public interface SimpleCallback {
        void onSuccess();
        void onError(String error);
    }

    private interface ProfileDataCallback {
        void onSuccess(UserProfileData profileData);
        void onError(String error);
    }

    private interface ProfileCallback {
        void onSuccess(long profileId);
        void onError(String error);
    }

    // Response classes
    private static class AuthResponse {
        String access_token;
        String refresh_token;
        User user;
    }

    private static class User {
        String id;
        String email;
    }

    private static class UserProfileData {
        long id;
        String name;
        String email;
        String auth_id;
        String role;  // Added role field
    }
}