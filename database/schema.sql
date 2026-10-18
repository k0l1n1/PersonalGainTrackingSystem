-- ============================================================================
-- PERSONAL GAIN TRACKING SYSTEM - SUPABASE POSTGRESQL SCHEMA
-- Database Schema with Tables, Constraints, Indexes, RLS Policies & Triggers
-- ============================================================================

-- Enable pgcrypto for UUID generation if not enabled
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Ensure auth schema and auth.users table exist (for standalone/local PostgreSQL compatibility)
CREATE SCHEMA IF NOT EXISTS auth;
CREATE TABLE IF NOT EXISTS auth.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT UNIQUE,
    raw_user_meta_data JSONB,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- ============================================================================
-- 1. USER PROFILES TABLE
-- Linked to Supabase Auth (auth.users)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT UNIQUE NOT NULL,
    full_name TEXT NOT NULL DEFAULT '',
    role TEXT NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('MEMBER', 'TRAINER')),
    user_code TEXT UNIQUE,
    gender TEXT CHECK (gender IN ('Male', 'Female', 'Non-Binary', 'Other', 'Prefer not to say')),
    date_of_birth DATE,
    height_cm NUMERIC(5,2) CHECK (height_cm > 0),
    target_weight_kg NUMERIC(5,2) CHECK (target_weight_kg > 0),
    daily_calorie_target INT DEFAULT 2000 CHECK (daily_calorie_target >= 500 AND daily_calorie_target <= 10000),
    daily_water_target_ml INT DEFAULT 2500 CHECK (daily_water_target_ml >= 500 AND daily_water_target_ml <= 10000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- 2. WORKOUTS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.workouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    workout_type TEXT NOT NULL CHECK (workout_type IN ('Strength', 'Cardio', 'HIIT', 'Yoga', 'Flexibility', 'Endurance', 'Other')),
    workout_date DATE NOT NULL DEFAULT CURRENT_DATE,
    duration_minutes INT NOT NULL CHECK (duration_minutes >= 0 AND duration_minutes <= 1440),
    calories_burned INT DEFAULT 0 CHECK (calories_burned >= 0),
    status TEXT NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('COMPLETED', 'PLANNED')),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- 3. WORKOUT EXERCISES TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.workout_exercises (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workout_id UUID NOT NULL REFERENCES public.workouts(id) ON DELETE CASCADE,
    exercise_name TEXT NOT NULL,
    sets INT NOT NULL DEFAULT 1 CHECK (sets >= 0),
    reps INT NOT NULL DEFAULT 1 CHECK (reps >= 0),
    weight_kg NUMERIC(6,2) DEFAULT 0.0 CHECK (weight_kg >= 0),
    notes TEXT,
    order_index INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- 4. FITNESS GOALS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    category TEXT NOT NULL CHECK (category IN ('Weight', 'Workout Frequency', 'Strength', 'Endurance', 'Nutrition', 'Habit')),
    target_value NUMERIC(8,2) NOT NULL CHECK (target_value >= 0),
    current_value NUMERIC(8,2) NOT NULL DEFAULT 0.0 CHECK (current_value >= 0),
    unit TEXT NOT NULL,
    start_date DATE NOT NULL DEFAULT CURRENT_DATE,
    target_date DATE NOT NULL,
    status TEXT NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'OVERDUE', 'CANCELLED')),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- 5. BODY MEASUREMENTS & WEIGHT TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.body_measurements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    recorded_date DATE NOT NULL DEFAULT CURRENT_DATE,
    weight_kg NUMERIC(5,2) NOT NULL CHECK (weight_kg > 0 AND weight_kg < 500),
    height_cm NUMERIC(5,2) CHECK (height_cm > 0 AND height_cm < 300),
    bmi NUMERIC(4,1) CHECK (bmi > 0),
    waist_cm NUMERIC(5,2) CHECK (waist_cm >= 0),
    chest_cm NUMERIC(5,2) CHECK (chest_cm >= 0),
    arm_cm NUMERIC(5,2) CHECK (arm_cm >= 0),
    hips_cm NUMERIC(5,2) CHECK (hips_cm >= 0),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- 6. NUTRITION ENTRIES TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.nutrition_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    food_name TEXT NOT NULL,
    meal_type TEXT NOT NULL CHECK (meal_type IN ('BREAKFAST', 'LUNCH', 'DINNER', 'SNACK')),
    serving_size TEXT DEFAULT '1 serving',
    calories INT NOT NULL CHECK (calories >= 0 AND calories <= 10000),
    protein_g NUMERIC(5,1) DEFAULT 0 CHECK (protein_g >= 0),
    carbs_g NUMERIC(5,1) DEFAULT 0 CHECK (carbs_g >= 0),
    fat_g NUMERIC(5,1) DEFAULT 0 CHECK (fat_g >= 0),
    entry_date DATE NOT NULL DEFAULT CURRENT_DATE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- 7. WATER INTAKE TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.water_intake (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    amount_ml INT NOT NULL CHECK (amount_ml > 0 AND amount_ml <= 5000),
    recorded_date DATE NOT NULL DEFAULT CURRENT_DATE,
    recorded_time TIME NOT NULL DEFAULT CURRENT_TIME,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- 8. SLEEP RECORDS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.sleep_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    sleep_date DATE NOT NULL DEFAULT CURRENT_DATE,
    bedtime TIMESTAMPTZ NOT NULL,
    wake_time TIMESTAMPTZ NOT NULL,
    duration_minutes INT NOT NULL CHECK (duration_minutes >= 0 AND duration_minutes <= 1440),
    quality_score INT CHECK (quality_score BETWEEN 1 AND 5),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- 9. REMINDERS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    reminder_date DATE NOT NULL,
    reminder_time TIME NOT NULL,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- 10. ACHIEVEMENTS CATALOG TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.achievements (
    id VARCHAR(50) PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    badge_icon TEXT NOT NULL,
    category TEXT NOT NULL CHECK (category IN ('WORKOUT', 'GOAL', 'STREAK', 'NUTRITION', 'WATER', 'WEIGHT'))
);

-- Seed predefined achievements
INSERT INTO public.achievements (id, name, description, badge_icon, category)
VALUES
    ('FIRST_WORKOUT', 'First Step', 'Completed your first recorded workout', 'dumbbell', 'WORKOUT'),
    ('WORKOUT_STREAK_3', 'On Fire (3-Day Streak)', 'Logged workouts on 3 consecutive days', 'fire', 'STREAK'),
    ('WORKOUT_STREAK_7', 'Unstoppable (7-Day Streak)', 'Logged workouts on 7 consecutive days', 'flame', 'STREAK'),
    ('WORKOUT_10', 'Fitness Enthusiast', 'Completed 10 total workout sessions', 'medal', 'WORKOUT'),
    ('WORKOUT_50', 'Iron Will', 'Completed 50 total workout sessions', 'trophy', 'WORKOUT'),
    ('FIRST_GOAL', 'Goal Getter', 'Successfully achieved your first fitness goal', 'flag', 'GOAL'),
    ('GOAL_MASTER', 'Master of Goals', 'Completed 5 fitness goals', 'crown', 'GOAL'),
    ('HYDRATION_HERO', 'Hydration Hero', 'Met your daily water intake target for the first time', 'droplet', 'WATER'),
    ('NUTRITION_TRACKER', 'Clean Fuel', 'Logged nutrition entries for an entire day', 'apple', 'NUTRITION'),
    ('BODY_TRACKER', 'Self Awareness', 'Recorded 5 body measurement check-ins', 'ruler', 'WEIGHT')
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- 11. USER ACHIEVEMENTS TABLE (Earned Badges)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.user_achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    achievement_id VARCHAR(50) NOT NULL REFERENCES public.achievements(id) ON DELETE CASCADE,
    unlocked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, achievement_id)
);

