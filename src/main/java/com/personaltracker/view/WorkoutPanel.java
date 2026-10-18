package com.personaltracker.view;

import com.personaltracker.components.ModernButton;
import com.personaltracker.components.ModernCard;
import com.personaltracker.components.ModernTable;
import com.personaltracker.components.ModernTextField;
import com.personaltracker.components.ToastNotification;
import com.personaltracker.components.WorkoutTimerPanel;
import com.personaltracker.config.ThemeConfig;
import com.personaltracker.dao.AchievementDAO;
import com.personaltracker.dao.WorkoutDAO;
import com.personaltracker.model.Workout;
import com.personaltracker.model.WorkoutExercise;
import com.personaltracker.util.DateUtil;
import com.personaltracker.util.HealthCalculator;
import com.personaltracker.util.IconFactory;
import com.personaltracker.util.ValidationUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Workout Management view with search, filter, interactive timer, and exercise sets logging.
 */
public class WorkoutPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final WorkoutDAO workoutDAO = new WorkoutDAO();
    private final AchievementDAO achievementDAO = new AchievementDAO();

    private final ModernTextField txtSearch = new ModernTextField("", "Search workouts by title or type...");
    private final JComboBox<String> cmbTypeFilter = new JComboBox<>(new String[]{"All Types", "Strength", "Cardio", "HIIT", "Yoga", "Flexibility", "Endurance", "Other"});
    private final JComboBox<String> cmbStatusFilter = new JComboBox<>(new String[]{"All Statuses", "COMPLETED", "PLANNED"});

    private final DefaultTableModel tableModel;
    private final ModernTable table;
    private final TableRowSorter<DefaultTableModel> sorter;

    private final WorkoutTimerPanel timerPanel = new WorkoutTimerPanel();
    private final JLabel lblSummaryStats = new JLabel("Loading workouts...");

    private List<Workout> currentWorkouts = new ArrayList<>();

    public WorkoutPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));

        // 1. Header & Actions
        contentPanel.add(createHeaderPanel());
        contentPanel.add(Box.createVerticalStrut(16));

        // 2. Interactive Stopwatch Timer
        timerPanel.setOnLogCallback(mins -> openAddWorkoutDialog(mins));
        contentPanel.add(timerPanel);
        contentPanel.add(Box.createVerticalStrut(16));

        // 3. Search & Filter Bar
        contentPanel.add(createFilterPanel());
        contentPanel.add(Box.createVerticalStrut(12));

        // 4. Workouts Table
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

        JScrollPane scrollTable = ModernTable.wrapInScrollPane(table);
        scrollTable.setPreferredSize(new Dimension(0, 320));
        contentPanel.add(scrollTable);
        contentPanel.add(Box.createVerticalStrut(12));

        // 5. Bottom Controls & Summary
        contentPanel.add(createBottomBar());

        JScrollPane mainScroll = new JScrollPane(contentPanel);
        mainScroll.setBorder(null);
        mainScroll.setOpaque(false);
        mainScroll.getViewport().setOpaque(false);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(mainScroll, BorderLayout.CENTER);

        // Setup filter listener
        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
        });
        cmbTypeFilter.addActionListener(e -> applyFilters());
        cmbStatusFilter.addActionListener(e -> applyFilters());
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        textPanel.setOpaque(false);
        JLabel title = new JLabel("Workout Management");
        title.setFont(ThemeConfig.FONT_TITLE_LARGE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        textPanel.add(title);

        JLabel subtitle = new JLabel("Plan routines, log exercises & track workout volume");
        subtitle.setFont(ThemeConfig.FONT_BODY);
        subtitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        textPanel.add(subtitle);

        header.add(textPanel, BorderLayout.WEST);

        ModernButton btnNew = new ModernButton("+ Log Workout", ModernButton.ButtonStyle.PRIMARY);
        btnNew.addActionListener(e -> openAddWorkoutDialog(0));
        header.add(btnNew, BorderLayout.EAST);

        return header;
    }

    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        panel.add(txtSearch, BorderLayout.CENTER);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filters.setOpaque(false);

        cmbTypeFilter.setPreferredSize(new Dimension(130, 36));
        cmbStatusFilter.setPreferredSize(new Dimension(130, 36));

        filters.add(cmbTypeFilter);
        filters.add(cmbStatusFilter);
        panel.add(filters, BorderLayout.EAST);

        return panel;
    }

    private JPanel createBottomBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);

        lblSummaryStats.setFont(ThemeConfig.FONT_BODY);
        lblSummaryStats.setForeground(ThemeConfig.TEXT_SECONDARY);
        bar.add(lblSummaryStats, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        ModernButton btnEdit = new ModernButton("Edit Selected", ModernButton.ButtonStyle.SECONDARY);
        btnEdit.addActionListener(e -> editSelectedWorkout());

        ModernButton btnDelete = new ModernButton("Delete", ModernButton.ButtonStyle.DANGER);
        btnDelete.addActionListener(e -> deleteSelectedWorkout());

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
                } catch (Exception e) {
                    lblSummaryStats.setText("Failed to load workouts: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void openAddWorkoutDialog(int initialMinutes) {
        WorkoutDialog dialog = new WorkoutDialog(SwingUtilities.getWindowAncestor(this), null, initialMinutes);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            ToastNotification.showToast(this, "Workout logged successfully! 💪", ToastNotification.ToastType.SUCCESS);
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
            super(owner, workout == null ? "Log New Workout" : "Edit Workout", ModalityType.APPLICATION_MODAL);
            this.editingWorkout = workout;
            setSize(590, 720);
            setLocationRelativeTo(owner);
            setResizable(false);
            getContentPane().setBackground(ThemeConfig.BG_DARK);

            JPanel main = new JPanel();
            main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
            main.setOpaque(false);
            main.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

            // Section 1: Quick Workout Template Chips
            if (workout == null) {
                JLabel lblTpl = new JLabel("Quick Workout Presets");
                lblTpl.setFont(ThemeConfig.FONT_SMALL);
                lblTpl.setForeground(ThemeConfig.TEXT_SECONDARY);
                main.add(lblTpl);
                main.add(Box.createVerticalStrut(4));

                JPanel tplRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
                tplRow.setOpaque(false);
                tplRow.add(createTemplateChip("🏋️ Push Day", "Push Day (Chest & Triceps)", "Strength", 50, new String[][]{{"Bench Press", "3", "10", "60.0"}, {"Incline Dumbbell Press", "3", "12", "20.0"}}));
                tplRow.add(createTemplateChip("💪 Pull Day", "Pull Day (Back & Biceps)", "Strength", 50, new String[][]{{"Lat Pulldown", "3", "10", "55.0"}, {"Barbell Row", "3", "10", "50.0"}}));
                tplRow.add(createTemplateChip("🦵 Leg Day", "Leg Day (Quads & Hamstrings)", "Strength", 50, new String[][]{{"Squats", "3", "8", "80.0"}, {"Leg Press", "3", "12", "120.0"}}));
                tplRow.add(createTemplateChip("🏃 5K Run", "5K Outdoor Run", "Cardio", 30, null));
                tplRow.add(createTemplateChip("🔥 HIIT", "Full Body HIIT", "HIIT", 25, null));
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
            JScrollPane scrollEx = ModernTable.wrapInScrollPane(exercisesTable);
            scrollEx.setPreferredSize(new Dimension(0, 95));
            main.add(scrollEx);
            main.add(Box.createVerticalStrut(6));

            JPanel exBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            exBtnRow.setOpaque(false);
            ModernButton btnAddEx = new ModernButton("+ Custom Exercise", ModernButton.ButtonStyle.SECONDARY);
            btnAddEx.addActionListener(e -> exercisesModel.addRow(new Object[]{"Exercise Name", 3, 10, 0.0}));
            ModernButton btnDelEx = new ModernButton("- Remove Selected", ModernButton.ButtonStyle.GHOST);
            btnDelEx.addActionListener(e -> {
                int r = exercisesTable.getSelectedRow();
                if (r >= 0) exercisesModel.removeRow(r);
            });
            exBtnRow.add(btnAddEx);
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
                    if (editingWorkout == null) {
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
}
