package com.personaltracker.dao;

import com.personaltracker.model.NutritionEntry;
import com.personaltracker.service.SupabaseClient;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Data Access Object for Nutrition Entries.
 */
public class NutritionDAO {
    private final SupabaseClient client = SupabaseClient.getInstance();

    public List<NutritionEntry> getAllEntries() throws Exception {
        String uid = client.getCurrentUserId();
        if (uid == null) return Collections.emptyList();

        List<NutritionEntry> list = client.executeGetList("nutrition_entries",
                "user_id=eq." + uid + "&order=entry_date.desc,created_at.desc",
                NutritionEntry.class);

        return list != null ? list : Collections.emptyList();
    }

    public List<NutritionEntry> getEntriesForDate(LocalDate date) throws Exception {
        String uid = client.getCurrentUserId();
        if (uid == null || date == null) return Collections.emptyList();

        List<NutritionEntry> list = client.executeGetList("nutrition_entries",
                "user_id=eq." + uid + "&entry_date=eq." + date + "&order=created_at.asc",
                NutritionEntry.class);

        return list != null ? list : Collections.emptyList();
    }

    public NutritionEntry saveEntry(NutritionEntry entry) throws Exception {
        entry.setUserId(client.getCurrentUserId());
        return client.executePost("nutrition_entries", entry, NutritionEntry.class);
    }

    public void updateEntry(NutritionEntry entry) throws Exception {
        if (entry.getId() == null) return;
        client.executePatch("nutrition_entries", "id=eq." + entry.getId(), entry);
    }

    public void deleteEntry(String id) throws Exception {
        if (id == null) return;
        client.executeDelete("nutrition_entries", "id=eq." + id);
    }
}
