package com.example.zappatrack.databaseitems;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

/**
 * Model class for lighters in the collection
 */
public class Lighter {
    private long id;
    private String name;
    private String description;
    private long owner;

    @SerializedName("owner_name")
    private String ownerName;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("created_at")
    private Date createdAt;

    @SerializedName("updated_at")
    private Date updatedAt;

    // Additional fields that might be useful
    private boolean isPublic = true;
    private String brand;
    private String model;
    private int yearMade;

    // Constructors
    public Lighter() {}

    public Lighter(String name, String description, long owner) {
        this.name = name;
        this.description = description;
        this.owner = owner;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getOwner() {
        return owner;
    }

    public void setOwner(long owner) {
        this.owner = owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    /**
     * Get the image URL for this lighter
     * @return The image URL or null if no image
     */
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getYearMade() {
        return yearMade;
    }

    public void setYearMade(int yearMade) {
        this.yearMade = yearMade;
    }

    /**
     * Check if lighter has an image
     */
    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isEmpty();
    }

    @Override
    public String toString() {
        return "Lighter{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", owner=" + owner +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}