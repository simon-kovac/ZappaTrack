package com.example.zappatrack.databaseitems;

import com.google.gson.annotations.SerializedName;

/**
 * Model class for lighter images stored in the database
 */
public class LighterImage {
    private long id;

    @SerializedName("lighter_id")
    private long lighterId;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("thumbnail_url")
    private String thumbnailUrl;

    @SerializedName("uploaded_by")
    private long uploadedBy;

    @SerializedName("uploaded_at")
    private String uploadedAt;

    @SerializedName("is_primary")
    private boolean isPrimary;

    private String description;

    // Default constructor
    public LighterImage() {}

    // Getters
    public long getId() {
        return id;
    }

    public long getLighterId() {
        return lighterId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public long getUploadedBy() {
        return uploadedBy;
    }

    public String getUploadedAt() {
        return uploadedAt;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public String getDescription() {
        return description;
    }

    // Setters
    public void setId(long id) {
        this.id = id;
    }

    public void setLighterId(long lighterId) {
        this.lighterId = lighterId;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void setUploadedBy(long uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public void setUploadedAt(String uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}