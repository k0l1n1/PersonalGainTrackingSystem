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

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Fitness and Personal Goals management panel.
 */
public class GoalPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final GoalDAO goalDAO = new GoalDAO();
    private final AchievementDAO achievementDAO = new AchievementDAO();

    private final JPanel pnlActiveGoals = new JPanel();
    private final JPanel pnlCompletedGoals = new JPanel();

    // Metric KPI summary labels
    private final JLabel lblKpiActive = new JLabel("0");
    private final JLabel lblKpiCompleted = new JLabel("0");
    private final JLabel lblKpiRate = new JLabel("0%");
    private final JLabel lblKpiNextTarget = new JLabel("--");

    // Section headers & badges
    private final JLabel lblActiveBadge = new JLabel("0 ACTIVE");
    private final JLabel lblDoneBadge = new JLabel("0 COMPLETED");

    private List<Goal> currentGoals = new ArrayList<>();

    public GoalPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(22, 28, 28, 28));

        // 1. Header with Title & Action Button
        content.add(createHeaderPanel());
        content.add(Box.createVerticalStrut(16));

        // 2. KPI Metric Cards Row
        content.add(createKpiCardsPanel());
        content.add(Box.createVerticalStrut(22));

        // 3. Active Goals Section Header
        content.add(createSectionHeader("Active & In-Progress Goals", lblActiveBadge, IconFactory.IconType.GOAL, ThemeConfig.PRIMARY_ACCENT));
        content.add(Box.createVerticalStrut(12));

        pnlActiveGoals.setLayout(new GridLayout(0, 2, 16, 16));
        pnlActiveGoals.setOpaque(false);
        pnlActiveGoals.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(pnlActiveGoals);
        content.add(Box.createVerticalStrut(26));

        // 4. Completed / Overdue Goals Section Header
        content.add(createSectionHeader("Achieved & Past Goals", lblDoneBadge, IconFactory.IconType.CHECK, ThemeConfig.SUCCESS_EMERALD));
        content.add(Box.createVerticalStrut(12));

        pnlCompletedGoals.setLayout(new GridLayout(0, 2, 16, 16));
        pnlCompletedGoals.setOpaque(false);
        pnlCompletedGoals.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(pnlCompletedGoals);

        // Vertical glue consumes any leftover space, preventing vertical component elongation
        content.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("Fitness & Health Goals");
        title.setFont(ThemeConfig.FONT_TITLE_LARGE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(3));

        JLabel subtitle = new JLabel("Set milestones, update current values & track your progress");
        subtitle.setFont(ThemeConfig.FONT_BODY);
        subtitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        titlePanel.add(subtitle);
        header.add(titlePanel, BorderLayout.WEST);

        JPanel btnWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnWrapper.setOpaque(false);
        ModernButton btnNewGoal = new ModernButton("Create New Goal", ModernButton.ButtonStyle.PRIMARY);
        btnNewGoal.setIcon(IconFactory.createIcon(IconFactory.IconType.PLUS, 13, Color.WHITE));
        btnNewGoal.setIconTextGap(8);
        btnNewGoal.setPreferredSize(new Dimension(172, 40));
        btnNewGoal.addActionListener(e -> openGoalDialog(null));
        btnWrapper.add(btnNewGoal);

        header.add(btnWrapper, BorderLayout.EAST);
        return header;
    }

    private JPanel createKpiCardsPanel() {
        JPanel grid = new JPanel(new GridLayout(1, 4, 14, 0));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        grid.add(createMetricCard("Active Goals", lblKpiActive, "In-progress targets", IconFactory.IconType.GOAL, ThemeConfig.PRIMARY_ACCENT));
        grid.add(createMetricCard("Goals Achieved", lblKpiCompleted, "Milestones completed", IconFactory.IconType.CHECK, ThemeConfig.SUCCESS_EMERALD));
        grid.add(createMetricCard("Completion Rate", lblKpiRate, "Success percentage", IconFactory.IconType.FIRE, new Color(0xF5, 0x9E, 0x0B)));
        grid.add(createMetricCard("Nearest Deadline", lblKpiNextTarget, "Next upcoming target", IconFactory.IconType.TIMER, new Color(0x3B, 0x82, 0xF6)));

        return grid;
    }

    private ModernCard createMetricCard(String title, JLabel valLabel, String sub, IconFactory.IconType iconType, Color iconColor) {
        ModernCard card = new ModernCard(12, new Insets(12, 14, 12, 14));
        card.setLayout(new BorderLayout(10, 0));

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 2));
        left.setOpaque(false);
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(ThemeConfig.FONT_SMALL);
        lblTitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        left.add(lblTitle);

        valLabel.setFont(ThemeConfig.FONT_TITLE);
        valLabel.setForeground(ThemeConfig.TEXT_PRIMARY);
        left.add(valLabel);

        card.add(left, BorderLayout.CENTER);

        JLabel icon = new JLabel(IconFactory.createIcon(iconType, 22, iconColor));
        card.add(icon, BorderLayout.EAST);

        return card;
    }

    private JPanel createSectionHeader(String title, JLabel badgeLabel, IconFactory.IconType iconType, Color accentColor) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel icon = new JLabel(IconFactory.createIcon(iconType, 16, accentColor));
        panel.add(icon);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(ThemeConfig.FONT_SUBTITLE);
        lblTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
        panel.add(lblTitle);

        badgeLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        badgeLabel.setForeground(accentColor);
        badgeLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 120), 1, true),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        panel.add(badgeLabel);

        return panel;
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
                    LocalDate nearestDate = null;

                    for (Goal g : currentGoals) {
                        if (g.isCompleted()) {
                            completedCount++;
                            pnlCompletedGoals.add(createGoalCard(g));
                        } else {
                            activeCount++;
                            pnlActiveGoals.add(createGoalCard(g));
                            if (g.getTargetDate() != null) {
                                if (nearestDate == null || g.getTargetDate().isBefore(nearestDate)) {
                                    nearestDate = g.getTargetDate();
                                }
                            }
                        }
                    }

                    // Update KPIs
                    lblKpiActive.setText(String.valueOf(activeCount));
                    lblKpiCompleted.setText(String.valueOf(completedCount));
                    int total = activeCount + completedCount;
                    int rate = total > 0 ? (int) Math.round((completedCount * 100.0) / total) : 0;
                    lblKpiRate.setText(rate + "%");
                    lblKpiNextTarget.setText(nearestDate != null ? DateUtil.formatDate(nearestDate) : "--");

                    // Update Section Badges
                    lblActiveBadge.setText(activeCount + " ACTIVE");
                    lblDoneBadge.setText(completedCount + " COMPLETED");

                    if (activeCount == 0) {
                        pnlActiveGoals.add(createEmptyCard("No active goals right now. Set your next target!"));
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
        ModernCard card = new ModernCard(14, new Insets(16, 20, 16, 20));
        card.setHoverEffect(true);
        card.setLayout(new BorderLayout(0, 12));

        // Top Header: Category Tag & Status Tag
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);

        JLabel catTag = new JLabel(goal.getCategory().toUpperCase());
        catTag.setFont(new Font("Segoe UI", Font.BOLD, 10));
        catTag.setForeground(ThemeConfig.PRIMARY_ACCENT);
        catTag.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xF0, 0x44, 0x44, 120), 1, true),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        top.add(catTag, BorderLayout.WEST);

        JLabel statusTag = new JLabel();
        statusTag.setFont(new Font("Segoe UI", Font.BOLD, 10));
        if (goal.isCompleted()) {
            statusTag.setText("✓ COMPLETED");
            statusTag.setForeground(ThemeConfig.SUCCESS_EMERALD);
            statusTag.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0x10, 0xB9, 0x81, 120), 1, true),
                    BorderFactory.createEmptyBorder(2, 8, 2, 8)
            ));
        } else if (goal.isOverdue()) {
            statusTag.setText("! OVERDUE");
            statusTag.setForeground(ThemeConfig.DANGER_ROSE);
            statusTag.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xF4, 0x3F, 0x5E, 120), 1, true),
                    BorderFactory.createEmptyBorder(2, 8, 2, 8)
            ));
        } else {
            statusTag.setText("● IN PROGRESS");
            statusTag.setForeground(ThemeConfig.WARNING_AMBER);
            statusTag.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xF5, 0x9E, 0x0B, 120), 1, true),
                    BorderFactory.createEmptyBorder(2, 8, 2, 8)
            ));
        }
        top.add(statusTag, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        // Center: Title, Values & Progress
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JLabel titleLbl = new JLabel(goal.getTitle());
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLbl.setForeground(ThemeConfig.TEXT_PRIMARY);
        center.add(titleLbl);
        center.add(Box.createVerticalStrut(10));

        // Values & Progress Row
        JPanel valRow = new JPanel(new BorderLayout());
        valRow.setOpaque(false);

        JLabel vals = new JLabel(String.format("%.1f / %.1f %s", goal.getCurrentValue(), goal.getTargetValue(), goal.getUnit()));
        vals.setFont(ThemeConfig.FONT_BODY_BOLD);
        vals.setForeground(ThemeConfig.TEXT_PRIMARY);
        valRow.add(vals, BorderLayout.WEST);

        JLabel pct = new JLabel(goal.getProgressPercentage() + "%");
        pct.setFont(ThemeConfig.FONT_BODY_BOLD);
        pct.setForeground(goal.isCompleted() ? ThemeConfig.SUCCESS_EMERALD : (goal.isOverdue() ? ThemeConfig.DANGER_ROSE : ThemeConfig.PRIMARY_ACCENT));
        valRow.add(pct, BorderLayout.EAST);
        center.add(valRow);
        center.add(Box.createVerticalStrut(6));

        ModernProgressBar bar = new ModernProgressBar(0, 100);
        bar.setValue(goal.getProgressPercentage());
        bar.setPreferredSize(new Dimension(0, 8));
        if (goal.isCompleted()) {
            bar.setProgressColor(ThemeConfig.SUCCESS_EMERALD);
        } else if (goal.isOverdue()) {
            bar.setProgressColor(ThemeConfig.DANGER_ROSE);
        } else {
            bar.setProgressColor(ThemeConfig.PRIMARY_ACCENT);
        }
        center.add(bar);

        card.add(center, BorderLayout.CENTER);

        // Bottom: Deadline & Action Buttons
        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setOpaque(false);

        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        datePanel.setOpaque(false);
        JLabel iconCal = new JLabel(IconFactory.createIcon(IconFactory.IconType.TIMER, 13, ThemeConfig.TEXT_MUTED));
        datePanel.add(iconCal);
        JLabel dates = new JLabel("Target: " + DateUtil.formatDate(goal.getTargetDate()));
        dates.setFont(ThemeConfig.FONT_SMALL);
        dates.setForeground(ThemeConfig.TEXT_MUTED);
        datePanel.add(dates);
        bottom.add(datePanel, BorderLayout.WEST);

        JPanel btnBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnBox.setOpaque(false);

        ModernButton btnUpdate = new ModernButton("Update", ModernButton.ButtonStyle.SECONDARY);
        btnUpdate.setIcon(IconFactory.createIcon(IconFactory.IconType.PLUS, 11, ThemeConfig.TEXT_PRIMARY));
        btnUpdate.setIconTextGap(4);
        btnUpdate.setFont(ThemeConfig.FONT_SMALL);
        btnUpdate.setPreferredSize(new Dimension(92, 30));
        btnUpdate.addActionListener(e -> updateGoalProgressDialog(goal));

        ModernButton btnEdit = new ModernButton("Edit", ModernButton.ButtonStyle.GHOST);
        btnEdit.setIcon(IconFactory.createIcon(IconFactory.IconType.EDIT, 11, ThemeConfig.TEXT_SECONDARY));
        btnEdit.setIconTextGap(4);
        btnEdit.setFont(ThemeConfig.FONT_SMALL);
        btnEdit.setPreferredSize(new Dimension(74, 30));
        btnEdit.addActionListener(e -> openGoalDialog(goal));

        ModernButton btnDel = new ModernButton("Delete", ModernButton.ButtonStyle.GHOST);
        btnDel.setIcon(IconFactory.createIcon(IconFactory.IconType.TRASH, 11, ThemeConfig.DANGER_ROSE));
        btnDel.setIconTextGap(4);
        btnDel.setFont(ThemeConfig.FONT_SMALL);
        btnDel.setForeground(ThemeConfig.DANGER_ROSE);
        btnDel.setPreferredSize(new Dimension(84, 30));
        btnDel.addActionListener(e -> deleteGoal(goal));

        btnBox.add(btnUpdate);
        btnBox.add(btnEdit);
        btnBox.add(btnDel);
        bottom.add(btnBox, BorderLayout.EAST);

        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private ModernCard createEmptyCard(String text) {
        ModernCard card = new ModernCard(14, new Insets(24, 24, 24, 24));
        card.setLayout(new BorderLayout());

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JLabel icon = new JLabel(IconFactory.createIcon(IconFactory.IconType.GOAL, 28, ThemeConfig.TEXT_MUTED));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(icon);
        center.add(Box.createVerticalStrut(10));

        JLabel lbl = new JLabel(text);
        lbl.setFont(ThemeConfig.FONT_BODY);
        lbl.setForeground(ThemeConfig.TEXT_MUTED);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(lbl);
        center.add(Box.createVerticalStrut(12));

        ModernButton btn = new ModernButton("Set New Goal", ModernButton.ButtonStyle.PRIMARY);
        btn.setIcon(IconFactory.createIcon(IconFactory.IconType.PLUS, 12, Color.WHITE));
        btn.setIconTextGap(6);
        btn.setFont(ThemeConfig.FONT_SMALL);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setPreferredSize(new Dimension(145, 34));
        btn.setMaximumSize(new Dimension(145, 34));
        btn.addActionListener(e -> openGoalDialog(null));
        center.add(btn);

        card.add(center, BorderLayout.CENTER);
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
                        if (goal.isCompleted()) {
                            ToastNotification.showToast(GoalPanel.this, "Goal Completed! Incredible progress!", ToastNotification.ToastType.SUCCESS);
                        } else {
                            ToastNotification.showToast(GoalPanel.this, "Goal progress updated!", ToastNotification.ToastType.SUCCESS);
                        }
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
                    try {
                        get();
                        loadGoals();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(GoalPanel.this, "Failed to delete goal: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    // Modal Goal Dialog
    private class GoalDialog extends JDialog {
        private static final long serialVersionUID = 1L;
        private boolean saved = false;
        private final Goal editing;

        private final ModernTextField txtTitle = new ModernTextField("Bench Press 100kg");
        private final JComboBox<String> cmbCat = new JComboBox<>(new String[]{"Weight", "Workout Frequency", "Strength", "Endurance", "Nutrition", "Habit"});
        private final JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"IN_PROGRESS", "COMPLETED", "OVERDUE", "CANCELLED"});
        private final ModernTextField txtTarget = new ModernTextField("100.0");
        private final ModernTextField txtCurrent = new ModernTextField("80.0");
        private final ModernTextField txtUnit = new ModernTextField("kg");
        private final ModernTextField txtTargetDate = new ModernTextField(LocalDate.now().plusMonths(1).toString());
        private final JLabel lblLiveProgress = new JLabel("Progress: 80%");
        private final ModernProgressBar prgPreview = new ModernProgressBar(0, 100);

        public GoalDialog(Window owner, Goal goal) {
            super(owner, goal == null ? "Create Fitness Goal" : "Edit Goal", ModalityType.APPLICATION_MODAL);
            this.editing = goal;
            setSize(490, 640);
            setLocationRelativeTo(owner);
            setResizable(false);
            getContentPane().setBackground(ThemeConfig.BG_DARK);

            JPanel main = new JPanel();
            main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
            main.setOpaque(false);
            main.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

            // Section 1: Quick Preset Chips (Only if creating new)
            if (goal == null) {
                JLabel lblPresets = new JLabel("Quick Goal Presets");
                lblPresets.setFont(ThemeConfig.FONT_SMALL);
                lblPresets.setForeground(ThemeConfig.TEXT_SECONDARY);
                main.add(lblPresets);
                main.add(Box.createVerticalStrut(4));

                JPanel presetRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
                presetRow.setOpaque(false);
                presetRow.add(createGoalPresetChip("Weight Loss", "Reach Target Weight", "Weight", "kg", 70.0, 75.0, 60));
                presetRow.add(createGoalPresetChip("Bench 100kg", "Bench Press 100kg", "Strength", "kg", 100.0, 80.0, 90));
                presetRow.add(createGoalPresetChip("20 Workouts", "Log 20 Workouts in a Month", "Workout Frequency", "workouts", 20.0, 0.0, 30));
                presetRow.add(createGoalPresetChip("10K Run", "Run 10K Distance", "Endurance", "km", 10.0, 5.0, 60));
                main.add(presetRow);
                main.add(Box.createVerticalStrut(12));
            }

            // Title
            main.add(createFieldRow("Goal Title *", txtTitle));
            main.add(Box.createVerticalStrut(10));

            // Category, Unit & Status
            JPanel r2 = new JPanel(new GridLayout(1, 3, 10, 0));
            r2.setOpaque(false);
            r2.add(createFieldRow("Category", cmbCat));
            r2.add(createFieldRow("Unit", txtUnit));
            r2.add(createFieldRow("Status", cmbStatus));
            main.add(r2);
            main.add(Box.createVerticalStrut(10));

            // Target & Current Values
            JPanel r3 = new JPanel(new GridLayout(1, 2, 10, 0));
            r3.setOpaque(false);
            r3.add(createFieldRow("Target Value *", txtTarget));
            r3.add(createFieldRow("Current Value *", txtCurrent));
            main.add(r3);
            main.add(Box.createVerticalStrut(10));

            // Live Progress Preview Box
            JPanel progressBox = new JPanel(new BorderLayout(8, 4));
            progressBox.setBackground(ThemeConfig.BG_CARD);
            progressBox.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
            JLabel lblPrgTag = new JLabel("LIVE PROGRESS ESTIMATE");
            lblPrgTag.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lblPrgTag.setForeground(ThemeConfig.TEXT_SECONDARY);
            lblLiveProgress.setFont(ThemeConfig.FONT_SMALL);
            lblLiveProgress.setForeground(ThemeConfig.SUCCESS_EMERALD);
            progressBox.add(lblPrgTag, BorderLayout.NORTH);
            progressBox.add(prgPreview, BorderLayout.CENTER);
            progressBox.add(lblLiveProgress, BorderLayout.SOUTH);
            main.add(progressBox);
            main.add(Box.createVerticalStrut(12));

            // Deadline with Quick Date Chips
            JPanel dateSection = new JPanel();
            dateSection.setLayout(new BoxLayout(dateSection, BoxLayout.Y_AXIS));
            dateSection.setOpaque(false);
            JLabel lblDateTag = new JLabel("Target Deadline");
            lblDateTag.setFont(ThemeConfig.FONT_SMALL);
            lblDateTag.setForeground(ThemeConfig.TEXT_SECONDARY);
            dateSection.add(lblDateTag);
            dateSection.add(Box.createVerticalStrut(4));

            JPanel datePills = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            datePills.setOpaque(false);
            datePills.add(createDeadlinePill("+30 Days", 30));
            datePills.add(createDeadlinePill("+60 Days", 60));
            datePills.add(createDeadlinePill("+90 Days", 90));
            datePills.add(createDeadlinePill("+6 Months", 180));
            txtTargetDate.setPreferredSize(new Dimension(120, 30));
            datePills.add(txtTargetDate);
            dateSection.add(datePills);
            main.add(dateSection);

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
            ModernButton btnSave = new ModernButton("Save Goal", ModernButton.ButtonStyle.PRIMARY);
            btnSave.addActionListener(e -> saveGoalData());
            btnRow.add(btnCancel);
            btnRow.add(btnSave);
            actions.add(btnRow, BorderLayout.EAST);
            main.add(actions);

            setContentPane(main);

            // Document listeners for real-time progress preview
            DocumentListener prgListener = new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { updateProgressPreview(); }
                @Override public void removeUpdate(DocumentEvent e) { updateProgressPreview(); }
                @Override public void changedUpdate(DocumentEvent e) { updateProgressPreview(); }
            };
            txtTarget.getDocument().addDocumentListener(prgListener);
            txtCurrent.getDocument().addDocumentListener(prgListener);

            // Shortcuts
            txtTitle.addActionListener(e -> saveGoalData());
            txtUnit.addActionListener(e -> saveGoalData());
            txtTarget.addActionListener(e -> saveGoalData());
            txtCurrent.addActionListener(e -> saveGoalData());
            txtTargetDate.addActionListener(e -> saveGoalData());

            getRootPane().registerKeyboardAction(e -> dispose(),
                    KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);

            if (goal != null) {
                txtTitle.setText(goal.getTitle());
                cmbCat.setSelectedItem(goal.getCategory());
                txtTarget.setText(String.valueOf(goal.getTargetValue()));
                txtCurrent.setText(String.valueOf(goal.getCurrentValue()));
                txtUnit.setText(goal.getUnit());
                txtTargetDate.setText(goal.getTargetDate() != null ? goal.getTargetDate().toString() : "");
                cmbStatus.setSelectedItem(goal.getStatus());
                updateProgressPreview();
            } else {
                updateProgressPreview();
            }

            SwingUtilities.invokeLater(() -> {
                txtTitle.requestFocusInWindow();
                txtTitle.selectAll();
            });
        }

        private JButton createGoalPresetChip(String label, String title, String category, String unit, double target, double current, int days) {
            JButton btn = new JButton(label);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btn.setForeground(ThemeConfig.TEXT_PRIMARY);
            btn.setBackground(ThemeConfig.BG_CARD);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));
            btn.addActionListener(e -> {
                txtTitle.setText(title);
                cmbCat.setSelectedItem(category);
                txtUnit.setText(unit);
                txtTarget.setText(String.valueOf(target));
                txtCurrent.setText(String.valueOf(current));
                txtTargetDate.setText(LocalDate.now().plusDays(days).toString());
                updateProgressPreview();
            });
            return btn;
        }

        private JButton createDeadlinePill(String text, int days) {
            JButton btn = new JButton(text);
            btn.setFont(ThemeConfig.FONT_SMALL);
            btn.setForeground(ThemeConfig.TEXT_PRIMARY);
            btn.setBackground(ThemeConfig.BG_CARD);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));
            btn.addActionListener(e -> txtTargetDate.setText(LocalDate.now().plusDays(days).toString()));
            return btn;
        }

        private void updateProgressPreview() {
            try {
                double target = Double.parseDouble(txtTarget.getText().trim());
                double current = Double.parseDouble(txtCurrent.getText().trim());
                if (target > 0) {
                    int pct = (int) Math.min(100, Math.max(0, (current / target) * 100));
                    prgPreview.setValue(pct);
                    if (current >= target) {
                        lblLiveProgress.setText(String.format("Goal Reached! 100%% complete (%.1f / %.1f %s)", current, target, txtUnit.getText().trim()));
                        cmbStatus.setSelectedItem("COMPLETED");
                    } else {
                        lblLiveProgress.setText(String.format("%.1f%% complete (%.1f / %.1f %s)", (current / target) * 100, current, target, txtUnit.getText().trim()));
                        if ("COMPLETED".equals(cmbStatus.getSelectedItem())) {
                            cmbStatus.setSelectedItem("IN_PROGRESS");
                        }
                    }
                    return;
                }
            } catch (Exception ignored) {}
            prgPreview.setValue(0);
            lblLiveProgress.setText("Progress: --");
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
            String selectedSt = (String) cmbStatus.getSelectedItem();
            g.setStatus(selectedSt != null ? selectedSt : "IN_PROGRESS");
            g.checkStatus();

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
                    try {
                        get();
                        saved = true;
                        ToastNotification.showToast(GoalPanel.this, editing == null ? "Goal created!" : "Goal updated!", ToastNotification.ToastType.SUCCESS);
                        dispose();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(GoalDialog.this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
