package com.personaltracker;

import com.personaltracker.model.Workout;
import com.personaltracker.model.WorkoutExercise;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Workout Duration and Statistics Unit Tests")
public class WorkoutStatsTest {

    @Test
    @DisplayName("Should correctly calculate exercise volume load (sets * reps * weight)")
    void testExerciseVolumeLoad() {
        WorkoutExercise ex = new WorkoutExercise("Bench Press", 4, 10, 80.0);
        // 4 * 10 * 80 = 3200 kg
        assertEquals(3200.0, ex.getVolumeLoad(), 0.01);
    }

    @Test
    @DisplayName("Should calculate total weight lifted across all exercises in workout")
    void testTotalWorkoutLoad() {
        Workout w = new Workout("Chest & Triceps", "Strength", LocalDate.now(), 60, 450);
        w.addExercise(new WorkoutExercise("Bench Press", 4, 8, 80.0));       // 2560 kg
        w.addExercise(new WorkoutExercise("Incline Dumbbell", 3, 10, 30.0)); // 900 kg
        w.addExercise(new WorkoutExercise("Triceps Pushdown", 3, 12, 25.0)); // 900 kg

        assertEquals(4360.0, w.getTotalWeightLiftedKg(), 0.01);
    }

    @Test
    @DisplayName("Should handle workout without exercises safely")
    void testEmptyExercisesLoad() {
        Workout w = new Workout("Morning Jog", "Cardio", LocalDate.now(), 30, 280);
        assertEquals(0.0, w.getTotalWeightLiftedKg());
    }

    @Test
    @DisplayName("Should verify that quick-log preset workout saves properly even with initially null ID")
    void testQuickLogPresetSaveAndFallback() throws Exception {
        com.personaltracker.dao.WorkoutDAO dao = new com.personaltracker.dao.WorkoutDAO();
        Workout preset = new Workout();
        preset.setTitle("Quick Push Day");
        preset.setWorkoutType("Strength");
        preset.setDurationMinutes(45);
        preset.setCaloriesBurned(380);
        preset.setWorkoutDate(LocalDate.now());

        assertNull(preset.getId());
        // Verify saveWorkout generates an ID and saves
        Workout saved = dao.saveWorkout(preset);
        assertNotNull(saved.getId());

        // Verify updateWorkout delegates to save if ID was null/blank
        Workout unpersisted = new Workout();
        unpersisted.setTitle("Quick Pull Day");
        dao.updateWorkout(unpersisted);
        assertNotNull(unpersisted.getId());
    }

    @Test
    @DisplayName("Should return valid daily routines for all days of the week")
    void testDailySplitRoutines() {
        for (java.time.DayOfWeek day : java.time.DayOfWeek.values()) {
            com.personaltracker.view.WorkoutPanel.DailyRoutine routine = com.personaltracker.view.WorkoutPanel.getRoutineForDay(day);
            assertNotNull(routine);
            assertEquals(day, routine.dayOfWeek);
            assertNotNull(routine.title);
            assertFalse(routine.title.isBlank());
            assertTrue(routine.durationMinutes > 0);
            assertTrue(routine.calories > 0);
        }
    }

    @Test
    @DisplayName("Should offer more than 5 comprehensive workout splits with full 7-day schedules")
    void testMultipleWorkoutSplits() {
        com.personaltracker.view.WorkoutPanel.WorkoutSplit[] splits = com.personaltracker.view.WorkoutPanel.WORKOUT_SPLITS;
        assertTrue(splits.length >= 7, "Should offer at least 7 comprehensive workout splits");

        for (com.personaltracker.view.WorkoutPanel.WorkoutSplit split : splits) {
            assertNotNull(split.id);
            assertNotNull(split.name);
            assertNotNull(split.description);
            assertEquals(7, split.routines.length, "Each split must provide routines for all 7 days");

            for (java.time.DayOfWeek day : java.time.DayOfWeek.values()) {
                com.personaltracker.view.WorkoutPanel.DailyRoutine r = com.personaltracker.view.WorkoutPanel.getRoutineForDay(split, day);
                assertNotNull(r, "Split " + split.name + " must provide routine for " + day);
                assertEquals(day, r.dayOfWeek);
                assertTrue(r.durationMinutes > 0);
                assertTrue(r.calories > 0);
                assertNotNull(r.exercises);
                assertTrue(r.exercises.length > 0, "Split " + split.name + " routine must have exercises defined");
            }
        }
    }
}
