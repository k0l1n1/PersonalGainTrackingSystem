package com.personaltracker;

import com.personaltracker.config.AppConfig;
import com.personaltracker.model.UserProfile;
import com.personaltracker.model.UserSession;
import com.personaltracker.service.SupabaseAuthService;
import com.personaltracker.service.SupabaseClient;
import com.personaltracker.view.OnboardingDialog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Existing User Account Direct Login & Bypass Filling Process Test")
public class ExistingUserLoginTest {

    private SupabaseAuthService authService;
    private SupabaseClient client;

    @BeforeEach
    void setUp() {
        AppConfig.getInstance().setMockMode(true);
        client = SupabaseClient.getInstance();
        authService = SupabaseAuthService.getInstance();
    }

    @Test
    @DisplayName("If user has already created an account, registering with that email directly logs in")
    void testExistingAccountDirectLoginOnRegister() throws Exception {
        String testEmail = "veteran.athlete@example.com";
        String testName = "Veteran Athlete";

        // First registration: creates the account
        UserSession firstSession = authService.register(testEmail, "secretPass123", testName, UserProfile.ROLE_MEMBER);
        assertNotNull(firstSession);
        assertTrue(firstSession.isNewRegistration(), "First-time account creation is marked as new registration");

        // Simulate returning to the app and trying to register again with the same email
        UserSession secondSession = authService.register(testEmail, "secretPass123", testName, UserProfile.ROLE_MEMBER);
        assertNotNull(secondSession);
        assertEquals(testEmail, secondSession.getEmail());
        assertFalse(secondSession.isNewRegistration(), "Existing account must be directly logged in and NOT marked as new registration");
    }

    @Test
    @DisplayName("Existing user logging in has newRegistration false and bypasses onboarding filling process")
    void testExistingUserBypassesOnboardingFilling() throws Exception {
        String testEmail = "existing.member@example.com";
        UserSession session = authService.login(testEmail, "password123");

        assertNotNull(session);
        assertEquals(testEmail, session.getEmail());
        assertFalse(session.isNewRegistration(), "Existing user login must have newRegistration = false");

        // Verify that showIfNewUser safely bypasses dialog when session.isNewRegistration() is false
        // Even if height or target weight is null on an existing account, onboarding should not force them
        UserProfile emptyProfile = new UserProfile(session.getUserId(), testEmail, "Member");
        emptyProfile.setHeightCm(null);
        emptyProfile.setTargetWeightKg(null);
        session.setProfile(emptyProfile);

        // Calling showIfNewUser with headless/null owner should return without throwing or blocking
        assertDoesNotThrow(() -> OnboardingDialog.showIfNewUser(null));
    }

    @Test
    @DisplayName("Session persistence and restoration for direct auto-login")
    void testSessionRestore() {
        String uid = "user-persisted-123";
        String email = "persisted@example.com";
        UserSession session = new UserSession(uid, email, "access-token-abc", "refresh-token-xyz", 3600);
        session.setNewRegistration(false);

        // Save session
        authService.saveSession(session);

        // Restore session
        UserSession restored = authService.restoreSession();
        assertNotNull(restored);
        assertEquals(email, restored.getEmail());
        assertFalse(restored.isNewRegistration(), "Restored session must be an existing account with newRegistration = false");

        // Cleanup
        authService.clearSavedSession();
        assertNull(AppConfig.getInstance().getSavedRefreshToken());
    }

    @Test
    @DisplayName("Error detection identifies various Supabase account already exists error messages")
    void testIsUserAlreadyExistsError() {
        assertTrue(SupabaseAuthService.isUserAlreadyExistsError("User already registered", ""));
        assertTrue(SupabaseAuthService.isUserAlreadyExistsError("A user with this email address has already been registered", ""));
        assertTrue(SupabaseAuthService.isUserAlreadyExistsError("email already in use", ""));
        assertTrue(SupabaseAuthService.isUserAlreadyExistsError("User already exists", ""));
        assertTrue(SupabaseAuthService.isUserAlreadyExistsError("", "user_already_exists"));
        assertFalse(SupabaseAuthService.isUserAlreadyExistsError("Invalid login credentials", ""));
    }

    @Test
    @DisplayName("New person creating an account receives newRegistration true and is directed to data filling")
    void testNewPersonAccountCreationRequiresOnboarding() throws Exception {
        String newEmail = "brandnew.athlete." + System.currentTimeMillis() + "@example.com";
        UserSession newSession = authService.register(newEmail, "pass123456", "New Athlete", UserProfile.ROLE_MEMBER);

        assertNotNull(newSession);
        assertTrue(newSession.isNewRegistration(), "Brand new person joining must have newRegistration = true");
        assertTrue(OnboardingDialog.isNewUser(newSession.getProfile()), "Profile must be flagged as new user needing data filling");

        // Verify that dialog instantiation works cleanly without error
        OnboardingDialog dialog = new OnboardingDialog(null, newSession.getProfile(), true);
        assertNotNull(dialog);
        assertTrue(dialog.isMandatory());
        dialog.dispose();
    }
}
