package com.personaltracker.view;

import com.personaltracker.components.ModernButton;
import com.personaltracker.components.ModernCard;
import com.personaltracker.components.ModernProgressBar;
import com.personaltracker.components.StatCard;
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
import java.util.function.Consumer;

/**
 * Main personalized Dashboard view with interactive metric cards, charts, and activity feeds.
 */
public class DashboardPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final JLabel lblGreeting = new JLabel("Welcome back!");
    private final JLabel lblDate = new JLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));

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
    private final JLabel lblWaterValue = new JLabel("0 / 2500 ml (0%)");
    private final JLabel lblSleepValue = new JLabel("No sleep record logged");

    // Recent activity & Reminders
    private final JPanel pnlRecentActivity = new JPanel();
    private final JPanel pnlReminders = new JPanel();

    // Navigation callbacks
    private Consumer<SidebarPanel.NavItem> navigationConsumer;

    public DashboardPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));

        // 1. Welcome Header
        contentPanel.add(createHeaderPanel());
        contentPanel.add(Box.createVerticalStrut(20));

        // 2. Metric Cards Grid
        contentPanel.add(createStatsGrid());
        contentPanel.add(Box.createVerticalStrut(20));

        // 3. Middle Section: Charts Grid (Weekly Workout Activity & Weight Progression)
        contentPanel.add(createChartsGrid());
        contentPanel.add(Box.createVerticalStrut(20));

        // 4. Bottom Section: Goals, Hydration/Sleep, & Reminders
        contentPanel.add(createBottomGrid());

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setNavigationConsumer(Consumer<SidebarPanel.NavItem> consumer) {
        this.navigationConsumer = consumer;
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        textPanel.setOpaque(false);

        lblGreeting.setFont(ThemeConfig.FONT_TITLE_LARGE);
        lblGreeting.setForeground(ThemeConfig.TEXT_PRIMARY);
        textPanel.add(lblGreeting);

        lblDate.setFont(ThemeConfig.FONT_BODY);
        lblDate.setForeground(ThemeConfig.TEXT_SECONDARY);
        textPanel.add(lblDate);

        header.add(textPanel, BorderLayout.WEST);

        // Quick action buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        ModernButton btnLogWorkout = new ModernButton("+ Log Workout", ModernButton.ButtonStyle.PRIMARY);
        btnLogWorkout.addActionListener(e -> {
            if (navigationConsumer != null) navigationConsumer.accept(SidebarPanel.NavItem.WORKOUTS);
        });

        ModernButton btnLogFood = new ModernButton("+ Add Meal", ModernButton.ButtonStyle.SECONDARY);
        btnLogFood.addActionListener(e -> {
            if (navigationConsumer != null) navigationConsumer.accept(SidebarPanel.NavItem.NUTRITION);
        });

        ModernButton btnQuickWater = new ModernButton("+ 250ml Water", ModernButton.ButtonStyle.SECONDARY);
        btnQuickWater.addActionListener(e -> quickAddWater(250));

        actions.add(btnLogWorkout);
        actions.add(btnLogFood);
        actions.add(btnQuickWater);

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
        JLabel lblChartTitle1 = new JLabel("Weekly Workout Duration (Mins)");
        lblChartTitle1.setFont(ThemeConfig.FONT_SUBTITLE);
        lblChartTitle1.setForeground(ThemeConfig.TEXT_PRIMARY);
        cardWorkoutChart.add(lblChartTitle1, BorderLayout.NORTH);
        pnlWorkoutChart.setOpaque(false);
        cardWorkoutChart.add(pnlWorkoutChart, BorderLayout.CENTER);
        grid.add(cardWorkoutChart);

        // Weight Trend Card
        ModernCard cardWeightChart = new ModernCard(14, new Insets(14, 16, 14, 16));
        JLabel lblChartTitle2 = new JLabel("Weight Progression Trend (kg)");
        lblChartTitle2.setFont(ThemeConfig.FONT_SUBTITLE);
        lblChartTitle2.setForeground(ThemeConfig.TEXT_PRIMARY);
        cardWeightChart.add(lblChartTitle2, BorderLayout.NORTH);
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
        JLabel lblGoalsTitle = new JLabel("Active Goals");
        lblGoalsTitle.setFont(ThemeConfig.FONT_SUBTITLE);
        lblGoalsTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
        cardGoals.add(lblGoalsTitle, BorderLayout.NORTH);

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
        JLabel lblDailyTitle = new JLabel("Today's Hydration & Sleep");
        lblDailyTitle.setFont(ThemeConfig.FONT_SUBTITLE);
        lblDailyTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
        cardDailyHealth.add(lblDailyTitle, BorderLayout.NORTH);

        JPanel pnlDailyCenter = new JPanel();
        pnlDailyCenter.setLayout(new BoxLayout(pnlDailyCenter, BoxLayout.Y_AXIS));
        pnlDailyCenter.setOpaque(false);
        pnlDailyCenter.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JLabel lblWaterHeading = new JLabel("Water Intake Target");
        lblWaterHeading.setFont(ThemeConfig.FONT_BODY_BOLD);
        lblWaterHeading.setForeground(ThemeConfig.TEXT_SECONDARY);
        pnlDailyCenter.add(lblWaterHeading);
        pnlDailyCenter.add(Box.createVerticalStrut(6));

        barWater.setColors(ThemeConfig.PRIMARY_BLUE, ThemeConfig.ACCENT_CYAN);
        pnlDailyCenter.add(barWater);
        pnlDailyCenter.add(Box.createVerticalStrut(4));

        lblWaterValue.setFont(ThemeConfig.FONT_SMALL);
        lblWaterValue.setForeground(ThemeConfig.TEXT_MUTED);
        pnlDailyCenter.add(lblWaterValue);

        pnlDailyCenter.add(Box.createVerticalStrut(18));

        JLabel lblSleepHeading = new JLabel("Last Night's Sleep");
        lblSleepHeading.setFont(ThemeConfig.FONT_BODY_BOLD);
        lblSleepHeading.setForeground(ThemeConfig.TEXT_SECONDARY);
        pnlDailyCenter.add(lblSleepHeading);
        pnlDailyCenter.add(Box.createVerticalStrut(6));

        lblSleepValue.setFont(ThemeConfig.FONT_BODY);
        lblSleepValue.setForeground(ThemeConfig.TEXT_PRIMARY);
        pnlDailyCenter.add(lblSleepValue);

        cardDailyHealth.add(pnlDailyCenter, BorderLayout.CENTER);
        grid.add(cardDailyHealth);

        // 3. Reminders & Recent Activity
        ModernCard cardReminders = new ModernCard(14, new Insets(16, 18, 16, 18));
        JLabel lblRemindersTitle = new JLabel("Upcoming Reminders");
        lblRemindersTitle.setFont(ThemeConfig.FONT_SUBTITLE);
        lblRemindersTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
        cardReminders.add(lblRemindersTitle, BorderLayout.NORTH);

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

    public void refreshDashboardData() {
        UserSession session = SupabaseClient.getInstance().getCurrentSession();
        if (session != null && session.getProfile() != null) {
            lblGreeting.setText("Welcome back, " + session.getProfile().getDisplayName() + "! 💪");
        }

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
        cardStreak.setSubtitle(streak >= 3 ? "🔥 You're on fire!" : "Log daily workouts to build a streak");

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
                pnlGoalsList.add(createEmptyLabel("All goals completed! Great work! 🎉"));
            }
        } else {
            pnlGoalsList.add(createEmptyLabel("No goals yet. Set your first fitness target!"));
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
        lblWaterValue.setText(currentWaterMl + " / " + waterTarget + " ml (" + waterPct + "%)");

        // 9. Sleep
        if (sleep != null && !sleep.isEmpty()) {
            SleepRecord latestSleep = sleep.get(0);
            lblSleepValue.setText(String.format("😴 %.1f hours (Quality: %d/5)",
                    latestSleep.getDurationHours(), latestSleep.getQualityScore() != null ? latestSleep.getQualityScore() : 3));
        } else {
            lblSleepValue.setText("No sleep record logged yet");
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
                pnlReminders.add(createEmptyLabel("No upcoming reminders"));
            }
        } else {
            pnlReminders.add(createEmptyLabel("No upcoming reminders"));
        }
        pnlReminders.revalidate();
        pnlReminders.repaint();
    }

    private JPanel createGoalRow(Goal g) {
        JPanel row = new JPanel(new BorderLayout(8, 4));
        row.setOpaque(false);

        JLabel lblTitle = new JLabel(g.getTitle());
        lblTitle.setFont(ThemeConfig.FONT_BODY_BOLD);
        lblTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
        row.add(lblTitle, BorderLayout.WEST);

        JLabel lblPct = new JLabel(g.getProgressPercentage() + "%");
        lblPct.setFont(ThemeConfig.FONT_SMALL);
        lblPct.setForeground(ThemeConfig.PRIMARY_BLUE);
        row.add(lblPct, BorderLayout.EAST);

        ModernProgressBar bar = new ModernProgressBar(0, 100);
        bar.setValue(g.getProgressPercentage());
        row.add(bar, BorderLayout.SOUTH);
        return row;
    }

    private JPanel createReminderRow(Reminder r) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);

        JLabel icon = new JLabel(IconFactory.createIcon(IconFactory.IconType.REMINDERS, 14, ThemeConfig.PRIMARY_BLUE));
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
                refreshDashboardData();
            }
        };
        worker.execute();
    }
}
