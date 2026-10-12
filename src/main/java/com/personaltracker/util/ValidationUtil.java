package com.personaltracker.util;

import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Validation utilities for application input.
 */
public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidPassword(String password) {
        if (password == null) return false;
        // Supabase requires at least 6 characters
        return password.trim().length() >= 6;
    }

    public static boolean isNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }

    public static boolean isPositive(double value) {
        return value > 0;
    }

    public static boolean isNonNegative(double value) {
        return value >= 0;
    }

    public static boolean isPositive(int value) {
        return value > 0;
    }

    public static boolean isNonNegative(int value) {
        return value >= 0;
    }

    public static boolean isValidTargetDate(LocalDate startDate, LocalDate targetDate) {
        if (targetDate == null) return false;
        if (startDate == null) startDate = LocalDate.now();
        return !targetDate.isBefore(startDate);
    }

    public static boolean isValidWeight(double weightKg) {
        return weightKg >= 20.0 && weightKg <= 400.0;
    }

    public static boolean isValidHeight(double heightCm) {
        return heightCm >= 50.0 && heightCm <= 270.0;
    }

    public static boolean isValidCalorieCount(int calories) {
        return calories >= 0 && calories <= 10000;
    }

    public static boolean isValidWaterIntake(int ml) {
        return ml > 0 && ml <= 5000;
    }
}
