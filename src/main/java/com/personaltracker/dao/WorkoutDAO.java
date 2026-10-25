package com.personaltracker.dao;

import com.personaltracker.model.Workout;
import com.personaltracker.model.WorkoutExercise;
import com.personaltracker.service.SupabaseClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data Access Object for Workouts and Exercises.
 */
public class WorkoutDAO {
    private final SupabaseClient client = SupabaseClient.getInstance();

    public List<Workout> getAllWorkouts() throws Exception {
        String uid = client.getCurrentUserId();
        if (uid == null) return Collections.emptyList();

        List<Workout> workouts = client.executeGetList("workouts",
                "user_id=eq." + uid + "&order=workout_date.desc,created_at.desc",
                Workout.class);

        if (workouts != null && !workouts.isEmpty()) {
            for (Workout w : workouts) {
                if (w.getId() != null && !w.getId().isBlank()) {
                    if (w.getExercises() == null || w.getExercises().isEmpty()) {
                        try {
                            List<WorkoutExercise> exs = getExercisesForWorkout(w.getId());
                            if (exs != null && !exs.isEmpty()) {
                                w.setExercises(exs);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        return workouts != null ? workouts : Collections.emptyList();
    }

    public Workout saveWorkout(Workout workout) throws Exception {
        workout.setUserId(client.getCurrentUserId());
        if (workout.getId() == null || workout.getId().isBlank()) {
            workout.setId(java.util.UUID.randomUUID().toString());
        }
        Workout saved = client.executePost("workouts", workout, Workout.class);
        String finalWorkoutId = (saved != null && saved.getId() != null) ? saved.getId() : workout.getId();
        if (workout.getExercises() != null && !workout.getExercises().isEmpty()) {
            int order = 0;
            for (WorkoutExercise ex : new ArrayList<>(workout.getExercises())) {
                ex.setWorkoutId(finalWorkoutId);
                if (ex.getId() == null || ex.getId().isBlank()) {
                    ex.setId(java.util.UUID.randomUUID().toString());
                }
                ex.setOrderIndex(order++);
                client.executePost("workout_exercises", ex, WorkoutExercise.class);
            }
            if (saved != null) {
                saved.setExercises(workout.getExercises());
            }
        }
        return saved != null ? saved : workout;
    }

    public void updateWorkout(Workout workout) throws Exception {
        if (workout.getId() == null || workout.getId().isBlank()) {
            saveWorkout(workout);
            return;
        }
        client.executePatch("workouts", "id=eq." + workout.getId(), workout);
        if (workout.getExercises() != null) {
            try {
                client.executeDelete("workout_exercises", "workout_id=eq." + workout.getId());
            } catch (Exception ignored) {}
            int order = 0;
            for (WorkoutExercise ex : new ArrayList<>(workout.getExercises())) {
                ex.setWorkoutId(workout.getId());
                if (ex.getId() == null || ex.getId().isBlank()) {
                    ex.setId(java.util.UUID.randomUUID().toString());
                }
                ex.setOrderIndex(order++);
                client.executePost("workout_exercises", ex, WorkoutExercise.class);
            }
        }
    }

    public void deleteWorkout(String workoutId) throws Exception {
        if (workoutId == null) return;
        client.executeDelete("workouts", "id=eq." + workoutId);
    }

    public List<WorkoutExercise> getExercisesForWorkout(String workoutId) throws Exception {
        if (workoutId == null) return Collections.emptyList();
        return client.executeGetList("workout_exercises",
                "workout_id=eq." + workoutId + "&order=order_index.asc",
                WorkoutExercise.class);
    }
}
