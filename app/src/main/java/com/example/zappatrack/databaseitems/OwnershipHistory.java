package com.example.zappatrack.databaseitems;
import java.util.Date;

/**
 * Model class for tracking ownership history of lighters
 */
public class OwnershipHistory {
    private long id;
    private long lighter_id;
    private long owner_id;
    private String acquired_at;  // Timestamp as String for simplicity
    private String released_at;  // Null means current owner
    private String notes;
    private String created_at;

    // Additional fields for joined data (when fetching with owner info)
    private String owner_name;
    private String owner_email;
    private String lighter_name;
    private int duration_days;
    private boolean is_current;

    // Empty constructor for JSON parsing
    public OwnershipHistory() {
    }

    // Constructor for creating new ownership record
    public OwnershipHistory(long lighter_id, long owner_id, String notes) {
        this.lighter_id = lighter_id;
        this.owner_id = owner_id;
        this.notes = notes;
    }

    // Getters
    public long getId() {
        return id;
    }

    public long getLighter_id() {
        return lighter_id;
    }

    public long getOwner_id() {
        return owner_id;
    }

    public String getAcquired_at() {
        return acquired_at;
    }

    public String getReleased_at() {
        return released_at;
    }

    public String getNotes() {
        return notes;
    }

    public String getCreated_at() {
        return created_at;
    }

    public String getOwner_name() {
        return owner_name;
    }

    public String getOwner_email() {
        return owner_email;
    }

    public String getLighter_name() {
        return lighter_name;
    }

    public int getDuration_days() {
        return duration_days;
    }

    public boolean isIs_current() {
        return is_current;
    }

    // Setters
    public void setId(long id) {
        this.id = id;
    }

    public void setLighter_id(long lighter_id) {
        this.lighter_id = lighter_id;
    }

    public void setOwner_id(long owner_id) {
        this.owner_id = owner_id;
    }

    public void setAcquired_at(String acquired_at) {
        this.acquired_at = acquired_at;
    }

    public void setReleased_at(String released_at) {
        this.released_at = released_at;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public void setOwner_name(String owner_name) {
        this.owner_name = owner_name;
    }

    public void setOwner_email(String owner_email) {
        this.owner_email = owner_email;
    }

    public void setLighter_name(String lighter_name) {
        this.lighter_name = lighter_name;
    }

    public void setDuration_days(int duration_days) {
        this.duration_days = duration_days;
    }

    public void setIs_current(boolean is_current) {
        this.is_current = is_current;
    }

    // Helper methods
    public boolean isCurrent() {
        return released_at == null || released_at.isEmpty();
    }

    public String getFormattedDuration() {
        if (duration_days == 0) {
            return "Today";
        } else if (duration_days == 1) {
            return "1 day";
        } else if (duration_days < 30) {
            return duration_days + " days";
        } else if (duration_days < 365) {
            int months = duration_days / 30;
            return months + (months == 1 ? " month" : " months");
        } else {
            int years = duration_days / 365;
            return years + (years == 1 ? " year" : " years");
        }
    }

    @Override
    public String toString() {
        return "OwnershipHistory{" +
                "lighter_id=" + lighter_id +
                ", owner_name='" + owner_name + '\'' +
                ", acquired_at='" + acquired_at + '\'' +
                ", released_at='" + released_at + '\'' +
                ", is_current=" + is_current +
                ", duration=" + getFormattedDuration() +
                '}';
    }
}