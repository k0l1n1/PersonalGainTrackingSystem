package com.personaltracker.dao;

import com.personaltracker.model.WaterIntake;
import com.personaltracker.service.SupabaseClient;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Data Access Object for Water Intake.
 */
public class WaterDAO {
    private final SupabaseClient client = SupabaseClient.getInstance();

    public List<WaterIntake> getEntriesForDate(LocalDate date) throws Exception {
        String uid = client.getCurrentUserId();
        if (uid == null || date == null) return Collections.emptyList();

        List<WaterIntake> list = client.executeGetList("water_intake",
                "user_id=eq." + uid + "&recorded_date=eq." + date + "&order=recorded_time.asc",
                WaterIntake.class);

        return list != null ? list : Collections.emptyList();
    }

    public List<WaterIntake> getAllEntries() throws Exception {
        String uid = client.getCurrentUserId();
        if (uid == null) return Collections.emptyList();

        List<WaterIntake> list = client.executeGetList("water_intake",
                "user_id=eq." + uid + "&order=recorded_date.desc,recorded_time.desc",
                WaterIntake.class);

        return list != null ? list : Collections.emptyList();
    }

    public WaterIntake logWater(int amountMl, LocalDate date) throws Exception {
        WaterIntake wi = new WaterIntake(amountMl, date, java.time.LocalTime.now());
        wi.setUserId(client.getCurrentUserId());
        return client.executePost("water_intake", wi, WaterIntake.class);
    }

    public void deleteEntry(String id) throws Exception {
        if (id == null) return;
        client.executeDelete("water_intake", "id=eq." + id);
    }
}
