package com.personaltracker.view;

import com.personaltracker.components.ModernButton;
import com.personaltracker.components.ModernCard;
import com.personaltracker.components.ModernProgressBar;
import com.personaltracker.components.StatCard;
import com.personaltracker.components.ToastNotification;
import com.personaltracker.config.ThemeConfig;
import com.personaltracker.dao.*;
import com.personaltracker.model.*;
import com.personaltracker.service.SupabaseClient;
import com.personaltracker.util.DateUtil;
import com.personaltracker.util.HealthCalculator;
import com.personaltracker.util.IconFactory;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Main personalized Dashboard view with interactive metric cards, charts, and activity feeds.
 */
public class DashboardPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private static final String[] MOTIVATIONAL_FOCUS_LINES = {
        "Small daily improvements over time lead to stunning results.",
        "Consistency beats intensity every single time.",
        "Every rep counts. Make today's session progress toward your goals.",
        "Discipline is choosing between what you want now and what you want most.",
        "Your only limit is you. Let's make today count.",
        "Stay patient, stay focused, and trust the process."
    };

    private final JLabel lblGreeting = new JLabel("Welcome back!");
    private final JLabel lblDate = new JLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
    private final JLabel lblFocusQuote = new JLabel();

    // Top Stat Cards
    private final StatCard cardWorkouts = new StatCard("TOTAL WORKOUTS", "0", "0 mins this week", IconFactory.IconType.WORKOUT, ThemeConfig.PRIMARY_ACCENT);
    private final StatCard cardWeight = new StatCard("CURRENT WEIGHT", "-- kg", "BMI: --", IconFactory.IconType.MEASUREMENT, ThemeConfig.SUCCESS);
    private final StatCard cardCalories = new StatCard("TODAY'S CALORIES", "0 kcal", "Target: 2,000 kcal", IconFactory.IconType.NUTRITION, ThemeConfig.WARNING);
    private final StatCard cardStreak = new StatCard("WORKOUT STREAK", "0 Days", "Consistency is key", IconFactory.IconType.FIRE, ThemeConfig.PRIMARY_ACCENT);

    // Dynamic Chart Panels
    private final JPanel pnlWorkoutChart = new JPanel(new BorderLayout());
    private final JPanel pnlWeightChart = new JPanel(new BorderLayout());

    // Goals container
    private final JPanel pnlGoalsList = new JPanel();

    // Hydration & Sleep summary
    private final ModernProgressBar barWater = new ModernProgressBar(0, 2500);
    private final JLabel lblWaterValue = new JLabel("0 / 2500 ml");
    private final JLabel lblWaterPctBadge = new JLabel("0%");
    private final JLabel lblWaterSub = new JLabel("2,500 ml remaining");
    private final JLabel lblSleepDurationVal = new JLabel("No sleep logged");
    private final JLabel lblSleepSchedule = new JLabel("Bedtime: -- | Wake: --");
    private final JLabel lblSleepQualityScore = new JLabel("");

    // Recent activity & Reminders
    private final JPanel pnlRecentActivity = new JPanel();
    private final JPanel pnlReminders = new JPanel();

    // Navigation callbacks
    private Consumer<SidebarPanel.NavItem> navigationConsumer;
    private Runnable onOpenRemindersCallback;

    public void setOnOpenRemindersCallback(Runnable callback) {
        this.onOpenRemindersCallback = callback;
    }

    private void openRemindersView() {
        if (onOpenRemindersCallback != null) {
            onOpenRemindersCallback.run();
        } else if (navigationConsumer != null) {
            navigationConsumer.accept(SidebarPanel.NavItem.WORKOUTS);
        }
    }

    public DashboardPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(22, 28, 28, 28));

        // 1. Welcome Header with Dynamic Greeting & Focus Line
        contentPanel.add(createHeaderPanel());
        contentPanel.add(Box.createVerticalStrut(18));

        // 2. Interactive Metric Cards Grid
        contentPanel.add(createStatsGrid());
        contentPanel.add(Box.createVerticalStrut(18));

        // 3. Middle Section: Charts Grid (Weekly Workout Activity & Weight Progression)
        contentPanel.add(createChartsGrid());
        contentPanel.add(Box.createVerticalStrut(18));

        // 4. Bottom Section: Goals, Hydration/Sleep, & Reminders
        contentPanel.add(createBottomGrid());

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Hook up stat card click-throughs for effortless navigation
        setupStatCardClickThroughs();
        updateHeaderGreeting();
    }

    public void setNavigationConsumer(Consumer<SidebarPanel.NavItem> consumer) {
        this.navigationConsumer = consumer;
    }

    private void setupStatCardClickThroughs() {
        cardWorkouts.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cardWorkouts.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (navigationConsumer != null) navigationConsumer.accept(SidebarPanel.NavItem.WORKOUTS);
            }
        });

        cardWeight.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cardWeight.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (navigationConsumer != null) navigationConsumer.accept(SidebarPanel.NavItem.MEASUREMENTS);
            }
        });

        cardCalories.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cardCalories.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (navigationConsumer != null) navigationConsumer.accept(SidebarPanel.NavItem.NUTRITION);
            }
        });

        cardStreak.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cardStreak.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (navigationConsumer != null) navigationConsumer.accept(SidebarPanel.NavItem.WORKOUTS);
            }
        });
    }

    private void updateHeaderGreeting() {
        UserSession sess = SupabaseClient.getInstance().getCurrentSession();
        String name = "Athlete";
        boolean isTrainer = false;
        if (sess != null && sess.getProfile() != null) {
            name = sess.getProfile().getDisplayName();
            isTrainer = sess.getProfile().isTrainer();
        }

        int hour = java.time.LocalTime.now().getHour();
        String timeGreeting = (hour < 12) ? "Good morning" : (hour < 17) ? "Good afternoon" : "Good evening";
        String rolePrefix = isTrainer ? "Coach " : "";
        lblGreeting.setText(timeGreeting + ", " + rolePrefix + name);

        int quoteIndex = Math.abs(LocalDate.now().getDayOfYear()) % MOTIVATIONAL_FOCUS_LINES.length;
        lblFocusQuote.setText("“" + MOTIVATIONAL_FOCUS_LINES[quoteIndex] + "”");
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        lblGreeting.setFont(ThemeConfig.FONT_TITLE_LARGE);
        lblGreeting.setForeground(ThemeConfig.TEXT_PRIMARY);
        textPanel.add(lblGreeting);
        textPanel.add(Box.createVerticalStrut(3));

        lblDate.setFont(ThemeConfig.FONT_BODY);
        lblDate.setForeground(ThemeConfig.TEXT_SECONDARY);
        textPanel.add(lblDate);
        textPanel.add(Box.createVerticalStrut(4));

        lblFocusQuote.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblFocusQuote.setForeground(new Color(0xA8, 0xA8, 0xA8));
        textPanel.add(lblFocusQuote);

        header.add(textPanel, BorderLayout.WEST);

        // Minimalist Quick Action Pills Bar
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actions.setOpaque(false);

        ModernButton btnLogWorkout = new ModernButton("+ Workout", ModernButton.ButtonStyle.PRIMARY);
        btnLogWorkout.addActionListener(e -> {
            if (navigationConsumer != null) navigationConsumer.accept(SidebarPanel.NavItem.WORKOUTS);
        });

        ModernButton btnLogFood = new ModernButton("+ Meal", ModernButton.ButtonStyle.SECONDARY);
        btnLogFood.addActionListener(e -> {
            if (navigationConsumer != null) navigationConsumer.accept(SidebarPanel.NavItem.NUTRITION);
        });

        ModernButton btnQuickWater = new ModernButton("+250ml Water", ModernButton.ButtonStyle.SECONDARY);
        btnQuickWater.setIcon(IconFactory.createIcon(IconFactory.IconType.WATER, 13, ThemeConfig.PRIMARY_ACCENT));
        btnQuickWater.setIconTextGap(6);
        btnQuickWater.setHorizontalTextPosition(SwingConstants.RIGHT);
        btnQuickWater.addActionListener(e -> quickAddWater(250));

        ModernButton btnQuickGoal = new ModernButton("+ Goal", ModernButton.ButtonStyle.GHOST);
        btnQuickGoal.addActionListener(e -> {
            if (navigationConsumer != null) navigationConsumer.accept(SidebarPanel.NavItem.GOALS);
        });

        actions.add(btnLogWorkout);
        actions.add(btnLogFood);
        actions.add(btnQuickWater);
        actions.add(btnQuickGoal);

        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JPanel createStatsGrid() {
        JPanel grid = new JPanel(new GridLayout(1, 4, 16, 0));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        grid.add(cardWorkouts);
        grid.add(cardWeight);
        grid.add(cardCalories);
        grid.add(cardStreak);
        return grid;
    }

    private JPanel createChartsGrid() {
        JPanel grid = new JPanel(new GridLayout(1, 2, 16, 0));
        grid.setOpaque(false);
        grid.setPreferredSize(new Dimension(0, 260));
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        // Workout Activity Card
        ModernCard cardWorkoutChart = new ModernCard(14, new Insets(14, 16, 14, 16));
        JPanel chartHeader1 = new JPanel(new BorderLayout());
        chartHeader1.setOpaque(false);
        JLabel lblChartTitle1 = new JLabel("Weekly Workout Duration (Mins)");
        lblChartTitle1.setFont(ThemeConfig.FONT_SUBTITLE);
        lblChartTitle1.setForeground(ThemeConfig.TEXT_PRIMARY);
        chartHeader1.add(lblChartTitle1, BorderLayout.WEST);

        cardWorkoutChart.add(chartHeader1, BorderLayout.NORTH);
        pnlWorkoutChart.setOpaque(false);
        cardWorkoutChart.add(pnlWorkoutChart, BorderLayout.CENTER);
        grid.add(cardWorkoutChart);

        // Weight Trend Card
        ModernCard cardWeightChart = new ModernCard(14, new Insets(14, 16, 14, 16));
        JPanel chartHeader2 = new JPanel(new BorderLayout());
        chartHeader2.setOpaque(false);
        JLabel lblChartTitle2 = new JLabel("Weight Progression Trend (kg)");
        lblChartTitle2.setFont(ThemeConfig.FONT_SUBTITLE);
        lblChartTitle2.setForeground(ThemeConfig.TEXT_PRIMARY);
        chartHeader2.add(lblChartTitle2, BorderLayout.WEST);

        cardWeightChart.add(chartHeader2, BorderLayout.NORTH);
        pnlWeightChart.setOpaque(false);
        cardWeightChart.add(pnlWeightChart, BorderLayout.CENTER);
        grid.add(cardWeightChart);

        return grid;
    }

    private JPanel createBottomGrid() {
        JPanel grid = new JPanel(new GridLayout(1, 3, 16, 0));
        grid.setOpaque(false);
        grid.setPreferredSize(new Dimension(0, 280));
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));

        // 1. Goals card
        ModernCard cardGoals = new ModernCard(14, new Insets(16, 18, 16, 18));
        cardGoals.setHoverEffect(true);
        JPanel goalsHeader = new JPanel(new BorderLayout());
        goalsHeader.setOpaque(false);

        JLabel lblGoalsTitle = new JLabel("Active Goals");
        lblGoalsTitle.setFont(ThemeConfig.FONT_SUBTITLE);
        lblGoalsTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
        lblGoalsTitle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblGoalsTitle.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (navigationConsumer != null) navigationConsumer.accept(SidebarPanel.NavItem.GOALS);
            }
        });
        goalsHeader.add(lblGoalsTitle, BorderLayout.WEST);

        JLabel lblViewAllGoals = new JLabel("View All →");
        lblViewAllGoals.setFont(ThemeConfig.FONT_SMALL);
        lblViewAllGoals.setForeground(ThemeConfig.PRIMARY_ACCENT);
        lblViewAllGoals.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblViewAllGoals.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (navigationConsumer != null) navigationConsumer.accept(SidebarPanel.NavItem.GOALS);
            }
        });
        goalsHeader.add(lblViewAllGoals, BorderLayout.EAST);
        cardGoals.add(goalsHeader, BorderLayout.NORTH);

        pnlGoalsList.setLayout(new BoxLayout(pnlGoalsList, BoxLayout.Y_AXIS));
        pnlGoalsList.setOpaque(false);
        JScrollPane scrollGoals = new JScrollPane(pnlGoalsList);
        scrollGoals.setOpaque(false);
        scrollGoals.getViewport().setOpaque(false);
        scrollGoals.setBorder(null);
        cardGoals.add(scrollGoals, BorderLayout.CENTER);
        grid.add(cardGoals);

        // 2. Hydration & Sleep Snapshot
        ModernCard cardDailyHealth = new ModernCard(14, new Insets(16, 18, 16, 18));
        cardDailyHealth.setHoverEffect(true);
        JPanel healthHeader = new JPanel(new BorderLayout());
        healthHeader.setOpaque(false);

        JPanel titleWithIcon = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleWithIcon.setOpaque(false);
        JLabel iconHealth = new JLabel(IconFactory.createIcon(IconFactory.IconType.WATER, 16, ThemeConfig.PRIMARY_ACCENT));
        titleWithIcon.add(iconHealth);

        JLabel lblDailyTitle = new JLabel("Today's Hydration & Sleep");
        lblDailyTitle.setFont(ThemeConfig.FONT_SUBTITLE);
        lblDailyTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
        lblDailyTitle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblDailyTitle.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (navigationConsumer != null) navigationConsumer.accept(SidebarPanel.NavItem.WATER_SLEEP);
            }
        });
        titleWithIcon.add(lblDailyTitle);
        healthHeader.add(titleWithIcon, BorderLayout.WEST);

        JLabel lblViewHealth = new JLabel("Details →");
        lblViewHealth.setFont(ThemeConfig.FONT_SMALL);
        lblViewHealth.setForeground(ThemeConfig.PRIMARY_ACCENT);
        lblViewHealth.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblViewHealth.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (navigationConsumer != null) navigationConsumer.accept(SidebarPanel.NavItem.WATER_SLEEP);
            }
        });
        healthHeader.add(lblViewHealth, BorderLayout.EAST);
        cardDailyHealth.add(healthHeader, BorderLayout.NORTH);

        JPanel pnlDailyCenter = new JPanel();
        pnlDailyCenter.setLayout(new BoxLayout(pnlDailyCenter, BoxLayout.Y_AXIS));
        pnlDailyCenter.setOpaque(false);
        pnlDailyCenter.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        // Pod 1: Hydration Pod
        ModernCard podWater = new ModernCard(10, new Insets(10, 12, 10, 12));
        podWater.setBackgroundColor(ThemeConfig.BG_INPUT);
        podWater.setMaximumSize(new Dimension(Integer.MAX_VALUE, 98));

        JPanel waterBox = new JPanel();
        waterBox.setLayout(new BoxLayout(waterBox, BoxLayout.Y_AXIS));
        waterBox.setOpaque(false);

        JPanel waterHeaderRow = new JPanel(new BorderLayout());
        waterHeaderRow.setOpaque(false);

        JPanel wTitleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        wTitleBox.setOpaque(false);
        wTitleBox.add(new JLabel(IconFactory.createIcon(IconFactory.IconType.WATER, 13, ThemeConfig.PRIMARY_ACCENT)));
        JLabel lblWaterHeading = new JLabel("Water Intake");
        lblWaterHeading.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblWaterHeading.setForeground(ThemeConfig.TEXT_PRIMARY);
        wTitleBox.add(lblWaterHeading);
        waterHeaderRow.add(wTitleBox, BorderLayout.WEST);

        JPanel wMetricsBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        wMetricsBox.setOpaque(false);
        lblWaterValue.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblWaterValue.setForeground(ThemeConfig.PRIMARY_ACCENT);
        wMetricsBox.add(lblWaterValue);

        lblWaterPctBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblWaterPctBadge.setForeground(ThemeConfig.TEXT_PRIMARY);
        lblWaterPctBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(1, 5, 1, 5)
        ));
        wMetricsBox.add(lblWaterPctBadge);
        waterHeaderRow.add(wMetricsBox, BorderLayout.EAST);
        waterBox.add(waterHeaderRow);
        waterBox.add(Box.createVerticalStrut(6));

        barWater.setColors(ThemeConfig.PRIMARY_ACCENT, ThemeConfig.ACCENT_HOVER);
        barWater.setPreferredSize(new Dimension(0, 8));
        barWater.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        waterBox.add(barWater);
        waterBox.add(Box.createVerticalStrut(8));

        JPanel waterBottomRow = new JPanel(new BorderLayout());
        waterBottomRow.setOpaque(false);

        lblWaterSub.setFont(ThemeConfig.FONT_SMALL);
        lblWaterSub.setForeground(ThemeConfig.TEXT_MUTED);
        waterBottomRow.add(lblWaterSub, BorderLayout.WEST);

        JPanel quickWaterPills = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        quickWaterPills.setOpaque(false);
        quickWaterPills.add(createMiniWaterPill("+250ml", 250));
        quickWaterPills.add(createMiniWaterPill("+500ml", 500));
        waterBottomRow.add(quickWaterPills, BorderLayout.EAST);
        waterBox.add(waterBottomRow);

        podWater.add(waterBox, BorderLayout.CENTER);
        pnlDailyCenter.add(podWater);
        pnlDailyCenter.add(Box.createVerticalStrut(10));

        // Pod 2: Sleep & Recovery Pod
        ModernCard podSleep = new ModernCard(10, new Insets(10, 12, 10, 12));
        podSleep.setBackgroundColor(ThemeConfig.BG_INPUT);
        podSleep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        JPanel sleepBox = new JPanel();
        sleepBox.setLayout(new BoxLayout(sleepBox, BoxLayout.Y_AXIS));
        sleepBox.setOpaque(false);

        JPanel sleepHeaderRow = new JPanel(new BorderLayout());
        sleepHeaderRow.setOpaque(false);

        JPanel sTitleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        sTitleBox.setOpaque(false);
        sTitleBox.add(new JLabel(IconFactory.createIcon(IconFactory.IconType.SLEEP, 13, ThemeConfig.PRIMARY_ACCENT)));
        JLabel lblSleepHeading = new JLabel("Last Night's Sleep");
        lblSleepHeading.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSleepHeading.setForeground(ThemeConfig.TEXT_PRIMARY);
        sTitleBox.add(lblSleepHeading);
        sleepHeaderRow.add(sTitleBox, BorderLayout.WEST);

        lblSleepDurationVal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblSleepDurationVal.setForeground(ThemeConfig.PRIMARY_ACCENT);
        sleepHeaderRow.add(lblSleepDurationVal, BorderLayout.EAST);
        sleepBox.add(sleepHeaderRow);
        sleepBox.add(Box.createVerticalStrut(6));

        JPanel sleepDetailsRow = new JPanel(new BorderLayout());
        sleepDetailsRow.setOpaque(false);

        lblSleepSchedule.setFont(ThemeConfig.FONT_SMALL);
        lblSleepSchedule.setForeground(ThemeConfig.TEXT_MUTED);
        sleepDetailsRow.add(lblSleepSchedule, BorderLayout.WEST);

        lblSleepQualityScore.setFont(ThemeConfig.FONT_SMALL);
        lblSleepQualityScore.setForeground(ThemeConfig.WARNING);
        sleepDetailsRow.add(lblSleepQualityScore, BorderLayout.EAST);
        sleepBox.add(sleepDetailsRow);

        podSleep.add(sleepBox, BorderLayout.CENTER);
        pnlDailyCenter.add(podSleep);

        cardDailyHealth.add(pnlDailyCenter, BorderLayout.CENTER);
        grid.add(cardDailyHealth);

        // 3. Reminders & Recent Activity
        ModernCard cardReminders = new ModernCard(14, new Insets(16, 18, 16, 18));
        cardReminders.setHoverEffect(true);
        JPanel remindersHeader = new JPanel(new BorderLayout());
        remindersHeader.setOpaque(false);

        JLabel lblRemindersTitle = new JLabel("Upcoming Reminders");
        lblRemindersTitle.setFont(ThemeConfig.FONT_SUBTITLE);
        lblRemindersTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
        lblRemindersTitle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblRemindersTitle.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                openRemindersView();
            }
        });
        remindersHeader.add(lblRemindersTitle, BorderLayout.WEST);

        JLabel lblViewReminders = new JLabel("Manage →");
        lblViewReminders.setFont(ThemeConfig.FONT_SMALL);
        lblViewReminders.setForeground(ThemeConfig.PRIMARY_ACCENT);
        lblViewReminders.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblViewReminders.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                openRemindersView();
            }
        });
        remindersHeader.add(lblViewReminders, BorderLayout.EAST);
        cardReminders.add(remindersHeader, BorderLayout.NORTH);

        pnlReminders.setLayout(new BoxLayout(pnlReminders, BoxLayout.Y_AXIS));
        pnlReminders.setOpaque(false);
        JScrollPane scrollReminders = new JScrollPane(pnlReminders);
        scrollReminders.setOpaque(false);
        scrollReminders.getViewport().setOpaque(false);
        scrollReminders.setBorder(null);
        cardReminders.add(scrollReminders, BorderLayout.CENTER);
        grid.add(cardReminders);

        return grid;
    }

    private JButton createMiniWaterPill(String text, int amount) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btn.setForeground(ThemeConfig.TEXT_SECONDARY);
        btn.setBackground(ThemeConfig.BG_DARK);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> quickAddWater(amount));
        return btn;
    }

    public void refreshDashboardData() {
        updateHeaderGreeting();

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private List<Workout> workouts;
            private List<Goal> goals;
            private List<BodyMeasurement> measurements;
            private List<NutritionEntry> nutritionEntries;
            private List<WaterIntake> waterIntakes;
            private List<SleepRecord> sleepRecords;
            private List<Reminder> reminders;

            @Override
            protected Void doInBackground() throws Exception {
                WorkoutDAO workoutDAO = new WorkoutDAO();
                GoalDAO goalDAO = new GoalDAO();
                MeasurementDAO measurementDAO = new MeasurementDAO();
                NutritionDAO nutritionDAO = new NutritionDAO();
                WaterDAO waterDAO = new WaterDAO();
                SleepDAO sleepDAO = new SleepDAO();
                ReminderDAO reminderDAO = new ReminderDAO();

                workouts = workoutDAO.getAllWorkouts();
                goals = goalDAO.getAllGoals();
                measurements = measurementDAO.getAllMeasurements();
                nutritionEntries = nutritionDAO.getEntriesForDate(LocalDate.now());
                waterIntakes = waterDAO.getEntriesForDate(LocalDate.now());
                sleepRecords = sleepDAO.getAllRecords();
                reminders = reminderDAO.getAllReminders();

                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    updateMetrics(workouts, goals, measurements, nutritionEntries, waterIntakes, sleepRecords, reminders);
                } catch (Exception e) {
                    System.err.println("Dashboard refresh error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void updateMetrics(List<Workout> workouts, List<Goal> goals, List<BodyMeasurement> measurements,
                               List<NutritionEntry> nutrition, List<WaterIntake> water, List<SleepRecord> sleep,
                               List<Reminder> reminders) {
        // 1. Workouts Stat
        int totalWorkouts = workouts != null ? workouts.size() : 0;
        int weeklyMinutes = 0;
        LocalDate weekAgo = LocalDate.now().minusDays(7);
        if (workouts != null) {
            for (Workout w : workouts) {
                if (w.getWorkoutDate() != null && !w.getWorkoutDate().isBefore(weekAgo)) {
                    weeklyMinutes += w.getDurationMinutes();
                }
            }
        }
        cardWorkouts.setValue(String.valueOf(totalWorkouts));
        cardWorkouts.setSubtitle(weeklyMinutes + " mins this week");

        // 2. Weight & BMI Stat
        if (measurements != null && !measurements.isEmpty()) {
            BodyMeasurement latest = measurements.get(0);
            cardWeight.setValue(String.format("%.1f kg", latest.getWeightKg()));
            double bmi = latest.getBmi() != null ? latest.getBmi() : HealthCalculator.calculateBmi(latest.getWeightKg(), latest.getHeightCm() != null ? latest.getHeightCm() : 175.0);
            cardWeight.setSubtitle("BMI: " + String.format("%.1f", bmi) + " (" + HealthCalculator.getBmiCategory(bmi) + ")");
        } else {
            cardWeight.setValue("-- kg");
            cardWeight.setSubtitle("No measurements recorded");
        }

        // 3. Nutrition Calories Stat
        int totalCal = 0;
        if (nutrition != null) {
            for (NutritionEntry n : nutrition) {
                totalCal += n.getCalories();
            }
        }
        int calTarget = 2000;
        UserSession sess = SupabaseClient.getInstance().getCurrentSession();
        if (sess != null && sess.getProfile() != null && sess.getProfile().getDailyCalorieTarget() > 0) {
            calTarget = sess.getProfile().getDailyCalorieTarget();
        }
        cardCalories.setValue(totalCal + " kcal");
        cardCalories.setSubtitle("Target: " + calTarget + " kcal (" + (int) Math.round(((double) totalCal / calTarget) * 100) + "%)");

        // 4. Streak
        int streak = HealthCalculator.calculateWorkoutStreak(workouts);
        cardStreak.setValue(streak + (streak == 1 ? " Day" : " Days"));
        cardStreak.setSubtitle(streak >= 3 ? "Active daily workout streak!" : "Log daily workouts to build a streak");

        // 5. Weekly Workout Chart
        updateWorkoutChart(workouts);

        // 6. Weight Progression Chart
        updateWeightChart(measurements);

        // 7. Active Goals List
        pnlGoalsList.removeAll();
        if (goals != null && !goals.isEmpty()) {
            int shown = 0;
            for (Goal g : goals) {
                if (!g.isCompleted() && shown < 3) {
                    pnlGoalsList.add(createGoalRow(g));
                    pnlGoalsList.add(Box.createVerticalStrut(10));
                    shown++;
                }
            }
            if (shown == 0) {
                pnlGoalsList.add(createEmptyActionRow("🎉 All goals completed!", "+ Set Goal", SidebarPanel.NavItem.GOALS));
            }
        } else {
            pnlGoalsList.add(createEmptyActionRow("No active goals yet.", "+ Create Goal", SidebarPanel.NavItem.GOALS));
        }
        pnlGoalsList.revalidate();
        pnlGoalsList.repaint();

        // 8. Water Intake
        int currentWaterMl = 0;
        if (water != null) {
            for (WaterIntake wi : water) {
                currentWaterMl += wi.getAmountMl();
            }
        }
        int waterTarget = 2500;
        if (sess != null && sess.getProfile() != null && sess.getProfile().getDailyWaterTargetMl() > 0) {
            waterTarget = sess.getProfile().getDailyWaterTargetMl();
        }
        barWater.setMaximum(waterTarget);
        barWater.setValue(currentWaterMl);
        int waterPct = (int) Math.round(((double) currentWaterMl / waterTarget) * 100);
        int remaining = Math.max(0, waterTarget - currentWaterMl);

        lblWaterValue.setText(String.format("%,d / %,d ml", currentWaterMl, waterTarget));
        lblWaterPctBadge.setText(waterPct + "%");
        lblWaterSub.setText(remaining > 0 ? String.format("%,d ml to goal", remaining) : "Daily goal met! 🎉");

        // 9. Sleep
        if (sleep != null && !sleep.isEmpty()) {
            SleepRecord latestSleep = sleep.get(0);
            lblSleepDurationVal.setText(String.format(Locale.US, "%.1f hrs", latestSleep.getDurationHours()));
            if (latestSleep.getBedtime() != null && latestSleep.getWakeTime() != null) {
                lblSleepSchedule.setText(DateUtil.formatTime(latestSleep.getBedtime()) + " → " + DateUtil.formatTime(latestSleep.getWakeTime()));
            } else {
                lblSleepSchedule.setText(latestSleep.getSleepDate() != null ? DateUtil.formatDate(latestSleep.getSleepDate()) : "Recorded");
            }
            int score = latestSleep.getQualityScore() != null ? latestSleep.getQualityScore() : 3;
            lblSleepQualityScore.setIcon(IconFactory.createIcon(IconFactory.IconType.STAR, 11, ThemeConfig.WARNING));
            lblSleepQualityScore.setText(score + "/5");
            lblSleepQualityScore.setVisible(true);
        } else {
            lblSleepDurationVal.setText("No sleep logged");
            lblSleepSchedule.setText("Tap Details → to log");
            lblSleepQualityScore.setIcon(null);
            lblSleepQualityScore.setText("");
            lblSleepQualityScore.setVisible(false);
        }

        // 10. Reminders
        pnlReminders.removeAll();
        if (reminders != null && !reminders.isEmpty()) {
            int count = 0;
            for (Reminder r : reminders) {
                if (!r.isCompleted() && count < 3) {
                    pnlReminders.add(createReminderRow(r));
                    pnlReminders.add(Box.createVerticalStrut(8));
                    count++;
                }
            }
            if (count == 0) {
                pnlReminders.add(createEmptyActionRow("✓ You're all caught up!", "+ Add Reminder", e -> openRemindersView()));
            }
        } else {
            pnlReminders.add(createEmptyActionRow("No upcoming reminders.", "+ Add Reminder", e -> openRemindersView()));
        }
        pnlReminders.revalidate();
        pnlReminders.repaint();
    }

    private JPanel createGoalRow(Goal g) {
        JPanel row = new JPanel(new BorderLayout(8, 4));
        row.setOpaque(false);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (navigationConsumer != null) navigationConsumer.accept(SidebarPanel.NavItem.GOALS);
            }
        });

        JLabel lblTitle = new JLabel(g.getTitle());
        lblTitle.setFont(ThemeConfig.FONT_BODY_BOLD);
        lblTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
        row.add(lblTitle, BorderLayout.WEST);

        JLabel lblPct = new JLabel(g.getProgressPercentage() + "%");
        lblPct.setFont(ThemeConfig.FONT_SMALL);
        lblPct.setForeground(g.isCompleted() ? ThemeConfig.SUCCESS : ThemeConfig.PRIMARY_ACCENT);
        row.add(lblPct, BorderLayout.EAST);

        ModernProgressBar bar = new ModernProgressBar(0, 100);
        bar.setValue(g.getProgressPercentage());
        if (g.isCompleted()) {
            bar.setProgressColor(ThemeConfig.SUCCESS);
        }
        row.add(bar, BorderLayout.SOUTH);
        return row;
    }

    private JPanel createReminderRow(Reminder r) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                openRemindersView();
            }
        });

        JLabel icon = new JLabel(IconFactory.createIcon(IconFactory.IconType.REMINDERS, 14, ThemeConfig.PRIMARY_ACCENT));
        row.add(icon, BorderLayout.WEST);

        JLabel title = new JLabel(r.getTitle());
        title.setFont(ThemeConfig.FONT_BODY);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        row.add(title, BorderLayout.CENTER);

        JLabel time = new JLabel(DateUtil.formatTime(r.getReminderTime()));
        time.setFont(ThemeConfig.FONT_SMALL);
        time.setForeground(ThemeConfig.TEXT_MUTED);
        row.add(time, BorderLayout.EAST);
        return row;
    }

    private JPanel createEmptyActionRow(String message, String actionLabel, SidebarPanel.NavItem targetNav) {
        return createEmptyActionRow(message, actionLabel, e -> {
            if (navigationConsumer != null) navigationConsumer.accept(targetNav);
        });
    }

    private JPanel createEmptyActionRow(String message, String actionLabel, java.awt.event.ActionListener action) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setOpaque(false);
        JLabel lbl = new JLabel(message);
        lbl.setFont(ThemeConfig.FONT_SMALL);
        lbl.setForeground(ThemeConfig.TEXT_MUTED);
        p.add(lbl);

        ModernButton btn = new ModernButton(actionLabel, ModernButton.ButtonStyle.GHOST);
        btn.setFont(ThemeConfig.FONT_SMALL);
        btn.addActionListener(action);
        p.add(btn);
        return p;
    }

    private JLabel createEmptyLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(ThemeConfig.FONT_SMALL);
        lbl.setForeground(ThemeConfig.TEXT_MUTED);
        return lbl;
    }

    private void updateWorkoutChart(List<Workout> workouts) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        LocalDate today = LocalDate.now();
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEE");

        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            int mins = 0;
            if (workouts != null) {
                for (Workout w : workouts) {
                    if (w.getWorkoutDate() != null && w.getWorkoutDate().isEqual(d)) {
                        mins += w.getDurationMinutes();
                    }
                }
            }
            dataset.addValue(mins, "Duration", d.format(dayFmt));
        }

        JFreeChart chart = ChartFactory.createBarChart(
                null, null, null, dataset, PlotOrientation.VERTICAL, false, false, false);

        applyDarkChartTheme(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, ThemeConfig.PRIMARY_ACCENT);
        renderer.setMaximumBarWidth(0.12);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setOpaque(false);
        chartPanel.setBackground(new Color(0, 0, 0, 0));

        pnlWorkoutChart.removeAll();
        pnlWorkoutChart.add(chartPanel, BorderLayout.CENTER);
        pnlWorkoutChart.revalidate();
        pnlWorkoutChart.repaint();
    }

    private void updateWeightChart(List<BodyMeasurement> measurements) {
        TimeSeries series = new TimeSeries("Weight");
        if (measurements != null) {
            for (int i = measurements.size() - 1; i >= 0; i--) {
                BodyMeasurement m = measurements.get(i);
                if (m.getRecordedDate() != null) {
                    LocalDate ld = m.getRecordedDate();
                    series.addOrUpdate(new Day(ld.getDayOfMonth(), ld.getMonthValue(), ld.getYear()), m.getWeightKg());
                }
            }
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection(series);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                null, null, null, dataset, false, false, false);

        applyDarkChartTheme(chart);

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);
        renderer.setSeriesPaint(0, ThemeConfig.SUCCESS);
        renderer.setSeriesStroke(0, new BasicStroke(2.2f));
        plot.setRenderer(renderer);

        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new java.text.SimpleDateFormat("dd MMM"));

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setOpaque(false);
        chartPanel.setBackground(new Color(0, 0, 0, 0));

        pnlWeightChart.removeAll();
        pnlWeightChart.add(chartPanel, BorderLayout.CENTER);
        pnlWeightChart.revalidate();
        pnlWeightChart.repaint();
    }

    public static void applyDarkChartTheme(JFreeChart chart) {
        chart.setBackgroundPaint(new Color(0, 0, 0, 0));
        chart.setAntiAlias(true);

        if (chart.getPlot() instanceof CategoryPlot) {
            CategoryPlot plot = chart.getCategoryPlot();
            plot.setBackgroundPaint(new Color(0x1C, 0x1C, 0x1C, 120));
            plot.setOutlinePaint(null);
            plot.setRangeGridlinePaint(new Color(0x35, 0x35, 0x35, 120));
            plot.setDomainGridlinePaint(new Color(0x35, 0x35, 0x35, 60));

            CategoryAxis domain = plot.getDomainAxis();
            domain.setTickLabelPaint(ThemeConfig.TEXT_SECONDARY);
            domain.setTickLabelFont(ThemeConfig.FONT_SMALL);

            NumberAxis range = (NumberAxis) plot.getRangeAxis();
            range.setTickLabelPaint(ThemeConfig.TEXT_SECONDARY);
            range.setTickLabelFont(ThemeConfig.FONT_SMALL);
        } else if (chart.getPlot() instanceof XYPlot) {
            XYPlot plot = chart.getXYPlot();
            plot.setBackgroundPaint(new Color(0x1C, 0x1C, 0x1C, 120));
            plot.setOutlinePaint(null);
            plot.setRangeGridlinePaint(new Color(0x35, 0x35, 0x35, 120));
            plot.setDomainGridlinePaint(new Color(0x35, 0x35, 0x35, 60));

            plot.getDomainAxis().setTickLabelPaint(ThemeConfig.TEXT_SECONDARY);
            plot.getDomainAxis().setTickLabelFont(ThemeConfig.FONT_SMALL);
            plot.getRangeAxis().setTickLabelPaint(ThemeConfig.TEXT_SECONDARY);
            plot.getRangeAxis().setTickLabelFont(ThemeConfig.FONT_SMALL);
        }
    }

    private void quickAddWater(int ml) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                new WaterDAO().logWater(ml, LocalDate.now());
                return null;
            }

            @Override
            protected void done() {
                ToastNotification.showToast(DashboardPanel.this, "Logged +" + ml + "ml water intake", ToastNotification.ToastType.SUCCESS);
                refreshDashboardData();
            }
        };
        worker.execute();
    }
}
