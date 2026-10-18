# Personal Gain Tracking System

[![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://www.oracle.com/java/)
[![GUI](https://img.shields.io/badge/GUI-Java%20Swing%20%2B%20FlatLaf-0F172A.svg)](https://www.formdev.com/flatlaf/)
[![Backend](https://img.shields.io/badge/Backend-Supabase%20PostgreSQL-3ECF8E.svg)](https://supabase.com/)
[![Build](https://img.shields.io/badge/Build-Maven-C71A36.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

A production-grade, object-oriented desktop application built in **Java** and **Java Swing** for fitness and health management, backed by **Supabase PostgreSQL** cloud database and **Supabase GoTrue Auth**.

The application features a modern dark navy, blue, and grey UI/UX design powered by **FlatLaf** and **JFreeChart**, strict client-enforced Row Level Security (RLS), full CRUD operations, and non-blocking asynchronous execution via `SwingWorker`.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Key Features](#2-key-features)
3. [Architecture and Design](#3-architecture-and-design)
4. [Prerequisites](#4-prerequisites)
5. [Java and Maven Setup](#5-java-and-maven-setup)
6. [Supabase Project & Database Setup](#6-supabase-project--database-setup)
7. [Environment Variable Configuration](#7-environment-variable-configuration)
8. [Required Dependencies](#8-required-dependencies)
9. [How to Run the Application](#9-how-to-run-the-application)
10. [How to Run Tests](#10-how-to-run-tests)
11. [Troubleshooting & Common Errors](#11-troubleshooting--common-errors)

---

## 1. Project Overview

The **Personal Gain Tracking System** was developed for university and professional Object-Oriented Programming (OOP) benchmarks. Rather than acting as a standard CRUD table, it incorporates:
- Fully asynchronous networking on background threads (`SwingWorker`) so the Swing UI never freezes.
- Vector-rendered icons and anti-aliased custom Swing components.
- Direct authenticated Supabase REST API communication passing JWT Bearer tokens to PostgreSQL Row Level Security (RLS) policies.
- Automatic offline/demo mode with realistic mock athlete data if cloud credentials are not yet configured.

---

## 2. Key Features

- **User Authentication**:
  - Email & password registration and login via Supabase GoTrue Auth.
  - Password recovery email dispatch.
  - Session handling with JWT Bearer token propagation.
  - Safe local fallback for offline evaluation.

- **Interactive Dashboard**:
  - Welcome greeting with user's custom name.
  - 4 Key Metric Cards (Total Workouts & Weekly minutes, Weight & WHO BMI status, Calories & Budget percentage, Workout Streak).
  - JFreeChart Weekly Workout Bar Chart and Weight Progression Line Chart.
  - Active goals progress meters.
  - Today's hydration and sleep recovery snapshot.
  - Upcoming reminders.

- **Workout Management**:
  - Full CRUD: log, view, edit, search, and delete workout sessions.
  - Interactive Stopwatch Timer (Start, Pause, Resume, Reset, and "Record As Workout").
  - Detailed exercise logging: Exercise name, sets, repetitions, and weight lifted (kg).
  - Automatic MET-based caloric expenditure estimator.
  - Table sorting and instant text filtering.

- **Fitness & Personal Goals**:
  - Goal categories: Weight, Workout Frequency, Strength, Endurance, Nutrition, Habit.
  - Target vs. Current values with progress bar percentage indicators.
  - Status tracking: In-Progress, Completed, Overdue.
  - Milestone achievement unlock triggers.

- **Body Measurements & Weight**:
  - Historical weight logging with WHO classification (Underweight, Normal, Overweight, Obese).
  - Calculated BMI from valid height and weight measurements.
  - Circumference records: waist, chest, arms.
  - JFreeChart progression time-series chart.

- **Nutrition & Calorie Tracking**:
  - Meal categorisation: Breakfast, Lunch, Dinner, Snacks.
  - Automatic calculation of daily calories and macronutrient totals (Protein, Carbs, Healthy Fats).
  - Interactive JFreeChart macronutrient donut ring chart.
  - Prev/Next day navigator.

- **Water & Sleep Tracking**:
  - Daily hydration progress bar against customized target.
  - Quick-add water buttons (+250ml glass, +500ml bottle, +750ml shaker, custom ml).
  - Sleep duration tracking crossing midnight (e.g. 23:15 to 07:00).
  - Quality score (1-5 stars) and sleep trend charts.

- **Analytics & CSV Reports**:
  - Multi-range filters (Last 7 Days, Last 30 Days, Last 90 Days, All Time).
  - 4 JFreeChart visual trend graphs.
  - **Export to CSV**: Download workout and health reports directly to CSV files.

- **Gamification & Badges**:
  - 10 unlockable achievement badges (e.g., *First Step*, *3-Day Streak*, *Hydration Hero*, *Goal Getter*).
  - Visual status (Golden Unlocked vs. Locked).

---

## 3. Architecture and Design

The codebase implements a modular **MVC (Model-View-Controller)** pattern with distinct layers:

```
src/main/java/com/personaltracker/
├── Main.java                     # Application entry point (runs on Swing EDT)
├── config/
│   ├── AppConfig.java            # Environment variables & runtime credentials
│   └── ThemeConfig.java          # Dark Navy design system, FlatLaf styling
├── model/                        # Encapsulated domain entities
│   ├── UserProfile.java
│   ├── UserSession.java
│   ├── Workout.java
│   ├── WorkoutExercise.java
│   ├── Goal.java
│   ├── BodyMeasurement.java
│   ├── NutritionEntry.java
│   ├── WaterIntake.java
│   ├── SleepRecord.java
│   ├── Reminder.java
│   └── Achievement.java
├── service/
│   ├── SupabaseClient.java       # HTTP REST client with JWT Bearer auth & RLS
│   └── SupabaseAuthService.java  # GoTrue Auth API integration
├── dao/                          # Data Access Objects
│   ├── ProfileDAO.java
│   ├── WorkoutDAO.java
│   ├── GoalDAO.java
│   ├── MeasurementDAO.java
│   ├── NutritionDAO.java
│   ├── WaterDAO.java
│   ├── SleepDAO.java
│   ├── ReminderDAO.java
│   └── AchievementDAO.java
├── util/
│   ├── HealthCalculator.java     # BMI, sleep duration, MET calories, streaks
│   ├── ValidationUtil.java       # Email, password, positive value checks
│   ├── DateUtil.java             # Thread-safe formatting & relative labels
│   ├── CsvExporter.java          # CSV data generation
│   └── IconFactory.java          # Scalable vector icon glyph rendering
├── components/                   # Custom Swing components
│   ├── ModernCard.java           # Rounded card panel (#1E293B)
│   ├── StatCard.java             # KPI card with badge & bold metric
│   ├── ModernButton.java         # Primary, Success, Danger, Ghost styles
│   ├── ModernTextField.java      # Rounded inputs with focus indicators
│   ├── ModernPasswordField.java
│   ├── ModernProgressBar.java    # Gradient-filled progress bar
│   ├── ModernTable.java          # Alternating dark rows, styled headers
│   ├── WorkoutTimerPanel.java    # Interactive stopwatch timer
│   └── ToastNotification.java    # Floating message overlay
└── view/                         # Complete user interface views
    ├── MainFrame.java            # Root container with Sidebar & CardLayout
    ├── SidebarPanel.java         # Fixed navigation sidebar
    ├── AuthDialog.java           # Login, Sign Up & Password Reset dialog
    ├── OnboardingDialog.java     # Interactive profile setup wizard for new users
    ├── DashboardPanel.java       # Overview dashboard with charts
    ├── WorkoutPanel.java         # Workouts list, filters, dialog
    ├── GoalPanel.java            # Fitness goals tracking
    ├── MeasurementPanel.java     # Body weight & BMI tracking
    ├── NutritionPanel.java       # Meals & macro tracker
    ├── WaterSleepPanel.java      # Hydration & sleep recovery
    ├── AnalyticsPanel.java       # JFreeChart trends & CSV export
    ├── ReminderBadgePanel.java   # Reminders & trophies
    └── SettingsPanel.java        # Profile editor & Supabase settings
```

---

## 4. Prerequisites

- **Java**: Java 17, 21, or later (Tested on Oracle JDK 27).
- **Maven**: Version 3.6+ or the included Maven Wrapper (`mvnw` / `mvnw.cmd`).
- **OS**: Windows, macOS, or Linux.
- **Supabase Account**: Free tier at [supabase.com](https://supabase.com) (or run in built-in offline demo mode).

---

## 5. Java and Maven Setup

Verify your Java installation:
```bash
java -version
```

If `JAVA_HOME` is not set on your machine:
- **Windows (PowerShell)**:
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Java\jdk-27"
  ```
- **macOS / Linux**:
  ```bash
  export JAVA_HOME=$(/usr/libexec/java_home -v 17)
  ```

---

## 6. Supabase Project & Database Setup

1. Create a free project at [supabase.com](https://supabase.com).
2. Go to the **SQL Editor** in your Supabase Dashboard.
3. Open the file `database/schema.sql` in this project.
4. Copy and paste the entire contents into the SQL Editor and click **Run**.

The script automatically sets up:
- The 11 tables (`profiles`, `workouts`, `workout_exercises`, `goals`, `body_measurements`, `nutrition_entries`, `water_intake`, `sleep_records`, `reminders`, `achievements`, `user_achievements`).
- Check constraints and foreign keys with cascading deletes.
- Performance indexes on `(user_id, date)`.
- **Row Level Security (RLS)** on every table, strictly limiting row access to `auth.uid() = user_id`.
- The `handle_new_user()` trigger for automated profile generation on signup.
- Pre-populated achievement milestone badges.

---

## 7. Environment Variable Configuration

Copy the template file:
```bash
cp .env.example .env
```

Open `.env` and fill in your Supabase project credentials:
```env
SUPABASE_URL=https://your-project-id.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...your-anon-key-here
```

> **Security Note:** The `SUPABASE_ANON_KEY` is designed for public client use and relies on PostgreSQL Row Level Security (RLS) to enforce authorization. Never provide your `service_role` key in client software!

---

## 8. Required Dependencies

All libraries are managed via `pom.xml`:
- **FlatLaf** (`3.5.4`): Modern dark navy Look and Feel with DPI scaling.
- **JFreeChart** (`1.5.4`): Anti-aliased interactive charts (Bars, Lines, Rings).
- **Jackson Databind & JavaTime** (`2.17.2`): JSON serialization for PostgREST & Auth.
- **Dotenv Java** (`3.0.0`): Loads `.env` file credentials.
- **PostgreSQL JDBC Driver** (`42.7.3`): PostgreSQL JDBC driver.
- **JUnit Jupiter** (`5.10.2`): Unit and parameter testing suite.

---

## 9. How to Run the Application

### Option A: Using Maven Wrapper (Recommended)
On Windows:
```powershell
.\mvnw.cmd compile exec:java -Dexec.mainClass="com.personaltracker.Main"
```
On macOS / Linux:
```bash
./mvnw compile exec:java -Dexec.mainClass="com.personaltracker.Main"
```

### Option B: Build and Run Executable JAR
```powershell
.\mvnw.cmd package -DskipTests
java -jar target/personal-gain-tracking-system-1.0.0.jar
```

### Option C: Using IDE (IntelliJ IDEA / Eclipse / VS Code)
1. Open the folder as a Maven project.
2. Run `com.personaltracker.Main`.

---

## 10. How to Run Tests

Execute the automated JUnit 5 test suite:
```powershell
.\mvnw.cmd test
```

Unit tests verify:
- Accurate BMI calculation across WHO categories.
- Goal completion transitions and progress percentages.
- Workout duration, volume loads (sets × reps × weight), and MET caloric burn.
- Daily macronutrient summations and calorie budgets.
- Water intake calculations.
- Sleep durations crossing midnight.
- Email, password, and input bounds validation.
- Gamified achievement unlock conditions.

---

## 11. Troubleshooting & Common Errors

| Issue | Cause | Solution |
|---|---|---|
| `JAVA_HOME not found` | Environment variable not configured | Set `$env:JAVA_HOME = "C:\Program Files\Java\jdk-XX"` in your terminal. |
| `Offline Demo Mode` indicator | `.env` missing or contains placeholders | Update `.env` with your Supabase URL & Anon Key, or configure it live in the **Profile & Settings** panel. |
| `Invalid login credentials` | User not registered in Supabase Auth | Click **Sign Up** in the dialog to create an account, or click **Continue in Demo Mode**. |
| `Email not confirmed` | Supabase project requires email confirmation | Confirm your email via the confirmation link or disable *Confirm email* under *Supabase Auth Settings -> Providers*. |
| `Network Timeout` | Firewall or offline connection | The system will gracefully maintain full functionality using the local in-memory store. |

---

## Author & Academic Integrity
Developed as a complete Object-Oriented Programming (OOP) and Software Engineering desktop application demonstrating separation of concerns, secure cloud backend integration, and rich Java Swing UI design.