-- ============================================================================
-- 12. PROGRESS PHOTOS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.progress_photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    photo_url TEXT NOT NULL,
    storage_path TEXT NOT NULL,
    photo_date DATE NOT NULL DEFAULT CURRENT_DATE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- 13. TRAINER-STUDENT ENROLLMENTS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.trainer_students (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trainer_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    enrolled_date DATE NOT NULL DEFAULT CURRENT_DATE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (trainer_id, student_id)
);

-- ============================================================================
-- INDEXES FOR PERFORMANCE
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_workouts_user_date ON public.workouts(user_id, workout_date DESC);
CREATE INDEX IF NOT EXISTS idx_workout_exercises_workout ON public.workout_exercises(workout_id);
CREATE INDEX IF NOT EXISTS idx_goals_user_status ON public.goals(user_id, status);
CREATE INDEX IF NOT EXISTS idx_measurements_user_date ON public.body_measurements(user_id, recorded_date DESC);
CREATE INDEX IF NOT EXISTS idx_nutrition_user_date ON public.nutrition_entries(user_id, entry_date DESC);
CREATE INDEX IF NOT EXISTS idx_water_user_date ON public.water_intake(user_id, recorded_date DESC);
CREATE INDEX IF NOT EXISTS idx_sleep_user_date ON public.sleep_records(user_id, sleep_date DESC);
CREATE INDEX IF NOT EXISTS idx_reminders_user_date ON public.reminders(user_id, reminder_date, is_completed);
CREATE INDEX IF NOT EXISTS idx_user_achievements_user ON public.user_achievements(user_id);
CREATE INDEX IF NOT EXISTS idx_photos_user_date ON public.progress_photos(user_id, photo_date DESC);
CREATE INDEX IF NOT EXISTS idx_trainer_students_trainer ON public.trainer_students(trainer_id);
CREATE INDEX IF NOT EXISTS idx_trainer_students_student ON public.trainer_students(student_id);
CREATE INDEX IF NOT EXISTS idx_profiles_user_code ON public.profiles(user_code);

