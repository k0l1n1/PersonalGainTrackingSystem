package com.personaltracker.view;

import com.personaltracker.components.ModernButton;
import com.personaltracker.components.ModernCard;
import com.personaltracker.components.StatCard;
import com.personaltracker.config.ThemeConfig;
import com.personaltracker.dao.*;
import com.personaltracker.model.*;
import com.personaltracker.util.CsvExporter;
import com.personaltracker.util.DateUtil;
import com.personaltracker.util.IconFactory;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Progress Analytics and CSV Reporting view with multiple JFreeChart visualizations.
 */
public class AnalyticsPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final WorkoutDAO workoutDAO = new WorkoutDAO();
    private final MeasurementDAO measurementDAO = new MeasurementDAO();
    private final NutritionDAO nutritionDAO = new NutritionDAO();
    private final WaterDAO waterDAO = new WaterDAO();
    private final SleepDAO sleepDAO = new SleepDAO();

    private final JComboBox<String> cmbDateRange = new JComboBox<>(new String[]{"Last 7 Days", "Last 30 Days", "Last 90 Days", "All Time"});

    // Stat cards
    private final StatCard cardWorkouts = new StatCard("WORKOUT SESSIONS", "0", "0 mins total", IconFactory.IconType.WORKOUT, ThemeConfig.PRIMARY_ACCENT);
    private final StatCard cardCalories = new StatCard("CALORIES BURNED", "0 kcal", "Estimated total", IconFactory.IconType.FIRE, ThemeConfig.WARNING);
    private final StatCard cardAvgSleep = new StatCard("AVG SLEEP DURATION", "-- hrs", "Recovery index", IconFactory.IconType.SLEEP, ThemeConfig.PRIMARY_ACCENT);
    private final StatCard cardAvgWater = new StatCard("AVG DAILY WATER", "-- ml", "Hydration level", IconFactory.IconType.WATER, ThemeConfig.PRIMARY_ACCENT);

    // Chart panels
    private final JPanel pnlWorkoutVolumeChart = new JPanel(new BorderLayout());
    private final JPanel pnlCalorieTrendChart = new JPanel(new BorderLayout());
    private final JPanel pnlSleepTrendChart = new JPanel(new BorderLayout());
    private final JPanel pnlWeightTrendChart = new JPanel(new BorderLayout());

    private List<Workout> allWorkouts;
    private List<BodyMeasurement> allMeasurements;
    private List<NutritionEntry> allNutrition;
    private List<WaterIntake> allWater;
    private List<SleepRecord> allSleep;

    public AnalyticsPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        titlePanel.setOpaque(false);
        JLabel title = new JLabel("Progress Analytics & Reports");
        title.setFont(ThemeConfig.FONT_TITLE_LARGE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        titlePanel.add(title);

        JLabel subtitle = new JLabel("Comprehensive health trends, workout history & data export");
        subtitle.setFont(ThemeConfig.FONT_BODY);
        subtitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        titlePanel.add(subtitle);
        header.add(titlePanel, BorderLayout.WEST);

        // Actions: Filter & CSV Export
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        cmbDateRange.setPreferredSize(new Dimension(130, 36));
        cmbDateRange.addActionListener(e -> updateAnalytics());

        ModernButton btnExport = new ModernButton("📥 Export CSV Reports", ModernButton.ButtonStyle.SECONDARY);
        btnExport.addActionListener(e -> exportDataToCsv());

        actions.add(cmbDateRange);
        actions.add(btnExport);
        header.add(actions, BorderLayout.EAST);

        content.add(header);
        content.add(Box.createVerticalStrut(20));

        // Top Summary Cards
        JPanel statsGrid = new JPanel(new GridLayout(1, 4, 16, 0));
        statsGrid.setOpaque(false);
        statsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        statsGrid.add(cardWorkouts);
        statsGrid.add(cardCalories);
        statsGrid.add(cardAvgSleep);
        statsGrid.add(cardAvgWater);
        content.add(statsGrid);
        content.add(Box.createVerticalStrut(20));

        // Grid 1 of Charts: Workout Volume & Calorie Trends
        JPanel chartGrid1 = new JPanel(new GridLayout(1, 2, 16, 0));
        chartGrid1.setOpaque(false);
        chartGrid1.setPreferredSize(new Dimension(0, 260));
        chartGrid1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        chartGrid1.add(createChartCard("Workout Duration (Minutes)", pnlWorkoutVolumeChart));
        chartGrid1.add(createChartCard("Daily Calorie Intake (kcal)", pnlCalorieTrendChart));
        content.add(chartGrid1);
        content.add(Box.createVerticalStrut(20));

        // Grid 2 of Charts: Sleep Duration & Weight Progression
        JPanel chartGrid2 = new JPanel(new GridLayout(1, 2, 16, 0));
        chartGrid2.setOpaque(false);
        chartGrid2.setPreferredSize(new Dimension(0, 260));
        chartGrid2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        chartGrid2.add(createChartCard("Sleep Duration Trends (Hours)", pnlSleepTrendChart));
        chartGrid2.add(createChartCard("Water Intake Progression (ml)", pnlWeightTrendChart));
        content.add(chartGrid2);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private ModernCard createChartCard(String title, JPanel chartContainer) {
        ModernCard card = new ModernCard(14, new Insets(16, 18, 16, 18));
        JLabel lbl = new JLabel(title);
        lbl.setFont(ThemeConfig.FONT_SUBTITLE);
        lbl.setForeground(ThemeConfig.TEXT_PRIMARY);
        card.add(lbl, BorderLayout.NORTH);

        chartContainer.setOpaque(false);
        card.add(chartContainer, BorderLayout.CENTER);
        return card;
    }

    public void loadAnalytics() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                allWorkouts = workoutDAO.getAllWorkouts();
                allMeasurements = measurementDAO.getAllMeasurements();
                allNutrition = nutritionDAO.getAllEntries();
                allWater = waterDAO.getAllEntries();
                allSleep = sleepDAO.getAllRecords();
                return null;
            }

            @Override
            protected void done() {
                updateAnalytics();
            }
        };
        worker.execute();
    }

    private void updateAnalytics() {
        if (allWorkouts == null) return;

        int days = 30;
        String range = (String) cmbDateRange.getSelectedItem();
        if ("Last 7 Days".equals(range)) days = 7;
        else if ("Last 30 Days".equals(range)) days = 30;
        else if ("Last 90 Days".equals(range)) days = 90;
        else days = 365;

        LocalDate cutoff = LocalDate.now().minusDays(days);

        // Filter workouts
        int workoutCount = 0;
        int workoutMins = 0;
        int caloriesBurned = 0;
        for (Workout w : allWorkouts) {
            if (w.getWorkoutDate() != null && !w.getWorkoutDate().isBefore(cutoff)) {
                workoutCount++;
                workoutMins += w.getDurationMinutes();
                caloriesBurned += w.getCaloriesBurned();
            }
        }
        cardWorkouts.setValue(String.valueOf(workoutCount));
        cardWorkouts.setSubtitle(workoutMins + " mins total");
        cardCalories.setValue(caloriesBurned + " kcal");
        cardCalories.setSubtitle(String.format("Avg %d kcal / session", workoutCount > 0 ? caloriesBurned / workoutCount : 0));

        // Sleep average
        double sleepTotal = 0;
        int sleepCount = 0;
        if (allSleep != null) {
            for (SleepRecord s : allSleep) {
                if (s.getSleepDate() != null && !s.getSleepDate().isBefore(cutoff)) {
                    sleepTotal += s.getDurationHours();
                    sleepCount++;
                }
            }
        }
        cardAvgSleep.setValue(sleepCount > 0 ? String.format("%.1f hrs", sleepTotal / sleepCount) : "-- hrs");
        cardAvgSleep.setSubtitle(sleepCount + " nights tracked");

        // Water average
        int waterTotal = 0;
        int waterCount = 0;
        if (allWater != null) {
            for (WaterIntake wi : allWater) {
                if (wi.getRecordedDate() != null && !wi.getRecordedDate().isBefore(cutoff)) {
                    waterTotal += wi.getAmountMl();
                    waterCount++;
                }
            }
        }
        cardAvgWater.setValue(waterCount > 0 ? String.format("%d ml", waterTotal / Math.max(1, days)) : "-- ml");
        cardAvgWater.setSubtitle(waterCount + " entries recorded");

        // Build charts
        buildWorkoutVolumeChart(days);
        buildCalorieTrendChart(days);
        buildSleepTrendChart(days);
        buildWaterTrendChart(days);
    }

    private void buildWorkoutVolumeChart(int days) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        LocalDate today = LocalDate.now();
        int step = Math.max(1, days / 7);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd");

        for (int i = days - 1; i >= 0; i -= step) {
            LocalDate d = today.minusDays(i);
            int mins = 0;
            if (allWorkouts != null) {
                for (Workout w : allWorkouts) {
                    if (w.getWorkoutDate() != null && w.getWorkoutDate().isEqual(d)) {
                        mins += w.getDurationMinutes();
                    }
                }
            }
            dataset.addValue(mins, "Minutes", d.format(fmt));
        }

        JFreeChart chart = ChartFactory.createBarChart(null, null, null, dataset, PlotOrientation.VERTICAL, false, false, false);
        DashboardPanel.applyDarkChartTheme(chart);
        BarRenderer r = (BarRenderer) chart.getCategoryPlot().getRenderer();
        r.setSeriesPaint(0, ThemeConfig.PRIMARY_ACCENT);
        r.setMaximumBarWidth(0.12);

        ChartPanel cp = new ChartPanel(chart);
        cp.setOpaque(false);
        cp.setBackground(new Color(0, 0, 0, 0));
        pnlWorkoutVolumeChart.removeAll();
        pnlWorkoutVolumeChart.add(cp, BorderLayout.CENTER);
        pnlWorkoutVolumeChart.revalidate();
        pnlWorkoutVolumeChart.repaint();
    }

    private void buildCalorieTrendChart(int days) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        LocalDate today = LocalDate.now();
        int step = Math.max(1, days / 7);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd");

        for (int i = days - 1; i >= 0; i -= step) {
            LocalDate d = today.minusDays(i);
            int cal = 0;
            if (allNutrition != null) {
                for (NutritionEntry n : allNutrition) {
                    if (n.getEntryDate() != null && n.getEntryDate().isEqual(d)) {
                        cal += n.getCalories();
                    }
                }
            }
            dataset.addValue(cal, "Calories", d.format(fmt));
        }

        JFreeChart chart = ChartFactory.createBarChart(null, null, null, dataset, PlotOrientation.VERTICAL, false, false, false);
        DashboardPanel.applyDarkChartTheme(chart);
        BarRenderer r = (BarRenderer) chart.getCategoryPlot().getRenderer();
        r.setSeriesPaint(0, ThemeConfig.WARNING);
        r.setMaximumBarWidth(0.12);

        ChartPanel cp = new ChartPanel(chart);
        cp.setOpaque(false);
        cp.setBackground(new Color(0, 0, 0, 0));
        pnlCalorieTrendChart.removeAll();
        pnlCalorieTrendChart.add(cp, BorderLayout.CENTER);
        pnlCalorieTrendChart.revalidate();
        pnlCalorieTrendChart.repaint();
    }

    private void buildSleepTrendChart(int days) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        LocalDate today = LocalDate.now();
        int step = Math.max(1, days / 7);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd");

        for (int i = days - 1; i >= 0; i -= step) {
            LocalDate d = today.minusDays(i);
            double hrs = 0;
            if (allSleep != null) {
                for (SleepRecord s : allSleep) {
                    if (s.getSleepDate() != null && s.getSleepDate().isEqual(d)) {
                        hrs = s.getDurationHours();
                        break;
                    }
                }
            }
            dataset.addValue(hrs, "Hours", d.format(fmt));
        }

        JFreeChart chart = ChartFactory.createBarChart(null, null, null, dataset, PlotOrientation.VERTICAL, false, false, false);
        DashboardPanel.applyDarkChartTheme(chart);
        org.jfree.chart.plot.CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer r = (BarRenderer) plot.getRenderer();
        r.setSeriesPaint(0, ThemeConfig.PRIMARY_ACCENT);
        r.setBarPainter(new DashboardPanel.ModernRoundedBarPainter());
        r.setMaximumBarWidth(0.13);
        r.setShadowVisible(false);
        r.setDrawBarOutline(false);

        org.jfree.chart.axis.NumberAxis range = (org.jfree.chart.axis.NumberAxis) plot.getRangeAxis();
        range.setStandardTickUnits(org.jfree.chart.axis.NumberAxis.createIntegerTickUnits());
        range.setRange(0.0, 12.0);

        ChartPanel cp = new ChartPanel(chart);
        cp.setOpaque(false);
        cp.setBackground(new Color(0, 0, 0, 0));
        pnlSleepTrendChart.removeAll();
        pnlSleepTrendChart.add(cp, BorderLayout.CENTER);
        pnlSleepTrendChart.revalidate();
        pnlSleepTrendChart.repaint();
    }

    private void buildWaterTrendChart(int days) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        LocalDate today = LocalDate.now();
        int step = Math.max(1, days / 7);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd");

        for (int i = days - 1; i >= 0; i -= step) {
            LocalDate d = today.minusDays(i);
            int ml = 0;
            if (allWater != null) {
                for (WaterIntake wi : allWater) {
                    if (wi.getRecordedDate() != null && wi.getRecordedDate().isEqual(d)) {
                        ml += wi.getAmountMl();
                    }
                }
            }
            dataset.addValue(ml, "Water", d.format(fmt));
        }

        JFreeChart chart = ChartFactory.createBarChart(null, null, null, dataset, PlotOrientation.VERTICAL, false, false, false);
        DashboardPanel.applyDarkChartTheme(chart);
        BarRenderer r = (BarRenderer) chart.getCategoryPlot().getRenderer();
        r.setSeriesPaint(0, ThemeConfig.PRIMARY_ACCENT);
        r.setMaximumBarWidth(0.12);

        ChartPanel cp = new ChartPanel(chart);
        cp.setOpaque(false);
        cp.setBackground(new Color(0, 0, 0, 0));
        pnlWeightTrendChart.removeAll();
        pnlWeightTrendChart.add(cp, BorderLayout.CENTER);
        pnlWeightTrendChart.revalidate();
        pnlWeightTrendChart.repaint();
    }

    private void exportDataToCsv() {
        String[] reportOptions = {"Workouts Report", "Body Measurements Report", "Nutrition Entries Report"};
        String selectedReport = (String) JOptionPane.showInputDialog(
                this,
                "Select data report to export as CSV:",
                "Export Analytics Data",
                JOptionPane.QUESTION_MESSAGE,
                null,
                reportOptions,
                reportOptions[0]
        );
        if (selectedReport == null) return;

        JFileChooser fileChooser = new JFileChooser();
        String defaultFileName;
        if ("Body Measurements Report".equals(selectedReport)) {
            defaultFileName = "PersonalTracker_Measurements_" + LocalDate.now() + ".csv";
        } else if ("Nutrition Entries Report".equals(selectedReport)) {
            defaultFileName = "PersonalTracker_Nutrition_" + LocalDate.now() + ".csv";
        } else {
            defaultFileName = "PersonalTracker_Workouts_" + LocalDate.now() + ".csv";
        }
        fileChooser.setDialogTitle("Export " + selectedReport + " to CSV");
        fileChooser.setSelectedFile(new File(defaultFileName));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                if ("Body Measurements Report".equals(selectedReport)) {
                    if (allMeasurements != null) {
                        CsvExporter.exportMeasurements(allMeasurements, fileToSave);
                    }
                } else if ("Nutrition Entries Report".equals(selectedReport)) {
                    if (allNutrition != null) {
                        CsvExporter.exportNutrition(allNutrition, fileToSave);
                    }
                } else {
                    if (allWorkouts != null) {
                        CsvExporter.exportWorkouts(allWorkouts, fileToSave);
                    }
                }
                JOptionPane.showMessageDialog(this, selectedReport + " exported successfully to:\n" + fileToSave.getAbsolutePath(), "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to export CSV: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
