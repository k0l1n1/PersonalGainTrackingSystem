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
}
