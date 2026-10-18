package com.personaltracker.dao;

import com.personaltracker.model.Workout;
import com.personaltracker.model.WorkoutExercise;
import com.personaltracker.service.SupabaseClient;

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

        return workouts != null ? workouts : Collections.emptyList();
    }

    public Workout saveWorkout(Workout workout) throws Exception {
        workout.setUserId(client.getCurrentUserId());
        Workout saved = client.executePost("workouts", workout, Workout.class);
        if (saved != null && workout.getExercises() != null && !workout.getExercises().isEmpty()) {
            for (WorkoutExercise ex : workout.getExercises()) {
                ex.setWorkoutId(saved.getId());
                client.executePost("workout_exercises", ex, WorkoutExercise.class);
            }
            saved.setExercises(workout.getExercises());
        }
        return saved != null ? saved : workout;
    }

    public void updateWorkout(Workout workout) throws Exception {
        if (workout.getId() == null) return;
        client.executePatch("workouts", "id=eq." + workout.getId(), workout);
        if (workout.getExercises() != null && !com.personaltracker.config.AppConfig.getInstance().isMockMode()) {
            client.executeDelete("workout_exercises", "workout_id=eq." + workout.getId());
            for (WorkoutExercise ex : workout.getExercises()) {
                ex.setWorkoutId(workout.getId());
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
