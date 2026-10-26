package com.personaltracker.view;

import com.personaltracker.components.ModernButton;
import com.personaltracker.components.ModernCard;
import com.personaltracker.components.ModernTable;
import com.personaltracker.components.ModernTextField;
import com.personaltracker.components.ToastNotification;
import com.personaltracker.components.WorkoutTimerPanel;
import com.personaltracker.config.ThemeConfig;
import com.personaltracker.dao.AchievementDAO;
import com.personaltracker.dao.MeasurementDAO;
import com.personaltracker.dao.ReminderDAO;
import com.personaltracker.dao.TrainerAssignmentDAO;
import com.personaltracker.dao.WorkoutDAO;
import com.personaltracker.model.BodyMeasurement;
import com.personaltracker.model.Reminder;
import com.personaltracker.model.UserSession;
import com.personaltracker.model.Workout;
import com.personaltracker.model.WorkoutExercise;
import com.personaltracker.service.SupabaseClient;
import com.personaltracker.util.DataSyncManager;
import com.personaltracker.util.DateUtil;
import com.personaltracker.util.HealthCalculator;
import com.personaltracker.util.IconFactory;
import com.personaltracker.util.ValidationUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Workout Management view with search, filter, interactive timer, and exercise sets logging.
 */
public class WorkoutPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    public static class DailyRoutine {
        public final DayOfWeek dayOfWeek;
        public final String shortDay;
        public final String title;
        public final String type;
        public final int durationMinutes;
        public final int calories;
        public final String[][] exercises;

