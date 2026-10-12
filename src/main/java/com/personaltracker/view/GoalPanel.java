package com.personaltracker.view;

import com.personaltracker.components.ModernButton;
import com.personaltracker.components.ModernCard;
import com.personaltracker.components.ModernProgressBar;
import com.personaltracker.components.ModernTextField;
import com.personaltracker.components.ToastNotification;
import com.personaltracker.config.ThemeConfig;
import com.personaltracker.dao.AchievementDAO;
import com.personaltracker.dao.GoalDAO;
import com.personaltracker.model.Goal;
import com.personaltracker.util.DateUtil;
import com.personaltracker.util.IconFactory;
import com.personaltracker.util.ValidationUtil;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Fitness and Personal Goals management panel.
 */
public class GoalPanel extends JPanel {

    private final GoalDAO goalDAO = new GoalDAO();
    private final AchievementDAO achievementDAO = new AchievementDAO();

    private final JPanel pnlActiveGoals = new JPanel();
    private final JPanel pnlCompletedGoals = new JPanel();
    private final JLabel lblStatsSummary = new JLabel("Tracking goals...");

    private List<Goal> currentGoals = new ArrayList<>();

    public GoalPanel() {
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
        JLabel title = new JLabel("Fitness & Health Goals");
        title.setFont(ThemeConfig.FONT_TITLE_LARGE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        titlePanel.add(title);

        JLabel subtitle = new JLabel("Set milestones, update current values & track progress");
        subtitle.setFont(ThemeConfig.FONT_BODY);
        subtitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        titlePanel.add(subtitle);
        header.add(titlePanel, BorderLayout.WEST);

        ModernButton btnNewGoal = new ModernButton("+ Create New Goal", ModernButton.ButtonStyle.PRIMARY);
        btnNewGoal.addActionListener(e -> openGoalDialog(null));
        header.add(btnNewGoal, BorderLayout.EAST);
        content.add(header);
        content.add(Box.createVerticalStrut(18));

        // Active Goals Section
        JLabel lblActiveSection = new JLabel("Active & In-Progress Goals");
        lblActiveSection.setFont(ThemeConfig.FONT_SUBTITLE);
        lblActiveSection.setForeground(ThemeConfig.TEXT_PRIMARY);
        content.add(lblActiveSection);
        content.add(Box.createVerticalStrut(10));

        pnlActiveGoals.setLayout(new GridLayout(0, 2, 16, 16));
        pnlActiveGoals.setOpaque(false);
        content.add(pnlActiveGoals);
        content.add(Box.createVerticalStrut(24));

        // Completed / Overdue Goals Section
        JLabel lblDoneSection = new JLabel("Achieved & Past Goals");
        lblDoneSection.setFont(ThemeConfig.FONT_SUBTITLE);
        lblDoneSection.setForeground(ThemeConfig.TEXT_SECONDARY);
        content.add(lblDoneSection);
        content.add(Box.createVerticalStrut(10));

        pnlCompletedGoals.setLayout(new GridLayout(0, 2, 16, 16));
        pnlCompletedGoals.setOpaque(false);
        content.add(pnlCompletedGoals);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void loadGoals() {
        SwingWorker<List<Goal>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Goal> doInBackground() throws Exception {
                return goalDAO.getAllGoals();
            }

            @Override
            protected void done() {
                try {
                    currentGoals = get();
                    pnlActiveGoals.removeAll();
                    pnlCompletedGoals.removeAll();

                    int activeCount = 0;
                    int completedCount = 0;

                    for (Goal g : currentGoals) {
                        if (g.isCompleted()) {
                            completedCount++;
                            pnlCompletedGoals.add(createGoalCard(g));
                        } else {
                            activeCount++;
                            pnlActiveGoals.add(createGoalCard(g));
                        }
                    }

                    if (activeCount == 0) {
                        pnlActiveGoals.add(createEmptyCard("No active goals. Click '+ Create New Goal' above!"));
                    }
                    if (completedCount == 0) {
                        pnlCompletedGoals.add(createEmptyCard("No completed goals yet. Keep working towards your milestones!"));
                    }

                    pnlActiveGoals.revalidate();
                    pnlActiveGoals.repaint();
                    pnlCompletedGoals.revalidate();
                    pnlCompletedGoals.repaint();

                } catch (Exception e) {
                    System.err.println("Failed to load goals: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private ModernCard createGoalCard(Goal goal) {
        ModernCard card = new ModernCard(14, new Insets(16, 18, 16, 18));
        card.setHoverEffect(true);

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);

        JLabel catTag = new JLabel(" " + goal.getCategory().toUpperCase() + " ");
        catTag.setFont(new Font("Segoe UI", Font.BOLD, 10));
        catTag.setOpaque(true);
        catTag.setBackground(new Color(59, 130, 246, 30));
        catTag.setForeground(ThemeConfig.PRIMARY_BLUE);
        top.add(catTag, BorderLayout.WEST);

        JLabel statusTag = new JLabel(goal.getStatus());
        statusTag.setFont(ThemeConfig.FONT_SMALL);
        if (goal.isCompleted()) {
            statusTag.setForeground(ThemeConfig.SUCCESS_EMERALD);
        } else if (goal.isOverdue()) {
            statusTag.setForeground(ThemeConfig.DANGER_ROSE);
        } else {
            statusTag.setForeground(ThemeConfig.WARNING_AMBER);
        }
        top.add(statusTag, BorderLayout.EAST);

        card.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(12, 0, 10, 0));

        JLabel titleLbl = new JLabel(goal.getTitle());
        titleLbl.setFont(ThemeConfig.FONT_SUBTITLE);
        titleLbl.setForeground(ThemeConfig.TEXT_PRIMARY);
        center.add(titleLbl);
        center.add(Box.createVerticalStrut(8));

        // Values & Progress Bar
        JPanel valRow = new JPanel(new BorderLayout());
        valRow.setOpaque(false);

        JLabel vals = new JLabel(String.format("%.1f / %.1f %s", goal.getCurrentValue(), goal.getTargetValue(), goal.getUnit()));
        vals.setFont(ThemeConfig.FONT_BODY_BOLD);
        vals.setForeground(ThemeConfig.TEXT_PRIMARY);
        valRow.add(vals, BorderLayout.WEST);

        JLabel pct = new JLabel(goal.getProgressPercentage() + "%");
        pct.setFont(ThemeConfig.FONT_BODY_BOLD);
        pct.setForeground(goal.isCompleted() ? ThemeConfig.SUCCESS_EMERALD : ThemeConfig.PRIMARY_BLUE);
        valRow.add(pct, BorderLayout.EAST);
        center.add(valRow);
        center.add(Box.createVerticalStrut(6));

        ModernProgressBar bar = new ModernProgressBar(0, 100);
        bar.setValue(goal.getProgressPercentage());
        if (goal.isCompleted()) {
            bar.setProgressColor(ThemeConfig.SUCCESS_EMERALD);
        }
        center.add(bar);
        center.add(Box.createVerticalStrut(8));

        JLabel dates = new JLabel("Target deadline: " + DateUtil.formatDate(goal.getTargetDate()));
        dates.setFont(ThemeConfig.FONT_SMALL);
        dates.setForeground(ThemeConfig.TEXT_MUTED);
        center.add(dates);

        card.add(center, BorderLayout.CENTER);

        // Bottom action buttons
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        bottom.setOpaque(false);

        ModernButton btnUpdate = new ModernButton("+ Update Value", ModernButton.ButtonStyle.SECONDARY);
        btnUpdate.setFont(ThemeConfig.FONT_SMALL);
        btnUpdate.addActionListener(e -> updateGoalProgressDialog(goal));

        ModernButton btnEdit = new ModernButton("Edit", ModernButton.ButtonStyle.GHOST);
        btnEdit.setFont(ThemeConfig.FONT_SMALL);
        btnEdit.addActionListener(e -> openGoalDialog(goal));

        ModernButton btnDel = new ModernButton("Delete", ModernButton.ButtonStyle.GHOST);
        btnDel.setFont(ThemeConfig.FONT_SMALL);
        btnDel.setForeground(ThemeConfig.DANGER_ROSE);
        btnDel.addActionListener(e -> deleteGoal(goal));

        bottom.add(btnUpdate);
        bottom.add(btnEdit);
        bottom.add(btnDel);
        card.add(bottom, BorderLayout.SOUTH);

        return card;
    }

    private ModernCard createEmptyCard(String text) {
        ModernCard card = new ModernCard(14, new Insets(24, 20, 24, 20));
        JLabel lbl = new JLabel(text);
        lbl.setFont(ThemeConfig.FONT_BODY);
        lbl.setForeground(ThemeConfig.TEXT_MUTED);
        card.add(lbl, BorderLayout.CENTER);
        return card;
    }

    private void updateGoalProgressDialog(Goal goal) {
        String input = JOptionPane.showInputDialog(this,
                "Enter new current value for \"" + goal.getTitle() + "\" (" + goal.getUnit() + "):",
                goal.getCurrentValue());

        if (input != null && !input.trim().isEmpty()) {
            try {
                double newVal = Double.parseDouble(input.trim());
                if (newVal < 0) throw new NumberFormatException();

                goal.setCurrentValue(newVal);

                SwingWorker<Void, Void> worker = new SwingWorker<>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        goalDAO.updateGoal(goal);
                        if (goal.isCompleted()) {
                            achievementDAO.unlockAchievement("FIRST_GOAL");
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        ToastNotification.showToast(GoalPanel.this, "Goal progress updated!", ToastNotification.ToastType.SUCCESS);
                        loadGoals();
                    }
                };
                worker.execute();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid non-negative number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openGoalDialog(Goal editing) {
        GoalDialog dialog = new GoalDialog(SwingUtilities.getWindowAncestor(this), editing);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadGoals();
        }
    }

    private void deleteGoal(Goal goal) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete goal \"" + goal.getTitle() + "\"?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    goalDAO.deleteGoal(goal.getId());
                    return null;
                }

                @Override
                protected void done() {
                    loadGoals();
                }
            };
            worker.execute();
        }
    }

