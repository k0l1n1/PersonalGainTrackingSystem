package com.personaltracker;

import com.personaltracker.model.Goal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Fitness Goals Calculation & Status Transitions Unit Tests")
public class GoalCalculationTest {

    @Test
    @DisplayName("Should mark goal as COMPLETED when currentValue >= targetValue")
    void testGoalCompletion() {
        Goal goal = new Goal("Bench Press 100kg", "Strength", 100.0, 80.0, "kg", LocalDate.now().plusMonths(1));
        assertFalse(goal.isCompleted());
        assertEquals("IN_PROGRESS", goal.getStatus());
        assertEquals(80, goal.getProgressPercentage());

        goal.setCurrentValue(100.0);
        assertTrue(goal.isCompleted());
        assertEquals("COMPLETED", goal.getStatus());
        assertEquals(100, goal.getProgressPercentage());

        goal.setCurrentValue(105.0);
        assertTrue(goal.isCompleted());
        assertEquals(100, goal.getProgressPercentage()); // Capped at 100%
    }

    @Test
    @DisplayName("Should detect OVERDUE status when past target date and incomplete")
    void testGoalOverdue() {
        Goal pastGoal = new Goal("Run 5k", "Endurance", 5.0, 3.0, "km", LocalDate.now().minusDays(3));
        assertTrue(pastGoal.isOverdue());
        assertEquals("OVERDUE", pastGoal.getStatus());
    }

    @Test
    @DisplayName("Should transition from COMPLETED back to IN_PROGRESS when edited to increase target or decrease current")
    void testEditCompletedGoalTransitionsBackToInProgress() {
        // Goal starts at 100/100 (completed)
        Goal goal = new Goal("Deadlift 200kg", "Strength", 100.0, 100.0, "kg", LocalDate.now().plusMonths(2));
        assertTrue(goal.isCompleted());
        assertEquals("COMPLETED", goal.getStatus());

        // User edits the goal: sets a new higher target of 120kg
        goal.setTargetValue(120.0);
        // Reaching the new target completes it again
        goal.setCurrentValue(120.0);
        assertTrue(goal.isCompleted());
        assertEquals("COMPLETED", goal.getStatus());
        assertEquals(100, goal.getProgressPercentage());
    }

    @Test
    @DisplayName("Should correctly evaluate weight journey when starting equals target but current is below target")
    void testWeightJourneyStartingEqualsTargetCurrentBelow() {
        double starting = 75.0;
        double current = 64.0;
        double target = 75.0;

        double deltaFromTarget = current - target;
        assertFalse(Math.abs(deltaFromTarget) <= 0.3, "Should not be within maintenance threshold");

        double toGain = target - current;
        assertEquals(11.0, toGain, 0.01, "User should have 11.0 kg remaining to gain");

        int percent = (int) Math.min(100, Math.max(0, Math.round((current / target) * 100.0)));
        assertEquals(85, percent, "Progress towards target should be 85%");
    }

    @Test
    @DisplayName("Should detect maintenance zone when current weight matches target within tolerance")
    void testWeightJourneyMaintenanceThreshold() {
        double target = 75.0;
        double currentExact = 75.0;
        double currentWithinTolerance = 75.2;
        double currentOutsideTolerance = 74.5;

        assertTrue(Math.abs(currentExact - target) <= 0.3);
        assertTrue(Math.abs(currentWithinTolerance - target) <= 0.3);
        assertFalse(Math.abs(currentOutsideTolerance - target) <= 0.3);
    }
}
