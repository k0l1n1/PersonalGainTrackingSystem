package com.personaltracker.dao;

import com.personaltracker.model.Reminder;
import com.personaltracker.service.SupabaseClient;

import java.util.Collections;
import java.util.List;

/**
 * Data Access Object for Reminders.
 */
public class ReminderDAO {
    private final SupabaseClient client = SupabaseClient.getInstance();

    public List<Reminder> getAllReminders() throws Exception {
        String uid = client.getCurrentUserId();
        if (uid == null) return Collections.emptyList();

        List<Reminder> list = client.executeGetList("reminders",
                "user_id=eq." + uid + "&order=reminder_date.asc,reminder_time.asc",
                Reminder.class);

        return list != null ? list : Collections.emptyList();
    }

    public Reminder saveReminder(Reminder reminder) throws Exception {
        reminder.setUserId(client.getCurrentUserId());
        return client.executePost("reminders", reminder, Reminder.class);
    }

    public void updateReminder(Reminder reminder) throws Exception {
        if (reminder.getId() == null) return;
        client.executePatch("reminders", "id=eq." + reminder.getId(), reminder);
    }

    public void deleteReminder(String id) throws Exception {
        if (id == null) return;
        client.executeDelete("reminders", "id=eq." + id);
    }
}
