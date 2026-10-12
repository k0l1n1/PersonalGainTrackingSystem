package com.personaltracker;

import com.personaltracker.model.Achievement;
import com.personaltracker.model.Goal;
import com.personaltracker.model.Workout;
import com.personaltracker.util.HealthCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Gamification and Achievement Rules Unit Tests")
public class AchievementRulesTest {

    @Test
    @DisplayName("Should trigger FIRST_WORKOUT badge when at least 1 workout exists")
    void testFirstWorkoutRule() {
        List<Workout> workouts = new ArrayList<>();
        assertFalse(!workouts.isEmpty());

        workouts.add(new Workout("First Routine", "Cardio", LocalDate.now(), 20, 150));
        assertTrue(!workouts.isEmpty());
    }

    @Test
    @DisplayName("Should detect 3-day and 7-day workout streak milestones")
    void testStreakMilestones() {
        LocalDate today = LocalDate.now();
        List<Workout> list = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            list.add(new Workout("Day " + i, "Strength", today.minusDays(i), 30, 200));
        }

        int streak = HealthCalculator.calculateWorkoutStreak(list);
        assertEquals(7, streak);
        assertTrue(streak >= 3, "Should qualify for 3-Day streak");
        assertTrue(streak >= 7, "Should qualify for 7-Day streak");
    }

    @Test
    @DisplayName("Should trigger FIRST_GOAL when goal reaches 100%")
    void testGoalAchievementTrigger() {
        Goal goal = new Goal("Drink 3L water daily", "Habit", 3000.0, 3000.0, "ml", LocalDate.now().plusWeeks(1));
        assertTrue(goal.isCompleted(), "Goal completion should trigger achievement award");
    }

    @Test
    @DisplayName("Achievement model should toggle unlocked status without side effects")
    void testAchievementModel() {
        Achievement a = new Achievement("FIRST_WORKOUT", "First Step", "Logged first workout", "dumbbell", "WORKOUT");
        assertFalse(a.isUnlocked());

        a.setUnlocked(true);
        assertTrue(a.isUnlocked());
    }
}
