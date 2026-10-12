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
}
