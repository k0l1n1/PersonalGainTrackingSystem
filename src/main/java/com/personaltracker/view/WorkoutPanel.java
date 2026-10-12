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

    private final WorkoutDAO workoutDAO = new WorkoutDAO();
    private final AchievementDAO achievementDAO = new AchievementDAO();

    private final ModernTextField txtSearch = new ModernTextField("Search workouts by title or type...");
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
            loadWorkouts();
            checkWorkoutAchievements();
        }
    }

    private void editSelectedWorkout() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a workout to edit.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(selectedRow);
        Workout w = currentWorkouts.get(modelRow);

        WorkoutDialog dialog = new WorkoutDialog(SwingUtilities.getWindowAncestor(this), w, 0);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadWorkouts();
        }
    }

    private void deleteSelectedWorkout() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a workout to delete.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
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
        private boolean saved = false;
        private final Workout editingWorkout;

        private final ModernTextField txtTitle = new ModernTextField("Morning Upper Body");
        private final JComboBox<String> cmbType = new JComboBox<>(new String[]{"Strength", "Cardio", "HIIT", "Yoga", "Flexibility", "Endurance", "Other"});
        private final ModernTextField txtDate = new ModernTextField(LocalDate.now().toString());
        private final ModernTextField txtDuration = new ModernTextField("45");
        private final ModernTextField txtCalories = new ModernTextField("350");
        private final JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"COMPLETED", "PLANNED"});
        private final JTextArea txtNotes = new JTextArea(3, 20);

        private final DefaultTableModel exercisesModel;
        private final ModernTable exercisesTable;

        public WorkoutDialog(Window owner, Workout workout, int prefillDurationMinutes) {
            super(owner, workout == null ? "Log New Workout" : "Edit Workout", ModalityType.APPLICATION_MODAL);
            this.editingWorkout = workout;
            setSize(560, 680);
            setLocationRelativeTo(owner);
            setResizable(false);
            getContentPane().setBackground(ThemeConfig.BG_DARK);

            JPanel main = new JPanel();
            main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
            main.setOpaque(false);
            main.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

            // Form inputs
            main.add(createFieldRow("Workout Title", txtTitle));
            main.add(Box.createVerticalStrut(10));

            JPanel row2 = new JPanel(new GridLayout(1, 2, 12, 0));
            row2.setOpaque(false);
            row2.add(createFieldRow("Workout Type", cmbType));
            row2.add(createFieldRow("Date (YYYY-MM-DD)", txtDate));
            main.add(row2);
            main.add(Box.createVerticalStrut(10));

            JPanel row3 = new JPanel(new GridLayout(1, 3, 12, 0));
            row3.setOpaque(false);
            row3.add(createFieldRow("Duration (Mins)", txtDuration));
            row3.add(createFieldRow("Calories Burned", txtCalories));
            row3.add(createFieldRow("Status", cmbStatus));
            main.add(row3);
            main.add(Box.createVerticalStrut(14));

            // Auto-calculate calories button
            ModernButton btnEstimateCal = new ModernButton("⚡ Estimate Calories Burned", ModernButton.ButtonStyle.SECONDARY);
            btnEstimateCal.addActionListener(e -> {
                try {
                    int mins = Integer.parseInt(txtDuration.getText().trim());
                    String type = (String) cmbType.getSelectedItem();
                    int estimated = HealthCalculator.estimateCaloriesBurned(type, mins, 75.0);
                    txtCalories.setText(String.valueOf(estimated));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Please enter a valid duration first.");
                }
            });
            main.add(btnEstimateCal);
            main.add(Box.createVerticalStrut(14));

            // Exercises section
            JLabel lblExTitle = new JLabel("Exercises & Sets (Optional)");
            lblExTitle.setFont(ThemeConfig.FONT_SUBTITLE);
            lblExTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
            main.add(lblExTitle);
            main.add(Box.createVerticalStrut(6));

            String[] exCols = {"Exercise", "Sets", "Reps", "Weight (kg)"};
            exercisesModel = new DefaultTableModel(exCols, 0);
            exercisesTable = new ModernTable(exercisesModel);
            exercisesTable.setRowHeight(30);
            JScrollPane scrollEx = ModernTable.wrapInScrollPane(exercisesTable);
            scrollEx.setPreferredSize(new Dimension(0, 110));
            main.add(scrollEx);
            main.add(Box.createVerticalStrut(8));

            JPanel exBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            exBtnRow.setOpaque(false);
            ModernButton btnAddEx = new ModernButton("+ Add Exercise Row", ModernButton.ButtonStyle.SECONDARY);
            btnAddEx.addActionListener(e -> exercisesModel.addRow(new Object[]{"Bench Press", 3, 10, 60.0}));
            ModernButton btnDelEx = new ModernButton("- Remove Row", ModernButton.ButtonStyle.GHOST);
            btnDelEx.addActionListener(e -> {
                int r = exercisesTable.getSelectedRow();
                if (r >= 0) exercisesModel.removeRow(r);
            });
            exBtnRow.add(btnAddEx);
            exBtnRow.add(btnDelEx);
            main.add(exBtnRow);
            main.add(Box.createVerticalStrut(12));

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
            main.add(Box.createVerticalStrut(16));

            // Save / Cancel buttons
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            actions.setOpaque(false);
            ModernButton btnCancel = new ModernButton("Cancel", ModernButton.ButtonStyle.GHOST);
            btnCancel.addActionListener(e -> dispose());
            ModernButton btnSave = new ModernButton("Save Workout", ModernButton.ButtonStyle.PRIMARY);
            btnSave.addActionListener(e -> saveWorkoutData());
            actions.add(btnCancel);
            actions.add(btnSave);
            main.add(actions);

            setContentPane(main);

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
                int est = HealthCalculator.estimateCaloriesBurned("Strength", prefillDurationMinutes, 75.0);
                txtCalories.setText(String.valueOf(est));
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

            // Collect exercises
            List<WorkoutExercise> exercises = new ArrayList<>();
            for (int r = 0; r < exercisesModel.getRowCount(); r++) {
                String exName = exercisesModel.getValueAt(r, 0).toString().trim();
                int sets = Integer.parseInt(exercisesModel.getValueAt(r, 1).toString().trim());
                int reps = Integer.parseInt(exercisesModel.getValueAt(r, 2).toString().trim());
                double weight = Double.parseDouble(exercisesModel.getValueAt(r, 3).toString().trim());
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
