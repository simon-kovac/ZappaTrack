package com.example.zappatrack;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.example.zappatrack.databaseitems.Lighter;
import com.example.zappatrack.databaseitems.Comment;
import com.example.zappatrack.databaseitems.UserProfile;
import com.example.zappatrack.databaseitems.LighterImage;
import com.example.zappatrack.databaseitems.OwnershipHistory;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Supabase API client for database operations
 */
public class SupabaseClient {
    private static final String TAG = "SupabaseClient";

    // Supabase configuration
    private static final String SUPABASE_URL = "https://sdzcakkewwwtpwiobrhi.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNkemNha2tld3d3dHB3aW9icmhpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA2MTAxMTMsImV4cCI6MjA3NjE4NjExM30.Mn53rQ8aDKsG_ctEb4sGQX7CdVlGU6mKKoOzDawz28I";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();

    private String authToken = null;

    // Callback interfaces
    public interface DataCallback {
        void onSuccess(Object data);
        void onError(String error);
    }

    public interface SingleItemCallback {
        void onSuccess(Object item);
        void onError(String error);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String error);
    }

    /**
     * Set auth token for authenticated requests
     */
    public void setAuthToken(String token) {
        this.authToken = token;
    }

    /**
     * Get all lighters
     */
    public void getLighters(DataCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/lighters?select=*");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                if (authToken != null) {
                    conn.setRequestProperty("Authorization", "Bearer " + authToken);
                }

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

                    Lighter[] lighters = gson.fromJson(response.toString(), Lighter[].class);
                    List<Lighter> lighterList = Arrays.asList(lighters != null ? lighters : new Lighter[0]);

                    mainHandler.post(() -> callback.onSuccess(lighterList));
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error fetching lighters", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Get lighter by ID
     */
    public void getLighterById(long lighterId, SingleItemCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/lighters?id=eq." + lighterId + "&select=*");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);

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

                    Lighter[] lighters = gson.fromJson(response.toString(), Lighter[].class);

                    if (lighters != null && lighters.length > 0) {
                        mainHandler.post(() -> callback.onSuccess(lighters[0]));
                    } else {
                        mainHandler.post(() -> callback.onError("Lighter not found"));
                    }
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error getting lighter", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Get lighters owned by a specific user
     */
    public void getUserLighters(long userId, DataCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/lighters?owner=eq." + userId + "&select=*");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);

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

                    Lighter[] lighters = gson.fromJson(response.toString(), Lighter[].class);
                    List<Lighter> lighterList = Arrays.asList(lighters != null ? lighters : new Lighter[0]);

                    mainHandler.post(() -> callback.onSuccess(lighterList));
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error fetching user lighters", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Update lighter details
     */
    public void updateLighter(long lighterId, String name, String description, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/lighters?id=eq." + lighterId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("PATCH");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JsonObject updateData = new JsonObject();
                updateData.addProperty("name", name);
                updateData.addProperty("description", description);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = gson.toJson(updateData).getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == 204) {
                    mainHandler.post(() -> callback.onSuccess());
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error updating lighter", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Delete a lighter
     */
    public void deleteLighter(long lighterId, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/lighters?id=eq." + lighterId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_NO_CONTENT || responseCode == 204) {
                    mainHandler.post(() -> callback.onSuccess());
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error deleting lighter", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void createLighterWithResponse(String name, String description, String imageUrl, long ownerId, SingleItemCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/lighters");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Prefer", "return=representation"); // This makes it return the created object
                conn.setDoOutput(true);

                JsonObject lighterData = new JsonObject();
                lighterData.addProperty("name", name);
                lighterData.addProperty("description", description);
                lighterData.addProperty("owner", ownerId);
                if (imageUrl != null) {
                    lighterData.addProperty("image_url", imageUrl);
                }

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = gson.toJson(lighterData).getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == 201) {
                    // Read the response to get the created lighter
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Parse the response as an array (Supabase returns array even for single insert)
                    Lighter[] lighters = gson.fromJson(response.toString(), Lighter[].class);
                    if (lighters != null && lighters.length > 0) {
                        Lighter createdLighter = lighters[0];
                        mainHandler.post(() -> callback.onSuccess(createdLighter));
                    } else {
                        mainHandler.post(() -> callback.onError("Failed to parse created lighter"));
                    }
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error creating lighter", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Get user profile
     */
    public void getUserProfile(long userId, SingleItemCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/user_profiles?id=eq." + userId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);

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

                    UserProfile[] profiles = gson.fromJson(response.toString(), UserProfile[].class);

                    if (profiles != null && profiles.length > 0) {
                        mainHandler.post(() -> callback.onSuccess(profiles[0]));
                    } else {
                        mainHandler.post(() -> callback.onError("Profile not found"));
                    }
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error fetching profile", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Update user bio
     */
    public void updateUserBio(long userId, String bio, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/user_profiles?id=eq." + userId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("PATCH");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JsonObject updateData = new JsonObject();
                updateData.addProperty("bio", bio);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = gson.toJson(updateData).getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == 204) {
                    mainHandler.post(() -> callback.onSuccess());
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error updating bio", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Get comments for a lighter
     */
    public void getLighterComments(long lighterId, DataCallback callback) {
        executor.execute(() -> {
            try {
                // Fetch comments and join with user_profiles to get names
                URL url = new URL(SUPABASE_URL + "/rest/v1/comments?belongs_to=eq." + lighterId +
                        "&select=*,user_profiles!author(id,name)&order=created_at.desc");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);

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

                    // Parse the response with nested user data
                    CommentWithUser[] commentsWithUsers = gson.fromJson(response.toString(), CommentWithUser[].class);

                    // Convert to regular comments and extract usernames
                    List<Comment> commentList = new ArrayList<>();
                    Map<Long, String> usernameMap = new HashMap<>();

                    if (commentsWithUsers != null) {
                        for (CommentWithUser cwu : commentsWithUsers) {
                            commentList.add(cwu.toComment());
                            if (cwu.user_profiles != null) {
                                usernameMap.put(cwu.author, cwu.user_profiles.name);
                            }
                        }
                    }

                    // Pass both comments and username map
                    mainHandler.post(() -> callback.onSuccess(new CommentsData(commentList, usernameMap)));
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error getting comments", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // Helper classes for parsing comments with user data
    private static class CommentWithUser {
        long id;
        long belongs_to;
        long author;
        String content;
        String created_at;
        UserProfileNested user_profiles;

        Comment toComment() {
            Comment comment = new Comment();
            comment.setId(id);
            comment.setBelongsTo(belongs_to);
            comment.setAuthor(author);
            comment.setContent(content);
            comment.setCreatedAt(created_at);
            return comment;
        }
    }

    // Data class to pass both comments and usernames
    public static class CommentsData {
        public final List<Comment> comments;
        public final Map<Long, String> usernames;

        public CommentsData(List<Comment> comments, Map<Long, String> usernames) {
            this.comments = comments;
            this.usernames = usernames;
        }
    }

    /**
     * Post a comment
     */
    public void postComment(long lighterId, long authorId, String content, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/comments");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JsonObject commentData = new JsonObject();
                commentData.addProperty("belongs_to", lighterId);
                commentData.addProperty("author", authorId);
                commentData.addProperty("content", content);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = gson.toJson(commentData).getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == 201) {
                    mainHandler.post(() -> callback.onSuccess());
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error posting comment", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Delete a comment
     */
    public void deleteComment(long commentId, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/comments?id=eq." + commentId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_NO_CONTENT || responseCode == 204) {
                    mainHandler.post(() -> callback.onSuccess());
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error deleting comment", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Upload profile photo (simplified - returns mock URL)
     */


    // Upload callback interface
    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onError(String error);
    }
    /**
     * Transfer lighter ownership using the database function
     */
    public void transferLighterOwnership(long lighterId, long newOwnerId, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                // Call the database function instead of directly updating
                URL url = new URL(SUPABASE_URL + "/rest/v1/rpc/transfer_lighter_ownership");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // Parameters for the function
                JsonObject params = new JsonObject();
                params.addProperty("p_lighter_id", lighterId);
                params.addProperty("p_new_owner_id", newOwnerId);
                params.addProperty("p_transfer_notes", "Ownership transferred via app");

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = gson.toJson(params).getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == 200) {
                    // Read the response to ensure it completed
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    Log.d(TAG, "Transfer response: " + response.toString());
                    mainHandler.post(() -> callback.onSuccess());
                } else {
                    // Read error response
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    reader.close();

                    Log.e(TAG, "Transfer failed: " + responseCode + " - " + errorResponse.toString());
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error transferring ownership", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Find user by username
     */
    public void getUserByUsername(String username, SingleItemCallback callback) {
        executor.execute(() -> {
            try {
                String encodedName = java.net.URLEncoder.encode(username, "UTF-8");
                URL url = new URL(SUPABASE_URL + "/rest/v1/user_profiles?name=eq." + encodedName);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);

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

                    UserProfile[] profiles = gson.fromJson(response.toString(), UserProfile[].class);

                    if (profiles != null && profiles.length > 0) {
                        mainHandler.post(() -> callback.onSuccess(profiles[0]));
                    } else {
                        mainHandler.post(() -> callback.onError("User not found"));
                    }
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error finding user", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
    /**
     * Search for lighters by name or description
     */
    public void searchLighters(String query, DataCallback callback) {
        executor.execute(() -> {
            try {
                // Use ilike for case-insensitive partial matching
                String searchPattern = "%" + query + "%";
                String encodedPattern = java.net.URLEncoder.encode(searchPattern, "UTF-8");

                // Search in both name and description fields
                URL url = new URL(SUPABASE_URL + "/rest/v1/lighters?or=(name.ilike." + encodedPattern +
                        ",description.ilike." + encodedPattern + ")&select=*");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);

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

                    Lighter[] lighters = gson.fromJson(response.toString(), Lighter[].class);
                    List<Lighter> lighterList = Arrays.asList(lighters != null ? lighters : new Lighter[0]);

                    mainHandler.post(() -> callback.onSuccess(lighterList));
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error searching lighters", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Search for users by name or bio
     */
    public void searchUsers(String query, DataCallback callback) {
        executor.execute(() -> {
            try {
                // Use ilike for case-insensitive partial matching
                String searchPattern = "%" + query + "%";
                String encodedPattern = java.net.URLEncoder.encode(searchPattern, "UTF-8");

                // Search in both name and bio fields
                URL url = new URL(SUPABASE_URL + "/rest/v1/user_profiles?or=(name.ilike." + encodedPattern +
                        ",bio.ilike." + encodedPattern + ")&select=*");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);

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

                    UserProfile[] users = gson.fromJson(response.toString(), UserProfile[].class);
                    List<UserProfile> userList = Arrays.asList(users != null ? users : new UserProfile[0]);

                    mainHandler.post(() -> callback.onSuccess(userList));
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error searching users", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
    private static class UserProfileNested {
        long id;
        String name;
    }

    /**
     * Upload image to Supabase Storage and save record
     */
    public void uploadLighterPhoto(long lighterId, String base64Image, String description,
                                   long uploadedBy, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                // Generate unique filename
                String fileName = "lighter_" + lighterId + "_" + System.currentTimeMillis() + ".jpg";

                // Decode base64 to bytes
                byte[] imageBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);

                // Upload to Supabase Storage
                URL url = new URL(SUPABASE_URL + "/storage/v1/object/lighter-images/" + fileName);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
                conn.setRequestProperty("Content-Type", "image/jpeg"); // Correct Content-Type
                conn.setRequestProperty("Content-Length", String.valueOf(imageBytes.length));
                conn.setDoOutput(true);
                conn.setFixedLengthStreamingMode(imageBytes.length);

                // Write image bytes
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(imageBytes);
                    os.flush();
                }

                int responseCode = conn.getResponseCode();

                if (responseCode == 200 || responseCode == 201) {
                    // Success! Now save to database
                    String imageUrl = SUPABASE_URL + "/storage/v1/object/public/lighter-images/" + fileName;

                    // Save record to lighter_images table
                    URL dbUrl = new URL(SUPABASE_URL + "/rest/v1/lighter_images");
                    HttpURLConnection dbConn = (HttpURLConnection) dbUrl.openConnection();

                    dbConn.setRequestMethod("POST");
                    dbConn.setRequestProperty("apikey", SUPABASE_KEY);
                    dbConn.setRequestProperty("Authorization", "Bearer " + authToken);
                    dbConn.setRequestProperty("Content-Type", "application/json");
                    dbConn.setDoOutput(true);

                    JsonObject imageData = new JsonObject();
                    imageData.addProperty("lighter_id", lighterId);
                    imageData.addProperty("image_url", imageUrl);
                    imageData.addProperty("uploaded_by", uploadedBy);
                    if (description != null && !description.isEmpty()) {
                        imageData.addProperty("description", description);
                    }

                    try (OutputStream os = dbConn.getOutputStream()) {
                        byte[] input = gson.toJson(imageData).getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }

                    int dbResponse = dbConn.getResponseCode();
                    dbConn.disconnect();

                    if (dbResponse == 201 || dbResponse == 200) {
                        mainHandler.post(() -> callback.onSuccess());
                    } else {
                        // Try to read error for debugging
                        BufferedReader errorReader = new BufferedReader(
                                new InputStreamReader(dbConn.getErrorStream()));
                        StringBuilder dbError = new StringBuilder();
                        String errorLine;
                        while ((errorLine = errorReader.readLine()) != null) {
                            dbError.append(errorLine);
                        }
                        errorReader.close();

                        Log.e(TAG, "Database save failed: " + dbResponse + " - " + dbError.toString());
                        // Still call success since image was uploaded
                        mainHandler.post(() -> callback.onSuccess());
                    }
                } else {
                    // Upload failed - read error
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getErrorStream()));
                    StringBuilder error = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line);
                    }
                    reader.close();

                    Log.e(TAG, "Upload failed: " + responseCode + " - " + error.toString());
                    mainHandler.post(() -> callback.onError("Upload failed: " + error.toString()));
                }

                conn.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Error uploading photo", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Get images for a lighter
     */

    public void getLighterImagesWithUploaders(long lighterId, DataCallback callback) {
        executor.execute(() -> {
            try {
                // Fetch images and join with user_profiles to get uploader names
                URL url = new URL(SUPABASE_URL + "/rest/v1/lighter_images?lighter_id=eq." +
                        lighterId + "&select=*,user_profiles!uploaded_by(id,name)" +
                        "&order=uploaded_at.desc");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);

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

                    // Parse response with nested user data
                    ImageWithUploader[] imagesWithUploaders = gson.fromJson(response.toString(),
                            ImageWithUploader[].class);

                    // Convert to regular images and extract usernames
                    List<LighterImage> imageList = new ArrayList<>();
                    Map<Long, String> uploaderMap = new HashMap<>();

                    if (imagesWithUploaders != null) {
                        for (ImageWithUploader iwu : imagesWithUploaders) {
                            imageList.add(iwu.toImage());
                            if (iwu.user_profiles != null) {
                                uploaderMap.put(iwu.uploaded_by, iwu.user_profiles.name);
                            }
                        }
                    }

                    // Pass both images and uploader names
                    mainHandler.post(() -> callback.onSuccess(new ImagesWithUploaders(imageList, uploaderMap)));
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error getting lighter images", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // Helper classes for parsing images with uploader data
    private static class ImageWithUploader {
        long id;
        long lighter_id;
        String image_url;
        String thumbnail_url;
        Long uploaded_by;
        String uploaded_at;
        boolean is_primary;
        String description;
        UserProfileNested user_profiles;

        LighterImage toImage() {
            LighterImage image = new LighterImage();
            image.setId(id);
            image.setLighterId(lighter_id);
            image.setImageUrl(image_url);
            image.setThumbnailUrl(thumbnail_url);
            image.setUploadedBy(uploaded_by != null ? uploaded_by : 0);
            image.setUploadedAt(uploaded_at);
            image.setPrimary(is_primary);
            image.setDescription(description);
            return image;
        }
    }

    // Data class to pass both images and uploader names
    public static class ImagesWithUploaders {
        public final List<LighterImage> images;
        public final Map<Long, String> uploaderNames;

        public ImagesWithUploaders(List<LighterImage> images, Map<Long, String> uploaderNames) {
            this.images = images;
            this.uploaderNames = uploaderNames;
        }
    }

    public void getLighterImages(long lighterId, DataCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/lighter_images?lighter_id=eq." +
                        lighterId + "&select=*&order=is_primary.desc,uploaded_at.desc");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);

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

                    LighterImage[] images = gson.fromJson(response.toString(), LighterImage[].class);
                    List<LighterImage> imageList = Arrays.asList(images != null ? images : new LighterImage[0]);

                    mainHandler.post(() -> callback.onSuccess(imageList));
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error getting lighter images", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Upload profile photo to storage
     */
    public void uploadProfilePhoto(long userId, String base64Image, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                String fileName = "profile_" + userId + "_" + System.currentTimeMillis() + ".jpg";

                // Upload to Supabase Storage
                URL storageUrl = new URL(SUPABASE_URL + "/storage/v1/object/profile-photos/" + fileName);
                HttpURLConnection storageConn = (HttpURLConnection) storageUrl.openConnection();

                storageConn.setRequestMethod("POST");
                storageConn.setRequestProperty("apikey", SUPABASE_KEY);
                storageConn.setRequestProperty("Authorization", "Bearer " + authToken);
                storageConn.setRequestProperty("Content-Type", "image/jpeg");
                storageConn.setDoOutput(true);

                // Decode base64 and upload
                byte[] imageBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                try (OutputStream os = storageConn.getOutputStream()) {
                    os.write(imageBytes);
                }

                int storageResponse = storageConn.getResponseCode();
                storageConn.disconnect();

                if (storageResponse == 200 || storageResponse == 201) {
                    // Get public URL
                    String imageUrl = SUPABASE_URL + "/storage/v1/object/public/profile-photos/" + fileName;

                    // Update user profile with new photo URL
                    URL dbUrl = new URL(SUPABASE_URL + "/rest/v1/user_profiles?id=eq." + userId);
                    HttpURLConnection dbConn = (HttpURLConnection) dbUrl.openConnection();

                    dbConn.setRequestMethod("PATCH");
                    dbConn.setRequestProperty("apikey", SUPABASE_KEY);
                    dbConn.setRequestProperty("Authorization", "Bearer " + authToken);
                    dbConn.setRequestProperty("Content-Type", "application/json");
                    dbConn.setDoOutput(true);

                    JsonObject updateData = new JsonObject();
                    updateData.addProperty("profile_photo_url", imageUrl);

                    try (OutputStream os = dbConn.getOutputStream()) {
                        byte[] input = gson.toJson(updateData).getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }

                    int dbResponse = dbConn.getResponseCode();
                    dbConn.disconnect();

                    if (dbResponse == 200 || dbResponse == 204) {
                        mainHandler.post(() -> callback.onSuccess());
                    } else {
                        mainHandler.post(() -> callback.onError("Failed to update profile"));
                    }
                } else {
                    mainHandler.post(() -> callback.onError("Failed to upload photo"));
                }

            } catch (Exception e) {
                Log.e(TAG, "Error uploading profile photo", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Delete a lighter image
     */
    public void deleteLighterImage(long imageId, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/lighter_images?id=eq." + imageId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_NO_CONTENT || responseCode == 204) {
                    mainHandler.post(() -> callback.onSuccess());
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error deleting image", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void getMostTransferredLighters(int limit, DataCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL +
                        "/rest/v1/ownership_history?select=lighter_id,lighters!lighter_id(id,name,owner,user_profiles!owner(id,name),lighter_images(image_url))");

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);

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

                    JsonArray historyArray = JsonParser.parseString(response.toString()).getAsJsonArray();


                    Map<Long, Integer> transferCounts = new HashMap<>();
                    Map<Long, String> lighterNames = new HashMap<>();
                    Map<Long, String> ownerNames = new HashMap<>();
                    Map<Long, Long> ownerIds = new HashMap<>();
                    Map<Long, String> imageUrls = new HashMap<>();

                    for (JsonElement element : historyArray) {
                        JsonObject record = element.getAsJsonObject();
                        if (!record.has("lighter_id") || record.get("lighter_id").isJsonNull()) continue;

                        long lighterId = record.get("lighter_id").getAsLong();
                        transferCounts.put(lighterId, transferCounts.getOrDefault(lighterId, 0) + 1);

                        if (record.has("lighters") && !record.get("lighters").isJsonNull()) {
                            JsonObject lighter = record.getAsJsonObject("lighters");
                            lighterNames.put(lighterId, lighter.get("name").getAsString());

                            if (lighter.has("user_profiles") && !lighter.get("user_profiles").isJsonNull()) {
                                JsonObject owner = lighter.getAsJsonObject("user_profiles");
                                ownerNames.put(lighterId, owner.get("name").getAsString());
                                ownerIds.put(lighterId, owner.get("id").getAsLong());
                            }

                            if (lighter.has("lighter_images") && !lighter.get("lighter_images").isJsonNull()) {
                                JsonArray images = lighter.getAsJsonArray("lighter_images");
                                if (images.size() > 0) {
                                    String imageUrl = images.get(0).getAsJsonObject().get("image_url").getAsString();
                                    imageUrls.put(lighterId, imageUrl);
                                }
                            }
                        }
                    }

                    List<Map.Entry<Long, Integer>> sorted = new ArrayList<>(transferCounts.entrySet());
                    sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

                    List<LeaderboardEntry.TransferredLighter> result = new ArrayList<>();
                    for (int i = 0; i < Math.min(limit, sorted.size()); i++) {
                        long lighterId = sorted.get(i).getKey();
                        result.add(new LeaderboardEntry.TransferredLighter(
                                lighterId,
                                lighterNames.getOrDefault(lighterId, "Unknown"),
                                sorted.get(i).getValue(),
                                ownerNames.getOrDefault(lighterId, "Unknown"),
                                ownerIds.getOrDefault(lighterId, 0L),
                                imageUrls.getOrDefault(lighterId, null)
                        ));
                    }

                    mainHandler.post(() -> callback.onSuccess(result));
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error getting most transferred lighters", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
    /**
     * Get top users by lighter count
     */
    public void getTopLighterOwners(int limit, DataCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL +
                        "/rest/v1/lighters?select=owner,user_profiles!owner(id,name,profile_photo_url)");

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);

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

                    JsonArray lightersArray = JsonParser.parseString(response.toString()).getAsJsonArray();

                    Map<Long, Integer> ownerCounts = new HashMap<>();
                    Map<Long, String> ownerNames = new HashMap<>();
                    Map<Long, String> ownerPhotos = new HashMap<>();

                    for (JsonElement element : lightersArray) {
                        JsonObject lighter = element.getAsJsonObject();
                        if (!lighter.has("owner") || lighter.get("owner").isJsonNull()) continue;

                        long ownerId = lighter.get("owner").getAsLong();
                        ownerCounts.put(ownerId, ownerCounts.getOrDefault(ownerId, 0) + 1);

                        if (lighter.has("user_profiles") && !lighter.get("user_profiles").isJsonNull()) {
                            JsonObject profile = lighter.getAsJsonObject("user_profiles");
                            ownerNames.put(ownerId, profile.get("name").getAsString());
                            ownerPhotos.put(ownerId,
                                    profile.has("profile_photo_url") && !profile.get("profile_photo_url").isJsonNull()
                                            ? profile.get("profile_photo_url").getAsString() : null);
                        }
                    }

                    List<Map.Entry<Long, Integer>> sorted = new ArrayList<>(ownerCounts.entrySet());
                    sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

                    List<LeaderboardEntry.TopUser> result = new ArrayList<>();
                    for (int i = 0; i < Math.min(limit, sorted.size()); i++) {
                        long ownerId = sorted.get(i).getKey();
                        result.add(new LeaderboardEntry.TopUser(
                                ownerId,
                                ownerNames.getOrDefault(ownerId, "Unknown"),
                                sorted.get(i).getValue(),
                                ownerPhotos.getOrDefault(ownerId, null)
                        ));
                    }

                    mainHandler.post(() -> callback.onSuccess(result));
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error getting top lighter owners", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // Helper class for sorting users by count
    private static class UserWithCount {
        long id;
        String name;
        String photoUrl;
        int count;

        UserWithCount(long id, String name, String photoUrl, int count) {
            this.id = id;
            this.name = name;
            this.photoUrl = photoUrl;
            this.count = count;
        }
    }

    /**
     * Add a user to favorites (friend)
     */
    public void addFavoriteUser(long userId, long favoritedUserId, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/favorite_users");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JsonObject favoriteData = new JsonObject();
                favoriteData.addProperty("user_id", userId);
                favoriteData.addProperty("favorited_user_id", favoritedUserId);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = gson.toJson(favoriteData).getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == 201 || responseCode == 409) {
                    mainHandler.post(() -> callback.onSuccess());
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error adding favorite user", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Remove a user from favorites
     */
    public void removeFavoriteUser(long userId, long favoritedUserId, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/favorite_users?user_id=eq." + userId +
                        "&favorited_user_id=eq." + favoritedUserId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_NO_CONTENT || responseCode == 204 ||
                        responseCode == HttpURLConnection.HTTP_OK) {
                    mainHandler.post(() -> callback.onSuccess());
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error removing favorite user", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Check if a user is favorited
     */
    public void isUserFavorited(long userId, long favoritedUserId, SingleItemCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/favorite_users?user_id=eq." + userId +
                        "&favorited_user_id=eq." + favoritedUserId + "&select=id");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);

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

                    JsonArray array = JsonParser.parseString(response.toString()).getAsJsonArray();
                    boolean isFavorited = array.size() > 0;
                    mainHandler.post(() -> callback.onSuccess(isFavorited));
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error checking favorite user", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Get all favorite users (friends) for a user
     */
    public void getFavoriteUsers(long userId, DataCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/favorite_users?user_id=eq." + userId +
                        "&select=*,user_profiles!favorited_user_id(id,name,profile_photo_url,bio)");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);

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

                    JsonArray array = JsonParser.parseString(response.toString()).getAsJsonArray();
                    List<UserProfile> users = new ArrayList<>();

                    for (JsonElement element : array) {
                        JsonObject obj = element.getAsJsonObject();
                        if (obj.has("user_profiles") && !obj.get("user_profiles").isJsonNull()) {
                            JsonObject userObj = obj.getAsJsonObject("user_profiles");
                            UserProfile user = new UserProfile();
                            user.setId(userObj.get("id").getAsLong());
                            user.setName(userObj.get("name").getAsString());
                            if (userObj.has("profile_photo_url") && !userObj.get("profile_photo_url").isJsonNull()) {
                                user.setProfilePhotoUrl(userObj.get("profile_photo_url").getAsString());
                            }
                            if (userObj.has("bio") && !userObj.get("bio").isJsonNull()) {
                                user.setBio(userObj.get("bio").getAsString());
                            }
                            users.add(user);
                        }
                    }

                    mainHandler.post(() -> callback.onSuccess(users));
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error getting favorite users", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Add a lighter to favorites
     */
    public void addFavoriteLighter(long userId, long lighterId, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/favorite_lighters");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JsonObject favoriteData = new JsonObject();
                favoriteData.addProperty("user_id", userId);
                favoriteData.addProperty("lighter_id", lighterId);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = gson.toJson(favoriteData).getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == 201 || responseCode == 409) {
                    mainHandler.post(() -> callback.onSuccess());
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error adding favorite lighter", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Remove a lighter from favorites
     */
    public void removeFavoriteLighter(long userId, long lighterId, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/favorite_lighters?user_id=eq." + userId +
                        "&lighter_id=eq." + lighterId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_NO_CONTENT || responseCode == 204 ||
                        responseCode == HttpURLConnection.HTTP_OK) {
                    mainHandler.post(() -> callback.onSuccess());
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error removing favorite lighter", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Check if a lighter is favorited
     */
    public void isLighterFavorited(long userId, long lighterId, SingleItemCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/favorite_lighters?user_id=eq." + userId +
                        "&lighter_id=eq." + lighterId + "&select=id");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);

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

                    JsonArray array = JsonParser.parseString(response.toString()).getAsJsonArray();
                    boolean isFavorited = array.size() > 0;
                    mainHandler.post(() -> callback.onSuccess(isFavorited));
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error checking favorite lighter", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Get all favorite lighters for a user
     */
    /**
     * Get all favorite lighters for a user
     */
    public void getFavoriteLighters(long userId, DataCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/favorite_lighters?user_id=eq." + userId +
                        "&select=*,lighters(id,name,description,owner)");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + authToken);

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

                    JsonArray array = JsonParser.parseString(response.toString()).getAsJsonArray();
                    List<Lighter> lighters = new ArrayList<>();

                    for (JsonElement element : array) {
                        JsonObject obj = element.getAsJsonObject();
                        if (obj.has("lighters") && !obj.get("lighters").isJsonNull()) {
                            JsonObject lighterObj = obj.getAsJsonObject("lighters");
                            Lighter lighter = new Lighter();
                            lighter.setId(lighterObj.get("id").getAsLong());
                            lighter.setName(lighterObj.get("name").getAsString());
                            if (lighterObj.has("description") && !lighterObj.get("description").isJsonNull()) {
                                lighter.setDescription(lighterObj.get("description").getAsString());
                            }
                            if (lighterObj.has("owner") && !lighterObj.get("owner").isJsonNull()) {
                                lighter.setOwner(lighterObj.get("owner").getAsLong());
                            }
                            lighters.add(lighter);
                        }
                    }

                    mainHandler.post(() -> callback.onSuccess(lighters));
                } else {
                    mainHandler.post(() -> callback.onError("Failed: " + responseCode));
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error getting favorite lighters", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
}