-- ============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- Ensures each authenticated user can ONLY access and modify their own records!
-- ============================================================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.trainer_students ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.workouts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.workout_exercises ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.goals ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.body_measurements ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.nutrition_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.water_intake ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.sleep_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reminders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.achievements ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_achievements ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.progress_photos ENABLE ROW LEVEL SECURITY;

-- 1. Profiles Policies
DROP POLICY IF EXISTS "Users can view own profile" ON public.profiles;
CREATE POLICY "Users can view own profile" ON public.profiles
    FOR SELECT USING (auth.uid() = id);

DROP POLICY IF EXISTS "Users can insert own profile" ON public.profiles;
CREATE POLICY "Users can insert own profile" ON public.profiles
    FOR INSERT WITH CHECK (auth.uid() = id);

DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
CREATE POLICY "Users can update own profile" ON public.profiles
    FOR UPDATE USING (auth.uid() = id);

-- 2. Workouts Policies
DROP POLICY IF EXISTS "Users can manage own workouts" ON public.workouts;
CREATE POLICY "Users can manage own workouts" ON public.workouts
    FOR ALL USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 3. Workout Exercises Policies
DROP POLICY IF EXISTS "Users can manage own workout exercises" ON public.workout_exercises;
CREATE POLICY "Users can manage own workout exercises" ON public.workout_exercises
    FOR ALL USING (
        EXISTS (
            SELECT 1 FROM public.workouts w
            WHERE w.id = workout_exercises.workout_id
            AND w.user_id = auth.uid()
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.workouts w
            WHERE w.id = workout_exercises.workout_id
            AND w.user_id = auth.uid()
        )
    );

-- 4. Goals Policies
DROP POLICY IF EXISTS "Users can manage own goals" ON public.goals;
CREATE POLICY "Users can manage own goals" ON public.goals
    FOR ALL USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 5. Body Measurements Policies
DROP POLICY IF EXISTS "Users can manage own body measurements" ON public.body_measurements;
CREATE POLICY "Users can manage own body measurements" ON public.body_measurements
    FOR ALL USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 6. Nutrition Entries Policies
DROP POLICY IF EXISTS "Users can manage own nutrition entries" ON public.nutrition_entries;
CREATE POLICY "Users can manage own nutrition entries" ON public.nutrition_entries
    FOR ALL USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 7. Water Intake Policies
DROP POLICY IF EXISTS "Users can manage own water intake" ON public.water_intake;
CREATE POLICY "Users can manage own water intake" ON public.water_intake
    FOR ALL USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 8. Sleep Records Policies