        public DailyRoutine(DayOfWeek dayOfWeek, String shortDay, String title, String type, int durationMinutes, int calories, String[][] exercises) {
            this.dayOfWeek = dayOfWeek;
            this.shortDay = shortDay;
            this.title = title;
            this.type = type;
            this.durationMinutes = durationMinutes;
            this.calories = calories;
            this.exercises = exercises;
        }
    }

    public static class WorkoutSplit {
        public final String id;
        public final String name;
        public final String description;
        public final DailyRoutine[] routines;

        public WorkoutSplit(String id, String name, String description, DailyRoutine[] routines) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.routines = routines;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final WorkoutSplit[] WORKOUT_SPLITS = new WorkoutSplit[]{
        new WorkoutSplit("PPL", "Push / Pull / Legs (PPL 6-Day)", "Hypertrophy & progressive overload with dedicated push, pull, leg, and conditioning sessions", new DailyRoutine[]{
            new DailyRoutine(DayOfWeek.MONDAY, "Mon", "Push Day (Chest, Delts & Triceps)", "Strength", 50, 420, new String[][]{
                {"Barbell Bench Press", "4", "8", "75.0"},
                {"Overhead Military Press", "3", "10", "45.0"},
                {"Incline Dumbbell Press", "3", "12", "22.0"},
                {"Tricep Cable Pushdown", "3", "12", "25.0"}
            }),
            new DailyRoutine(DayOfWeek.TUESDAY, "Tue", "Pull Day (Back, Biceps & Rear Delts)", "Strength", 50, 410, new String[][]{
                {"Barbell Deadlift", "4", "6", "100.0"},
                {"Lat Pulldown", "3", "10", "55.0"},
                {"Barbell Bent-Over Row", "3", "10", "50.0"},
                {"Dumbbell Bicep Curls", "3", "12", "14.0"}
            }),
            new DailyRoutine(DayOfWeek.WEDNESDAY, "Wed", "Leg Day (Quads, Hamstrings & Calves)", "Strength", 55, 480, new String[][]{
                {"Barbell Back Squat", "4", "8", "90.0"},
                {"Romanian Deadlift", "3", "10", "75.0"},
                {"Leg Press", "3", "12", "140.0"},
                {"Standing Calf Raises", "4", "15", "45.0"}
            }),
            new DailyRoutine(DayOfWeek.THURSDAY, "Thu", "Cardio Conditioning & Core", "Cardio", 40, 380, new String[][]{
                {"5K Interval Run", "1", "1", "0.0"},
                {"Hanging Leg Raises", "3", "12", "0.0"},
                {"Plank Hold", "3", "60", "0.0"},
                {"Russian Twists", "3", "20", "10.0"}
            }),
            new DailyRoutine(DayOfWeek.FRIDAY, "Fri", "Upper Body Hypertrophy & Arms", "Strength", 45, 390, new String[][]{
                {"Incline Dumbbell Bench", "4", "10", "24.0"},
                {"Cable Seated Rows", "3", "12", "50.0"},
                {"Lateral Dumbbell Raises", "4", "15", "10.0"},
                {"Skull Crushers", "3", "12", "25.0"}
            }),
            new DailyRoutine(DayOfWeek.SATURDAY, "Sat", "Full Body HIIT & Power", "HIIT", 35, 410, new String[][]{
                {"Kettlebell Swings", "4", "20", "20.0"},
                {"Burpees", "4", "12", "0.0"},
                {"Box Jumps", "3", "12", "0.0"},
                {"Dumbbell Thrusters", "3", "12", "16.0"}
            }),
            new DailyRoutine(DayOfWeek.SUNDAY, "Sun", "Active Recovery & Mobility", "Flexibility", 30, 180, new String[][]{
                {"Deep Dynamic Mobility", "1", "15", "0.0"},
                {"Foam Rolling & Release", "1", "15", "0.0"},
                {"Core Breathing & Stretch", "1", "10", "0.0"}
            })
        }),

        new WorkoutSplit("UPPER_LOWER", "Upper / Lower Split (4-5 Day)", "Heavy compound upper and lower days balanced with athletic intervals and core", new DailyRoutine[]{
            new DailyRoutine(DayOfWeek.MONDAY, "Mon", "Upper Body Heavy Strength", "Strength", 55, 440, new String[][]{
                {"Barbell Bench Press", "4", "6", "80.0"},
                {"Barbell Bent-Over Row", "4", "6", "70.0"},
                {"Overhead Military Press", "3", "8", "48.0"},
                {"Weighted Pull-ups", "3", "8", "0.0"}
            }),
            new DailyRoutine(DayOfWeek.TUESDAY, "Tue", "Lower Body Heavy Strength", "Strength", 55, 490, new String[][]{
                {"Barbell Back Squat", "4", "6", "95.0"},
                {"Romanian Deadlift", "4", "8", "80.0"},
                {"Walking Dumbbell Lunges", "3", "12", "20.0"},
                {"Standing Calf Raises", "4", "15", "45.0"}
            }),
            new DailyRoutine(DayOfWeek.WEDNESDAY, "Wed", "Cardio Conditioning & Core", "Cardio", 35, 320, new String[][]{
                {"Zone 2 Incline Walk", "1", "30", "0.0"},
                {"Hanging Knee Tucks", "3", "15", "0.0"},
                {"Cable Woodchoppers", "3", "12", "15.0"}
            }),
            new DailyRoutine(DayOfWeek.THURSDAY, "Thu", "Upper Body Hypertrophy & Volume", "Strength", 50, 420, new String[][]{
                {"Incline Dumbbell Press", "4", "10", "26.0"},
                {"Cable Lat Pulldown", "3", "12", "60.0"},
                {"Dumbbell Lateral Raises", "4", "15", "10.0"},
                {"Incline Dumbbell Curls", "3", "12", "12.0"},
                {"Rope Tricep Pushdown", "3", "15", "22.0"}
            }),
            new DailyRoutine(DayOfWeek.FRIDAY, "Fri", "Lower Body Hypertrophy & Glutes", "Strength", 50, 460, new String[][]{
                {"Front Squat", "3", "8", "70.0"},
                {"Leg Press", "4", "12", "150.0"},
                {"Lying Leg Curls", "3", "12", "45.0"},
                {"Seated Calf Raises", "4", "15", "40.0"}
            }),
            new DailyRoutine(DayOfWeek.SATURDAY, "Sat", "HIIT Sprint Conditioning", "HIIT", 30, 350, new String[][]{
                {"Rowing Machine Sprints", "5", "1", "0.0"},
                {"Kettlebell Snatch", "3", "12", "16.0"},
                {"Mountain Climbers", "3", "30", "0.0"}
            }),
            new DailyRoutine(DayOfWeek.SUNDAY, "Sun", "Active Mobility & Foam Rolling", "Flexibility", 30, 160, new String[][]{
                {"Full Body Foam Rolling", "1", "15", "0.0"},
                {"Hip & Ankle Mobility Flow", "1", "15", "0.0"}
            })
        }),

        new WorkoutSplit("ARNOLD", "Arnold Golden Era Split", "Legendary high-volume chest/back, shoulders/arms, and legs antagonist super-sets", new DailyRoutine[]{
            new DailyRoutine(DayOfWeek.MONDAY, "Mon", "Chest & Back Super-sets", "Strength", 55, 470, new String[][]{
                {"Flat Barbell Bench Press", "4", "10", "75.0"},
                {"Wide-Grip Lat Pulldown", "4", "10", "60.0"},
                {"Incline Dumbbell Flyes", "3", "12", "18.0"},
                {"T-Bar Row", "3", "10", "55.0"}
            }),
            new DailyRoutine(DayOfWeek.TUESDAY, "Tue", "Shoulders & Arms Blast", "Strength", 55, 450, new String[][]{
                {"Seated Dumbbell Shoulder Press", "4", "10", "22.0"},
                {"Barbell Standing Bicep Curl", "4", "10", "30.0"},
                {"Close-Grip Bench Press", "4", "10", "60.0"},
                {"Dumbbell Hammer Curls", "3", "12", "14.0"},
                {"Overhead Tricep Extension", "3", "12", "25.0"}
            }),
            new DailyRoutine(DayOfWeek.WEDNESDAY, "Wed", "Legs & Lower Back Power", "Strength", 55, 500, new String[][]{
                {"Barbell Back Squat", "5", "8", "95.0"},
                {"Stiff-Legged Deadlift", "4", "10", "80.0"},
                {"Leg Extension", "3", "15", "50.0"},
                {"Standing Calf Raises", "4", "15", "50.0"}
            }),
            new DailyRoutine(DayOfWeek.THURSDAY, "Thu", "Chest & Back Detail & Pump", "Strength", 50, 430, new String[][]{
                {"Incline Barbell Bench", "4", "10", "65.0"},
                {"Cable Seated Rows", "4", "12", "55.0"},
                {"Dumbbell Pullovers", "3", "12", "24.0"},
                {"Chin-ups", "3", "8", "0.0"}
            }),
            new DailyRoutine(DayOfWeek.FRIDAY, "Fri", "Shoulders & Arms Volume", "Strength", 50, 420, new String[][]{
                {"Arnold Dumbbell Press", "4", "12", "18.0"},
                {"Cable Lateral Raises", "4", "15", "10.0"},
                {"Preacher Curls", "3", "12", "25.0"},
                {"Parallel Bar Dips", "3", "12", "0.0"}
            }),
            new DailyRoutine(DayOfWeek.SATURDAY, "Sat", "Legs & Abs Conditioning", "Strength", 45, 410, new String[][]{
                {"Hack Squat", "4", "10", "100.0"},
                {"Lying Hamstring Curls", "4", "12", "40.0"},
                {"Hanging Leg Raises", "4", "15", "0.0"}
            }),
            new DailyRoutine(DayOfWeek.SUNDAY, "Sun", "Rest & Golden Era Walk", "Flexibility", 25, 130, new String[][]{
                {"Brisk Walking & Stretches", "1", "20", "0.0"}
            })
        }),

        new WorkoutSplit("BRO_SPLIT", "Classic Bro Split (Body Part)", "1 muscle group per day targeting maximum pump, intensity, and deep recovery", new DailyRoutine[]{
            new DailyRoutine(DayOfWeek.MONDAY, "Mon", "Chest Day Demolition", "Strength", 50, 410, new String[][]{
                {"Flat Barbell Bench Press", "4", "8", "75.0"},
                {"Incline Dumbbell Press", "4", "10", "24.0"},
                {"Cable Crossover Flyes", "3", "12", "15.0"},
                {"Push-ups to Failure", "3", "15", "0.0"}
            }),
            new DailyRoutine(DayOfWeek.TUESDAY, "Tue", "Heavy Back & Thickness", "Strength", 50, 430, new String[][]{
                {"Conventional Deadlift", "4", "6", "105.0"},
                {"Lat Pulldowns", "4", "10", "55.0"},
                {"One-Arm Dumbbell Rows", "3", "10", "28.0"},
                {"Barbell Shrugs", "4", "12", "70.0"}
            }),
            new DailyRoutine(DayOfWeek.WEDNESDAY, "Wed", "Leg Day Demolition", "Strength", 55, 480, new String[][]{
                {"Barbell Back Squat", "4", "8", "90.0"},
                {"Leg Press", "4", "12", "140.0"},
                {"Seated Hamstring Curls", "3", "12", "45.0"},
                {"Standing Calf Raises", "4", "15", "45.0"}
            }),
            new DailyRoutine(DayOfWeek.THURSDAY, "Thu", "Shoulders & Traps Boulders", "Strength", 45, 370, new String[][]{
                {"Overhead Military Press", "4", "8", "45.0"},
                {"Dumbbell Lateral Raises", "4", "12", "10.0"},
                {"Face Pulls", "3", "15", "25.0"},
                {"Front Dumbbell Raises", "3", "12", "10.0"}
            }),
            new DailyRoutine(DayOfWeek.FRIDAY, "Fri", "Arm Blaster (Bi's & Tri's)", "Strength", 45, 360, new String[][]{
                {"Barbell Bicep Curls", "4", "10", "30.0"},
                {"Skull Crushers", "4", "10", "28.0"},
                {"Cable Rope Hammer Curls", "3", "12", "20.0"},
                {"Overhead Cable Tricep Extension", "3", "12", "25.0"}
            }),
            new DailyRoutine(DayOfWeek.SATURDAY, "Sat", "Cardio & Core Shred", "Cardio", 40, 390, new String[][]{
                {"5K Treadmill Run", "1", "1", "0.0"},
                {"Bicycle Crunches", "3", "20", "0.0"},
                {"Hanging Leg Raises", "3", "12", "0.0"}
            }),
            new DailyRoutine(DayOfWeek.SUNDAY, "Sun", "Rest & Active Stretching", "Flexibility", 25, 140, new String[][]{
                {"Full Body Dynamic Stretch", "1", "20", "0.0"}
            })
        }),

        new WorkoutSplit("FULL_BODY", "Full Body Functional (3-Day)", "Whole-body compound foundation with active recovery and mobility on off-days", new DailyRoutine[]{
            new DailyRoutine(DayOfWeek.MONDAY, "Mon", "Full Body A (Squat, Bench, Row)", "Strength", 55, 460, new String[][]{
                {"Barbell Back Squat", "4", "6", "90.0"},
                {"Barbell Bench Press", "4", "6", "75.0"},
                {"Barbell Bent-Over Row", "3", "8", "60.0"},
                {"Plank Hold", "3", "60", "0.0"}
            }),
            new DailyRoutine(DayOfWeek.TUESDAY, "Tue", "Zone 2 Steady Cardio", "Cardio", 40, 290, new String[][]{
                {"30-Min Incline Treadmill Walk", "1", "30", "0.0"},
                {"Thoracic Spine Flow", "1", "15", "0.0"}
            }),
            new DailyRoutine(DayOfWeek.WEDNESDAY, "Wed", "Full Body B (Deadlift, OHP, Pull-up)", "Strength", 55, 470, new String[][]{
                {"Conventional Deadlift", "4", "5", "105.0"},
                {"Overhead Military Press", "4", "6", "45.0"},
                {"Weighted Pull-ups", "3", "6", "5.0"},
                {"Hanging Leg Raises", "3", "12", "0.0"}
            }),
            new DailyRoutine(DayOfWeek.THURSDAY, "Thu", "Active Recovery & Mobility", "Flexibility", 30, 170, new String[][]{
                {"Foam Rolling & Joint Mobility", "1", "20", "0.0"}
            }),
            new DailyRoutine(DayOfWeek.FRIDAY, "Fri", "Full Body C (Front Squat, Incline, Dips)", "Strength", 50, 430, new String[][]{
                {"Front Squat", "3", "8", "70.0"},
                {"Incline Dumbbell Press", "3", "10", "24.0"},
                {"Dumbbell Romanian Deadlift", "3", "10", "26.0"},
                {"Parallel Bar Dips", "3", "10", "0.0"}
            }),
            new DailyRoutine(DayOfWeek.SATURDAY, "Sat", "HIIT Intervals & MetCon", "HIIT", 30, 360, new String[][]{
                {"Kettlebell Swings", "4", "20", "20.0"},
                {"Assault Bike Intervals", "5", "1", "0.0"},
                {"Burpees", "3", "12", "0.0"}
            }),
            new DailyRoutine(DayOfWeek.SUNDAY, "Sun", "Rest & Yoga Recovery Flow", "Flexibility", 25, 140, new String[][]{
                {"Restorative Yoga", "1", "25", "0.0"}
            })
        }),

        new WorkoutSplit("HIIT_FATLOSS", "HIIT & Fat Loss Conditioning", "Calorie incineration, metabolic conditioning, kettlebells, and endurance intervals", new DailyRoutine[]{
            new DailyRoutine(DayOfWeek.MONDAY, "Mon", "HIIT MetCon & Core Burner", "HIIT", 40, 450, new String[][]{
                {"Kettlebell Swings", "4", "20", "20.0"},
                {"Burpees", "4", "15", "0.0"},
                {"Mountain Climbers", "4", "30", "0.0"},
                {"Plank Jacks", "3", "25", "0.0"}
            }),
            new DailyRoutine(DayOfWeek.TUESDAY, "Tue", "Functional Upper & Intervals", "HIIT", 45, 440, new String[][]{
                {"Dumbbell Thrusters", "4", "12", "14.0"},
                {"Push-ups", "4", "15", "0.0"},
                {"Renegade Rows", "3", "12", "12.0"},
                {"Jump Rope Intervals", "5", "1", "0.0"}
            }),
            new DailyRoutine(DayOfWeek.WEDNESDAY, "Wed", "Lower Body Tabata & Plyometrics", "HIIT", 40, 420, new String[][]{
                {"Jump Squats", "4", "15", "0.0"},
                {"Walking Lunges", "3", "16", "12.0"},
                {"Box Jumps", "3", "12", "0.0"},
                {"Wall Sit Hold", "3", "45", "0.0"}
            }),
            new DailyRoutine(DayOfWeek.THURSDAY, "Thu", "Steady State Cardio (Zone 2)", "Cardio", 45, 410, new String[][]{
                {"5K Outdoor or Treadmill Run", "1", "1", "0.0"},
                {"Core Hollow Body Hold", "3", "45", "0.0"}
            }),
            new DailyRoutine(DayOfWeek.FRIDAY, "Fri", "Full Body Kettlebell Circuit", "HIIT", 40, 430, new String[][]{
                {"Kettlebell Clean & Press", "4", "10", "16.0"},
                {"Goblet Squat", "4", "15", "24.0"},
                {"Russian Twists", "4", "20", "10.0"},
                {"Battle Ropes", "4", "30", "0.0"}
            }),
            new DailyRoutine(DayOfWeek.SATURDAY, "Sat", "Sprint Intervals (Tabata Protocol)", "Cardio", 30, 340, new String[][]{
                {"100m Sprints", "6", "1", "0.0"},
                {"Hanging Knee Raises", "3", "15", "0.0"}
            }),
            new DailyRoutine(DayOfWeek.SUNDAY, "Sun", "Restorative Yoga & Recovery", "Flexibility", 30, 160, new String[][]{
                {"Vinyasa Flow", "1", "30", "0.0"}
            })
        }),

        new WorkoutSplit("CALISTHENICS", "Calisthenics & Bodyweight Mastery", "Pure bodyweight strength, relative power, gymnastic core, and balance mastery", new DailyRoutine[]{
            new DailyRoutine(DayOfWeek.MONDAY, "Mon", "Push Mastery (Dips & Push-ups)", "Strength", 45, 360, new String[][]{
                {"Parallel Bar Dips", "4", "10", "0.0"},
                {"Decline Push-ups", "3", "15", "0.0"},
                {"Pike Push-ups", "3", "10", "0.0"},
                {"Diamond Push-ups", "3", "12", "0.0"}
            }),
            new DailyRoutine(DayOfWeek.TUESDAY, "Tue", "Pull Mastery (Pull-ups & Rows)", "Strength", 45, 370, new String[][]{
                {"Wide-Grip Pull-ups", "4", "8", "0.0"},
                {"Chin-ups", "3", "8", "0.0"},
                {"Inverted Bodyweight Rows", "3", "12", "0.0"},
                {"Dead Hang Hold", "3", "45", "0.0"}
            }),
            new DailyRoutine(DayOfWeek.WEDNESDAY, "Wed", "Explosive Lower Body & Pistol Squats", "Strength", 45, 390, new String[][]{
                {"Pistol Squats", "3", "8", "0.0"},
                {"Jump Squats", "4", "15", "0.0"},
                {"Bulgarian Split Squats", "3", "12", "0.0"},
                {"Single-Leg Calf Raises", "4", "15", "0.0"}
            }),
            new DailyRoutine(DayOfWeek.THURSDAY, "Thu", "Core Strength & Handstand Balance", "Strength", 40, 310, new String[][]{
                {"Handstand Wall Hold", "4", "30", "0.0"},
                {"Hanging Leg Raises", "4", "12", "0.0"},
                {"L-Sit Hold", "4", "15", "0.0"}
            }),
            new DailyRoutine(DayOfWeek.FRIDAY, "Fri", "Full Body Calisthenics Combo", "Strength", 50, 410, new String[][]{
                {"High Pull-up Progression", "4", "5", "0.0"},
                {"Parallel Bar Dips", "4", "12", "0.0"},
                {"Archer Push-ups", "3", "10", "0.0"},
                {"Bodyweight Walking Lunges", "3", "20", "0.0"}
            }),
            new DailyRoutine(DayOfWeek.SATURDAY, "Sat", "Endurance & Speed Circuit", "HIIT", 35, 340, new String[][]{
                {"Burpees to Pull-up", "3", "10", "0.0"},
                {"Bear Crawls", "3", "20", "0.0"},
                {"Mountain Climbers", "3", "30", "0.0"}
            }),
            new DailyRoutine(DayOfWeek.SUNDAY, "Sun", "Mobility & Shoulder Dislocates", "Flexibility", 30, 150, new String[][]{
                {"Shoulder Dislocates & Wrist Flow", "1", "15", "0.0"},
                {"Hamstring & Hip Mobility", "1", "20", "0.0"}
            })
        })
    };

    public static final DailyRoutine[] WEEKLY_ROUTINES = WORKOUT_SPLITS[0].routines;
    private static WorkoutSplit currentSelectedSplit = WORKOUT_SPLITS[0];

    public static WorkoutSplit getCurrentSelectedSplit() {
        return currentSelectedSplit;
    }

    public static void setCurrentSelectedSplit(WorkoutSplit split) {
        if (split != null) {
            currentSelectedSplit = split;
        }
    }

    public static DailyRoutine getRoutineForDay(DayOfWeek day) {
        return getRoutineForDay(currentSelectedSplit, day);
    }

    public static DailyRoutine getRoutineForDay(WorkoutSplit split, DayOfWeek day) {
        WorkoutSplit targetSplit = split != null ? split : currentSelectedSplit;
        if (targetSplit != null && targetSplit.routines != null) {
            for (DailyRoutine r : targetSplit.routines) {
                if (r.dayOfWeek == day) return r;
            }
        }
        for (DailyRoutine r : WEEKLY_ROUTINES) {
            if (r.dayOfWeek == day) return r;
        }
        return WEEKLY_ROUTINES[0];
    }

    public static class WorkoutRowItem {
        public final LocalDate date;
        public final Workout workout;
        public final boolean isRestDay;

        public WorkoutRowItem(LocalDate date, Workout workout, boolean isRestDay) {
            this.date = date;
            this.workout = workout;
            this.isRestDay = isRestDay;
        }
    }

    private final WorkoutDAO workoutDAO = new WorkoutDAO();
    private final AchievementDAO achievementDAO = new AchievementDAO();
    private final ReminderDAO reminderDAO = new ReminderDAO();

    private final CardLayout viewCardLayout = new CardLayout();
    private final JPanel viewContainer = new JPanel(viewCardLayout);
    private final JButton btnTabWorkouts = new JButton("Workouts Log");
    private final JButton btnTabCoachAssigned = new JButton("Coach Planned");
    private final JButton btnTabReminders = new JButton("Schedule & Reminders");

    // Header Action Buttons (only corresponding button shown per tab)
    private final ModernButton btnHeaderLogWorkout = new ModernButton("Log Workout", ModernButton.ButtonStyle.PRIMARY);
    private final ModernButton btnHeaderRefreshCoach = new ModernButton("Refresh Routines", ModernButton.ButtonStyle.SECONDARY);
    private final ModernButton btnHeaderSetReminder = new ModernButton("Set Reminder", ModernButton.ButtonStyle.PRIMARY);

    // KPI Metric Labels
    private final JLabel lblKpiWorkouts = new JLabel("0");
    private final JLabel lblKpiDuration = new JLabel("0h 0m");
    private final JLabel lblKpiCalories = new JLabel("0 kcal");
    private final JLabel lblKpiStreak = new JLabel("0 Days");

    // Coach Planned Workouts Components
    private final JPanel pnlCoachWorkoutsList = new JPanel();
    private final JPanel pnlCoachFilters = new JPanel();
    private final JLabel lblCoachKpiTotal = new JLabel("0");
    private final JLabel lblCoachKpiPlanned = new JLabel("0");
    private final JLabel lblCoachKpiCompleted = new JLabel("0");
    private String coachFilter = "ALL";

    // Workouts Log Components
    private final ModernTextField txtSearch = new ModernTextField("", "Search workouts by title or type...");
    private final JComboBox<String> cmbTypeFilter = new JComboBox<>(new String[]{"All Types", "Strength", "Cardio", "HIIT", "Yoga", "Flexibility", "Endurance", "Recovery", "Other"});
    private final JComboBox<String> cmbStatusFilter = new JComboBox<>(new String[]{"All Statuses", "Coach Assigned", "COMPLETED", "PLANNED", "REST DAY"});
    private final ModernButton btnFilterCoachAssigned = new ModernButton("Coach Assigned", ModernButton.ButtonStyle.SECONDARY);

    private final DefaultTableModel tableModel;
    private final ModernTable table;
    private final TableRowSorter<DefaultTableModel> sorter;

    private final WorkoutTimerPanel timerPanel = new WorkoutTimerPanel();
    private final JLabel lblSummaryStats = new JLabel("Loading workouts...");

    private List<Workout> currentWorkouts = new ArrayList<>();
    private final List<WorkoutRowItem> displayedRowItems = new ArrayList<>();
    private double cachedUserWeightKg = 70.0;

    // Reminders & Schedule Components
    private final JPanel pnlTodayAgenda = new JPanel();
    private final JPanel pnlRemindersList = new JPanel();
    private final JPanel pnlScheduleFilters = new JPanel();
    private List<Reminder> currentReminders = new ArrayList<>();
    private String reminderFilter = "ALL";

    public WorkoutPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        DataSyncManager.registerListener(type -> {
            if (type == DataSyncManager.DataType.WORKOUTS || type == DataSyncManager.DataType.PROFILE) {
                loadWorkouts();
            } else if (type == DataSyncManager.DataType.REMINDERS) {
                loadReminders();
            }
        });

        // Setup Workouts Table Model
        String[] columnNames = {"Date", "Title", "Type", "Duration", "Calories", "Status", "Notes"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new ModernTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // Status column pill renderer
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                String val = value != null ? value.toString() : "";

                // Check if this row was assigned by a coach/trainer via column 6 notes
                int mRow = table.convertRowIndexToModel(row);
                Object notesObj = mRow < table.getModel().getRowCount() ? table.getModel().getValueAt(mRow, 6) : "";
                String notesStr = notesObj != null ? notesObj.toString() : "";
                boolean isCoachAssigned = notesStr.contains("[Coach") || notesStr.contains("[Trainer");

                if ("COMPLETED".equalsIgnoreCase(val)) {
                    if (isCoachAssigned) {
                        lbl.setText("COACH COMPLETED");
                        lbl.setIcon(IconFactory.createIcon(IconFactory.IconType.CHECK, 11, ThemeConfig.SUCCESS));
                        lbl.setForeground(ThemeConfig.SUCCESS);
                        lbl.setToolTipText("Coach-assigned workout completed! Notes: " + notesStr);
                    } else {
                        lbl.setText("COMPLETED");
                        lbl.setIcon(IconFactory.createIcon(IconFactory.IconType.CHECK, 11, ThemeConfig.SUCCESS));
                        lbl.setForeground(ThemeConfig.SUCCESS);
                        lbl.setToolTipText(null);
                    }
                } else if ("REST DAY".equalsIgnoreCase(val)) {
                    lbl.setText("REST DAY");
                    lbl.setIcon(IconFactory.createIcon(IconFactory.IconType.SLEEP, 11, new Color(0x94, 0xA3, 0xB8)));
                    lbl.setForeground(new Color(0x94, 0xA3, 0xB8)); // Modern cool slate
                    lbl.setToolTipText(null);
                } else {
                    if (isCoachAssigned) {
                        lbl.setText("COACH PLANNED");
                        lbl.setIcon(IconFactory.createIcon(IconFactory.IconType.WORKOUT, 12, new Color(0xFF, 0x7A, 0x59)));
                        lbl.setForeground(new Color(0xFF, 0x7A, 0x59));
                        lbl.setToolTipText("Assigned by Coach! Notes: " + notesStr);
                    } else {
                        lbl.setText("PLANNED");
                        lbl.setIcon(IconFactory.createIcon(IconFactory.IconType.TIMER, 11, ThemeConfig.WARNING));
                        lbl.setForeground(ThemeConfig.WARNING);
                        lbl.setToolTipText(null);
                    }
                }
                lbl.setIconTextGap(5);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
                if (!isSelected) {
                    lbl.setBackground(row % 2 == 0 ? ThemeConfig.BG_CARD : ThemeConfig.BG_CARD_HOVER);
                }
                return lbl;
            }
        });

        // Double click row to edit workout
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    editSelectedWorkout();
                }
            }
        });

        // 1. Header with View Tabs & Action Buttons
        add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. Center Content Views (CardLayout - fills entire window without page scroll!)
        viewContainer.setOpaque(false);
        viewContainer.add(createWorkoutsView(), "WORKOUTS");
        viewContainer.add(createCoachPlannedWorkoutsView(), "COACH_WORKOUTS");
        viewContainer.add(createRemindersView(), "REMINDERS");
        add(viewContainer, BorderLayout.CENTER);

        // Setup filter listener
        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
        });
        cmbTypeFilter.addActionListener(e -> applyFilters());
        cmbStatusFilter.addActionListener(e -> applyFilters());
    }

    private JPanel createWorkoutsView() {
        JPanel view = new JPanel(new BorderLayout(0, 8));
        view.setOpaque(false);

        // Top Section of Workouts view
        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);

        // 1. KPI Metric Cards Row
        topSection.add(createKpiCardsPanel());
        topSection.add(Box.createVerticalStrut(8));

        // 2. Interactive Stopwatch Timer (Full width with generous breathing room)
        timerPanel.setOnLogCallback(mins -> openAddWorkoutDialog(mins));
        timerPanel.setPreferredSize(new Dimension(0, 64));
        timerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        topSection.add(timerPanel);
        topSection.add(Box.createVerticalStrut(8));

        // 3. Quick Log Workout Presets Bar
        JPanel presetsBar = createQuickWorkoutPresetsBar();
        presetsBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        topSection.add(presetsBar);
        topSection.add(Box.createVerticalStrut(8));

        // 4. Search & Filter Bar
        topSection.add(createFilterPanel());

        view.add(topSection, BorderLayout.NORTH);

        // 4. Workouts Table - Fills all available center height in the frame!
        JScrollPane scrollTable = ModernTable.wrapInScrollPane(table);
        view.add(scrollTable, BorderLayout.CENTER);

        // 5. Bottom Controls & Summary
        view.add(createBottomBar(), BorderLayout.SOUTH);

        return view;
    }

    private JPanel createKpiCardsPanel() {
        JPanel grid = new JPanel(new GridLayout(1, 4, 10, 0));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        grid.setPreferredSize(new Dimension(0, 64));

        grid.add(createMetricCard("Workouts Logged", lblKpiWorkouts, IconFactory.IconType.WORKOUT, ThemeConfig.PRIMARY_ACCENT));
        grid.add(createMetricCard("Training Volume", lblKpiDuration, IconFactory.IconType.TIMER, ThemeConfig.PRIMARY_ACCENT));
        grid.add(createMetricCard("Calories Burned", lblKpiCalories, IconFactory.IconType.FIRE, ThemeConfig.WARNING));
        grid.add(createMetricCard("Current Streak", lblKpiStreak, IconFactory.IconType.CHECK, ThemeConfig.SUCCESS));

        return grid;
    }

    private ModernCard createMetricCard(String title, JLabel valLabel, IconFactory.IconType iconType, Color iconColor) {
        ModernCard card = new ModernCard(12, new Insets(8, 12, 8, 12));
        card.setLayout(new BorderLayout(8, 0));

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 1));
        left.setOpaque(false);
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblTitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        left.add(lblTitle);

        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        valLabel.setForeground(ThemeConfig.TEXT_PRIMARY);
        left.add(valLabel);

        card.add(left, BorderLayout.CENTER);

        JLabel icon = new JLabel(IconFactory.createIcon(iconType, 20, iconColor));
        card.add(icon, BorderLayout.EAST);

        return card;
    }

    private final JComboBox<WorkoutSplit> cmbSplitSelector = new JComboBox<>(WORKOUT_SPLITS);
    private final JPanel presetsChipsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));

    private JPanel createQuickWorkoutPresetsBar() {
        ModernCard card = new ModernCard(12, new Insets(5, 12, 5, 12));
        card.setLayout(new BorderLayout(8, 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        JLabel lbl = new JLabel("Daily Split:");
        lbl.setFont(ThemeConfig.FONT_SMALL);
        lbl.setForeground(ThemeConfig.TEXT_MUTED);
        left.add(lbl);

        cmbSplitSelector.setFont(new Font("Segoe UI", Font.BOLD, 11));
        cmbSplitSelector.setPreferredSize(new Dimension(215, 28));
        cmbSplitSelector.setSelectedItem(currentSelectedSplit);
        cmbSplitSelector.setToolTipText("Select your preferred workout split routine");
        cmbSplitSelector.addActionListener(e -> {
            WorkoutSplit selected = (WorkoutSplit) cmbSplitSelector.getSelectedItem();
            if (selected != null) {
                currentSelectedSplit = selected;
                rebuildPresetsChips();
                loadWorkouts();
            }
        });
        left.add(cmbSplitSelector);
        card.add(left, BorderLayout.WEST);

        presetsChipsPanel.setOpaque(false);
        rebuildPresetsChips();
        card.add(presetsChipsPanel, BorderLayout.CENTER);

        return card;
    }

    private void rebuildPresetsChips() {
        presetsChipsPanel.removeAll();

        DailyRoutine todayRoutine = getRoutineForDay(currentSelectedSplit, LocalDate.now().getDayOfWeek());
        JButton btnToday = new JButton("Today (" + todayRoutine.shortDay + "): " + todayRoutine.title + " (" + todayRoutine.durationMinutes + "m)");
        btnToday.setIcon(IconFactory.createIcon(IconFactory.IconType.STAR, 12, Color.WHITE));
        btnToday.setIconTextGap(6);
        btnToday.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnToday.setForeground(Color.WHITE);
        Color todayBg = ThemeConfig.isDarkMode() ? new Color(0x3B, 0x42, 0x52) : ThemeConfig.PRIMARY_ACCENT;
        Color todayBorder = ThemeConfig.isDarkMode() ? new Color(0x60, 0x6C, 0x80) : ThemeConfig.PRIMARY_ACCENT;
        btnToday.setBackground(todayBg);
        btnToday.setFocusPainted(false);
        btnToday.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnToday.setToolTipText("Today's recommended routine. Click to review & log, or Shift+Click to instantly log.");
        btnToday.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(todayBorder, 1, true),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        btnToday.addActionListener(e -> {
            boolean isShift = (e.getModifiers() & java.awt.event.ActionEvent.SHIFT_MASK) != 0;
            if (isShift) {
                quickSavePresetDirectly(todayRoutine.title, todayRoutine.type, todayRoutine.durationMinutes, todayRoutine.calories, todayRoutine.exercises);
            } else {
                openAddWorkoutDialogWithPreset(todayRoutine.title, todayRoutine.type, todayRoutine.durationMinutes, todayRoutine.calories, todayRoutine.exercises);
            }
        });
        presetsChipsPanel.add(btnToday);

        for (DailyRoutine r : currentSelectedSplit.routines) {
            presetsChipsPanel.add(createDailyRoutineChip(r));
        }

        presetsChipsPanel.revalidate();
        presetsChipsPanel.repaint();
    }

    private JButton createDailyRoutineChip(DailyRoutine r) {
        boolean isToday = LocalDate.now().getDayOfWeek() == r.dayOfWeek;
        JButton btn = new JButton(r.shortDay + ": " + r.title.split(" ")[0]);
        btn.setFont(new Font("Segoe UI", isToday ? Font.BOLD : Font.PLAIN, 11));

        Color bg;
        Color fg;
        Color border;
        if (ThemeConfig.isDarkMode()) {
            if (isToday) {
                bg = new Color(0x3B, 0x42, 0x52);
                fg = Color.WHITE;
                border = new Color(0x60, 0x6C, 0x80);
            } else {
                bg = ThemeConfig.BG_CARD;
                fg = ThemeConfig.TEXT_SECONDARY;
                border = ThemeConfig.BORDER_COLOR;
            }
        } else {
            if (isToday) {
                bg = ThemeConfig.PRIMARY_ACCENT;
                fg = Color.WHITE;
                border = ThemeConfig.PRIMARY_ACCENT;
            } else {
                bg = ThemeConfig.BG_CARD;
                fg = ThemeConfig.TEXT_SECONDARY;
                border = ThemeConfig.BORDER_COLOR;
            }
        }

        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText(r.dayOfWeek.name() + ": " + r.title + " (" + r.durationMinutes + "m, " + r.type + ") - Click to review, Shift+Click to instantly log");
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1, true),
                BorderFactory.createEmptyBorder(4, 7, 4, 7)
        ));
        btn.addActionListener(e -> {
            boolean isShift = (e.getModifiers() & java.awt.event.ActionEvent.SHIFT_MASK) != 0;
            if (isShift) {
                quickSavePresetDirectly(r.title, r.type, r.durationMinutes, r.calories, r.exercises);
            } else {
                openAddWorkoutDialogWithPreset(r.title, r.type, r.durationMinutes, r.calories, r.exercises);
            }
        });
        return btn;
    }

    private void openAddWorkoutDialogForDate(LocalDate targetDate) {
        DailyRoutine routine = getRoutineForDay(targetDate.getDayOfWeek());
        Workout w = new Workout();
        w.setTitle(routine.title);
        w.setWorkoutType(routine.type);
        w.setDurationMinutes(routine.durationMinutes);
        w.setCaloriesBurned(routine.calories);
        w.setWorkoutDate(targetDate);
        w.setStatus("COMPLETED");

        if (routine.exercises != null) {
            List<WorkoutExercise> exList = new ArrayList<>();
            for (String[] row : routine.exercises) {
                WorkoutExercise we = new WorkoutExercise();
                we.setExerciseName(row[0]);
                we.setSets(Integer.parseInt(row[1]));
                we.setReps(Integer.parseInt(row[2]));
                we.setWeightKg(Double.parseDouble(row[3]));
                exList.add(we);
            }
            w.setExercises(exList);
        }

        WorkoutDialog dialog = new WorkoutDialog(SwingUtilities.getWindowAncestor(this), w, routine.durationMinutes);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            ToastNotification.showToast(this, "Workout logged for " + DateUtil.formatDate(targetDate) + "!", ToastNotification.ToastType.SUCCESS);
            loadWorkouts();
            checkWorkoutAchievements();
        }
    }

    private void quickSavePresetDirectly(String title, String type, int durationMinutes, int calories, String[][] exercises) {
        Workout w = new Workout();
        w.setTitle(title);
        w.setWorkoutType(type);
        w.setDurationMinutes(durationMinutes);
        w.setCaloriesBurned(calories);
        w.setWorkoutDate(LocalDate.now());
        w.setStatus("COMPLETED");

        if (exercises != null) {
            List<WorkoutExercise> exList = new ArrayList<>();
            for (String[] row : exercises) {
                WorkoutExercise we = new WorkoutExercise();
                we.setExerciseName(row[0]);
                we.setSets(Integer.parseInt(row[1]));
                we.setReps(Integer.parseInt(row[2]));
                we.setWeightKg(Double.parseDouble(row[3]));
                exList.add(we);
            }
            w.setExercises(exList);
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                workoutDAO.saveWorkout(w);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    ToastNotification.showToast(WorkoutPanel.this, "Quick logged: " + title + " (" + durationMinutes + "m)", ToastNotification.ToastType.SUCCESS);
                    loadWorkouts();
                    checkWorkoutAchievements();
                    DataSyncManager.fireDataChanged(DataSyncManager.DataType.WORKOUTS, DataSyncManager.DataType.GOALS);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(WorkoutPanel.this, "Quick log failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void openAddWorkoutDialogWithPreset(String title, String type, int durationMinutes, int calories, String[][] exercises) {
        Workout w = new Workout();
        w.setTitle(title);
        w.setWorkoutType(type);
        w.setDurationMinutes(durationMinutes);
        w.setCaloriesBurned(calories);
        w.setWorkoutDate(LocalDate.now());
        w.setStatus("COMPLETED");

        if (exercises != null) {
            List<WorkoutExercise> exList = new ArrayList<>();
            for (String[] row : exercises) {
                WorkoutExercise we = new WorkoutExercise();
                we.setExerciseName(row[0]);
                we.setSets(Integer.parseInt(row[1]));
                we.setReps(Integer.parseInt(row[2]));
                we.setWeightKg(Double.parseDouble(row[3]));
                exList.add(we);
            }
            w.setExercises(exList);
        }

        WorkoutDialog dialog = new WorkoutDialog(SwingUtilities.getWindowAncestor(this), w, durationMinutes);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            ToastNotification.showToast(this, "Workout logged successfully!", ToastNotification.ToastType.SUCCESS);
            loadWorkouts();
            checkWorkoutAchievements();
        }
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);

        // Left Title
        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setOpaque(false);
        JLabel title = new JLabel("Workout Management");
        title.setFont(ThemeConfig.FONT_TITLE_LARGE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        textPanel.add(title);

        JLabel subtitle = new JLabel("Plan routines, log exercises & track volume");
        subtitle.setFont(ThemeConfig.FONT_SMALL);
        subtitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        textPanel.add(subtitle);
        header.add(textPanel, BorderLayout.WEST);

        // Center: Segmented View Switcher Tabs with crisp icons (no square glyphs)
        JPanel tabSwitcher = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        tabSwitcher.setOpaque(false);
        setupTabButton(btnTabWorkouts, "Workouts Log", true, IconFactory.IconType.WORKOUT);
        setupTabButton(btnTabCoachAssigned, "Coach Planned", false, IconFactory.IconType.WORKOUT);
        setupTabButton(btnTabReminders, "Schedule & Reminders", false, IconFactory.IconType.REMINDERS);
        btnTabWorkouts.addActionListener(e -> selectWorkoutsTab());
        btnTabCoachAssigned.addActionListener(e -> selectCoachPlannedTab());
        btnTabReminders.addActionListener(e -> selectRemindersTab());
        tabSwitcher.add(btnTabWorkouts);
        tabSwitcher.add(btnTabCoachAssigned);
        tabSwitcher.add(btnTabReminders);
        header.add(tabSwitcher, BorderLayout.CENTER);

        // Right Action Buttons (ONLY the active tab's corresponding action button is displayed)
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);

        btnHeaderLogWorkout.setIcon(IconFactory.createIcon(IconFactory.IconType.PLUS, 13, Color.WHITE));
        btnHeaderLogWorkout.setIconTextGap(6);
        btnHeaderLogWorkout.setPreferredSize(new Dimension(140, 34));
        btnHeaderLogWorkout.addActionListener(e -> openAddWorkoutDialog(0));
        btnBar.add(btnHeaderLogWorkout);

        btnHeaderRefreshCoach.setIcon(IconFactory.createIcon(IconFactory.IconType.REFRESH, 13, ThemeConfig.PRIMARY_ACCENT));
        btnHeaderRefreshCoach.setIconTextGap(6);
        btnHeaderRefreshCoach.setPreferredSize(new Dimension(150, 34));
        btnHeaderRefreshCoach.addActionListener(e -> {
            loadWorkouts();
            ToastNotification.showToast(this, "Refreshing coach routines...", ToastNotification.ToastType.INFO);
        });
        btnHeaderRefreshCoach.setVisible(false);
        btnBar.add(btnHeaderRefreshCoach);

        btnHeaderSetReminder.setIcon(IconFactory.createIcon(IconFactory.IconType.PLUS, 13, Color.WHITE));
        btnHeaderSetReminder.setIconTextGap(6);
        btnHeaderSetReminder.setPreferredSize(new Dimension(140, 34));
        btnHeaderSetReminder.addActionListener(e -> openReminderDialog());
        btnHeaderSetReminder.setVisible(false); // Hidden on workouts tab by default
        btnBar.add(btnHeaderSetReminder);

        header.add(btnBar, BorderLayout.EAST);

        return header;
    }

    private void setupTabButton(JButton btn, String text, boolean active, IconFactory.IconType iconType) {
        btn.setText(text);
        Color bg;
        Color fg;
        Color border;
        if (ThemeConfig.isDarkMode()) {
            if (active) {
                bg = new Color(0x3B, 0x42, 0x52);
                fg = Color.WHITE;
                border = new Color(0x60, 0x6C, 0x80);
            } else {
                bg = ThemeConfig.BG_CARD;
                fg = ThemeConfig.TEXT_SECONDARY;
                border = ThemeConfig.BORDER_COLOR;
            }
        } else {
            if (active) {
                bg = ThemeConfig.PRIMARY_ACCENT;
                fg = Color.WHITE;
                border = ThemeConfig.PRIMARY_ACCENT;
            } else {
                bg = ThemeConfig.BG_CARD;
                fg = ThemeConfig.TEXT_SECONDARY;
                border = ThemeConfig.BORDER_COLOR;
            }
        }
        btn.setIcon(IconFactory.createIcon(iconType, 15, fg));
        btn.setIconTextGap(7);
        btn.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 12));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1, true),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));
    }

    public void switchToTab(String tabName) {
        viewCardLayout.show(viewContainer, tabName);
        boolean isWorkouts = "WORKOUTS".equals(tabName);
        boolean isCoach = "COACH_WORKOUTS".equals(tabName);
        boolean isReminders = "REMINDERS".equals(tabName);

        long coachCount = currentWorkouts != null ? currentWorkouts.stream().filter(w -> w.getNotes() != null && (w.getNotes().contains("[Coach") || w.getNotes().contains("[Trainer"))).count() : 0;
        int remCount = currentReminders != null ? currentReminders.size() : 0;

        setupTabButton(btnTabWorkouts, "Workouts Log", isWorkouts, IconFactory.IconType.WORKOUT);
        setupTabButton(btnTabCoachAssigned, "Coach Planned (" + coachCount + ")", isCoach, IconFactory.IconType.WORKOUT);
        setupTabButton(btnTabReminders, "Schedule & Reminders (" + remCount + ")", isReminders, IconFactory.IconType.REMINDERS);

        // Show ONLY the corresponding action button for current view
        btnHeaderLogWorkout.setVisible(isWorkouts);
        btnHeaderRefreshCoach.setVisible(isCoach);
        btnHeaderSetReminder.setVisible(isReminders);
        if (btnHeaderLogWorkout.getParent() != null) {
            btnHeaderLogWorkout.getParent().revalidate();
            btnHeaderLogWorkout.getParent().repaint();
        }

        if (isCoach) {
            refreshCoachPlannedWorkoutsView();
        }
    }

    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        panel.setPreferredSize(new Dimension(0, 34));

        panel.add(txtSearch, BorderLayout.CENTER);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filters.setOpaque(false);

        btnFilterCoachAssigned.setPreferredSize(new Dimension(165, 32));
        btnFilterCoachAssigned.setIcon(IconFactory.createIcon(IconFactory.IconType.WORKOUT, 12, ThemeConfig.PRIMARY_ACCENT));
        btnFilterCoachAssigned.setIconTextGap(5);
        btnFilterCoachAssigned.setVisible(false); // shown when coach workouts exist
        btnFilterCoachAssigned.addActionListener(e -> selectCoachPlannedTab());

        cmbTypeFilter.setPreferredSize(new Dimension(115, 32));
        cmbStatusFilter.setPreferredSize(new Dimension(135, 32));

        filters.add(btnFilterCoachAssigned);
        filters.add(cmbTypeFilter);
        filters.add(cmbStatusFilter);
        panel.add(filters, BorderLayout.EAST);

        return panel;
    }

    private JPanel createBottomBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setOpaque(false);

        lblSummaryStats.setFont(ThemeConfig.FONT_SMALL);
        lblSummaryStats.setForeground(ThemeConfig.TEXT_SECONDARY);
        bar.add(lblSummaryStats, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        ModernButton btnGoReminders = new ModernButton("Schedule & Reminders →", ModernButton.ButtonStyle.GHOST);
        btnGoReminders.setIcon(IconFactory.createIcon(IconFactory.IconType.REMINDERS, 12, ThemeConfig.PRIMARY_ACCENT));
        btnGoReminders.setIconTextGap(5);
        btnGoReminders.setPreferredSize(new Dimension(185, 30));
        btnGoReminders.addActionListener(e -> selectRemindersTab());

        ModernButton btnViewCoach = new ModernButton("View Coach Assigned", ModernButton.ButtonStyle.SECONDARY);
        btnViewCoach.setIcon(IconFactory.createIcon(IconFactory.IconType.WORKOUT, 12, ThemeConfig.PRIMARY_ACCENT));
        btnViewCoach.setIconTextGap(5);
        btnViewCoach.setPreferredSize(new Dimension(175, 30));
        btnViewCoach.addActionListener(e -> selectCoachPlannedTab());

        ModernButton btnEdit = new ModernButton("Edit Selected", ModernButton.ButtonStyle.SECONDARY);
        btnEdit.setPreferredSize(new Dimension(105, 30));
        btnEdit.addActionListener(e -> editSelectedWorkout());

        ModernButton btnDelete = new ModernButton("Delete", ModernButton.ButtonStyle.DANGER);
        btnDelete.setPreferredSize(new Dimension(80, 30));
        btnDelete.addActionListener(e -> deleteSelectedWorkout());

        actions.add(btnGoReminders);
        actions.add(btnViewCoach);
        actions.add(btnEdit);
        actions.add(btnDelete);
        bar.add(actions, BorderLayout.EAST);

        return bar;
    }

    public void filterByCoachAssigned() {
        cmbStatusFilter.setSelectedItem("Coach Assigned");
        applyFilters();
    }

    private void applyFilters() {
        String search = txtSearch.getText().trim().toLowerCase();
        String selectedType = (String) cmbTypeFilter.getSelectedItem();
        String selectedStatus = (String) cmbStatusFilter.getSelectedItem();

        boolean isCoachFilter = "Coach Assigned".equalsIgnoreCase(selectedStatus);
        btnFilterCoachAssigned.setStyle(isCoachFilter ? ModernButton.ButtonStyle.PRIMARY : ModernButton.ButtonStyle.SECONDARY);

        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                String title = entry.getStringValue(1).toLowerCase();
                String type = entry.getStringValue(2);
                String status = entry.getStringValue(5);
                String notes = entry.getStringValue(6);
                String notesLower = notes.toLowerCase();

                if (!search.isEmpty() && !title.contains(search) && !type.toLowerCase().contains(search) && !notesLower.contains(search)) {
                    return false;
                }
                if (!"All Types".equals(selectedType) && !type.equalsIgnoreCase(selectedType)) {
                    return false;
                }
                if ("Coach Assigned".equalsIgnoreCase(selectedStatus)) {
                    boolean isCoach = notesLower.contains("[coach") || notesLower.contains("[trainer");
                    if (!isCoach) return false;
                } else if (!"All Statuses".equals(selectedStatus) && !status.equalsIgnoreCase(selectedStatus)) {
                    return false;
                }
                return true;
            }
        });
    }

    public void loadWorkouts() {
        SwingWorker<List<Workout>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Workout> doInBackground() throws Exception {
                try {
                    MeasurementDAO mDao = new MeasurementDAO();
                    List<BodyMeasurement> ms = mDao.getAllMeasurements();
                    if (ms != null && !ms.isEmpty() && ms.get(0).getWeightKg() > 0) {
                        cachedUserWeightKg = ms.get(0).getWeightKg();
                    } else {
                        UserSession s = SupabaseClient.getInstance().getCurrentSession();
                        if (s != null && s.getProfile() != null && s.getProfile().getTargetWeightKg() != null && s.getProfile().getTargetWeightKg() > 0) {
                            cachedUserWeightKg = s.getProfile().getTargetWeightKg();
                        }
                    }
                } catch (Exception ignored) {}
                List<Workout> workouts = workoutDAO.getAllWorkouts();
                if (workouts != null) {
                    for (Workout w : workouts) {
                        if (w.getId() != null && !w.getId().isBlank()) {
                            if (w.getExercises() == null || w.getExercises().isEmpty()) {
                                try {
                                    List<WorkoutExercise> exs = workoutDAO.getExercisesForWorkout(w.getId());
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

            @Override
            protected void done() {
                try {
                    currentWorkouts = get();
                    tableModel.setRowCount(0);
                    displayedRowItems.clear();

                    int totalMins = 0;
                    int totalCal = 0;

                    for (Workout w : currentWorkouts) {
                        totalMins += w.getDurationMinutes();
                        totalCal += w.getCaloriesBurned();
                    }

                    LocalDate today = LocalDate.now();
                    LocalDate rollingWindowStart = today.minusDays(6);

                    Map<LocalDate, List<Workout>> byDate = new LinkedHashMap<>();
                    List<Workout> undatedWorkouts = new ArrayList<>();
                    for (Workout w : currentWorkouts) {
                        if (w.getWorkoutDate() != null) {
                            byDate.computeIfAbsent(w.getWorkoutDate(), k -> new ArrayList<>()).add(w);
                        } else {
                            undatedWorkouts.add(w);
                        }
                    }

                    List<LocalDate> sortedDates = new ArrayList<>(byDate.keySet());
                    sortedDates.sort(Collections.reverseOrder());

                    // 1. Future planned workouts (if any), in descending order
                    for (LocalDate date : sortedDates) {
                        if (date.isAfter(today)) {
                            for (Workout w : byDate.get(date)) {
                                displayedRowItems.add(new WorkoutRowItem(date, w, false));
                                tableModel.addRow(new Object[]{
                                        DateUtil.formatDate(w.getWorkoutDate()),
                                        w.getTitle(),
                                        w.getWorkoutType(),
                                        w.getDurationMinutes() + " mins",
                                        w.getCaloriesBurned() + " kcal",
                                        w.getStatus(),
                                        w.getNotes() != null ? w.getNotes() : ""
                                });
                            }
                        }
                    }

                    // 2. Rolling 7-day timeline (today down to today-6): each day shows its workout(s) or the day's routine rest day
                    for (LocalDate date = today; !date.isBefore(rollingWindowStart); date = date.minusDays(1)) {
                        List<Workout> dayWorkouts = byDate.get(date);
                        if (dayWorkouts != null && !dayWorkouts.isEmpty()) {
                            for (Workout w : dayWorkouts) {
                                displayedRowItems.add(new WorkoutRowItem(date, w, false));
                                tableModel.addRow(new Object[]{
                                        DateUtil.formatDate(w.getWorkoutDate()),
                                        w.getTitle(),
                                        w.getWorkoutType(),
                                        w.getDurationMinutes() + " mins",
                                        w.getCaloriesBurned() + " kcal",
                                        w.getStatus(),
                                        w.getNotes() != null ? w.getNotes() : ""
                                });
                            }
                        } else {
                            DailyRoutine r = getRoutineForDay(date.getDayOfWeek());
                            displayedRowItems.add(new WorkoutRowItem(date, null, true));
                            tableModel.addRow(new Object[]{
                                    DateUtil.formatDate(date),
                                    "Rest & Recovery (" + r.title + ")",
                                    "Recovery",
                                    "--",
                                    "--",
                                    "REST DAY",
                                    "No workout logged • Double-click to log"
                            });
                        }
                    }

                    // 3. Past logged workouts prior to the 7-day window, in descending order (no blank intervening rest days)
                    for (LocalDate date : sortedDates) {
                        if (date.isBefore(rollingWindowStart)) {
                            for (Workout w : byDate.get(date)) {
                                displayedRowItems.add(new WorkoutRowItem(date, w, false));
                                tableModel.addRow(new Object[]{
                                        DateUtil.formatDate(w.getWorkoutDate()),
                                        w.getTitle(),
                                        w.getWorkoutType(),
                                        w.getDurationMinutes() + " mins",
                                        w.getCaloriesBurned() + " kcal",
                                        w.getStatus(),
                                        w.getNotes() != null ? w.getNotes() : ""
                                });
                            }
                        }
                    }

                    // 4. Any undated workouts
                    for (Workout w : undatedWorkouts) {
                        displayedRowItems.add(new WorkoutRowItem(null, w, false));
                        tableModel.addRow(new Object[]{
                                "--",
                                w.getTitle(),
                                w.getWorkoutType(),
                                w.getDurationMinutes() + " mins",
                                w.getCaloriesBurned() + " kcal",
                                w.getStatus(),
                                w.getNotes() != null ? w.getNotes() : ""
                        });
                    }

                    lblSummaryStats.setText(String.format("Total: %d workouts | %d mins (%s) | %d kcal burned | %d days in timeline",
                            currentWorkouts.size(), totalMins, DateUtil.formatDuration(totalMins), totalCal, displayedRowItems.size()));

                    // Update KPI Metric Cards
                    int totalWorkouts = currentWorkouts.size();
                    lblKpiWorkouts.setText(String.valueOf(totalWorkouts));
                    int hrs = totalMins / 60;
                    int mins = totalMins % 60;
                    lblKpiDuration.setText(hrs > 0 ? String.format("%dh %02dm", hrs, mins) : totalMins + "m");
                    lblKpiCalories.setText(String.format("%,d kcal", totalCal));

                    int streak = HealthCalculator.calculateWorkoutStreak(currentWorkouts);
                    lblKpiStreak.setText(streak + (streak == 1 ? " Day" : " Days"));

                    long coachCount = currentWorkouts.stream().filter(w -> w.getNotes() != null && (w.getNotes().contains("[Coach") || w.getNotes().contains("[Trainer"))).count();
                    btnTabCoachAssigned.setText("Coach Planned (" + coachCount + ")");
                    btnFilterCoachAssigned.setText("Coach Assigned (" + coachCount + ")");
                    btnFilterCoachAssigned.setVisible(coachCount > 0);
                    btnFilterCoachAssigned.getParent().revalidate();
                    btnFilterCoachAssigned.getParent().repaint();
                    refreshCoachPlannedWorkoutsView();
                } catch (Exception e) {
                    lblSummaryStats.setText("Failed to load workouts: " + e.getMessage());
                }
                loadReminders();
            }
        };
        worker.execute();
    }

    private void openAddWorkoutDialog(int initialMinutes) {
        WorkoutDialog dialog = new WorkoutDialog(SwingUtilities.getWindowAncestor(this), null, initialMinutes);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            ToastNotification.showToast(this, "Workout logged successfully!", ToastNotification.ToastType.SUCCESS);
            loadWorkouts();
            checkWorkoutAchievements();
        }
    }

    private void editSelectedWorkout() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            ToastNotification.showToast(this, "Please select a workout to edit.", ToastNotification.ToastType.INFO);
            return;
        }
        int modelRow = table.convertRowIndexToModel(selectedRow);
        if (modelRow >= displayedRowItems.size()) return;

        WorkoutRowItem item = displayedRowItems.get(modelRow);
        if (item.isRestDay) {
            openAddWorkoutDialogForDate(item.date);
            return;
        }

        Workout w = item.workout;
        if (w != null && (w.getExercises() == null || w.getExercises().isEmpty()) && w.getId() != null && !w.getId().isBlank()) {
            try {
                List<WorkoutExercise> exs = workoutDAO.getExercisesForWorkout(w.getId());
                if (exs != null && !exs.isEmpty()) {
                    w.setExercises(exs);
                }
            } catch (Exception ignored) {}
        }

        WorkoutDialog dialog = new WorkoutDialog(SwingUtilities.getWindowAncestor(this), w, 0);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            ToastNotification.showToast(this, "Workout updated.", ToastNotification.ToastType.INFO);
            loadWorkouts();
        }
    }

    private void deleteSelectedWorkout() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            ToastNotification.showToast(this, "Please select a workout to delete.", ToastNotification.ToastType.INFO);
            return;
        }
        int modelRow = table.convertRowIndexToModel(selectedRow);
        if (modelRow >= displayedRowItems.size()) return;

        WorkoutRowItem item = displayedRowItems.get(modelRow);
        if (item.isRestDay) {
            ToastNotification.showToast(this, "This is a rest day with no workout logged.", ToastNotification.ToastType.INFO);
            return;
        }

        Workout w = item.workout;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete the workout \"" + w.getTitle() + "\"?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    workoutDAO.deleteWorkout(w.getId());
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        ToastNotification.showToast(WorkoutPanel.this, "Workout deleted successfully.", ToastNotification.ToastType.INFO);
                        loadWorkouts();
                        DataSyncManager.fireDataChanged(DataSyncManager.DataType.WORKOUTS, DataSyncManager.DataType.GOALS);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(WorkoutPanel.this, "Delete failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    private void checkWorkoutAchievements() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                List<Workout> list = workoutDAO.getAllWorkouts();
                if (list != null && !list.isEmpty()) {
                    achievementDAO.unlockAchievement("FIRST_WORKOUT");
                    if (list.size() >= 10) achievementDAO.unlockAchievement("WORKOUT_10");
                    if (list.size() >= 50) achievementDAO.unlockAchievement("WORKOUT_50");

                    int streak = HealthCalculator.calculateWorkoutStreak(list);
                    if (streak >= 3) achievementDAO.unlockAchievement("WORKOUT_STREAK_3");
                    if (streak >= 7) achievementDAO.unlockAchievement("WORKOUT_STREAK_7");
                }
                return null;
            }
        };
        worker.execute();
    }

    // =========================================================================
    // MODAL DIALOG: ADD / EDIT WORKOUT
    // =========================================================================

    private class WorkoutDialog extends JDialog {
        private static final long serialVersionUID = 1L;
        private boolean saved = false;
        private final Workout editingWorkout;

        private final ModernTextField txtTitle = new ModernTextField("", "e.g. Morning Upper Body");
        private final JComboBox<String> cmbType = new JComboBox<>(new String[]{"Strength", "Cardio", "HIIT", "Yoga", "Flexibility", "Endurance", "Other"});
        private final JComboBox<String> cmbIntensity = new JComboBox<>(new String[]{"Moderate (Standard)", "Light (Casual)", "Vigorous (High)"});
        private final ModernTextField txtDate = new ModernTextField(LocalDate.now().toString(), "YYYY-MM-DD");
        private final ModernTextField txtDuration = new ModernTextField("", "e.g. 45");
        private final ModernTextField txtCalories = new ModernTextField("", "e.g. 350");
        private final JLabel lblCalInfo = new JLabel("• MET & volume-based");
        private final JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"COMPLETED", "PLANNED"});
        private final JTextArea txtNotes = new JTextArea(2, 20);

        private final DefaultTableModel exercisesModel;
        private final ModernTable exercisesTable;

        public WorkoutDialog(Window owner, Workout workout, int prefillDurationMinutes) {
            super(owner, (workout == null || workout.getId() == null || workout.getId().isBlank()) ? "Log New Workout" : "Edit Workout", ModalityType.APPLICATION_MODAL);
            this.editingWorkout = (workout != null && workout.getId() != null && !workout.getId().isBlank()) ? workout : null;
            setSize(630, 730);
            setLocationRelativeTo(owner);
            setResizable(false);
            getContentPane().setBackground(ThemeConfig.BG_DARK);

            JPanel main = new JPanel();
            main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
            main.setOpaque(false);
            main.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

            // Section 1: Daily Workout Split Routine Chips
            if (workout == null || workout.getId() == null || workout.getId().isBlank()) {
                JPanel tplHeader = new JPanel(new BorderLayout());
                tplHeader.setOpaque(false);
                JLabel lblTpl = new JLabel("Daily Split Presets:");
                lblTpl.setFont(ThemeConfig.FONT_SMALL);
                lblTpl.setForeground(ThemeConfig.TEXT_SECONDARY);
                tplHeader.add(lblTpl, BorderLayout.WEST);

                JComboBox<WorkoutSplit> dialogSplitCombo = new JComboBox<>(WORKOUT_SPLITS);
                dialogSplitCombo.setSelectedItem(currentSelectedSplit);
                dialogSplitCombo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                dialogSplitCombo.setPreferredSize(new Dimension(210, 24));
                tplHeader.add(dialogSplitCombo, BorderLayout.EAST);
                main.add(tplHeader);
                main.add(Box.createVerticalStrut(4));

                JPanel tplRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
                tplRow.setOpaque(false);

                Runnable refreshDialogChips = () -> {
                    tplRow.removeAll();
                    WorkoutSplit split = (WorkoutSplit) dialogSplitCombo.getSelectedItem();
                    if (split != null) {
                        for (DailyRoutine r : split.routines) {
                            tplRow.add(createTemplateChip(r.shortDay + ": " + r.title.split(" ")[0], r.title, r.type, r.durationMinutes, r.exercises));
                        }
                    }
                    tplRow.revalidate();
                    tplRow.repaint();
                };

                dialogSplitCombo.addActionListener(e -> {
                    WorkoutSplit s = (WorkoutSplit) dialogSplitCombo.getSelectedItem();
                    if (s != null) {
                        setCurrentSelectedSplit(s);
                        refreshDialogChips.run();
                        rebuildPresetsChips();
                    }
                });
                refreshDialogChips.run();

                main.add(tplRow);
                main.add(Box.createVerticalStrut(10));
            }

            // Coach Assigned Notification Banner
            if (workout != null && workout.getNotes() != null && (workout.getNotes().contains("[Coach") || workout.getNotes().contains("[Trainer"))) {
                ModernCard coachCard = new ModernCard(10, new Insets(10, 14, 10, 14));
                coachCard.setLayout(new BorderLayout(10, 0));
                coachCard.setBorder(BorderFactory.createLineBorder(ThemeConfig.PRIMARY_ACCENT, 1, true));

                JLabel coachIcon = new JLabel(IconFactory.createIcon(IconFactory.IconType.WORKOUT, 20, ThemeConfig.PRIMARY_ACCENT));
                coachCard.add(coachIcon, BorderLayout.WEST);

                JPanel coachText = new JPanel(new GridLayout(2, 1, 0, 3));
                coachText.setOpaque(false);

                JLabel coachTitle = new JLabel("ASSIGNED BY YOUR PERSONAL TRAINER / COACH");
                coachTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
                coachTitle.setForeground(ThemeConfig.PRIMARY_ACCENT);
                coachText.add(coachTitle);

                JLabel coachNotes = new JLabel(workout.getNotes());
                coachNotes.setFont(ThemeConfig.FONT_SMALL);
                coachNotes.setForeground(ThemeConfig.TEXT_PRIMARY);
                coachText.add(coachNotes);

                coachCard.add(coachText, BorderLayout.CENTER);

                if ("PLANNED".equalsIgnoreCase(workout.getStatus())) {
                    ModernButton btnQuickDone = new ModernButton("✓ Complete Workout", ModernButton.ButtonStyle.SUCCESS);
                    btnQuickDone.setPreferredSize(new Dimension(150, 30));
                    btnQuickDone.addActionListener(ev -> {
                        cmbStatus.setSelectedItem("COMPLETED");
                        txtDate.setText(LocalDate.now().toString());
                        saveWorkoutData();
                    });
                    coachCard.add(btnQuickDone, BorderLayout.EAST);
                }

                main.add(coachCard);
                main.add(Box.createVerticalStrut(10));
            }

            // Title
            main.add(createFieldRow("Workout Title *", txtTitle));
            main.add(Box.createVerticalStrut(10));

            // Type & Date Row with Quick Pills
            JPanel row2 = new JPanel(new GridLayout(1, 2, 12, 0));
            row2.setOpaque(false);
            row2.add(createFieldRow("Workout Type", cmbType));

            JPanel dateCol = new JPanel();
            dateCol.setLayout(new BoxLayout(dateCol, BoxLayout.Y_AXIS));
            dateCol.setOpaque(false);
            JLabel lblDateTag = new JLabel("Date");
            lblDateTag.setFont(ThemeConfig.FONT_SMALL);
            lblDateTag.setForeground(ThemeConfig.TEXT_SECONDARY);
            dateCol.add(lblDateTag);
            dateCol.add(Box.createVerticalStrut(4));

            JPanel dateControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            dateControls.setOpaque(false);

            LocalDate initDate = (workout != null && workout.getWorkoutDate() != null) ? workout.getWorkoutDate() : LocalDate.now();
            boolean isInitToday = initDate.equals(LocalDate.now());
            boolean isInitYesterday = initDate.equals(LocalDate.now().minusDays(1));
            JButton btnToday = createQuickPill("Today", isInitToday);
            JButton btnYesterday = createQuickPill("Yesterday", isInitYesterday);
            btnToday.addActionListener(e -> {
                txtDate.setText(LocalDate.now().toString());
                ThemeConfig.styleQuickPill(btnToday, true);
                ThemeConfig.styleQuickPill(btnYesterday, false);
            });
            btnYesterday.addActionListener(e -> {
                txtDate.setText(LocalDate.now().minusDays(1).toString());
                ThemeConfig.styleQuickPill(btnYesterday, true);
                ThemeConfig.styleQuickPill(btnToday, false);
            });
            txtDate.setPreferredSize(new Dimension(105, 30));
            dateControls.add(btnToday);
            dateControls.add(btnYesterday);
            dateControls.add(txtDate);
            dateCol.add(dateControls);
            row2.add(dateCol);
            main.add(row2);
            main.add(Box.createVerticalStrut(10));

            // Duration with Steppers, Intensity, Auto Calories, Status (4 columns)
            JPanel row3 = new JPanel(new GridLayout(1, 4, 10, 0));
            row3.setOpaque(false);

            JPanel durPanel = new JPanel();
            durPanel.setLayout(new BoxLayout(durPanel, BoxLayout.Y_AXIS));
            durPanel.setOpaque(false);
            JLabel lblDur = new JLabel("Duration (Mins)");
            lblDur.setFont(ThemeConfig.FONT_SMALL);
            lblDur.setForeground(ThemeConfig.TEXT_SECONDARY);
            durPanel.add(lblDur);
            durPanel.add(Box.createVerticalStrut(4));
            durPanel.add(txtDuration);
            durPanel.add(Box.createVerticalStrut(4));

            JPanel durSteppers = new JPanel(new GridLayout(1, 4, 3, 0));
            durSteppers.setOpaque(false);
            durSteppers.add(createDurationAdjuster("-15", -15));
            durSteppers.add(createDurationAdjuster("-5", -5));
            durSteppers.add(createDurationAdjuster("+5", +5));
            durSteppers.add(createDurationAdjuster("+15", +15));
            durPanel.add(durSteppers);

            JPanel calPanel = new JPanel();
            calPanel.setLayout(new BoxLayout(calPanel, BoxLayout.Y_AXIS));
            calPanel.setOpaque(false);
            JLabel lblCal = new JLabel("Calories (Auto)");
            lblCal.setFont(ThemeConfig.FONT_SMALL);
            lblCal.setForeground(ThemeConfig.TEXT_SECONDARY);
            calPanel.add(lblCal);
            calPanel.add(Box.createVerticalStrut(4));
            calPanel.add(txtCalories);
            calPanel.add(Box.createVerticalStrut(4));

            lblCalInfo.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            lblCalInfo.setForeground(ThemeConfig.TEXT_MUTED);
            calPanel.add(lblCalInfo);

            row3.add(durPanel);
            row3.add(createFieldRow("Intensity Level", cmbIntensity));
            row3.add(calPanel);
            row3.add(createFieldRow("Status", cmbStatus));
            main.add(row3);
            main.add(Box.createVerticalStrut(12));

            // Exercises section with Quick Add Exercise Chips
            JPanel exHeader = new JPanel(new BorderLayout());
            exHeader.setOpaque(false);
            JLabel lblExTitle = new JLabel("Exercises & Sets (Optional)");
            lblExTitle.setFont(ThemeConfig.FONT_SUBTITLE);
            lblExTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
            exHeader.add(lblExTitle, BorderLayout.WEST);
            main.add(exHeader);
            main.add(Box.createVerticalStrut(4));

            // Quick Add Common Exercise Chips
            JPanel exChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            exChips.setOpaque(false);
            exChips.add(createExerciseChip("+ Bench Press", "Bench Press", 3, 10, 60.0));
            exChips.add(createExerciseChip("+ Squat", "Squat", 3, 8, 80.0));
            exChips.add(createExerciseChip("+ Deadlift", "Deadlift", 3, 5, 100.0));
            exChips.add(createExerciseChip("+ Pull-ups", "Pull-ups", 3, 8, 0.0));
            exChips.add(createExerciseChip("+ Curls", "Bicep Curls", 3, 12, 14.0));
            main.add(exChips);
            main.add(Box.createVerticalStrut(6));

            String[] exCols = {"Exercise", "Sets", "Reps", "Weight (kg)"};
            exercisesModel = new DefaultTableModel(exCols, 0);
            exercisesTable = new ModernTable(exercisesModel);
            exercisesTable.setRowHeight(28);

            // Configure cell editor with dark styling for direct table edits
            JTextField inlineEditorTf = new JTextField();
            inlineEditorTf.setBackground(ThemeConfig.BG_INPUT);
            inlineEditorTf.setForeground(ThemeConfig.TEXT_PRIMARY);
            inlineEditorTf.setCaretColor(ThemeConfig.TEXT_PRIMARY);
            inlineEditorTf.setFont(ThemeConfig.FONT_BODY);
            inlineEditorTf.setBorder(BorderFactory.createLineBorder(ThemeConfig.PRIMARY_ACCENT, 1));
            exercisesTable.setDefaultEditor(Object.class, new DefaultCellEditor(inlineEditorTf));

            // Double click to edit selected exercise in modal dialog
            exercisesTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int r = exercisesTable.getSelectedRow();
                        if (r >= 0) {
                            openExercisePrompt(r);
                        }
                    }
                }
            });

            JScrollPane scrollEx = ModernTable.wrapInScrollPane(exercisesTable);
            scrollEx.setPreferredSize(new Dimension(0, 95));
            main.add(scrollEx);
            main.add(Box.createVerticalStrut(6));

            JPanel exBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            exBtnRow.setOpaque(false);
            ModernButton btnAddEx = new ModernButton("+ Custom Exercise", ModernButton.ButtonStyle.SECONDARY);
            btnAddEx.addActionListener(e -> openExercisePrompt(null));
            ModernButton btnEditEx = new ModernButton("Edit Selected", ModernButton.ButtonStyle.GHOST);
            btnEditEx.addActionListener(e -> {
                int r = exercisesTable.getSelectedRow();
                if (r >= 0) {
                    openExercisePrompt(r);
                } else {
                    JOptionPane.showMessageDialog(this, "Please select an exercise from the table to edit.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
                }
            });
            ModernButton btnDelEx = new ModernButton("- Remove Selected", ModernButton.ButtonStyle.GHOST);
            btnDelEx.addActionListener(e -> {
                int r = exercisesTable.getSelectedRow();
                if (r >= 0) exercisesModel.removeRow(r);
            });
            exBtnRow.add(btnAddEx);
            exBtnRow.add(btnEditEx);
            exBtnRow.add(btnDelEx);
            main.add(exBtnRow);
            main.add(Box.createVerticalStrut(10));

            // Notes
            JLabel lblNotes = new JLabel("Notes & Performance");
            lblNotes.setFont(ThemeConfig.FONT_SMALL);
            lblNotes.setForeground(ThemeConfig.TEXT_SECONDARY);
            main.add(lblNotes);
            main.add(Box.createVerticalStrut(4));
            txtNotes.setFont(ThemeConfig.FONT_BODY);
            txtNotes.setForeground(ThemeConfig.TEXT_PRIMARY);
            txtNotes.setBackground(ThemeConfig.BG_INPUT);
            txtNotes.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1));
            txtNotes.setLineWrap(true);
            main.add(new JScrollPane(txtNotes));
            main.add(Box.createVerticalStrut(14));

            // Save / Cancel buttons
            JPanel actions = new JPanel(new BorderLayout());
            actions.setOpaque(false);
            JLabel lblHint = new JLabel("Tip: Press Enter to save");
            lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            lblHint.setForeground(ThemeConfig.TEXT_MUTED);
            actions.add(lblHint, BorderLayout.WEST);

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            btnRow.setOpaque(false);
            ModernButton btnCancel = new ModernButton("Cancel", ModernButton.ButtonStyle.GHOST);
            btnCancel.addActionListener(e -> dispose());
            ModernButton btnSave = new ModernButton("Save Workout", ModernButton.ButtonStyle.PRIMARY);
            btnSave.addActionListener(e -> saveWorkoutData());
            btnRow.add(btnCancel);
            btnRow.add(btnSave);
            actions.add(btnRow, BorderLayout.EAST);
            main.add(actions);

            setContentPane(main);

            // Real-time automatic calorie estimation on duration, type, intensity, or exercise table changes
            txtDuration.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { autoEstimateCalories(); }
                @Override public void removeUpdate(DocumentEvent e) { autoEstimateCalories(); }
                @Override public void changedUpdate(DocumentEvent e) { autoEstimateCalories(); }
            });
            cmbType.addActionListener(e -> autoEstimateCalories());
            cmbIntensity.addActionListener(e -> autoEstimateCalories());
            exercisesModel.addTableModelListener(e -> autoEstimateCalories());

            // Enter key shortcut to save
            txtTitle.addActionListener(e -> saveWorkoutData());
            txtDate.addActionListener(e -> saveWorkoutData());
            txtDuration.addActionListener(e -> saveWorkoutData());
            txtCalories.addActionListener(e -> saveWorkoutData());

            // Escape key to cancel
            getRootPane().registerKeyboardAction(e -> dispose(),
                    KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);

            // Populate if editing or prefilled
            if (workout != null) {
                txtTitle.setText(workout.getTitle());
                cmbType.setSelectedItem(workout.getWorkoutType());
                txtDate.setText(workout.getWorkoutDate() != null ? workout.getWorkoutDate().toString() : LocalDate.now().toString());
                txtDuration.setText(String.valueOf(workout.getDurationMinutes()));
                txtCalories.setText(String.valueOf(workout.getCaloriesBurned()));
                cmbStatus.setSelectedItem(workout.getStatus());
                txtNotes.setText(workout.getNotes() != null ? workout.getNotes() : "");

                if ((workout.getExercises() == null || workout.getExercises().isEmpty()) && workout.getId() != null && !workout.getId().isBlank()) {
                    try {
                        List<WorkoutExercise> fetched = workoutDAO.getExercisesForWorkout(workout.getId());
                        if (fetched != null && !fetched.isEmpty()) {
                            workout.setExercises(fetched);
                        }
                    } catch (Exception ignored) {}
                }
                if (workout.getExercises() != null) {
                    for (WorkoutExercise ex : workout.getExercises()) {
                        exercisesModel.addRow(new Object[]{ex.getExerciseName(), ex.getSets(), ex.getReps(), ex.getWeightKg()});
                    }
                }
                if (workout.getCaloriesBurned() > 0 && workout.getDurationMinutes() > 0) {
                    double rate = (double) workout.getCaloriesBurned() / workout.getDurationMinutes();
                    lblCalInfo.setText(String.format(Locale.US, "• ~%.1f kcal/min (@%.0fkg)", rate, cachedUserWeightKg));
                }
            } else {
                DailyRoutine r = getRoutineForDay(LocalDate.now().getDayOfWeek());
                txtTitle.setText(r.title);
                cmbType.setSelectedItem(r.type);
                txtDuration.setText(String.valueOf(prefillDurationMinutes > 0 ? prefillDurationMinutes : r.durationMinutes));
                if (r.exercises != null) {
                    for (String[] row : r.exercises) {
                        exercisesModel.addRow(new Object[]{row[0], Integer.parseInt(row[1]), Integer.parseInt(row[2]), Double.parseDouble(row[3])});
                    }
                }
                autoEstimateCalories();
            }

            // Auto-focus on Title
            SwingUtilities.invokeLater(() -> {
                txtTitle.requestFocusInWindow();
                txtTitle.selectAll();
            });
        }

        private JButton createTemplateChip(String label, String title, String type, int duration, String[][] defaultExercises) {
            JButton btn = new JButton(label);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btn.setForeground(ThemeConfig.TEXT_PRIMARY);
            btn.setBackground(ThemeConfig.BG_CARD);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));
            btn.addActionListener(e -> {
                txtTitle.setText(title);
                cmbType.setSelectedItem(type);
                txtDuration.setText(String.valueOf(duration));
                if (defaultExercises != null) {
                    exercisesModel.setRowCount(0);
                    for (String[] row : defaultExercises) {
                        exercisesModel.addRow(new Object[]{row[0], Integer.parseInt(row[1]), Integer.parseInt(row[2]), Double.parseDouble(row[3])});
                    }
                }
                autoEstimateCalories();
            });
            return btn;
        }

        private JButton createExerciseChip(String label, String exName, int sets, int reps, double weight) {
            JButton btn = new JButton(label);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            btn.setForeground(ThemeConfig.TEXT_SECONDARY);
            btn.setBackground(ThemeConfig.BG_CARD);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createEmptyBorder(3, 7, 3, 7));
            btn.addActionListener(e -> exercisesModel.addRow(new Object[]{exName, sets, reps, weight}));
            return btn;
        }

        private void openExercisePrompt(Integer editingRow) {
            if (exercisesTable.isEditing()) {
                exercisesTable.getCellEditor().cancelCellEditing();
            }

            JDialog prompt = new JDialog(this, editingRow == null ? "Add Custom Exercise" : "Edit Exercise", true);
            prompt.setResizable(false);
            prompt.getContentPane().setBackground(ThemeConfig.BG_CARD);

            JPanel pContent = new JPanel();
            pContent.setLayout(new BoxLayout(pContent, BoxLayout.Y_AXIS));
            pContent.setBackground(ThemeConfig.BG_CARD);
            pContent.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

            JLabel lblTitle = new JLabel(editingRow == null ? "Add Custom Exercise" : "Edit Exercise Details");
            lblTitle.setFont(ThemeConfig.FONT_TITLE);
            lblTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
            pContent.add(lblTitle);
            pContent.add(Box.createVerticalStrut(14));

            // Fields
            JLabel lblName = new JLabel("Exercise Name *");
            lblName.setFont(ThemeConfig.FONT_SMALL);
            lblName.setForeground(ThemeConfig.TEXT_SECONDARY);
            pContent.add(lblName);
            pContent.add(Box.createVerticalStrut(4));

            String initialName = editingRow != null && exercisesModel.getValueAt(editingRow, 0) != null
                    ? exercisesModel.getValueAt(editingRow, 0).toString()
                    : "";
            ModernTextField txtExName = new ModernTextField(initialName, "e.g. Incline Dumbbell Press");
            txtExName.setPreferredSize(new Dimension(300, 36));
            pContent.add(txtExName);
            pContent.add(Box.createVerticalStrut(10));

            JPanel rowStats = new JPanel(new GridLayout(1, 3, 10, 0));
            rowStats.setOpaque(false);

            // Sets
            JPanel pSets = new JPanel();
            pSets.setLayout(new BoxLayout(pSets, BoxLayout.Y_AXIS));
            pSets.setOpaque(false);
            JLabel lblSets = new JLabel("Sets");
            lblSets.setFont(ThemeConfig.FONT_SMALL);
            lblSets.setForeground(ThemeConfig.TEXT_SECONDARY);
            pSets.add(lblSets);
            pSets.add(Box.createVerticalStrut(4));
            String initialSets = editingRow != null && exercisesModel.getValueAt(editingRow, 1) != null
                    ? exercisesModel.getValueAt(editingRow, 1).toString()
                    : "3";
            ModernTextField txtSets = new ModernTextField(initialSets, "Sets");
            pSets.add(txtSets);

            // Reps
            JPanel pReps = new JPanel();
            pReps.setLayout(new BoxLayout(pReps, BoxLayout.Y_AXIS));
            pReps.setOpaque(false);
            JLabel lblReps = new JLabel("Reps");
            lblReps.setFont(ThemeConfig.FONT_SMALL);
            lblReps.setForeground(ThemeConfig.TEXT_SECONDARY);
            pReps.add(lblReps);
            pReps.add(Box.createVerticalStrut(4));
            String initialReps = editingRow != null && exercisesModel.getValueAt(editingRow, 2) != null
                    ? exercisesModel.getValueAt(editingRow, 2).toString()
                    : "10";
            ModernTextField txtReps = new ModernTextField(initialReps, "Reps");
            pReps.add(txtReps);

            // Weight
            JPanel pWeight = new JPanel();
            pWeight.setLayout(new BoxLayout(pWeight, BoxLayout.Y_AXIS));
            pWeight.setOpaque(false);
            JLabel lblWeight = new JLabel("Weight (kg)");
            lblWeight.setFont(ThemeConfig.FONT_SMALL);
            lblWeight.setForeground(ThemeConfig.TEXT_SECONDARY);
            pWeight.add(lblWeight);
            pWeight.add(Box.createVerticalStrut(4));
            String initialWeight = editingRow != null && exercisesModel.getValueAt(editingRow, 3) != null
                    ? exercisesModel.getValueAt(editingRow, 3).toString()
                    : "0.0";
            ModernTextField txtWeight = new ModernTextField(initialWeight, "Weight");
            pWeight.add(txtWeight);

            rowStats.add(pSets);
            rowStats.add(pReps);
            rowStats.add(pWeight);
            pContent.add(rowStats);
            pContent.add(Box.createVerticalStrut(16));

            // Actions
            JPanel actRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            actRow.setOpaque(false);
            ModernButton btnCancel = new ModernButton("Cancel", ModernButton.ButtonStyle.GHOST);
            btnCancel.addActionListener(ev -> prompt.dispose());
            ModernButton btnConfirm = new ModernButton(editingRow == null ? "Add Exercise" : "Save Changes", ModernButton.ButtonStyle.PRIMARY);
            btnConfirm.addActionListener(ev -> {
                String name = txtExName.getText().trim();
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(prompt, "Please enter an exercise name.", "Input Required", JOptionPane.WARNING_MESSAGE);
                    txtExName.requestFocusInWindow();
                    return;
                }
                int sets;
                int reps;
                double weight;
                try {
                    sets = Integer.parseInt(txtSets.getText().trim());
                    reps = Integer.parseInt(txtReps.getText().trim());
                    weight = Double.parseDouble(txtWeight.getText().trim());
                    if (sets < 0 || reps < 0 || weight < 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(prompt, "Sets, Reps, and Weight must be valid non-negative numbers.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                if (editingRow == null) {
                    exercisesModel.addRow(new Object[]{name, sets, reps, weight});
                } else {
                    exercisesModel.setValueAt(name, editingRow, 0);
                    exercisesModel.setValueAt(sets, editingRow, 1);
                    exercisesModel.setValueAt(reps, editingRow, 2);
                    exercisesModel.setValueAt(weight, editingRow, 3);
                }
                prompt.dispose();
            });

            actRow.add(btnCancel);
            actRow.add(btnConfirm);
            pContent.add(actRow);

            prompt.setContentPane(pContent);
            prompt.getRootPane().setDefaultButton(btnConfirm);
            prompt.getRootPane().registerKeyboardAction(
                    ev -> prompt.dispose(),
                    KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW
            );
            prompt.pack();
            prompt.setLocationRelativeTo(this);
            SwingUtilities.invokeLater(() -> {
                txtExName.requestFocusInWindow();
                txtExName.selectAll();
            });
            prompt.setVisible(true);
        }

        private JButton createQuickPill(String text, boolean active) {
            JButton btn = new JButton(text);
            ThemeConfig.styleQuickPill(btn, active);
            return btn;
        }

        private JButton createDurationAdjuster(String text, int delta) {
            JButton btn = new JButton(text);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 10));
            btn.setForeground(ThemeConfig.TEXT_PRIMARY);
            btn.setBackground(ThemeConfig.BG_CARD);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true));
            btn.addActionListener(e -> {
                try {
                    String curStr = txtDuration.getText().trim();
                    int current = curStr.isEmpty() ? 0 : Integer.parseInt(curStr);
                    int updated = Math.max(5, Math.min(300, current + delta));
                    txtDuration.setText(String.valueOf(updated));
                    autoEstimateCalories();
                } catch (Exception ignored) {}
            });
            return btn;
        }

        private void autoEstimateCalories() {
            try {
                String durStr = txtDuration.getText().trim();
                if (durStr.isEmpty()) {
                    txtCalories.setText("");
                    lblCalInfo.setText("• MET & volume-based");
                    return;
                }
                int mins = Integer.parseInt(durStr);
                if (mins <= 0) {
                    txtCalories.setText("");
                    lblCalInfo.setText("• MET & volume-based");
                    return;
                }

                String type = (String) cmbType.getSelectedItem();
                String intensity = (String) cmbIntensity.getSelectedItem();

                List<WorkoutExercise> exList = new ArrayList<>();
                for (int r = 0; r < exercisesModel.getRowCount(); r++) {
                    Object nameObj = exercisesModel.getValueAt(r, 0);
                    if (nameObj != null && !nameObj.toString().trim().isEmpty()) {
                        try {
                            int sets = Integer.parseInt(exercisesModel.getValueAt(r, 1).toString().trim());
                            int reps = Integer.parseInt(exercisesModel.getValueAt(r, 2).toString().trim());
                            double weight = Double.parseDouble(exercisesModel.getValueAt(r, 3).toString().trim());
                            exList.add(new WorkoutExercise(nameObj.toString().trim(), sets, reps, weight));
                        } catch (Exception ignored) {}
                    }
                }

                double userWeight = cachedUserWeightKg > 0 ? cachedUserWeightKg : 70.0;
                int estimated = HealthCalculator.estimateCaloriesBurned(type, intensity, mins, userWeight, exList);
                txtCalories.setText(String.valueOf(estimated));

                double rate = (double) estimated / mins;
                lblCalInfo.setText(String.format(Locale.US, "• ~%.1f kcal/min (@%.0fkg)", rate, userWeight));
            } catch (Exception e) {
                txtCalories.setText("");
                lblCalInfo.setText("• MET & volume-based");
            }
        }

        private JPanel createFieldRow(String label, JComponent comp) {
            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.setOpaque(false);
            JLabel l = new JLabel(label);
            l.setFont(ThemeConfig.FONT_SMALL);
            l.setForeground(ThemeConfig.TEXT_SECONDARY);
            p.add(l);
            p.add(Box.createVerticalStrut(4));
            p.add(comp);
            return p;
        }

        private void saveWorkoutData() {
            String title = txtTitle.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a workout title.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int duration;
            int calories;
            try {
                duration = Integer.parseInt(txtDuration.getText().trim());
                calories = Integer.parseInt(txtCalories.getText().trim());
                if (duration < 0 || calories < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Duration and Calories must be positive integers.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            LocalDate workoutDate = DateUtil.parseDateOrNull(txtDate.getText().trim());
            if (workoutDate == null) {
                JOptionPane.showMessageDialog(this, "Please enter a valid date (YYYY-MM-DD).", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Workout target = editingWorkout != null ? editingWorkout : new Workout();
            target.setTitle(title);
            target.setWorkoutType((String) cmbType.getSelectedItem());
            target.setWorkoutDate(workoutDate);
            target.setDurationMinutes(duration);
            target.setCaloriesBurned(calories);
            target.setStatus((String) cmbStatus.getSelectedItem());
            target.setNotes(txtNotes.getText().trim());

            if (exercisesTable.isEditing()) {
                exercisesTable.getCellEditor().stopCellEditing();
            }

            // Collect exercises
            List<WorkoutExercise> exercises = new ArrayList<>();
            for (int r = 0; r < exercisesModel.getRowCount(); r++) {
                Object nameObj = exercisesModel.getValueAt(r, 0);
                if (nameObj == null || nameObj.toString().trim().isEmpty()) continue;
                String exName = nameObj.toString().trim();
                int sets = 1;
                int reps = 1;
                double weight = 0.0;
                try {
                    if (exercisesModel.getValueAt(r, 1) != null) sets = Integer.parseInt(exercisesModel.getValueAt(r, 1).toString().trim());
                    if (exercisesModel.getValueAt(r, 2) != null) reps = Integer.parseInt(exercisesModel.getValueAt(r, 2).toString().trim());
                    if (exercisesModel.getValueAt(r, 3) != null) weight = Double.parseDouble(exercisesModel.getValueAt(r, 3).toString().trim());
                    if (sets < 0 || reps < 0 || weight < 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Sets, Reps, and Weight must be valid non-negative numbers.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                exercises.add(new WorkoutExercise(exName, sets, reps, weight));
            }
            target.setExercises(exercises);

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    if (editingWorkout == null || editingWorkout.getId() == null || editingWorkout.getId().isBlank()) {
                        workoutDAO.saveWorkout(target);
                    } else {
                        workoutDAO.updateWorkout(target);
                    }
                    if ("COMPLETED".equalsIgnoreCase(target.getStatus())) {
                        try {
                            String uid = SupabaseClient.getInstance().getCurrentUserId();
                            new TrainerAssignmentDAO().markAssignmentCompletedByAssociatedId(uid, target.getId());
                        } catch (Exception ignored) {}
                    }
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        saved = true;
                        dispose();
                        DataSyncManager.fireDataChanged(DataSyncManager.DataType.WORKOUTS, DataSyncManager.DataType.GOALS);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(WorkoutDialog.this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }

        public boolean isSaved() {
            return saved;
        }
    }

    // =========================================================================
    // COACH PLANNED WORKOUTS SUB-VIEW & ACTIONS
    // =========================================================================

    public void selectCoachPlannedTab() {
        switchToTab("COACH_WORKOUTS");
    }

    private JPanel createCoachPlannedWorkoutsView() {
        JPanel view = new JPanel(new BorderLayout(0, 10));
        view.setOpaque(false);

        // Top Section: Banner + KPI Summary + Filter Toolbar
        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);

        // 1. Hero Card with KPI metric badges
        ModernCard heroCard = new ModernCard(12, new Insets(12, 16, 12, 16));
        heroCard.setLayout(new BorderLayout(14, 0));

        JPanel leftHero = new JPanel(new GridLayout(2, 1, 0, 4));
        leftHero.setOpaque(false);
        JLabel lblHeroTitle = new JLabel("Workouts Planned by Trainer");
        lblHeroTitle.setFont(ThemeConfig.FONT_TITLE);
        lblHeroTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
        leftHero.add(lblHeroTitle);

        JLabel lblHeroSub = new JLabel("Routines, exercises, target sets and volume scheduled specifically for you by your personal coach");
        lblHeroSub.setFont(ThemeConfig.FONT_SMALL);
        lblHeroSub.setForeground(ThemeConfig.TEXT_SECONDARY);
        leftHero.add(lblHeroSub);
        heroCard.add(leftHero, BorderLayout.CENTER);

        JPanel heroKpis = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        heroKpis.setOpaque(false);
        heroKpis.add(createMiniKpiBadge("Total Assigned", lblCoachKpiTotal, ThemeConfig.PRIMARY_ACCENT));
        heroKpis.add(createMiniKpiBadge("Pending", lblCoachKpiPlanned, new Color(0xFF, 0x7A, 0x59)));
        heroKpis.add(createMiniKpiBadge("Completed", lblCoachKpiCompleted, ThemeConfig.SUCCESS));
        heroCard.add(heroKpis, BorderLayout.EAST);

        topSection.add(heroCard);
        topSection.add(Box.createVerticalStrut(8));

        // 2. Filter Toolbar
        JPanel filterToolbar = new JPanel(new BorderLayout(10, 0));
        filterToolbar.setOpaque(false);
        filterToolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JLabel lblListHeading = new JLabel("Scheduled Routines & Prescribed Exercises");
        lblListHeading.setFont(ThemeConfig.FONT_SUBTITLE);
        lblListHeading.setForeground(ThemeConfig.TEXT_PRIMARY);
        filterToolbar.add(lblListHeading, BorderLayout.WEST);

        pnlCoachFilters.setLayout(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pnlCoachFilters.setOpaque(false);
        filterToolbar.add(pnlCoachFilters, BorderLayout.EAST);

        topSection.add(filterToolbar);
        view.add(topSection, BorderLayout.NORTH);

        // Center: Scrollable List of Coach Workout Routine Cards
        pnlCoachWorkoutsList.setLayout(new BoxLayout(pnlCoachWorkoutsList, BoxLayout.Y_AXIS));
        pnlCoachWorkoutsList.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(pnlCoachWorkoutsList);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        view.add(scrollPane, BorderLayout.CENTER);

        // Bottom Bar
        JPanel bottomBar = new JPanel(new BorderLayout(10, 0));
        bottomBar.setOpaque(false);

        JLabel lblTip = new JLabel("Workouts completed here instantly update your workout streak, burned calories, and training volume.");
        lblTip.setFont(ThemeConfig.FONT_SMALL);
        lblTip.setForeground(ThemeConfig.TEXT_MUTED);
        bottomBar.add(lblTip, BorderLayout.WEST);

        ModernButton btnBack = new ModernButton("← Back to Workouts Log", ModernButton.ButtonStyle.SECONDARY);
        btnBack.setPreferredSize(new Dimension(185, 30));
        btnBack.addActionListener(e -> selectWorkoutsTab());
        bottomBar.add(btnBack, BorderLayout.EAST);

        view.add(bottomBar, BorderLayout.SOUTH);

        return view;
    }

    private JPanel createMiniKpiBadge(String label, JLabel valLbl, Color accentColor) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 1));
        p.setOpaque(true);
        p.setBackground(ThemeConfig.BG_CARD_HOVER);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accentColor, 1, true),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        JLabel title = new JLabel(label);
        title.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        title.setForeground(ThemeConfig.TEXT_MUTED);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        p.add(title);

        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        valLbl.setForeground(accentColor);
        valLbl.setHorizontalAlignment(SwingConstants.CENTER);
        p.add(valLbl);
        return p;
    }

    private JButton createCoachFilterChip(String text, String filterKey) {
        boolean active = filterKey.equalsIgnoreCase(coachFilter);
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 11));
        btn.setForeground(active ? ThemeConfig.TEXT_ON_ACCENT : ThemeConfig.TEXT_SECONDARY);
        btn.setBackground(active ? ThemeConfig.PRIMARY_ACCENT : ThemeConfig.BG_CARD);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(active ? ThemeConfig.PRIMARY_ACCENT : ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        btn.addActionListener(e -> {
            coachFilter = filterKey;
            refreshCoachPlannedWorkoutsView();
        });
        return btn;
    }

    private void refreshCoachPlannedWorkoutsView() {
        pnlCoachWorkoutsList.removeAll();
        pnlCoachFilters.removeAll();

        List<Workout> coachWorkouts = new ArrayList<>();
        if (currentWorkouts != null) {
            for (Workout w : currentWorkouts) {
                if (w.getNotes() != null && (w.getNotes().contains("[Coach") || w.getNotes().contains("[Trainer"))) {
                    coachWorkouts.add(w);
                }
            }
        }

        // Sort: PLANNED first, then by date descending
        coachWorkouts.sort((a, b) -> {
            boolean aPlanned = !"COMPLETED".equalsIgnoreCase(a.getStatus());
            boolean bPlanned = !"COMPLETED".equalsIgnoreCase(b.getStatus());
            if (aPlanned != bPlanned) {
                return aPlanned ? -1 : 1;
            }
            LocalDate da = a.getWorkoutDate() != null ? a.getWorkoutDate() : LocalDate.MIN;
            LocalDate db = b.getWorkoutDate() != null ? b.getWorkoutDate() : LocalDate.MIN;
            return db.compareTo(da);
        });

        int total = coachWorkouts.size();
        long plannedCount = coachWorkouts.stream().filter(w -> !"COMPLETED".equalsIgnoreCase(w.getStatus())).count();
        long completedCount = total - plannedCount;

        lblCoachKpiTotal.setText(String.valueOf(total));
        lblCoachKpiPlanned.setText(String.valueOf(plannedCount));
        lblCoachKpiCompleted.setText(String.valueOf(completedCount));

        pnlCoachFilters.add(createCoachFilterChip("All (" + total + ")", "ALL"));
        pnlCoachFilters.add(createCoachFilterChip("Pending (" + plannedCount + ")", "PLANNED"));
        pnlCoachFilters.add(createCoachFilterChip("Completed (" + completedCount + ")", "COMPLETED"));
        pnlCoachFilters.revalidate();
        pnlCoachFilters.repaint();

        List<Workout> filtered = new ArrayList<>();
        for (Workout w : coachWorkouts) {
            boolean isCompleted = "COMPLETED".equalsIgnoreCase(w.getStatus());
            if ("PLANNED".equals(coachFilter) && isCompleted) continue;
            if ("COMPLETED".equals(coachFilter) && !isCompleted) continue;
            filtered.add(w);
        }

        if (filtered.isEmpty()) {
            ModernCard emptyCard = new ModernCard(12, new Insets(36, 24, 36, 24));
            emptyCard.setLayout(new BoxLayout(emptyCard, BoxLayout.Y_AXIS));
            emptyCard.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel icon = new JLabel(IconFactory.createIcon(IconFactory.IconType.WORKOUT, 36, ThemeConfig.PRIMARY_ACCENT));
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyCard.add(icon);
            emptyCard.add(Box.createVerticalStrut(12));

            JLabel title = new JLabel(total == 0 ? "No Workouts Assigned by Your Trainer" : "No Workouts Match the Selected Filter");
            title.setFont(ThemeConfig.FONT_TITLE);
            title.setForeground(ThemeConfig.TEXT_PRIMARY);
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyCard.add(title);
            emptyCard.add(Box.createVerticalStrut(6));

            JLabel sub = new JLabel(total == 0
                    ? "When your trainer schedules a workout or custom routine, it will appear here with full exercise instructions."
                    : "Switch filter above to view all or completed trainer routines.");
            sub.setFont(ThemeConfig.FONT_BODY);
            sub.setForeground(ThemeConfig.TEXT_SECONDARY);
            sub.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyCard.add(sub);

            pnlCoachWorkoutsList.add(emptyCard);
        } else {
            for (Workout w : filtered) {
                pnlCoachWorkoutsList.add(createCoachWorkoutRoutineCard(w));
                pnlCoachWorkoutsList.add(Box.createVerticalStrut(10));
            }
        }

        pnlCoachWorkoutsList.revalidate();
        pnlCoachWorkoutsList.repaint();
    }

    private JPanel createCoachWorkoutRoutineCard(Workout w) {
        boolean isCompleted = "COMPLETED".equalsIgnoreCase(w.getStatus());
        Color statusColor = isCompleted ? ThemeConfig.SUCCESS : new Color(0xFF, 0x7A, 0x59);

        ModernCard card = new ModernCard(12, new Insets(14, 18, 14, 18));
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isCompleted ? ThemeConfig.BORDER_COLOR : new Color(0xFF, 0x7A, 0x59, 140), 1, true),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        // 1. Card Top Row: Date Pill, Title, Metrics, Status Badge
        JPanel topRow = new JPanel(new BorderLayout(10, 0));
        topRow.setOpaque(false);

        JPanel leftMeta = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftMeta.setOpaque(false);

        // Date Badge
        LocalDate wDate = w.getWorkoutDate() != null ? w.getWorkoutDate() : LocalDate.now();
        LocalDate today = LocalDate.now();
        String dateText;
        Color dateBg;
        if (wDate.equals(today)) {
            dateText = "TODAY";
            dateBg = ThemeConfig.PRIMARY_ACCENT;
        } else if (wDate.equals(today.plusDays(1))) {
            dateText = "TOMORROW";
            dateBg = new Color(0x3B, 0x82, 0xF6); // Blue
        } else if (wDate.equals(today.minusDays(1))) {
            dateText = "YESTERDAY";
            dateBg = new Color(0x64, 0x74, 0x8B); // Slate
        } else {
            dateText = DateUtil.formatDate(wDate);
            dateBg = new Color(0x33, 0x41, 0x55);
        }

        JLabel lblDate = new JLabel(dateText);
        lblDate.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblDate.setForeground(Color.WHITE);
        lblDate.setOpaque(true);
        lblDate.setBackground(dateBg);
        lblDate.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        leftMeta.add(lblDate);

        JLabel lblTitle = new JLabel(w.getTitle());
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
        leftMeta.add(lblTitle);

        JLabel lblType = new JLabel(w.getWorkoutType());
        lblType.setFont(ThemeConfig.FONT_SMALL);
        lblType.setForeground(ThemeConfig.TEXT_SECONDARY);
        lblType.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        leftMeta.add(lblType);

        JLabel lblDuration = new JLabel(w.getDurationMinutes() + " mins", IconFactory.createIcon(IconFactory.IconType.TIMER, 11, ThemeConfig.TEXT_MUTED), SwingConstants.LEFT);
        lblDuration.setFont(ThemeConfig.FONT_SMALL);
        lblDuration.setForeground(ThemeConfig.TEXT_MUTED);
        lblDuration.setIconTextGap(4);
        leftMeta.add(lblDuration);

        JLabel lblCal = new JLabel(w.getCaloriesBurned() + " kcal", IconFactory.createIcon(IconFactory.IconType.FIRE, 11, ThemeConfig.WARNING), SwingConstants.LEFT);
        lblCal.setFont(ThemeConfig.FONT_SMALL);
        lblCal.setForeground(ThemeConfig.WARNING);
        lblCal.setIconTextGap(4);
        leftMeta.add(lblCal);

        topRow.add(leftMeta, BorderLayout.CENTER);

        // Status pill on right
        JLabel lblStatus = new JLabel(isCompleted ? "COACH COMPLETED" : "COACH PLANNED");
        lblStatus.setIcon(IconFactory.createIcon(isCompleted ? IconFactory.IconType.CHECK : IconFactory.IconType.WORKOUT, 12, statusColor));
        lblStatus.setIconTextGap(5);
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblStatus.setForeground(statusColor);
        lblStatus.setOpaque(true);
        lblStatus.setBackground(new Color(statusColor.getRed(), statusColor.getGreen(), statusColor.getBlue(), 30));
        lblStatus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(statusColor, 1, true),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        topRow.add(lblStatus, BorderLayout.EAST);
        card.add(topRow, BorderLayout.NORTH);

        // 2. Center Section: Coach Notes Callout + Exercise Breakdown Table
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        // Coach Notes Callout
        String notes = w.getNotes() != null ? w.getNotes() : "";
        if (!notes.isBlank()) {
            JPanel notesCallout = new JPanel(new BorderLayout(8, 0));
            notesCallout.setOpaque(true);
            notesCallout.setBackground(new Color(0xEE, 0xF2, 0xFF));
            notesCallout.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 3, 0, 0, ThemeConfig.PRIMARY_ACCENT),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)
            ));

            JLabel lblNotes = new JLabel("<html><b style='color:#1D4ED8;'>Coach Instructions:</b> " + notes.replaceAll("<", "&lt;").replaceAll(">", "&gt;") + "</html>");
            lblNotes.setFont(ThemeConfig.FONT_SMALL);
            lblNotes.setForeground(ThemeConfig.TEXT_PRIMARY);
            notesCallout.add(lblNotes, BorderLayout.CENTER);
            centerPanel.add(notesCallout);
            centerPanel.add(Box.createVerticalStrut(8));
        }

        // Exercises Breakdown Table / Grid
        List<WorkoutExercise> exercises = w.getExercises();
        if (exercises != null && !exercises.isEmpty()) {
            JPanel exContainer = new JPanel();
            exContainer.setLayout(new BoxLayout(exContainer, BoxLayout.Y_AXIS));
            exContainer.setOpaque(true);
            exContainer.setBackground(ThemeConfig.BG_CARD_HOVER);
            exContainer.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)
            ));

            // Exercise Header Row
            JPanel exHeader = new JPanel(new GridLayout(1, 4, 8, 0));
            exHeader.setOpaque(false);
            JLabel hName = new JLabel("EXERCISE");
            hName.setFont(new Font("Segoe UI", Font.BOLD, 10));
            hName.setForeground(ThemeConfig.TEXT_MUTED);
            JLabel hSets = new JLabel("SETS");
            hSets.setFont(new Font("Segoe UI", Font.BOLD, 10));
            hSets.setForeground(ThemeConfig.TEXT_MUTED);
            JLabel hReps = new JLabel("REPS");
            hReps.setFont(new Font("Segoe UI", Font.BOLD, 10));
            hReps.setForeground(ThemeConfig.TEXT_MUTED);
            JLabel hWeight = new JLabel("TARGET WEIGHT");
            hWeight.setFont(new Font("Segoe UI", Font.BOLD, 10));
            hWeight.setForeground(ThemeConfig.TEXT_MUTED);
            exHeader.add(hName);
            exHeader.add(hSets);
            exHeader.add(hReps);
            exHeader.add(hWeight);
            exContainer.add(exHeader);
            exContainer.add(Box.createVerticalStrut(4));

            for (WorkoutExercise ex : exercises) {
                JPanel exRow = new JPanel(new GridLayout(1, 4, 8, 0));
                exRow.setOpaque(false);

                JLabel rName = new JLabel("• " + ex.getExerciseName());
                rName.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                rName.setForeground(ThemeConfig.TEXT_PRIMARY);

                JLabel rSets = new JLabel(ex.getSets() + " sets");
                rSets.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                rSets.setForeground(ThemeConfig.TEXT_SECONDARY);

                JLabel rReps = new JLabel(ex.getReps() + " reps");
                rReps.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                rReps.setForeground(ThemeConfig.TEXT_SECONDARY);

                String wtStr = ex.getWeightKg() > 0 ? String.format("%.1f kg", ex.getWeightKg()) : "Bodyweight";
                JLabel rWeight = new JLabel(wtStr);
                rWeight.setFont(new Font("Segoe UI", Font.BOLD, 11));
                rWeight.setForeground(ThemeConfig.PRIMARY_ACCENT);

                exRow.add(rName);
                exRow.add(rSets);
                exRow.add(rReps);
                exRow.add(rWeight);
                exContainer.add(exRow);
                exContainer.add(Box.createVerticalStrut(2));
            }
            centerPanel.add(exContainer);
        } else {
            JLabel lblNoEx = new JLabel("General training routine • Target duration: " + w.getDurationMinutes() + " mins • Volume: " + w.getCaloriesBurned() + " kcal");
            lblNoEx.setFont(ThemeConfig.FONT_SMALL);
            lblNoEx.setForeground(ThemeConfig.TEXT_MUTED);
            centerPanel.add(lblNoEx);
        }
        card.add(centerPanel, BorderLayout.CENTER);

        // 3. Card Footer Action Buttons
        JPanel bottomRow = new JPanel(new BorderLayout(8, 0));
        bottomRow.setOpaque(false);

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftActions.setOpaque(false);

        ModernButton btnTimer = new ModernButton("Start Workout Timer", ModernButton.ButtonStyle.GHOST);
        btnTimer.setIcon(IconFactory.createIcon(IconFactory.IconType.TIMER, 11, ThemeConfig.PRIMARY_ACCENT));
        btnTimer.setIconTextGap(5);
        btnTimer.setPreferredSize(new Dimension(155, 28));
        btnTimer.addActionListener(e -> {
            selectWorkoutsTab();
            timerPanel.startWithDuration(w.getDurationMinutes());
            ToastNotification.showToast(this, "Timer set for " + w.getDurationMinutes() + " mins: " + w.getTitle(), ToastNotification.ToastType.SUCCESS);
        });
        leftActions.add(btnTimer);

        ModernButton btnDetails = new ModernButton("Routine Details", ModernButton.ButtonStyle.SECONDARY);
        btnDetails.setPreferredSize(new Dimension(115, 28));
        btnDetails.addActionListener(e -> {
            if ((w.getExercises() == null || w.getExercises().isEmpty()) && w.getId() != null && !w.getId().isBlank()) {
                try {
                    List<WorkoutExercise> exs = workoutDAO.getExercisesForWorkout(w.getId());
                    if (exs != null && !exs.isEmpty()) {
                        w.setExercises(exs);
                    }
                } catch (Exception ignored) {}
            }
            WorkoutDialog dialog = new WorkoutDialog(SwingUtilities.getWindowAncestor(this), w, w.getDurationMinutes());
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                loadWorkouts();
            }
        });
        leftActions.add(btnDetails);
        bottomRow.add(leftActions, BorderLayout.WEST);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);

        if (!isCompleted) {
            ModernButton btnComplete = new ModernButton("Mark as Completed", ModernButton.ButtonStyle.SUCCESS);
            btnComplete.setIcon(IconFactory.createIcon(IconFactory.IconType.CHECK, 12, ThemeConfig.TEXT_ON_ACCENT));
            btnComplete.setPreferredSize(new Dimension(160, 28));
            btnComplete.addActionListener(e -> {
                w.setStatus("COMPLETED");
                SwingWorker<Void, Void> worker = new SwingWorker<>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        workoutDAO.updateWorkout(w);
                        try {
                            String uid = SupabaseClient.getInstance().getCurrentUserId();
                            new TrainerAssignmentDAO().markAssignmentCompletedByAssociatedId(uid, w.getId());
                        } catch (Exception ignored) {}
                        return null;
                    }
                    @Override
                    protected void done() {
                        try {
                            get();
                            ToastNotification.showToast(WorkoutPanel.this, "Completed: " + w.getTitle() + "! Great effort!", ToastNotification.ToastType.SUCCESS);
                            loadWorkouts();
                            checkWorkoutAchievements();
                            DataSyncManager.fireDataChanged(DataSyncManager.DataType.WORKOUTS, DataSyncManager.DataType.GOALS);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(WorkoutPanel.this, "Update failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                };
                worker.execute();
            });
            rightActions.add(btnComplete);
        } else {
            ModernButton btnReopen = new ModernButton("Re-open as Planned", ModernButton.ButtonStyle.SECONDARY);
            btnReopen.setPreferredSize(new Dimension(145, 28));
            btnReopen.addActionListener(e -> {
                w.setStatus("PLANNED");
                SwingWorker<Void, Void> worker = new SwingWorker<>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        workoutDAO.updateWorkout(w);
                        return null;
                    }
                    @Override
                    protected void done() {
                        try {
                            get();
                            ToastNotification.showToast(WorkoutPanel.this, "Re-opened: " + w.getTitle(), ToastNotification.ToastType.INFO);
                            loadWorkouts();
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(WorkoutPanel.this, "Update failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                };
                worker.execute();
            });
            rightActions.add(btnReopen);
        }
        bottomRow.add(rightActions, BorderLayout.EAST);
        card.add(bottomRow, BorderLayout.SOUTH);

        return card;
    }

    // =========================================================================
    // REMINDERS SUB-VIEW & ACTIONS
    // =========================================================================

    public void selectRemindersTab() {
        switchToTab("REMINDERS");
    }

    public void selectWorkoutsTab() {
        switchToTab("WORKOUTS");
    }

    public void scrollToReminders() {
        selectRemindersTab();
    }

    private JPanel createRemindersView() {
        JPanel view = new JPanel(new BorderLayout(0, 8));
        view.setOpaque(false);

        // Top Section of Reminders View
        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);

        // 1. Quick Schedule Presets Card
        ModernCard quickScheduleCard = new ModernCard(12, new Insets(6, 12, 6, 12));
        quickScheduleCard.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JLabel lblQuick = new JLabel("Quick Schedule:");
        lblQuick.setFont(ThemeConfig.FONT_SMALL);
        lblQuick.setForeground(ThemeConfig.TEXT_MUTED);
        quickScheduleCard.add(lblQuick);

        quickScheduleCard.add(createQuickScheduleChip("+ Tomorrow (07:00)", "Morning Workout Session", "Cardio or dynamic mobility routine", LocalDate.now().plusDays(1), LocalTime.of(7, 0)));
        quickScheduleCard.add(createQuickScheduleChip("+ Today Eve (18:00)", "Evening Training Session", "Strength training routine", LocalDate.now(), LocalTime.of(18, 0)));
        quickScheduleCard.add(createQuickScheduleChip("+ Hydration (14:00)", "Hydration Alert", "Drink 500ml of water & electrolyte", LocalDate.now(), LocalTime.of(14, 0)));
        quickScheduleCard.add(createQuickScheduleChip("+ Protein (20:30)", "Post-Workout Nutrition", "Protein shake & recovery meal", LocalDate.now(), LocalTime.of(20, 30)));

        topSection.add(quickScheduleCard);
        topSection.add(Box.createVerticalStrut(8));

        // 2. Today's Agenda Highlight Card
        pnlTodayAgenda.setLayout(new BorderLayout());
        pnlTodayAgenda.setOpaque(false);
        topSection.add(pnlTodayAgenda);
        topSection.add(Box.createVerticalStrut(8));

        // 3. Reminders Header & Filter Bar
        JPanel filterBar = new JPanel(new BorderLayout(10, 0));
        filterBar.setOpaque(false);
        filterBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JLabel lblListTitle = new JLabel("Scheduled Routines & Alerts");
        lblListTitle.setFont(ThemeConfig.FONT_SUBTITLE);
        lblListTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
        filterBar.add(lblListTitle, BorderLayout.WEST);

        pnlScheduleFilters.setLayout(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pnlScheduleFilters.setOpaque(false);
        filterBar.add(pnlScheduleFilters, BorderLayout.EAST);

        topSection.add(filterBar);

        view.add(topSection, BorderLayout.NORTH);

        // Center: Reminders List in a ModernCard with internal JScrollPane (fills remaining height!)
        ModernCard cardList = new ModernCard(12, new Insets(8, 12, 8, 12));
        cardList.setLayout(new BorderLayout());

        pnlRemindersList.setLayout(new BoxLayout(pnlRemindersList, BoxLayout.Y_AXIS));
        pnlRemindersList.setOpaque(false);

        JScrollPane scrollReminders = new JScrollPane(pnlRemindersList);
        scrollReminders.setBorder(null);
        scrollReminders.setOpaque(false);
        scrollReminders.getViewport().setOpaque(false);
        scrollReminders.getVerticalScrollBar().setUnitIncrement(16);
        cardList.add(scrollReminders, BorderLayout.CENTER);

        view.add(cardList, BorderLayout.CENTER);

        // Bottom Bar
        JPanel bottomBar = new JPanel(new BorderLayout(10, 0));
        bottomBar.setOpaque(false);

        JLabel lblTip = new JLabel("Tip: Check a reminder to mark it done, or snooze by +1 day.");
        lblTip.setFont(ThemeConfig.FONT_SMALL);
        lblTip.setForeground(ThemeConfig.TEXT_MUTED);
        bottomBar.add(lblTip, BorderLayout.WEST);

        ModernButton btnBackToWorkouts = new ModernButton("← Back to Workouts Log", ModernButton.ButtonStyle.SECONDARY);
        btnBackToWorkouts.setPreferredSize(new Dimension(175, 30));
        btnBackToWorkouts.addActionListener(e -> selectWorkoutsTab());
        bottomBar.add(btnBackToWorkouts, BorderLayout.EAST);

        view.add(bottomBar, BorderLayout.SOUTH);

        return view;
    }

    private JButton createQuickScheduleChip(String label, String title, String desc, LocalDate date, LocalTime time) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btn.setForeground(ThemeConfig.TEXT_PRIMARY);
        btn.setBackground(ThemeConfig.BG_CARD);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(4, 9, 4, 9)
        ));
        btn.addActionListener(e -> {
            Reminder r = new Reminder(title, desc, date, time);
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    reminderDAO.saveReminder(r);
                    return null;
                }
                @Override
                protected void done() {
                    ToastNotification.showToast(WorkoutPanel.this, "Scheduled: " + title, ToastNotification.ToastType.SUCCESS);
                    loadReminders();
                }
            };
            worker.execute();
        });
        return btn;
    }

    public void loadReminders() {
        SwingWorker<List<Reminder>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Reminder> doInBackground() throws Exception {
                return reminderDAO.getAllReminders();
            }

            @Override
            protected void done() {
                try {
                    currentReminders = get();
                    updateRemindersView();
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void updateRemindersView() {
        LocalDate today = LocalDate.now();

        // 1. Calculate filter counts
        int countAll = currentReminders.size();
        int countToday = 0;
        int countUpcoming = 0;
        int countCompleted = 0;

        Reminder nextTodayReminder = null;

        for (Reminder r : currentReminders) {
            if (r.isCompleted()) {
                countCompleted++;
            } else if (r.getReminderDate() != null && r.getReminderDate().isEqual(today)) {
                countToday++;
                if (nextTodayReminder == null) {
                    nextTodayReminder = r;
                }
            } else if (r.getReminderDate() != null && r.getReminderDate().isAfter(today)) {
                countUpcoming++;
            }
        }
        if (btnTabReminders != null) {
            btnTabReminders.setText("Schedule & Reminders (" + countAll + ")");
        }

        // 2. Rebuild Filter Tabs
        pnlScheduleFilters.removeAll();
        pnlScheduleFilters.add(createScheduleFilterButton("All (" + countAll + ")", "ALL"));
        pnlScheduleFilters.add(createScheduleFilterButton("Today (" + countToday + ")", "TODAY"));
        pnlScheduleFilters.add(createScheduleFilterButton("Upcoming (" + countUpcoming + ")", "UPCOMING"));
        pnlScheduleFilters.add(createScheduleFilterButton("Completed (" + countCompleted + ")", "COMPLETED"));
        pnlScheduleFilters.revalidate();
        pnlScheduleFilters.repaint();

        // 3. Update Today's Agenda Highlight Card
        pnlTodayAgenda.removeAll();
        if (nextTodayReminder != null) {
            ModernCard agendaCard = new ModernCard(12, new Insets(12, 16, 12, 16));
            agendaCard.setLayout(new BorderLayout(14, 0));

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            left.setOpaque(false);

            JLabel icn = new JLabel(IconFactory.createIcon(IconFactory.IconType.WORKOUT, 24, ThemeConfig.PRIMARY_ACCENT));
            left.add(icn);

            JPanel textPnl = new JPanel(new GridLayout(2, 1, 0, 2));
            textPnl.setOpaque(false);

            JLabel tag = new JLabel("TODAY'S TRAINING FOCUS");
            tag.setFont(new Font("Segoe UI", Font.BOLD, 10));
            tag.setForeground(ThemeConfig.PRIMARY_ACCENT);
            textPnl.add(tag);

            String timeStr = DateUtil.formatTime(nextTodayReminder.getReminderTime());
            JLabel titleLbl = new JLabel(nextTodayReminder.getTitle() + " • Due at " + timeStr);
            titleLbl.setFont(ThemeConfig.FONT_BODY_BOLD);
            titleLbl.setForeground(ThemeConfig.TEXT_PRIMARY);
            textPnl.add(titleLbl);
            left.add(textPnl);

            agendaCard.add(left, BorderLayout.CENTER);

            JPanel btnPnl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
            btnPnl.setOpaque(false);

            final Reminder focusReminder = nextTodayReminder;
            ModernButton btnMarkDone = new ModernButton("Mark Done", ModernButton.ButtonStyle.SECONDARY);
            btnMarkDone.setIcon(IconFactory.createIcon(IconFactory.IconType.CHECK, 12, ThemeConfig.TEXT_PRIMARY));
            btnMarkDone.setIconTextGap(6);
            btnMarkDone.addActionListener(e -> toggleReminderStatus(focusReminder, true));

            ModernButton btnStartWorkout = new ModernButton("Start Workout Now", ModernButton.ButtonStyle.PRIMARY);
            btnStartWorkout.setIcon(IconFactory.createIcon(IconFactory.IconType.TIMER, 12, Color.WHITE));
            btnStartWorkout.setIconTextGap(6);
            btnStartWorkout.addActionListener(e -> {
                timerPanel.startTimer();
                selectWorkoutsTab();
                ToastNotification.showToast(WorkoutPanel.this, "Workout timer started for: " + focusReminder.getTitle(), ToastNotification.ToastType.SUCCESS);
            });

            btnPnl.add(btnMarkDone);
            btnPnl.add(btnStartWorkout);
            agendaCard.add(btnPnl, BorderLayout.EAST);

            pnlTodayAgenda.add(agendaCard, BorderLayout.CENTER);
            pnlTodayAgenda.setVisible(true);
        } else {
            pnlTodayAgenda.setVisible(false);
        }
        pnlTodayAgenda.revalidate();
        pnlTodayAgenda.repaint();

        // 4. Filter List Items
        List<Reminder> filtered = new ArrayList<>();
        for (Reminder r : currentReminders) {
            if ("TODAY".equals(reminderFilter)) {
                if (r.getReminderDate() != null && r.getReminderDate().isEqual(today)) filtered.add(r);
            } else if ("UPCOMING".equals(reminderFilter)) {
                if (r.getReminderDate() != null && r.getReminderDate().isAfter(today) && !r.isCompleted()) filtered.add(r);
            } else if ("COMPLETED".equals(reminderFilter)) {
                if (r.isCompleted()) filtered.add(r);
            } else {
                filtered.add(r);
            }
        }

        // 5. Build Reminders List
        pnlRemindersList.removeAll();
        for (Reminder r : filtered) {
            JPanel row = new JPanel(new BorderLayout(14, 0));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeConfig.BORDER_COLOR),
                    BorderFactory.createEmptyBorder(10, 4, 10, 4)
            ));

            // Left: Checkbox + Category Icon
            JPanel leftRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            leftRow.setOpaque(false);

            JCheckBox chk = new JCheckBox();
            chk.setSelected(r.isCompleted());
            chk.setOpaque(false);
            chk.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            chk.addActionListener(e -> toggleReminderStatus(r, chk.isSelected()));
            leftRow.add(chk);

            IconFactory.IconType iconType = getReminderIconType(r.getTitle());
            Color iconColor = r.isCompleted() ? ThemeConfig.TEXT_MUTED : ThemeConfig.PRIMARY_ACCENT;
            JLabel catIcon = new JLabel(IconFactory.createIcon(iconType, 16, iconColor));
            leftRow.add(catIcon);

            row.add(leftRow, BorderLayout.WEST);

            // Center: Title + Badges + Subtitle
            JPanel center = new JPanel(new GridLayout(2, 1, 0, 3));
            center.setOpaque(false);

            JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            titleRow.setOpaque(false);

            JLabel title = new JLabel(r.getTitle());
            title.setFont(ThemeConfig.FONT_BODY_BOLD);
            title.setForeground(r.isCompleted() ? ThemeConfig.TEXT_MUTED : ThemeConfig.TEXT_PRIMARY);
            titleRow.add(title);

            // Status Tag
            JLabel statusTag = createReminderStatusTag(r, today);
            if (statusTag != null) {
                titleRow.add(statusTag);
            }

            if (r.getDescription() != null && r.getDescription().contains("Coach")) {
                JLabel coachTag = new JLabel("🛡️ COACH ASSIGNED");
                coachTag.setFont(new Font("Segoe UI", Font.BOLD, 9));
                coachTag.setForeground(new Color(0x1D, 0x4E, 0xD8));
                coachTag.setOpaque(true);
                coachTag.setBackground(new Color(0xDB, 0xEA, 0xFE));
                coachTag.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                titleRow.add(coachTag);
            }
            center.add(titleRow);

            String descText = (r.getDescription() != null && !r.getDescription().isBlank()) ? r.getDescription() + " • " : "";
            JLabel sub = new JLabel(descText + "Scheduled: " + DateUtil.formatDate(r.getReminderDate()) + " at " + DateUtil.formatTime(r.getReminderTime()));
            sub.setFont(ThemeConfig.FONT_SMALL);
            sub.setForeground(r.isDueOrOverdue() && !r.isCompleted() ? ThemeConfig.ERROR : ThemeConfig.TEXT_MUTED);
            center.add(sub);

            row.add(center, BorderLayout.CENTER);

            // Right: Actions
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            actions.setOpaque(false);

            if (!r.isCompleted()) {
                JButton btnSnooze = new JButton("Snooze +1d");
                btnSnooze.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                btnSnooze.setForeground(ThemeConfig.TEXT_SECONDARY);
                btnSnooze.setContentAreaFilled(false);
                btnSnooze.setBorderPainted(false);
                btnSnooze.setFocusPainted(false);
                btnSnooze.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btnSnooze.addActionListener(e -> snoozeReminder(r));
                actions.add(btnSnooze);
            }

            JButton btnEdit = new JButton("Edit");
            btnEdit.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btnEdit.setForeground(ThemeConfig.PRIMARY_ACCENT);
            btnEdit.setContentAreaFilled(false);
            btnEdit.setBorderPainted(false);
            btnEdit.setFocusPainted(false);
            btnEdit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnEdit.addActionListener(e -> openReminderDialog(r));
            actions.add(btnEdit);

            JButton btnDel = new JButton("Delete");
            btnDel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btnDel.setForeground(ThemeConfig.ERROR);
            btnDel.setContentAreaFilled(false);
            btnDel.setBorderPainted(false);
            btnDel.setFocusPainted(false);
            btnDel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnDel.addActionListener(e -> deleteReminder(r));
            actions.add(btnDel);

            row.add(actions, BorderLayout.EAST);
            pnlRemindersList.add(row);
        }

        if (filtered.isEmpty()) {
            JPanel emptyBox = new JPanel();
            emptyBox.setLayout(new BoxLayout(emptyBox, BoxLayout.Y_AXIS));
            emptyBox.setOpaque(false);
            emptyBox.setBorder(BorderFactory.createEmptyBorder(28, 20, 28, 20));

            JLabel lblEmpty = new JLabel("No workout reminders in this filter.");
            lblEmpty.setFont(ThemeConfig.FONT_BODY_BOLD);
            lblEmpty.setForeground(ThemeConfig.TEXT_SECONDARY);
            lblEmpty.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyBox.add(lblEmpty);
            emptyBox.add(Box.createVerticalStrut(6));

            JLabel lblSub = new JLabel("Schedule regular routine alerts, hydration checks, or recovery sessions.");
            lblSub.setFont(ThemeConfig.FONT_SMALL);
            lblSub.setForeground(ThemeConfig.TEXT_MUTED);
            lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyBox.add(lblSub);
            emptyBox.add(Box.createVerticalStrut(14));

            ModernButton btnAddPrompt = new ModernButton("+ Set Reminder", ModernButton.ButtonStyle.PRIMARY);
            btnAddPrompt.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnAddPrompt.addActionListener(e -> openReminderDialog(null));
            emptyBox.add(btnAddPrompt);

            pnlRemindersList.add(emptyBox);
        }

        pnlRemindersList.revalidate();
        pnlRemindersList.repaint();
    }

    private JButton createScheduleFilterButton(String text, String filterKey) {
        boolean active = reminderFilter.equals(filterKey);
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 11));
        btn.setForeground(active ? ThemeConfig.TEXT_ON_ACCENT : ThemeConfig.TEXT_PRIMARY);
        btn.setBackground(active ? ThemeConfig.PRIMARY_ACCENT : ThemeConfig.BG_CARD);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(active ? ThemeConfig.PRIMARY_ACCENT : ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        btn.addActionListener(e -> {
            reminderFilter = filterKey;
            updateRemindersView();
        });
        return btn;
    }

    private JLabel createReminderStatusTag(Reminder r, LocalDate today) {
        JLabel tag = new JLabel();
        tag.setFont(new Font("Segoe UI", Font.BOLD, 10));

        if (r.isCompleted()) {
            tag.setText("DONE");
            tag.setForeground(ThemeConfig.SUCCESS);
            return tag;
        }
        if (r.isDueOrOverdue()) {
            tag.setText("OVERDUE");
            tag.setForeground(ThemeConfig.ERROR);
            return tag;
        }
        if (r.getReminderDate() != null) {
            if (r.getReminderDate().isEqual(today)) {
                tag.setText("TODAY");
                tag.setForeground(ThemeConfig.WARNING);
                return tag;
            }
            if (r.getReminderDate().isEqual(today.plusDays(1))) {
                tag.setText("TOMORROW");
                tag.setForeground(ThemeConfig.PRIMARY_ACCENT);
                return tag;
            }
        }
        return null;
    }

    private IconFactory.IconType getReminderIconType(String title) {
        if (title == null) return IconFactory.IconType.REMINDERS;
        String t = title.toLowerCase();
        if (t.contains("workout") || t.contains("training") || t.contains("cardio") || t.contains("push") || t.contains("pull") || t.contains("leg") || t.contains("gym") || t.contains("run")) {
            return IconFactory.IconType.WORKOUT;
        }
        if (t.contains("water") || t.contains("hydrat")) {
            return IconFactory.IconType.WATER;
        }
        if (t.contains("food") || t.contains("nutrition") || t.contains("protein") || t.contains("meal") || t.contains("diet")) {
            return IconFactory.IconType.NUTRITION;
        }
        if (t.contains("sleep") || t.contains("bed") || t.contains("rest") || t.contains("recovery")) {
            return IconFactory.IconType.SLEEP;
        }
        return IconFactory.IconType.REMINDERS;
    }

    private void snoozeReminder(Reminder r) {
        LocalDate current = r.getReminderDate() != null ? r.getReminderDate() : LocalDate.now();
        r.setReminderDate(current.plusDays(1));
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                reminderDAO.updateReminder(r);
                return null;
            }
            @Override
            protected void done() {
                ToastNotification.showToast(WorkoutPanel.this, "Snoozed to tomorrow: " + r.getTitle(), ToastNotification.ToastType.INFO);
                loadReminders();
            }
        };
        worker.execute();
    }

    private void toggleReminderStatus(Reminder r, boolean completed) {
        r.setCompleted(completed);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                reminderDAO.updateReminder(r);
                if (completed) {
                    try {
                        String uid = SupabaseClient.getInstance().getCurrentUserId();
                        new TrainerAssignmentDAO().markAssignmentCompletedByAssociatedId(uid, r.getId());
                    } catch (Exception ignored) {}
                }
                return null;
            }
            @Override
            protected void done() {
                ToastNotification.showToast(WorkoutPanel.this, completed ? "Reminder marked as completed!" : "Reminder marked active.", ToastNotification.ToastType.SUCCESS);
                loadReminders();
                DataSyncManager.fireDataChanged(DataSyncManager.DataType.REMINDERS);
            }
        };
        worker.execute();
    }

    private void deleteReminder(Reminder r) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete the reminder \"" + r.getTitle() + "\"?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                reminderDAO.deleteReminder(r.getId());
                return null;
            }
            @Override
            protected void done() {
                ToastNotification.showToast(WorkoutPanel.this, "Reminder deleted.", ToastNotification.ToastType.INFO);
                loadReminders();
                DataSyncManager.fireDataChanged(DataSyncManager.DataType.REMINDERS);
            }
        };
        worker.execute();
    }

    public void openReminderDialog(Reminder editingReminder) {
        ReminderDialog dialog = new ReminderDialog(SwingUtilities.getWindowAncestor(this), editingReminder);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            ToastNotification.showToast(this, editingReminder == null ? "Reminder scheduled successfully!" : "Reminder updated!", ToastNotification.ToastType.SUCCESS);
            loadReminders();
        }
    }

    public void openReminderDialog() {
        openReminderDialog(null);
    }

    // Modal Reminder Dialog
    private class ReminderDialog extends JDialog {
        private static final long serialVersionUID = 1L;
        private boolean saved = false;
        private final Reminder editingReminder;

        private final ModernTextField txtTitle = new ModernTextField("", "e.g. Evening Workout Session");
        private final ModernTextField txtDesc = new ModernTextField("", "e.g. Leg day workout & protein shake");
        private final ModernTextField txtDate = new ModernTextField(LocalDate.now().toString(), "YYYY-MM-DD");
        private final ModernTextField txtTime = new ModernTextField("", "e.g. 18:00");

        public ReminderDialog(Window owner, Reminder reminder) {
            super(owner, reminder == null ? "Create New Reminder" : "Edit Reminder", ModalityType.APPLICATION_MODAL);
            this.editingReminder = reminder;
            setSize(490, 560);
            setLocationRelativeTo(owner);
            setResizable(false);
            getContentPane().setBackground(ThemeConfig.BG_DARK);

            JPanel main = new JPanel();
            main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
            main.setOpaque(false);
            main.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

            // Section 1: Quick Reminder Presets (only when creating new)
            if (reminder == null) {
                JLabel lblPresets = new JLabel("Quick Reminder Templates");
                lblPresets.setFont(ThemeConfig.FONT_SMALL);
                lblPresets.setForeground(ThemeConfig.TEXT_SECONDARY);
                main.add(lblPresets);
                main.add(Box.createVerticalStrut(4));

                JPanel presetRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
                presetRow.setOpaque(false);
                presetRow.add(createReminderPresetChip("Workout", "Workout Session", "Crush today's planned training", "18:00"));
                presetRow.add(createReminderPresetChip("Hydration", "Hydration Check", "Drink at least 500ml of water", "14:00"));
                presetRow.add(createReminderPresetChip("Log Food", "Log Daily Nutrition", "Review daily calories and macros", "20:30"));
                presetRow.add(createReminderPresetChip("Sleep", "Bedtime Wind-down", "Screens off, prepare for sleep", "22:30"));
                main.add(presetRow);
                main.add(Box.createVerticalStrut(12));
            }

            // Title & Description
            main.add(createFieldRow("Reminder Title *", txtTitle));
            main.add(Box.createVerticalStrut(10));
            main.add(createFieldRow("Description", txtDesc));
            main.add(Box.createVerticalStrut(10));

            // Date with Quick Pills
            JPanel dateRow = new JPanel(new BorderLayout(8, 0));
            dateRow.setOpaque(false);
            JLabel lblDateTag = new JLabel("Reminder Date");
            lblDateTag.setFont(ThemeConfig.FONT_SMALL);
            lblDateTag.setForeground(ThemeConfig.TEXT_SECONDARY);
            dateRow.add(lblDateTag, BorderLayout.NORTH);

            JPanel dateControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            dateControls.setOpaque(false);
            JButton btnToday = createQuickPill("Today", true);
            JButton btnTomorrow = createQuickPill("Tomorrow", false);
            btnToday.addActionListener(e -> {
                txtDate.setText(LocalDate.now().toString());
                ThemeConfig.styleQuickPill(btnToday, true);
                ThemeConfig.styleQuickPill(btnTomorrow, false);
            });
            btnTomorrow.addActionListener(e -> {
                txtDate.setText(LocalDate.now().plusDays(1).toString());
                ThemeConfig.styleQuickPill(btnTomorrow, true);
                ThemeConfig.styleQuickPill(btnToday, false);
            });
            txtDate.setPreferredSize(new Dimension(130, 32));
            dateControls.add(btnToday);
            dateControls.add(btnTomorrow);
            dateControls.add(txtDate);
            dateRow.add(dateControls, BorderLayout.CENTER);
            main.add(dateRow);
            main.add(Box.createVerticalStrut(12));

            // Time with Quick Hour Chips
            JPanel timeSection = new JPanel();
            timeSection.setLayout(new BoxLayout(timeSection, BoxLayout.Y_AXIS));
            timeSection.setOpaque(false);
            JLabel lblTimeTag = new JLabel("Time (HH:MM 24h) *");
            lblTimeTag.setFont(ThemeConfig.FONT_SMALL);
            lblTimeTag.setForeground(ThemeConfig.TEXT_SECONDARY);
            timeSection.add(lblTimeTag);
            timeSection.add(Box.createVerticalStrut(4));

            JPanel timeChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            timeChips.setOpaque(false);
            timeChips.add(createTimeChip("Morning 08:00", "08:00"));
            timeChips.add(createTimeChip("Noon 12:30", "12:30"));
            timeChips.add(createTimeChip("Evening 18:00", "18:00"));
            timeChips.add(createTimeChip("Night 21:30", "21:30"));
            txtTime.setPreferredSize(new Dimension(90, 30));
            timeChips.add(txtTime);
            timeSection.add(timeChips);
            main.add(timeSection);

            main.add(Box.createVerticalGlue());
            main.add(Box.createVerticalStrut(16));

            // Action Buttons
            JPanel actions = new JPanel(new BorderLayout());
            actions.setOpaque(false);
            JLabel lblHint = new JLabel("Tip: Press Enter to save");
            lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            lblHint.setForeground(ThemeConfig.TEXT_MUTED);
            actions.add(lblHint, BorderLayout.WEST);

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            btnRow.setOpaque(false);
            ModernButton btnCancel = new ModernButton("Cancel", ModernButton.ButtonStyle.GHOST);
            btnCancel.addActionListener(e -> dispose());
            ModernButton btnSave = new ModernButton(reminder == null ? "Set Reminder" : "Update Reminder", ModernButton.ButtonStyle.PRIMARY);
            btnSave.addActionListener(e -> saveReminder());
            btnRow.add(btnCancel);
            btnRow.add(btnSave);
            actions.add(btnRow, BorderLayout.EAST);
            main.add(actions);

            setContentPane(main);

            // Shortcuts
            txtTitle.addActionListener(e -> saveReminder());
            txtDesc.addActionListener(e -> saveReminder());
            txtDate.addActionListener(e -> saveReminder());
            txtTime.addActionListener(e -> saveReminder());

            getRootPane().registerKeyboardAction(e -> dispose(),
                    KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);

            if (reminder != null) {
                txtTitle.setText(reminder.getTitle());
                txtDesc.setText(reminder.getDescription() != null ? reminder.getDescription() : "");
                txtDate.setText(reminder.getReminderDate() != null ? reminder.getReminderDate().toString() : LocalDate.now().toString());
                txtTime.setText(reminder.getReminderTime() != null ? DateUtil.formatTime(reminder.getReminderTime()) : "18:00");
            }

            SwingUtilities.invokeLater(() -> {
                txtTitle.requestFocusInWindow();
                txtTitle.selectAll();
            });
        }

        private JButton createReminderPresetChip(String label, String title, String desc, String time) {
            JButton btn = new JButton(label);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btn.setForeground(ThemeConfig.TEXT_PRIMARY);
            btn.setBackground(ThemeConfig.BG_CARD);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));
            btn.addActionListener(e -> {
                txtTitle.setText(title);
                txtDesc.setText(desc);
                txtTime.setText(time);
            });
            return btn;
        }

        private JButton createTimeChip(String text, String timeVal) {
            JButton btn = new JButton(text);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btn.setForeground(ThemeConfig.TEXT_PRIMARY);
            btn.setBackground(ThemeConfig.BG_CARD);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            btn.addActionListener(e -> txtTime.setText(timeVal));
            return btn;
        }

        private JButton createQuickPill(String text, boolean active) {
            JButton btn = new JButton(text);
            ThemeConfig.styleQuickPill(btn, active);
            return btn;
        }

        private JPanel createFieldRow(String label, JComponent comp) {
            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.setOpaque(false);
            JLabel l = new JLabel(label);
            l.setFont(ThemeConfig.FONT_SMALL);
            l.setForeground(ThemeConfig.TEXT_SECONDARY);
            p.add(l);
            p.add(Box.createVerticalStrut(4));
            p.add(comp);
            return p;
        }

        private void saveReminder() {
            String title = txtTitle.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a reminder title.");
                return;
            }

            LocalDate date = DateUtil.parseDateOrNull(txtDate.getText().trim());
            if (date == null) {
                JOptionPane.showMessageDialog(this, "Please enter a valid date.");
                return;
            }

            try {
                String[] parts = txtTime.getText().trim().split(":");
                LocalTime time = LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));

                Reminder r = editingReminder != null ? editingReminder : new Reminder();
                r.setTitle(title);
                r.setDescription(txtDesc.getText().trim());
                r.setReminderDate(date);
                r.setReminderTime(time);

                SwingWorker<Void, Void> worker = new SwingWorker<>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        if (editingReminder != null && editingReminder.getId() != null) {
                            reminderDAO.updateReminder(r);
                        } else {
                            reminderDAO.saveReminder(r);
                        }
                        return null;
                    }
                    @Override
                    protected void done() {
                        try {
                            get();
                            saved = true;
                            dispose();
                            DataSyncManager.fireDataChanged(DataSyncManager.DataType.REMINDERS);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(ReminderDialog.this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                };
                worker.execute();

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Please enter time in HH:MM format (e.g. 18:30).");
            }
        }

        public boolean isSaved() {
            return saved;
        }
    }
}
