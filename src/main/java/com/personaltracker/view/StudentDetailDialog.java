package com.personaltracker.view;

import com.personaltracker.components.ModernButton;
import com.personaltracker.components.ModernCard;
import com.personaltracker.components.ModernProgressBar;
import com.personaltracker.config.ThemeConfig;
import com.personaltracker.model.*;
import com.personaltracker.util.DateUtil;
import com.personaltracker.util.HealthCalculator;
import com.personaltracker.util.IconFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * High-definition inspection dialog for Personal Trainers to review their students'
 * comprehensive workout history, goal milestones, weight journey, and nutrition logs.
 */
public class StudentDetailDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final StudentProgressDetail detail;

    public StudentDetailDialog(Window owner, StudentProgressDetail detail) {
        super(owner, "Student Progress Inspector - " + (detail.getProfile() != null ? detail.getProfile().getDisplayName() : "Athlete"), ModalityType.APPLICATION_MODAL);
        this.detail = detail;

        setSize(1000, 720);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(ThemeConfig.BG_DARK);
        setLayout(new BorderLayout());

        UserProfile p = detail.getProfile();

        // 1. Top Header Banner
        JPanel headerPanel = createHeaderBanner(p);
        add(headerPanel, BorderLayout.NORTH);

        // 2. Tabbed Content
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(ThemeConfig.BG_DARK);
        tabbedPane.setForeground(ThemeConfig.TEXT_PRIMARY);

        tabbedPane.addTab("Workouts (" + detail.getWorkouts().size() + ")", IconFactory.createIcon(IconFactory.IconType.WORKOUT, 14, ThemeConfig.PRIMARY_ACCENT), createWorkoutsTab());
        tabbedPane.addTab("Fitness Goals (" + detail.getGoals().size() + ")", IconFactory.createIcon(IconFactory.IconType.GOAL, 14, ThemeConfig.PRIMARY_ACCENT), createGoalsTab());
        tabbedPane.addTab("Weight & Measurements (" + detail.getMeasurements().size() + ")", IconFactory.createIcon(IconFactory.IconType.MEASUREMENT, 14, ThemeConfig.PRIMARY_ACCENT), createMeasurementsTab());
        tabbedPane.addTab("Nutrition Entries (" + detail.getNutritionEntries().size() + ")", IconFactory.createIcon(IconFactory.IconType.NUTRITION, 14, ThemeConfig.PRIMARY_ACCENT), createNutritionTab());

        add(tabbedPane, BorderLayout.CENTER);

        // 3. Bottom Close Bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 12));
        bottomBar.setBackground(ThemeConfig.BG_CARD);
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeConfig.BORDER_COLOR));

        ModernButton btnAssign = new ModernButton("➕ Assign Goal or Schedule Routine", ModernButton.ButtonStyle.PRIMARY);
        btnAssign.setPreferredSize(new Dimension(260, 36));
        btnAssign.addActionListener(e -> {
            if (detail.getProfile() != null) {
                AssignTaskDialog dlg = new AssignTaskDialog(StudentDetailDialog.this, detail.getProfile(), null);
                dlg.setVisible(true);
            }
        });
        bottomBar.add(btnAssign);

        ModernButton btnClose = new ModernButton("Close Inspector", ModernButton.ButtonStyle.SECONDARY);
        btnClose.setPreferredSize(new Dimension(140, 36));
        btnClose.addActionListener(e -> dispose());
        bottomBar.add(btnClose);

        add(bottomBar, BorderLayout.SOUTH);
    }

    private JPanel createHeaderBanner(UserProfile p) {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(ThemeConfig.BG_CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeConfig.BORDER_COLOR),
                BorderFactory.createEmptyBorder(16, 24, 16, 24)
        ));

        // Avatar
        String initials = p != null ? getInitials(p.getDisplayName()) : "ST";
        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(36, 36, 36));
                g2.fill(new Ellipse2D.Float(0, 0, 56, 56));
                g2.setColor(ThemeConfig.PRIMARY_ACCENT);
                g2.setStroke(new BasicStroke(2.5f));
                g2.draw(new Ellipse2D.Float(1, 1, 54, 54));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatar.setPreferredSize(new Dimension(56, 56));
        avatar.setOpaque(false);
        avatar.setLayout(new BorderLayout());
        JLabel lblInitials = new JLabel(initials, SwingConstants.CENTER);
        lblInitials.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblInitials.setForeground(Color.WHITE);
        avatar.add(lblInitials, BorderLayout.CENTER);
        header.add(avatar, BorderLayout.WEST);

        // Name, Email, Code
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS));
        namePanel.setOpaque(false);

        JLabel lblName = new JLabel(p != null ? p.getDisplayName() : "Enrolled Student");
        lblName.setFont(ThemeConfig.FONT_TITLE_LARGE);
        lblName.setForeground(ThemeConfig.TEXT_PRIMARY);
        namePanel.add(lblName);

        namePanel.add(Box.createVerticalStrut(4));

        JPanel tagsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tagsRow.setOpaque(false);

        JLabel lblCode = createBadge(p != null && p.getUserCode() != null ? "Code: " + p.getUserCode() : "Code: --", ThemeConfig.PRIMARY_ACCENT);
        tagsRow.add(lblCode);

        String email = p != null && p.getEmail() != null ? p.getEmail() : "No email";
        JLabel lblEmail = createBadge(email, ThemeConfig.TEXT_SECONDARY);
        tagsRow.add(lblEmail);

        namePanel.add(tagsRow);
        header.add(namePanel, BorderLayout.CENTER);

        // Quick Vitals Strip on the Right
        JPanel vitalsStrip = new JPanel(new GridLayout(1, 3, 10, 0));
        vitalsStrip.setOpaque(false);

        double curW = detail.getCurrentWeight();
        double tgtW = p != null && p.getTargetWeightKg() != null ? p.getTargetWeightKg() : 70.0;
        double h = p != null && p.getHeightCm() != null ? p.getHeightCm() : 175.0;

        vitalsStrip.add(createQuickVital("CURRENT WEIGHT", String.format(Locale.US, "%.1f kg", curW), ThemeConfig.PRIMARY_ACCENT));
        vitalsStrip.add(createQuickVital("TARGET GOAL", String.format(Locale.US, "%.1f kg", tgtW), ThemeConfig.SUCCESS));

        double bmi = HealthCalculator.calculateBmi(curW, h);
        String bmiCat = HealthCalculator.getBmiCategory(bmi);
        vitalsStrip.add(createQuickVital("BMI", String.format(Locale.US, "%.1f (%s)", bmi, bmiCat), ThemeConfig.PRIMARY_ACCENT));

        header.add(vitalsStrip, BorderLayout.EAST);
        return header;
    }

    private JPanel createWorkoutsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        String[] cols = {"Date", "Title", "Type", "Duration", "Calories", "Status", "Source / Coach", "Exercises"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        long coachAssignedCount = 0;
        for (Workout w : detail.getWorkouts()) {
            StringBuilder exDesc = new StringBuilder();
            if (w.getExercises() != null && !w.getExercises().isEmpty()) {
                for (int i = 0; i < w.getExercises().size(); i++) {
                    WorkoutExercise ex = w.getExercises().get(i);
                    if (i > 0) exDesc.append(", ");
                    exDesc.append(ex.getExerciseName()).append(" (").append(ex.getSets()).append("x").append(ex.getReps());
                    if (ex.getWeightKg() > 0) exDesc.append(" @ ").append(ex.getWeightKg()).append("kg");
                    exDesc.append(")");
                }
            } else {
                exDesc.append("General workout session");
            }

            String source = "Athlete Logged";
            if (w.getNotes() != null && (w.getNotes().contains("[Coach") || w.getNotes().contains("[Trainer"))) {
                source = "Coach Assigned";
                coachAssignedCount++;
            }

            model.addRow(new Object[]{
                    DateUtil.formatDate(w.getWorkoutDate()),
                    w.getTitle(),
                    w.getWorkoutType(),
                    w.getDurationMinutes() + " mins",
                    w.getCaloriesBurned() + " kcal",
                    w.getStatus(),
                    source,
                    exDesc.toString()
            });
        }

        JTable table = new JTable(model);
        styleTable(table);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        table.getColumnModel().getColumn(0).setPreferredWidth(90);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(85);
        table.getColumnModel().getColumn(3).setPreferredWidth(75);
        table.getColumnModel().getColumn(4).setPreferredWidth(75);
        table.getColumnModel().getColumn(5).setPreferredWidth(85);
        table.getColumnModel().getColumn(6).setPreferredWidth(130);
        table.getColumnModel().getColumn(7).setPreferredWidth(280);

        // Filter toolbar at the top of the workouts tab
        JPanel filterBar = new JPanel(new BorderLayout(8, 0));
        filterBar.setOpaque(false);

        JLabel lblCount = new JLabel(String.format("Logged Workouts: %d total", detail.getWorkouts().size()));
        lblCount.setFont(ThemeConfig.FONT_SMALL);
        lblCount.setForeground(ThemeConfig.TEXT_SECONDARY);
        filterBar.add(lblCount, BorderLayout.WEST);

        JPanel filterBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        filterBtns.setOpaque(false);

        JButton btnAll = new JButton("All Workouts (" + detail.getWorkouts().size() + ")");
        btnAll.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnAll.setForeground(Color.WHITE);
        btnAll.setBackground(ThemeConfig.PRIMARY_ACCENT);
        btnAll.setFocusPainted(false);
        btnAll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.PRIMARY_ACCENT, 1, true),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));

        JButton btnCoachOnly = new JButton("Assigned by Coach (" + coachAssignedCount + ")");
        btnCoachOnly.setIcon(IconFactory.createIcon(IconFactory.IconType.WORKOUT, 12, ThemeConfig.PRIMARY_ACCENT));
        btnCoachOnly.setIconTextGap(6);
        btnCoachOnly.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnCoachOnly.setForeground(ThemeConfig.TEXT_SECONDARY);
        btnCoachOnly.setBackground(ThemeConfig.BG_CARD);
        btnCoachOnly.setFocusPainted(false);
        btnCoachOnly.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));

        btnAll.addActionListener(e -> {
            sorter.setRowFilter(null);
            btnAll.setBackground(ThemeConfig.PRIMARY_ACCENT);
            btnAll.setForeground(Color.WHITE);
            btnCoachOnly.setBackground(ThemeConfig.BG_CARD);
            btnCoachOnly.setForeground(ThemeConfig.TEXT_SECONDARY);
        });

        btnCoachOnly.addActionListener(e -> {
            sorter.setRowFilter(RowFilter.regexFilter("Coach Assigned", 6));
            btnCoachOnly.setBackground(ThemeConfig.PRIMARY_ACCENT);
            btnCoachOnly.setForeground(Color.WHITE);
            btnAll.setBackground(ThemeConfig.BG_CARD);
            btnAll.setForeground(ThemeConfig.TEXT_SECONDARY);
        });

        filterBtns.add(btnAll);
        filterBtns.add(btnCoachOnly);
        filterBar.add(filterBtns, BorderLayout.EAST);
        panel.add(filterBar, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createGoalsTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        if (detail.getGoals().isEmpty()) {
            JLabel lblEmpty = new JLabel("No fitness goals logged by this student yet.");
            lblEmpty.setFont(ThemeConfig.FONT_BODY);
            lblEmpty.setForeground(ThemeConfig.TEXT_MUTED);
            panel.add(lblEmpty);
            return panel;
        }

        for (Goal g : detail.getGoals()) {
            ModernCard card = new ModernCard(12, new Insets(14, 16, 14, 16));
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);

            JLabel title = new JLabel(g.getTitle());
            title.setFont(ThemeConfig.FONT_SUBTITLE);
            title.setForeground(ThemeConfig.TEXT_PRIMARY);
            top.add(title, BorderLayout.WEST);

            Color statusColor = "COMPLETED".equalsIgnoreCase(g.getStatus()) ? ThemeConfig.SUCCESS : ThemeConfig.PRIMARY_ACCENT;
            JLabel badge = createBadge(g.getStatus(), statusColor);
            top.add(badge, BorderLayout.EAST);
            card.add(top);
            card.add(Box.createVerticalStrut(8));

            JPanel statsRow = new JPanel(new BorderLayout());
            statsRow.setOpaque(false);
            JLabel progressText = new JLabel(String.format(Locale.US, "Current: %.1f / %.1f %s", g.getCurrentValue(), g.getTargetValue(), g.getUnit()));
            progressText.setFont(ThemeConfig.FONT_SMALL);
            progressText.setForeground(ThemeConfig.TEXT_SECONDARY);
            statsRow.add(progressText, BorderLayout.WEST);

            JLabel datesText = new JLabel("Target: " + DateUtil.formatDate(g.getTargetDate()));
            datesText.setFont(ThemeConfig.FONT_SMALL);
            datesText.setForeground(ThemeConfig.TEXT_MUTED);
            statsRow.add(datesText, BorderLayout.EAST);
            card.add(statsRow);
            card.add(Box.createVerticalStrut(6));

            ModernProgressBar bar = new ModernProgressBar(0, 100);
            bar.setValue((int) g.getProgressPercentage());
            card.add(bar);

            panel.add(card);
            panel.add(Box.createVerticalStrut(10));
        }

        JScrollPane scroll = new JScrollPane(panel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);

        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);
        outer.add(scroll, BorderLayout.CENTER);
        return outer;
    }

    private JPanel createMeasurementsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        String[] cols = {"Date", "Weight (kg)", "Height (cm)", "BMI", "Waist (cm)", "Chest (cm)", "Arm (cm)"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        for (BodyMeasurement m : detail.getMeasurements()) {
            model.addRow(new Object[]{
                    DateUtil.formatDate(m.getRecordedDate()),
                    String.format(Locale.US, "%.1f", m.getWeightKg()),
                    m.getHeightCm() != null ? String.format(Locale.US, "%.1f", m.getHeightCm()) : "--",
                    m.getBmi() != null ? String.format(Locale.US, "%.1f", m.getBmi()) : "--",
                    m.getWaistCm() != null ? String.format(Locale.US, "%.1f", m.getWaistCm()) : "--",
                    m.getChestCm() != null ? String.format(Locale.US, "%.1f", m.getChestCm()) : "--",
                    m.getArmCm() != null ? String.format(Locale.US, "%.1f", m.getArmCm()) : "--"
            });
        }

        JTable table = new JTable(model);
        styleTable(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createNutritionTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        String[] cols = {"Date", "Meal Type", "Food Item", "Calories (kcal)", "Protein (g)", "Carbs (g)", "Fat (g)"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        for (NutritionEntry n : detail.getNutritionEntries()) {
            model.addRow(new Object[]{
                    DateUtil.formatDate(n.getEntryDate()),
                    n.getMealType(),
                    n.getFoodName(),
                    n.getCalories(),
                    String.format(Locale.US, "%.1f", n.getProteinG()),
                    String.format(Locale.US, "%.1f", n.getCarbsG()),
                    String.format(Locale.US, "%.1f", n.getFatG())
            });
        }

        JTable table = new JTable(model);
        styleTable(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void styleTable(JTable table) {
        table.setBackground(ThemeConfig.BG_CARD);
        table.setForeground(ThemeConfig.TEXT_PRIMARY);
        table.setGridColor(ThemeConfig.BORDER_COLOR);
        table.setRowHeight(32);
        table.setFont(ThemeConfig.FONT_BODY);
        table.getTableHeader().setBackground(ThemeConfig.BG_DARK);
        table.getTableHeader().setForeground(ThemeConfig.TEXT_SECONDARY);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            if (i != 1 && i != 6) { // Except title & exercise description
                table.getColumnModel().getColumn(i).setCellRenderer(center);
            }
        }
    }

    private JPanel createQuickVital(String header, String val, Color color) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setBackground(ThemeConfig.BG_DARK);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        JLabel h = new JLabel(header);
        h.setFont(new Font("Segoe UI", Font.BOLD, 9));
        h.setForeground(ThemeConfig.TEXT_MUTED);
        p.add(h);

        JLabel v = new JLabel(val);
        v.setFont(ThemeConfig.FONT_BODY_BOLD);
        v.setForeground(color);
        p.add(v);
        return p;
    }

    private JLabel createBadge(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(color);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1, true),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        return l;
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "ST";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }
}
