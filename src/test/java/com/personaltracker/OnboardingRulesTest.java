package com.personaltracker;

import com.personaltracker.model.UserProfile;
import com.personaltracker.view.OnboardingDialog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Onboarding User Profile Rules Test")
public class OnboardingRulesTest {

    @Test
    @DisplayName("Null profile should trigger onboarding")
    void testNullProfileTriggersOnboarding() {
        assertTrue(OnboardingDialog.isNewUser(null));
    }

    @Test
    @DisplayName("Profile without height or target weight should be flagged as new user")
    void testIncompleteProfileTriggersOnboarding() {
        UserProfile newRegisterUser = new UserProfile("uid-1", "newuser@test.com", "John Doe");
        // Fresh registration only has email/name, height and target weight are null
        assertTrue(OnboardingDialog.isNewUser(newRegisterUser));

        // Profile with only height but no target weight
        newRegisterUser.setHeightCm(180.0);
        assertTrue(OnboardingDialog.isNewUser(newRegisterUser));

        // Profile with only target weight but no height
        newRegisterUser.setHeightCm(null);
        newRegisterUser.setTargetWeightKg(75.0);
        assertTrue(OnboardingDialog.isNewUser(newRegisterUser));

        // Profile with non-positive values
        newRegisterUser.setHeightCm(0.0);
        newRegisterUser.setTargetWeightKg(0.0);
        assertTrue(OnboardingDialog.isNewUser(newRegisterUser));
    }

    @Test
    @DisplayName("Completed profile should NOT trigger onboarding (existing user goes straight to dashboard)")
    void testCompletedProfileSkipsOnboarding() {
        UserProfile completedUser = new UserProfile("uid-2", "existing@test.com", "Jane Doe");
        completedUser.setHeightCm(168.0);
        completedUser.setTargetWeightKg(62.0);
        completedUser.setGender("Female");
        completedUser.setDailyCalorieTarget(2100);
        completedUser.setDailyWaterTargetMl(2500);

        assertFalse(OnboardingDialog.isNewUser(completedUser));
    }
}
