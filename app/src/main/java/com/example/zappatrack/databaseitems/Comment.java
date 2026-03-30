package com.example.zappatrack.databaseitems;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class Comment {
    private long id;

    @SerializedName("belongs_to")
    private long belongsTo;

    private long author;
    private String content;

    @SerializedName("created_at")
    private String createdAt;  // Keep as String since Supabase returns ISO string

    // Getters
    public long getId() { return id; }
    public long getBelongsTo() { return belongsTo; }
    public long getAuthor() { return author; }
    public String getContent() { return content; }
    public String getCreatedAt() { return createdAt; }

    // Setters
    public void setId(long id) { this.id = id; }
    public void setBelongsTo(long belongsTo) { this.belongsTo = belongsTo; }
    public void setAuthor(long author) { this.author = author; }
    public void setContent(String content) { this.content = content; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    // Compatibility getter for old code if needed
    public long getLighterId() { return belongsTo; }
}