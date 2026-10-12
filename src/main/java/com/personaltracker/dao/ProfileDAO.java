package com.personaltracker.dao;

import com.personaltracker.model.UserProfile;
import com.personaltracker.service.SupabaseClient;

import java.util.List;

/**
 * Data Access Object for User Profiles.
 */
public class ProfileDAO {
    private final SupabaseClient client = SupabaseClient.getInstance();

    public UserProfile getProfile(String userId) throws Exception {
        if (client.getCurrentSession() == null) return null;
        List<UserProfile> list = client.executeGetList("profiles", "id=eq." + userId, UserProfile.class);
        if (list != null && !list.isEmpty()) {
            UserProfile p = list.get(0);
            if (client.getCurrentSession() != null) {
                client.getCurrentSession().setProfile(p);
            }
            return p;
        }
        return null;
    }

    public void updateProfile(UserProfile profile) throws Exception {
        if (profile == null || profile.getId() == null) return;
        client.executePatch("profiles", "id=eq." + profile.getId(), profile);
        if (client.getCurrentSession() != null) {
            client.getCurrentSession().setProfile(profile);
        }
    }
}
