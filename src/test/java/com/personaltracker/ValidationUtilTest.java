package com.personaltracker;

import com.personaltracker.util.ValidationUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Validation Utilities Unit Tests")
public class ValidationUtilTest {

    @Test
    @DisplayName("Should validate correct email addresses")
    void testValidEmails() {
        assertTrue(ValidationUtil.isValidEmail("user@example.com"));
        assertTrue(ValidationUtil.isValidEmail("alex.hunter@fitness.org"));
        assertTrue(ValidationUtil.isValidEmail("athlete_123@sub.domain.co"));
    }

    @Test
    @DisplayName("Should reject invalid email addresses")
    void testInvalidEmails() {
        assertFalse(ValidationUtil.isValidEmail(""));
        assertFalse(ValidationUtil.isValidEmail(null));
        assertFalse(ValidationUtil.isValidEmail("plainaddress"));
        assertFalse(ValidationUtil.isValidEmail("@missingusername.com"));
        assertFalse(ValidationUtil.isValidEmail("user@.com"));
    }

    @Test
    @DisplayName("Should validate password length constraints")
    void testValidPassword() {
        assertTrue(ValidationUtil.isValidPassword("secret123"));
        assertTrue(ValidationUtil.isValidPassword("123456"));
        assertFalse(ValidationUtil.isValidPassword("12345"));
        assertFalse(ValidationUtil.isValidPassword(""));
        assertFalse(ValidationUtil.isValidPassword(null));
    }

    @Test
    @DisplayName("Should validate weight boundaries")
    void testValidWeight() {
        assertTrue(ValidationUtil.isValidWeight(75.5));
        assertTrue(ValidationUtil.isValidWeight(20.0));
        assertTrue(ValidationUtil.isValidWeight(350.0));
        assertFalse(ValidationUtil.isValidWeight(15.0)); // Too low
        assertFalse(ValidationUtil.isValidWeight(450.0)); // Exceeds upper limit
    }

    @Test
    @DisplayName("Should validate target date not before start date")
    void testTargetDateValidation() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate target = LocalDate.of(2026, 11, 1);
        LocalDate past = LocalDate.of(2026, 9, 15);

        assertTrue(ValidationUtil.isValidTargetDate(start, target));
        assertFalse(ValidationUtil.isValidTargetDate(start, past));
    }
}
