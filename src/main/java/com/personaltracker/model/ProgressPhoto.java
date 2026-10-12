package com.personaltracker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Model representing a physical fitness progress photo entry.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProgressPhoto {
    private String id;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("photo_url")
    private String photoUrl;

    @JsonProperty("storage_path")
    private String storagePath;

    @JsonProperty("photo_date")
    private LocalDate photoDate = LocalDate.now();

    private String notes;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    // Transient image data if loaded locally or in-memory
    private byte[] imageBytes;

    public ProgressPhoto() {}

    public ProgressPhoto(String photoUrl, String storagePath, LocalDate photoDate, String notes) {
        this.photoUrl = photoUrl;
        this.storagePath = storagePath;
        this.photoDate = photoDate != null ? photoDate : LocalDate.now();
        this.notes = notes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public LocalDate getPhotoDate() { return photoDate; }
    public void setPhotoDate(LocalDate photoDate) { this.photoDate = photoDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public byte[] getImageBytes() { return imageBytes; }
    public void setImageBytes(byte[] imageBytes) { this.imageBytes = imageBytes; }
}
