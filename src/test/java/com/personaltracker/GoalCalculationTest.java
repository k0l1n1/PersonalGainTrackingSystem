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
        assertFalse(goal.isCompleted(), "Goal should no longer be completed after increasing target beyond current");
        assertEquals("IN_PROGRESS", goal.getStatus());
        assertEquals(83, goal.getProgressPercentage());

        // Reaching the new target completes it again
        goal.setCurrentValue(120.0);
        assertTrue(goal.isCompleted());
        assertEquals("COMPLETED", goal.getStatus());
        assertEquals(100, goal.getProgressPercentage());
    }
}
