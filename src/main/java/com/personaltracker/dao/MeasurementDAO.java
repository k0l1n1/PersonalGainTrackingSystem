package com.personaltracker.dao;

import com.personaltracker.model.BodyMeasurement;
import com.personaltracker.service.SupabaseClient;

import java.util.Collections;
import java.util.List;

/**
 * Data Access Object for Body Measurements.
 */
public class MeasurementDAO {
    private final SupabaseClient client = SupabaseClient.getInstance();

    public List<BodyMeasurement> getAllMeasurements() throws Exception {
        String uid = client.getCurrentUserId();
        if (uid == null) return Collections.emptyList();

        List<BodyMeasurement> list = client.executeGetList("body_measurements",
                "user_id=eq." + uid + "&order=recorded_date.desc,created_at.desc",
                BodyMeasurement.class);

        return list != null ? list : Collections.emptyList();
    }

    public BodyMeasurement saveMeasurement(BodyMeasurement measurement) throws Exception {
        measurement.setUserId(client.getCurrentUserId());
        return client.executePost("body_measurements", measurement, BodyMeasurement.class);
    }

    public void deleteMeasurement(String id) throws Exception {
        if (id == null) return;
        client.executeDelete("body_measurements", "id=eq." + id);
    }
}
