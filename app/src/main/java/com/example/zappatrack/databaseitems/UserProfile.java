package com.example.zappatrack.databaseitems;

/**
 * Model class for user profile with bio and photo support
 */
public class UserProfile {
    private long id;
    private String created_at;
    private String name;
    private String email;
    private String auth_id;
    private String role;
    private String bio;                  // New field
    private String profile_photo_url;    // New field

    // Empty constructor for JSON parsing
    public UserProfile() {
        this.role = "user";
    }

    // Constructor with basic fields
    public UserProfile(String name, String email, String auth_id) {
        this.name = name;
        this.email = email;
        this.auth_id = auth_id;
        this.role = "user";
    }

    // Full constructor
    public UserProfile(long id, String name, String email, String auth_id, String role, String bio, String profile_photo_url) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.auth_id = auth_id;
        this.role = role != null ? role : "user";
        this.bio = bio;
        this.profile_photo_url = profile_photo_url;
    }

    // Getters
    public long getId() {
        return id;
    }

    public String getCreated_at() {
        return created_at;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getAuth_id() {
        return auth_id;
    }

    public String getRole() {
        return role != null ? role : "user";
    }

    public String getBio() {
        return bio;
    }

    public String getProfilePhotoUrl() {
        return profile_photo_url;
    }

    // Setters
    public void setId(long id) {
        this.id = id;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAuth_id(String auth_id) {
        this.auth_id = auth_id;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setProfilePhotoUrl(String profile_photo_url) {
        this.profile_photo_url = profile_photo_url;
    }

    // Helper methods
    public boolean isAdmin() {
        return "admin".equals(role);
    }

    public boolean hasBio() {
        return bio != null && !bio.isEmpty();
    }

    public boolean hasProfilePhoto() {
        return profile_photo_url != null && !profile_photo_url.isEmpty();
    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", bio='" + bio + '\'' +
                ", hasPhoto=" + hasProfilePhoto() +
                '}';
    }
}