package com.example.zappatrack;

/**
 * Class to hold authentication session data including user role
 */
public class AuthSession {
    private final String userId;      // Supabase Auth UUID
    private final String email;
    private final String accessToken;
    private final String refreshToken;
    private String displayName;
    private long profileId = -1;      // user_profiles table ID
    private String role = "user";     // User role: 'user' or 'admin'

    public AuthSession(String userId, String email, String accessToken, String refreshToken) {
        this.userId = userId;
        this.email = email;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getProfileId() {
        return profileId;
    }

    public String getRole() {
        return role != null ? role : "user";
    }

    // Setters for mutable fields
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setProfileId(long profileId) {
        this.profileId = profileId;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // Helper methods
    public boolean hasProfileId() {
        return profileId > 0;
    }

    public boolean isAdmin() {
        return "admin".equals(role);
    }

    public boolean isUser() {
        return "user".equals(role);
    }


    @Override
    public String toString() {
        return "AuthSession{" +
                "userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", displayName='" + displayName + '\'' +
                ", profileId=" + profileId +
                ", role='" + role + '\'' +
                ", isAdmin=" + isAdmin() +
                '}';
    }
}