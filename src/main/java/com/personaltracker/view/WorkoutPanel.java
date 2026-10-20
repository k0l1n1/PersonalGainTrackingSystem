package com.personaltracker.view;

import com.personaltracker.components.ModernButton;
import com.personaltracker.components.ModernCard;
import com.personaltracker.components.ModernTable;
import com.personaltracker.components.ModernTextField;
import com.personaltracker.components.ToastNotification;
import com.personaltracker.components.WorkoutTimerPanel;
import com.personaltracker.config.ThemeConfig;
import com.personaltracker.dao.AchievementDAO;
import com.personaltracker.dao.ReminderDAO;
import com.personaltracker.dao.WorkoutDAO;
import com.personaltracker.model.Reminder;
import com.personaltracker.model.Workout;
import com.personaltracker.model.WorkoutExercise;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Workout Management view with search, filter, interactive timer, and exercise sets logging.
 */
public class WorkoutPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final WorkoutDAO workoutDAO = new WorkoutDAO();
    private final AchievementDAO achievementDAO = new AchievementDAO();
    private final ReminderDAO reminderDAO = new ReminderDAO();

    private final CardLayout viewCardLayout = new CardLayout();
    private final JPanel viewContainer = new JPanel(viewCardLayout);
    private final JButton btnTabWorkouts = new JButton("Workouts Log");
    private final JButton btnTabReminders = new JButton("Schedule & Reminders");

    // Header Action Buttons (only corresponding button shown per tab)
    private final ModernButton btnHeaderLogWorkout = new ModernButton("Log Workout", ModernButton.ButtonStyle.PRIMARY);
    private final ModernButton btnHeaderSetReminder = new ModernButton("Set Reminder", ModernButton.ButtonStyle.PRIMARY);

    // KPI Metric Labels
    private final JLabel lblKpiWorkouts = new JLabel("0");
    private final JLabel lblKpiDuration = new JLabel("0h 0m");
    private final JLabel lblKpiCalories = new JLabel("0 kcal");
    private final JLabel lblKpiStreak = new JLabel("0 Days");

    // Workouts Log Components
    private final ModernTextField txtSearch = new ModernTextField("", "Search workouts by title or type...");
    private final JComboBox<String> cmbTypeFilter = new JComboBox<>(new String[]{"All Types", "Strength", "Cardio", "HIIT", "Yoga", "Flexibility", "Endurance", "Other"});
    private final JComboBox<String> cmbStatusFilter = new JComboBox<>(new String[]{"All Statuses", "COMPLETED", "PLANNED"});

    private final DefaultTableModel tableModel;
    private final ModernTable table;
    private final TableRowSorter<DefaultTableModel> sorter;

    private final WorkoutTimerPanel timerPanel = new WorkoutTimerPanel();
    private final JLabel lblSummaryStats = new JLabel("Loading workouts...");

    private List<Workout> currentWorkouts = new ArrayList<>();

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
                if ("COMPLETED".equalsIgnoreCase(val)) {
                    lbl.setText("✓ COMPLETED");
                    lbl.setForeground(ThemeConfig.SUCCESS);
                } else {
                    lbl.setText("⏱ PLANNED");
                    lbl.setForeground(ThemeConfig.WARNING);
                }
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
                if (!isSelected) {
                    lbl.setBackground(row % 2 == 0 ? ThemeConfig.BG_CARD : new Color(0x1F, 0x1F, 0x1F));
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

    private JPanel createQuickWorkoutPresetsBar() {
        ModernCard card = new ModernCard(12, new Insets(6, 14, 6, 14));
        card.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 0));

        JLabel lbl = new JLabel("Quick Log:");
        lbl.setFont(ThemeConfig.FONT_SMALL);
        lbl.setForeground(ThemeConfig.TEXT_MUTED);
        card.add(lbl);

        card.add(createQuickLogChip("+ Push Day (45m)", "Push Day (Chest/Triceps)", "Strength", 45, 380, new String[][]{{"Bench Press", "3", "10", "60.0"}, {"Incline Dumbbell Press", "3", "12", "20.0"}}));
        card.add(createQuickLogChip("+ Pull Day (45m)", "Pull Day (Back/Biceps)", "Strength", 45, 360, new String[][]{{"Lat Pulldown", "3", "10", "55.0"}, {"Barbell Row", "3", "10", "50.0"}}));
        card.add(createQuickLogChip("+ Leg Day (50m)", "Leg Day (Quads/Hamstrings)", "Strength", 50, 420, new String[][]{{"Barbell Squats", "3", "8", "80.0"}, {"Leg Press", "3", "12", "120.0"}}));
        card.add(createQuickLogChip("+ 5K Run (30m)", "5K Outdoor Run", "Cardio", 30, 310, null));
        card.add(createQuickLogChip("+ HIIT Blitz (20m)", "Full Body HIIT", "HIIT", 20, 240, null));

        return card;
    }

    private JButton createQuickLogChip(String label, String title, String type, int duration, int calories, String[][] exercises) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btn.setForeground(ThemeConfig.TEXT_PRIMARY);
        btn.setBackground(ThemeConfig.BG_CARD);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Click to review & log, or Shift+Click to instantly save");
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        btn.addActionListener(e -> {
            boolean isShift = (e.getModifiers() & java.awt.event.ActionEvent.SHIFT_MASK) != 0;
            if (isShift) {
                quickSavePresetDirectly(title, type, duration, calories, exercises);
            } else {
                openAddWorkoutDialogWithPreset(title, type, duration, calories, exercises);
            }
        });
        return btn;
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
        setupTabButton(btnTabReminders, "Schedule & Reminders", false, IconFactory.IconType.REMINDERS);
        btnTabWorkouts.addActionListener(e -> selectWorkoutsTab());
        btnTabReminders.addActionListener(e -> selectRemindersTab());
        tabSwitcher.add(btnTabWorkouts);
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
        Color fg = active ? Color.WHITE : ThemeConfig.TEXT_SECONDARY;
        btn.setIcon(IconFactory.createIcon(iconType, 15, fg));
        btn.setIconTextGap(7);
        btn.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 12));
        btn.setForeground(fg);
        btn.setBackground(active ? ThemeConfig.PRIMARY_ACCENT : ThemeConfig.BG_CARD);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(active ? ThemeConfig.PRIMARY_ACCENT : ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));
    }

    public void switchToTab(String tabName) {
        viewCardLayout.show(viewContainer, tabName);
        boolean isWorkouts = "WORKOUTS".equals(tabName);
        setupTabButton(btnTabWorkouts, "Workouts Log", isWorkouts, IconFactory.IconType.WORKOUT);
        int remCount = currentReminders != null ? currentReminders.size() : 0;
        setupTabButton(btnTabReminders, "Schedule & Reminders (" + remCount + ")", !isWorkouts, IconFactory.IconType.REMINDERS);

        // Show ONLY the corresponding action button for current view
        btnHeaderLogWorkout.setVisible(isWorkouts);
        btnHeaderSetReminder.setVisible(!isWorkouts);
        if (btnHeaderLogWorkout.getParent() != null) {
            btnHeaderLogWorkout.getParent().revalidate();
            btnHeaderLogWorkout.getParent().repaint();
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

        cmbTypeFilter.setPreferredSize(new Dimension(120, 32));
        cmbStatusFilter.setPreferredSize(new Dimension(120, 32));

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

        ModernButton btnEdit = new ModernButton("Edit Selected", ModernButton.ButtonStyle.SECONDARY);
        btnEdit.setPreferredSize(new Dimension(105, 30));
        btnEdit.addActionListener(e -> editSelectedWorkout());

        ModernButton btnDelete = new ModernButton("Delete", ModernButton.ButtonStyle.DANGER);
        btnDelete.setPreferredSize(new Dimension(80, 30));
        btnDelete.addActionListener(e -> deleteSelectedWorkout());

        actions.add(btnGoReminders);
        actions.add(btnEdit);
        actions.add(btnDelete);
        bar.add(actions, BorderLayout.EAST);

        return bar;
    }

    private void applyFilters() {
        String search = txtSearch.getText().trim().toLowerCase();
        String selectedType = (String) cmbTypeFilter.getSelectedItem();
        String selectedStatus = (String) cmbStatusFilter.getSelectedItem();

        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                String title = entry.getStringValue(1).toLowerCase();
                String type = entry.getStringValue(2);
                String status = entry.getStringValue(5);
                String notes = entry.getStringValue(6).toLowerCase();

                if (!search.isEmpty() && !title.contains(search) && !type.toLowerCase().contains(search) && !notes.contains(search)) {
                    return false;
                }
                if (!"All Types".equals(selectedType) && !type.equalsIgnoreCase(selectedType)) {
                    return false;
                }
                if (!"All Statuses".equals(selectedStatus) && !status.equalsIgnoreCase(selectedStatus)) {
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
                return workoutDAO.getAllWorkouts();
            }

            @Override
            protected void done() {
                try {
                    currentWorkouts = get();
                    tableModel.setRowCount(0);

                    int totalMins = 0;
                    int totalCal = 0;

                    for (Workout w : currentWorkouts) {
                        tableModel.addRow(new Object[]{
                                DateUtil.formatDate(w.getWorkoutDate()),
                                w.getTitle(),
                                w.getWorkoutType(),
                                w.getDurationMinutes() + " mins",
                                w.getCaloriesBurned() + " kcal",
                                w.getStatus(),
                                w.getNotes() != null ? w.getNotes() : ""
                        });
                        totalMins += w.getDurationMinutes();
                        totalCal += w.getCaloriesBurned();
                    }

                    lblSummaryStats.setText(String.format("Total: %d workouts | %d mins (%s) | %d kcal burned",
                            currentWorkouts.size(), totalMins, DateUtil.formatDuration(totalMins), totalCal));

                    // Update KPI Metric Cards
                    int totalWorkouts = currentWorkouts.size();
                    lblKpiWorkouts.setText(String.valueOf(totalWorkouts));
                    int hrs = totalMins / 60;
                    int mins = totalMins % 60;
                    lblKpiDuration.setText(hrs > 0 ? String.format("%dh %02dm", hrs, mins) : totalMins + "m");
                    lblKpiCalories.setText(String.format("%,d kcal", totalCal));

                    int streak = HealthCalculator.calculateWorkoutStreak(currentWorkouts);
                    lblKpiStreak.setText(streak + (streak == 1 ? " Day" : " Days"));
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
        Workout w = currentWorkouts.get(modelRow);
        if (w.getExercises() == null && w.getId() != null) {
            try {
                w.setExercises(workoutDAO.getExercisesForWorkout(w.getId()));
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
        Workout w = currentWorkouts.get(modelRow);

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

        private final ModernTextField txtTitle = new ModernTextField("Morning Upper Body");
        private final JComboBox<String> cmbType = new JComboBox<>(new String[]{"Strength", "Cardio", "HIIT", "Yoga", "Flexibility", "Endurance", "Other"});
        private final ModernTextField txtDate = new ModernTextField(LocalDate.now().toString());
        private final ModernTextField txtDuration = new ModernTextField("45");
        private final ModernTextField txtCalories = new ModernTextField("350");
        private final JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"COMPLETED", "PLANNED"});
        private final JTextArea txtNotes = new JTextArea(2, 20);

        private final DefaultTableModel exercisesModel;
        private final ModernTable exercisesTable;

        public WorkoutDialog(Window owner, Workout workout, int prefillDurationMinutes) {
            super(owner, (workout == null || workout.getId() == null || workout.getId().isBlank()) ? "Log New Workout" : "Edit Workout", ModalityType.APPLICATION_MODAL);
            this.editingWorkout = (workout != null && workout.getId() != null && !workout.getId().isBlank()) ? workout : null;
            setSize(590, 720);
            setLocationRelativeTo(owner);
            setResizable(false);
            getContentPane().setBackground(ThemeConfig.BG_DARK);

            JPanel main = new JPanel();
            main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
            main.setOpaque(false);
            main.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

            // Section 1: Quick Workout Template Chips
            if (workout == null || workout.getId() == null || workout.getId().isBlank()) {
                JLabel lblTpl = new JLabel("Quick Workout Presets");
                lblTpl.setFont(ThemeConfig.FONT_SMALL);
                lblTpl.setForeground(ThemeConfig.TEXT_SECONDARY);
                main.add(lblTpl);
                main.add(Box.createVerticalStrut(4));

                JPanel tplRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
                tplRow.setOpaque(false);
                tplRow.add(createTemplateChip("Push Day", "Push Day (Chest & Triceps)", "Strength", 50, new String[][]{{"Bench Press", "3", "10", "60.0"}, {"Incline Dumbbell Press", "3", "12", "20.0"}}));
                tplRow.add(createTemplateChip("Pull Day", "Pull Day (Back & Biceps)", "Strength", 50, new String[][]{{"Lat Pulldown", "3", "10", "55.0"}, {"Barbell Row", "3", "10", "50.0"}}));
                tplRow.add(createTemplateChip("Leg Day", "Leg Day (Quads & Hamstrings)", "Strength", 50, new String[][]{{"Squats", "3", "8", "80.0"}, {"Leg Press", "3", "12", "120.0"}}));
                tplRow.add(createTemplateChip("5K Run", "5K Outdoor Run", "Cardio", 30, null));
                tplRow.add(createTemplateChip("HIIT Session", "Full Body HIIT", "HIIT", 25, null));
                main.add(tplRow);
                main.add(Box.createVerticalStrut(12));
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
            JButton btnToday = createQuickPill("Today", true);
            JButton btnYesterday = createQuickPill("Yesterday", false);
            btnToday.addActionListener(e -> {
                txtDate.setText(LocalDate.now().toString());
                btnToday.setBackground(ThemeConfig.PRIMARY_ACCENT);
                btnYesterday.setBackground(ThemeConfig.BG_CARD);
            });
            btnYesterday.addActionListener(e -> {
                txtDate.setText(LocalDate.now().minusDays(1).toString());
                btnYesterday.setBackground(ThemeConfig.PRIMARY_ACCENT);
                btnToday.setBackground(ThemeConfig.BG_CARD);
            });
            txtDate.setPreferredSize(new Dimension(105, 30));
            dateControls.add(btnToday);
            dateControls.add(btnYesterday);
            dateControls.add(txtDate);
            dateCol.add(dateControls);
            row2.add(dateCol);
            main.add(row2);
            main.add(Box.createVerticalStrut(10));

            // Duration with Steppers, Auto Calories, Status
            JPanel row3 = new JPanel(new GridLayout(1, 3, 10, 0));
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

            row3.add(durPanel);
            row3.add(createFieldRow("Calories Burned (Auto)", txtCalories));
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

            // Real-time automatic calorie estimation on duration or type changes
            txtDuration.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { autoEstimateCalories(); }
                @Override public void removeUpdate(DocumentEvent e) { autoEstimateCalories(); }
                @Override public void changedUpdate(DocumentEvent e) { autoEstimateCalories(); }
            });
            cmbType.addActionListener(e -> autoEstimateCalories());

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

                if (workout.getExercises() != null) {
                    for (WorkoutExercise ex : workout.getExercises()) {
                        exercisesModel.addRow(new Object[]{ex.getExerciseName(), ex.getSets(), ex.getReps(), ex.getWeightKg()});
                    }
                }
            } else if (prefillDurationMinutes > 0) {
                txtDuration.setText(String.valueOf(prefillDurationMinutes));
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
                autoEstimateCalories();
                if (defaultExercises != null) {
                    exercisesModel.setRowCount(0);
                    for (String[] row : defaultExercises) {
                        exercisesModel.addRow(new Object[]{row[0], Integer.parseInt(row[1]), Integer.parseInt(row[2]), Double.parseDouble(row[3])});
                    }
                }
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
            btn.setFont(ThemeConfig.FONT_SMALL);
            btn.setForeground(ThemeConfig.TEXT_PRIMARY);
            btn.setBackground(active ? ThemeConfig.PRIMARY_ACCENT : ThemeConfig.BG_CARD);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));
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
                    int current = Integer.parseInt(txtDuration.getText().trim());
                    txtDuration.setText(String.valueOf(Math.max(5, Math.min(300, current + delta))));
                    autoEstimateCalories();
                } catch (Exception ignored) {}
            });
            return btn;
        }

        private void autoEstimateCalories() {
            try {
                int mins = Integer.parseInt(txtDuration.getText().trim());
                String type = (String) cmbType.getSelectedItem();
                int estimated = HealthCalculator.estimateCaloriesBurned(type, mins, 75.0);
                txtCalories.setText(String.valueOf(estimated));
            } catch (Exception ignored) {}
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
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        saved = true;
                        ToastNotification.showToast(WorkoutPanel.this, "Workout saved successfully!", ToastNotification.ToastType.SUCCESS);
                        dispose();
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
            ModernButton btnMarkDone = new ModernButton("✓ Mark Done", ModernButton.ButtonStyle.SECONDARY);
            btnMarkDone.addActionListener(e -> toggleReminderStatus(focusReminder, true));

            ModernButton btnStartWorkout = new ModernButton("⏱ Start Workout Now", ModernButton.ButtonStyle.PRIMARY);
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
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x35, 0x35, 0x35, 120)),
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
        btn.setForeground(ThemeConfig.TEXT_PRIMARY);
        btn.setBackground(active ? ThemeConfig.PRIMARY_ACCENT : ThemeConfig.BG_CARD);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
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
                return null;
            }
            @Override
            protected void done() {
                ToastNotification.showToast(WorkoutPanel.this, completed ? "Reminder marked as completed!" : "Reminder marked active.", ToastNotification.ToastType.SUCCESS);
                loadReminders();
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

        private final ModernTextField txtTitle = new ModernTextField("Evening Workout Session");
        private final ModernTextField txtDesc = new ModernTextField("Leg day workout & protein shake");
        private final ModernTextField txtDate = new ModernTextField(LocalDate.now().toString());
        private final ModernTextField txtTime = new ModernTextField("18:00");

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
                btnToday.setBackground(ThemeConfig.PRIMARY_ACCENT);
                btnTomorrow.setBackground(ThemeConfig.BG_CARD);
            });
            btnTomorrow.addActionListener(e -> {
                txtDate.setText(LocalDate.now().plusDays(1).toString());
                btnTomorrow.setBackground(ThemeConfig.PRIMARY_ACCENT);
                btnToday.setBackground(ThemeConfig.BG_CARD);
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
            btn.setFont(ThemeConfig.FONT_SMALL);
            btn.setForeground(ThemeConfig.TEXT_PRIMARY);
            btn.setBackground(active ? ThemeConfig.PRIMARY_ACCENT : ThemeConfig.BG_CARD);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
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
