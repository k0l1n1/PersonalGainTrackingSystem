package com.personaltracker.dao;

import com.personaltracker.model.SleepRecord;
import com.personaltracker.service.SupabaseClient;

import java.util.Collections;
import java.util.List;

/**
 * Data Access Object for Sleep Records.
 */
public class SleepDAO {
    private final SupabaseClient client = SupabaseClient.getInstance();

    public List<SleepRecord> getAllRecords() throws Exception {
        String uid = client.getCurrentUserId();
        if (uid == null) return Collections.emptyList();

        List<SleepRecord> list = client.executeGetList("sleep_records",
                "user_id=eq." + uid + "&order=sleep_date.desc,created_at.desc",
                SleepRecord.class);

        return list != null ? list : Collections.emptyList();
    }

    public SleepRecord saveRecord(SleepRecord record) throws Exception {
        record.setUserId(client.getCurrentUserId());
        return client.executePost("sleep_records", record, SleepRecord.class);
    }

    public void deleteRecord(String id) throws Exception {
        if (id == null) return;
        client.executeDelete("sleep_records", "id=eq." + id);
    }
}
