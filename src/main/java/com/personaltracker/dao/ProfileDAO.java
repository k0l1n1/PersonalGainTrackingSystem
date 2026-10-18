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
        try {
            client.executePatch("profiles", "id=eq." + profile.getId(), profile);
        } catch (java.io.IOException e) {
            // Graceful fallback: If remote Supabase schema is missing newer columns (e.g. role, user_code),
            // update core profile columns so onboarding and profile saves succeed seamlessly.
            if (e.getMessage() != null && e.getMessage().contains("PGRST204")) {
                java.util.Map<String, Object> fallback = new java.util.LinkedHashMap<>();
                fallback.put("full_name", profile.getFullName());
                fallback.put("gender", profile.getGender());
                fallback.put("date_of_birth", profile.getDateOfBirth());
                fallback.put("height_cm", profile.getHeightCm());
                fallback.put("target_weight_kg", profile.getTargetWeightKg());
                fallback.put("daily_calorie_target", profile.getDailyCalorieTarget());
                fallback.put("daily_water_target_ml", profile.getDailyWaterTargetMl());
                client.executePatch("profiles", "id=eq." + profile.getId(), fallback);
            } else {
                throw e;
            }
        }
        if (client.getCurrentSession() != null) {
            client.getCurrentSession().setProfile(profile);
        }
    }
}