    // Modal Goal Dialog
    private class GoalDialog extends JDialog {
        private boolean saved = false;
        private final Goal editing;

        private final ModernTextField txtTitle = new ModernTextField("Bench Press 100kg");
        private final JComboBox<String> cmbCat = new JComboBox<>(new String[]{"Weight", "Workout Frequency", "Strength", "Endurance", "Nutrition", "Habit"});
        private final ModernTextField txtTarget = new ModernTextField("100.0");
        private final ModernTextField txtCurrent = new ModernTextField("80.0");
        private final ModernTextField txtUnit = new ModernTextField("kg");
        private final ModernTextField txtTargetDate = new ModernTextField(LocalDate.now().plusMonths(1).toString());

        public GoalDialog(Window owner, Goal goal) {
            super(owner, goal == null ? "Create Fitness Goal" : "Edit Goal", ModalityType.APPLICATION_MODAL);
            this.editing = goal;
            setSize(440, 480);
            setLocationRelativeTo(owner);
            setResizable(false);
            getContentPane().setBackground(ThemeConfig.BG_DARK);

            JPanel main = new JPanel();
            main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
            main.setOpaque(false);
            main.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

            main.add(createFieldRow("Goal Title", txtTitle));
            main.add(Box.createVerticalStrut(10));

            JPanel r2 = new JPanel(new GridLayout(1, 2, 10, 0));
            r2.setOpaque(false);
            r2.add(createFieldRow("Category", cmbCat));
            r2.add(createFieldRow("Unit (kg, workouts, etc)", txtUnit));
            main.add(r2);
            main.add(Box.createVerticalStrut(10));

            JPanel r3 = new JPanel(new GridLayout(1, 2, 10, 0));
            r3.setOpaque(false);
            r3.add(createFieldRow("Target Value", txtTarget));
            r3.add(createFieldRow("Current Value", txtCurrent));
            main.add(r3);
            main.add(Box.createVerticalStrut(10));

            main.add(createFieldRow("Target Deadline (YYYY-MM-DD)", txtTargetDate));
            main.add(Box.createVerticalStrut(20));

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            actions.setOpaque(false);
            ModernButton btnCancel = new ModernButton("Cancel", ModernButton.ButtonStyle.GHOST);
            btnCancel.addActionListener(e -> dispose());
            ModernButton btnSave = new ModernButton("Save Goal", ModernButton.ButtonStyle.PRIMARY);
            btnSave.addActionListener(e -> saveGoalData());
            actions.add(btnCancel);
            actions.add(btnSave);
            main.add(actions);

            setContentPane(main);

            if (goal != null) {
                txtTitle.setText(goal.getTitle());
                cmbCat.setSelectedItem(goal.getCategory());
                txtTarget.setText(String.valueOf(goal.getTargetValue()));
                txtCurrent.setText(String.valueOf(goal.getCurrentValue()));
                txtUnit.setText(goal.getUnit());
                txtTargetDate.setText(goal.getTargetDate() != null ? goal.getTargetDate().toString() : "");
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

        private void saveGoalData() {
            String title = txtTitle.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a goal title.");
                return;
            }

            double targetVal;
            double currentVal;
            try {
                targetVal = Double.parseDouble(txtTarget.getText().trim());
                currentVal = Double.parseDouble(txtCurrent.getText().trim());
                if (targetVal <= 0 || currentVal < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Target must be > 0 and Current must be >= 0.");
                return;
            }

            LocalDate deadline = DateUtil.parseDateOrNull(txtTargetDate.getText().trim());
            if (deadline == null) {
                JOptionPane.showMessageDialog(this, "Please enter a valid target date (YYYY-MM-DD).");
                return;
            }

            Goal g = editing != null ? editing : new Goal();
            g.setTitle(title);
            g.setCategory((String) cmbCat.getSelectedItem());
            g.setTargetValue(targetVal);
            g.setCurrentValue(currentVal);
            g.setUnit(txtUnit.getText().trim());
            g.setTargetDate(deadline);

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    if (editing == null) {
                        goalDAO.saveGoal(g);
                    } else {
                        goalDAO.updateGoal(g);
                    }
                    if (g.isCompleted()) {
                        achievementDAO.unlockAchievement("FIRST_GOAL");
                    }
                    return null;
                }

                @Override
                protected void done() {
                    saved = true;
                    dispose();
                }
            };
            worker.execute();
        }

        public boolean isSaved() {
            return saved;
        }
    }
}