DROP POLICY IF EXISTS "Users can manage own sleep records" ON public.sleep_records;
CREATE POLICY "Users can manage own sleep records" ON public.sleep_records
    FOR ALL USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 9. Reminders Policies
DROP POLICY IF EXISTS "Users can manage own reminders" ON public.reminders;
CREATE POLICY "Users can manage own reminders" ON public.reminders
    FOR ALL USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 10. Achievements Catalog (Public Read for Authenticated & Anon Users)
DROP POLICY IF EXISTS "Authenticated users can view achievements" ON public.achievements;
DROP POLICY IF EXISTS "Allow public read on achievements" ON public.achievements;
CREATE POLICY "Allow public read on achievements" ON public.achievements
    FOR SELECT USING (true);

-- 11. User Achievements Policies
DROP POLICY IF EXISTS "Users can manage own achievements" ON public.user_achievements;
CREATE POLICY "Users can manage own achievements" ON public.user_achievements
    FOR ALL USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 12. Progress Photos Policies
DROP POLICY IF EXISTS "Users can manage own progress photos" ON public.progress_photos;
CREATE POLICY "Users can manage own progress photos" ON public.progress_photos
    FOR ALL USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 13. Trainer-Student Policies
DROP POLICY IF EXISTS "Trainers can manage own student relationships" ON public.trainer_students;
CREATE POLICY "Trainers can manage own student relationships" ON public.trainer_students
    FOR ALL USING (auth.uid() = trainer_id)
    WITH CHECK (auth.uid() = trainer_id);

DROP POLICY IF EXISTS "Students can view trainer relationships" ON public.trainer_students;
CREATE POLICY "Students can view trainer relationships" ON public.trainer_students
    FOR SELECT USING (auth.uid() = student_id);

-- ============================================================================
-- AUTOMATIC PROFILE CREATION TRIGGER
-- When a user registers in auth.users, automatically initialize public.profiles
-- ============================================================================
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
DECLARE
    gen_code TEXT;
BEGIN
    gen_code := 'PG-' || upper(substr(md5(random()::text || clock_timestamp()::text), 1, 6));

    INSERT INTO public.profiles (id, email, full_name, role, user_code)
    VALUES (
        NEW.id,
        COALESCE(NEW.email, ''),
        COALESCE(NEW.raw_user_meta_data->>'full_name', split_part(COALESCE(NEW.email, ''), '@', 1), ''),
        COALESCE(NEW.raw_user_meta_data->>'role', 'MEMBER'),
        gen_code
    )
    ON CONFLICT (id) DO UPDATE
    SET email = EXCLUDED.email,
        full_name = CASE 
            WHEN EXCLUDED.full_name IS NOT NULL AND EXCLUDED.full_name <> '' 
            THEN EXCLUDED.full_name 
            ELSE public.profiles.full_name 
        END,
        role = COALESCE(NEW.raw_user_meta_data->>'role', public.profiles.role),
        updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ============================================================================
-- SUPABASE STORAGE BUCKET CONFIGURATION (Progress Photos)
-- ============================================================================
DO $$
BEGIN
    -- Check if storage schema exists (standard in Supabase)
    IF EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = 'storage') THEN
        INSERT INTO storage.buckets (id, name, public)
        VALUES ('progress_photos', 'progress_photos', true)
        ON CONFLICT (id) DO UPDATE SET public = true;

        -- RLS Policies for Storage
        DROP POLICY IF EXISTS "Allow authenticated uploads" ON storage.objects;
        CREATE POLICY "Allow authenticated uploads" ON storage.objects
            FOR INSERT TO authenticated WITH CHECK (bucket_id = 'progress_photos');

        DROP POLICY IF EXISTS "Allow public photo viewing" ON storage.objects;
        CREATE POLICY "Allow public photo viewing" ON storage.objects
            FOR SELECT USING (bucket_id = 'progress_photos');

        DROP POLICY IF EXISTS "Allow authenticated deletes" ON storage.objects;
        CREATE POLICY "Allow authenticated deletes" ON storage.objects
            FOR DELETE TO authenticated USING (bucket_id = 'progress_photos');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'Storage setup skipped or encountered notice: %', SQLERRM;
END $$;
