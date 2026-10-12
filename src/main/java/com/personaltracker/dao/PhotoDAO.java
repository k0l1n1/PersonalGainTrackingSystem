package com.personaltracker.dao;

import com.personaltracker.model.ProgressPhoto;
import com.personaltracker.service.SupabaseClient;

import java.util.Collections;
import java.util.List;

/**
 * Data Access Object for Progress Photos.
 */
public class PhotoDAO {
    private final SupabaseClient client = SupabaseClient.getInstance();

    public List<ProgressPhoto> getAllPhotos() throws Exception {
        String uid = client.getCurrentUserId();
        if (uid == null) return Collections.emptyList();

        List<ProgressPhoto> list = client.executeGetList("progress_photos",
                "user_id=eq." + uid + "&order=photo_date.desc,created_at.desc",
                ProgressPhoto.class);

        return list != null ? list : Collections.emptyList();
    }

    public ProgressPhoto savePhoto(ProgressPhoto photo) throws Exception {
        photo.setUserId(client.getCurrentUserId());
        return client.executePost("progress_photos", photo, ProgressPhoto.class);
    }

    public void deletePhoto(String id) throws Exception {
        if (id == null) return;
        client.executeDelete("progress_photos", "id=eq." + id);
    }
}
