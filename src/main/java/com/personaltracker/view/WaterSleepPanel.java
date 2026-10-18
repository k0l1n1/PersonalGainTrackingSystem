package com.personaltracker.view;

import com.personaltracker.components.ModernButton;
import com.personaltracker.components.ModernCard;
import com.personaltracker.components.ModernProgressBar;
import com.personaltracker.components.ModernTextField;
import com.personaltracker.components.ToastNotification;
import com.personaltracker.config.ThemeConfig;
import com.personaltracker.dao.AchievementDAO;
import com.personaltracker.dao.SleepDAO;
import com.personaltracker.dao.WaterDAO;
import com.personaltracker.model.SleepRecord;
import com.personaltracker.model.UserSession;
import com.personaltracker.model.WaterIntake;
import com.personaltracker.service.SupabaseClient;
import com.personaltracker.util.DateUtil;
import com.personaltracker.util.IconFactory;
import com.personaltracker.util.ValidationUtil;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Water Hydration and Sleep Tracking panel.
 */
public class WaterSleepPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final WaterDAO waterDAO = new WaterDAO();
    private final SleepDAO sleepDAO = new SleepDAO();
    private final AchievementDAO achievementDAO = new AchievementDAO();

    // Water state
    private final JLabel lblWaterTotal = new JLabel("0 ml");
    private final JLabel lblWaterTarget = new JLabel("Target: 2,500 ml (0%)");
    private final ModernProgressBar barWater = new ModernProgressBar(0, 2500);
    private final JPanel pnlWaterLogs = new JPanel();

    // Sleep state
    private final JLabel lblSleepDuration = new JLabel("No sleep logged");
    private final JLabel lblSleepTimes = new JLabel("Bedtime: -- | Wake: --");
    private final JLabel lblSleepQuality = new JLabel("Quality: --");
    private final JPanel pnlSleepChart = new JPanel(new BorderLayout());

    private List<WaterIntake> todayWater = new ArrayList<>();
    private List<SleepRecord> sleepRecords = new ArrayList<>();

    public WaterSleepPanel() {
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
        JLabel title = new JLabel("Hydration & Sleep Tracking");
        title.setFont(ThemeConfig.FONT_TITLE_LARGE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        titlePanel.add(title);

        JLabel subtitle = new JLabel("Monitor daily water consumption and sleep recovery trends");
        subtitle.setFont(ThemeConfig.FONT_BODY);
        subtitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        titlePanel.add(subtitle);
        header.add(titlePanel, BorderLayout.WEST);

        content.add(header);
        content.add(Box.createVerticalStrut(20));

        // Two Column Main Grid (Water on Left, Sleep on Right)
        JPanel mainGrid = new JPanel(new GridLayout(1, 2, 20, 0));
        mainGrid.setOpaque(false);

        // 1. Water Tracking Section
        mainGrid.add(createWaterSection());

        // 2. Sleep Tracking Section
        mainGrid.add(createSleepSection());

        content.add(mainGrid);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private ModernCard createWaterSection() {
        ModernCard card = new ModernCard(14, new Insets(20, 20, 20, 20));

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        JLabel head = new JLabel("Daily Hydration (Water)");
        head.setFont(ThemeConfig.FONT_TITLE);
        head.setForeground(ThemeConfig.TEXT_PRIMARY);
        top.add(head, BorderLayout.WEST);

        JLabel icon = new JLabel(IconFactory.createIcon(IconFactory.IconType.WATER, 24, ThemeConfig.PRIMARY_BLUE));
        top.add(icon, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(16, 0, 16, 0));

        lblWaterTotal.setFont(ThemeConfig.FONT_STAT_NUM);
        lblWaterTotal.setForeground(ThemeConfig.PRIMARY_ACCENT);
        center.add(lblWaterTotal);
        center.add(Box.createVerticalStrut(8));

        barWater.setColors(ThemeConfig.PRIMARY_ACCENT, ThemeConfig.ACCENT_HOVER);
        barWater.setPreferredSize(new Dimension(0, 14));
        center.add(barWater);
        center.add(Box.createVerticalStrut(6));

        lblWaterTarget.setFont(ThemeConfig.FONT_BODY);
        lblWaterTarget.setForeground(ThemeConfig.TEXT_MUTED);
        center.add(lblWaterTarget);
        center.add(Box.createVerticalStrut(20));

        // Quick add buttons
        JLabel lblQuick = new JLabel("Quick Add Intake");
        lblQuick.setFont(ThemeConfig.FONT_SUBTITLE);
        lblQuick.setForeground(ThemeConfig.TEXT_SECONDARY);
        center.add(lblQuick);
        center.add(Box.createVerticalStrut(8));

        JPanel btnRow = new JPanel(new GridLayout(2, 2, 8, 8));
        btnRow.setOpaque(false);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        ModernButton b250 = new ModernButton("+ 250 ml (Glass)", ModernButton.ButtonStyle.SECONDARY);
        b250.addActionListener(e -> logWater(250));

        ModernButton b500 = new ModernButton("+ 500 ml (Bottle)", ModernButton.ButtonStyle.SECONDARY);
        b500.addActionListener(e -> logWater(500));

        ModernButton b750 = new ModernButton("+ 750 ml (Shaker)", ModernButton.ButtonStyle.SECONDARY);
        b750.addActionListener(e -> logWater(750));

        ModernButton bCustom = new ModernButton("+ Custom Amount", ModernButton.ButtonStyle.PRIMARY);
        bCustom.addActionListener(e -> promptCustomWater());

        btnRow.add(b250);
        btnRow.add(b500);
        btnRow.add(b750);
        btnRow.add(bCustom);
        center.add(btnRow);
        center.add(Box.createVerticalStrut(20));

        // History logs
        JLabel lblLog = new JLabel("Today's Water Entries");
        lblLog.setFont(ThemeConfig.FONT_SUBTITLE);
        lblLog.setForeground(ThemeConfig.TEXT_SECONDARY);
        center.add(lblLog);
        center.add(Box.createVerticalStrut(8));

        pnlWaterLogs.setLayout(new BoxLayout(pnlWaterLogs, BoxLayout.Y_AXIS));
        pnlWaterLogs.setOpaque(false);
        JScrollPane scrollLogs = new JScrollPane(pnlWaterLogs);
        scrollLogs.setOpaque(false);
        scrollLogs.getViewport().setOpaque(false);
        scrollLogs.setBorder(null);
        scrollLogs.setPreferredSize(new Dimension(0, 160));
        center.add(scrollLogs);

        card.add(center, BorderLayout.CENTER);
        return card;
    }

    private ModernCard createSleepSection() {
        ModernCard card = new ModernCard(14, new Insets(20, 20, 20, 20));

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        JLabel head = new JLabel("Sleep & Recovery");
        head.setFont(ThemeConfig.FONT_TITLE);
        head.setForeground(ThemeConfig.TEXT_PRIMARY);
        top.add(head, BorderLayout.WEST);

        ModernButton btnLogSleep = new ModernButton("+ Log Sleep", ModernButton.ButtonStyle.PRIMARY);
        btnLogSleep.addActionListener(e -> openSleepDialog());
        top.add(btnLogSleep, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(16, 0, 16, 0));

        // Latest summary card
        ModernCard innerSummary = new ModernCard(10, new Insets(14, 16, 14, 16));
        innerSummary.setBackgroundColor(ThemeConfig.BG_INPUT);

        JPanel sumBox = new JPanel();
        sumBox.setLayout(new BoxLayout(sumBox, BoxLayout.Y_AXIS));
        sumBox.setOpaque(false);

        JLabel lblSumTitle = new JLabel("LAST NIGHT'S SLEEP");
        lblSumTitle.setFont(ThemeConfig.FONT_SMALL);
        lblSumTitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        sumBox.add(lblSumTitle);
        sumBox.add(Box.createVerticalStrut(4));

        lblSleepDuration.setFont(ThemeConfig.FONT_STAT_NUM);
        lblSleepDuration.setForeground(ThemeConfig.PRIMARY_ACCENT);
        sumBox.add(lblSleepDuration);
        sumBox.add(Box.createVerticalStrut(4));

        lblSleepTimes.setFont(ThemeConfig.FONT_BODY);
        lblSleepTimes.setForeground(ThemeConfig.TEXT_PRIMARY);
        sumBox.add(lblSleepTimes);
        sumBox.add(Box.createVerticalStrut(4));

        lblSleepQuality.setFont(ThemeConfig.FONT_SMALL);
        lblSleepQuality.setForeground(ThemeConfig.SUCCESS_EMERALD);
        sumBox.add(lblSleepQuality);

        innerSummary.add(sumBox, BorderLayout.CENTER);
        center.add(innerSummary);
        center.add(Box.createVerticalStrut(18));

        // Weekly sleep trend chart
        JLabel lblChartTitle = new JLabel("Weekly Sleep Duration (Hours)");
        lblChartTitle.setFont(ThemeConfig.FONT_SUBTITLE);
        lblChartTitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        center.add(lblChartTitle);
        center.add(Box.createVerticalStrut(8));

        pnlSleepChart.setOpaque(false);
        pnlSleepChart.setPreferredSize(new Dimension(0, 200));
        center.add(pnlSleepChart);

        card.add(center, BorderLayout.CENTER);
        return card;
    }

    public void loadData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                todayWater = waterDAO.getEntriesForDate(LocalDate.now());
                sleepRecords = sleepDAO.getAllRecords();
                return null;
            }

            @Override
            protected void done() {
                updateWaterView();
                updateSleepView();
            }
        };
        worker.execute();
    }

    private void updateWaterView() {
        int totalMl = 0;
        pnlWaterLogs.removeAll();

        for (WaterIntake wi : todayWater) {
            totalMl += wi.getAmountMl();

            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

            JLabel lbl = new JLabel(wi.getAmountMl() + " ml at " + DateUtil.formatTime(wi.getRecordedTime()));
            lbl.setFont(ThemeConfig.FONT_BODY);
            lbl.setForeground(ThemeConfig.TEXT_PRIMARY);
            row.add(lbl, BorderLayout.CENTER);

            JButton btnDel = new JButton("×");
            btnDel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            btnDel.setForeground(ThemeConfig.TEXT_MUTED);
            btnDel.setContentAreaFilled(false);
            btnDel.setBorderPainted(false);
            btnDel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnDel.addActionListener(e -> deleteWater(wi));
            row.add(btnDel, BorderLayout.EAST);

            pnlWaterLogs.add(row);
        }

        if (todayWater.isEmpty()) {
            JLabel lbl = new JLabel("No water recorded today");
            lbl.setFont(ThemeConfig.FONT_SMALL);
            lbl.setForeground(ThemeConfig.TEXT_MUTED);
            pnlWaterLogs.add(lbl);
        }

        int target = 2500;
        UserSession sess = SupabaseClient.getInstance().getCurrentSession();
        if (sess != null && sess.getProfile() != null && sess.getProfile().getDailyWaterTargetMl() > 0) {
            target = sess.getProfile().getDailyWaterTargetMl();
        }

        barWater.setMaximum(target);
        barWater.setValue(totalMl);
        lblWaterTotal.setText(totalMl + " ml");
        int pct = (int) Math.round(((double) totalMl / target) * 100);
        lblWaterTarget.setText("Target: " + target + " ml (" + pct + "%)");

        pnlWaterLogs.revalidate();
        pnlWaterLogs.repaint();

        if (totalMl >= target) {
            new Thread(() -> {
                try {
                    achievementDAO.unlockAchievement("HYDRATION_HERO");
                } catch (Exception ignored) {}
            }).start();
        }
    }

    private void updateSleepView() {
        if (!sleepRecords.isEmpty()) {
            SleepRecord latest = sleepRecords.get(0);
            lblSleepDuration.setText(String.format("%.1f Hours", latest.getDurationHours()));
            lblSleepTimes.setText("Bed: " + DateUtil.formatDateTime(latest.getBedtime()) + " | Wake: " + DateUtil.formatDateTime(latest.getWakeTime()));
            lblSleepQuality.setText("Quality: " + latest.getQualityScore() + "/5 ★ - " + (latest.getNotes() != null ? latest.getNotes() : ""));
        } else {
            lblSleepDuration.setText("No sleep logged");
            lblSleepTimes.setText("Bedtime: -- | Wake: --");
            lblSleepQuality.setText("Quality: --");
        }

        // Build 7-day sleep chart
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        LocalDate today = LocalDate.now();
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEE");

        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            double hours = 0.0;
            for (SleepRecord s : sleepRecords) {
                if (s.getSleepDate() != null && s.getSleepDate().isEqual(d)) {
                    hours = s.getDurationHours();
                    break;
                }
            }
            dataset.addValue(hours, "Sleep", d.format(dayFmt));
        }

        JFreeChart chart = ChartFactory.createBarChart(null, null, null, dataset, PlotOrientation.VERTICAL, false, false, false);
        DashboardPanel.applyDarkChartTheme(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, ThemeConfig.PRIMARY_ACCENT);
        renderer.setMaximumBarWidth(0.12);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setOpaque(false);
        chartPanel.setBackground(new Color(0, 0, 0, 0));

        pnlSleepChart.removeAll();
        pnlSleepChart.add(chartPanel, BorderLayout.CENTER);
        pnlSleepChart.revalidate();
        pnlSleepChart.repaint();
    }

    private void logWater(int ml) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                waterDAO.logWater(ml, LocalDate.now());
                return null;
            }

            @Override
            protected void done() {
                ToastNotification.showToast(WaterSleepPanel.this, "Logged +" + ml + "ml water intake", ToastNotification.ToastType.SUCCESS);
                loadData();
            }
        };
        worker.execute();
    }

    private void promptCustomWater() {
        String input = JOptionPane.showInputDialog(this, "Enter water amount in milliliters (ml):", "250");
        if (input != null && !input.trim().isEmpty()) {
            try {
                int ml = Integer.parseInt(input.trim());
                if (!ValidationUtil.isValidWaterIntake(ml)) throw new NumberFormatException();
                logWater(ml);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter a valid amount (1 - 5000 ml).");
            }
        }
    }

    private void deleteWater(WaterIntake wi) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                waterDAO.deleteEntry(wi.getId());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    ToastNotification.showToast(WaterSleepPanel.this, "Water entry removed", ToastNotification.ToastType.INFO);
                    loadData();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(WaterSleepPanel.this, "Failed to delete water intake: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void openSleepDialog() {
        SleepDialog dialog = new SleepDialog(SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadData();
        }
    }

    // Modal Sleep Log Dialog
    private class SleepDialog extends JDialog {
        private static final long serialVersionUID = 1L;
        private boolean saved = false;

        private final ModernTextField txtDate = new ModernTextField(LocalDate.now().toString());
        private final ModernTextField txtBedTime = new ModernTextField("23:15");
        private final ModernTextField txtWakeTime = new ModernTextField("07:00");
        private final JComboBox<Integer> cmbQuality = new JComboBox<>(new Integer[]{5, 4, 3, 2, 1});
        private final ModernTextField txtNotes = new ModernTextField("Felt well rested");

        public SleepDialog(Window owner) {
            super(owner, "Log Sleep Session", ModalityType.APPLICATION_MODAL);
            setSize(490, 620);
            setLocationRelativeTo(owner);
            setResizable(false);
            getContentPane().setBackground(ThemeConfig.BG_DARK);

            JPanel main = new JPanel();
            main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
            main.setOpaque(false);
            main.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

            // Section 1: Quick Sleep Routine Chips
            JLabel lblPresets = new JLabel("Quick Sleep Schedules");
            lblPresets.setFont(ThemeConfig.FONT_SMALL);
            lblPresets.setForeground(ThemeConfig.TEXT_SECONDARY);
            main.add(lblPresets);
            main.add(Box.createVerticalStrut(4));

            JPanel presetRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            presetRow.setOpaque(false);
            presetRow.add(createSleepScheduleChip("23:00 - 07:00 (8h)", "23:00", "07:00", 5, "Felt deeply rested"));
            presetRow.add(createSleepScheduleChip("23:30 - 07:30 (8h)", "23:30", "07:30", 4, "Good solid sleep"));
            presetRow.add(createSleepScheduleChip("00:00 - 07:00 (7h)", "00:00", "07:00", 4, "Normal night"));
            presetRow.add(createSleepScheduleChip("22:30 - 06:30 (8h)", "22:30", "06:30", 5, "Early to bed, high energy"));
            main.add(presetRow);
            main.add(Box.createVerticalStrut(12));

            // Section 2: Date Row with Quick Pills
            JPanel dateRow = new JPanel(new BorderLayout(8, 0));
            dateRow.setOpaque(false);
            JLabel lblDateTag = new JLabel("Sleep Date");
            lblDateTag.setFont(ThemeConfig.FONT_SMALL);
            lblDateTag.setForeground(ThemeConfig.TEXT_SECONDARY);
            dateRow.add(lblDateTag, BorderLayout.NORTH);

            JPanel dateControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
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
            txtDate.setPreferredSize(new Dimension(130, 32));
            dateControls.add(btnToday);
            dateControls.add(btnYesterday);
            dateControls.add(txtDate);
            dateRow.add(dateControls, BorderLayout.CENTER);
            main.add(dateRow);
            main.add(Box.createVerticalStrut(14));

            // Section 3: Bedtime & Wake Time
            JPanel r2 = new JPanel(new GridLayout(1, 2, 10, 0));
            r2.setOpaque(false);
            r2.add(createFieldRow("Bedtime (HH:MM 24h)*", txtBedTime));
            r2.add(createFieldRow("Wake-up Time (HH:MM)*", txtWakeTime));
            main.add(r2);
            main.add(Box.createVerticalStrut(12));

            // Live Sleep Duration Box
            JPanel durationBox = new JPanel(new BorderLayout(8, 4));
            durationBox.setBackground(ThemeConfig.BG_CARD);
            durationBox.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
            JLabel durTag = new JLabel("LIVE DURATION CALCULATION");
            durTag.setFont(new Font("Segoe UI", Font.BOLD, 10));
            durTag.setForeground(ThemeConfig.TEXT_SECONDARY);
            lblLiveDuration.setFont(ThemeConfig.FONT_BODY_BOLD);
            lblLiveDuration.setForeground(ThemeConfig.SUCCESS_EMERALD);
            durationBox.add(durTag, BorderLayout.NORTH);
            durationBox.add(lblLiveDuration, BorderLayout.CENTER);
            main.add(durationBox);
            main.add(Box.createVerticalStrut(12));

            // Section 4: Quality Rating with Quick Stars
            JPanel qualitySection = new JPanel();
            qualitySection.setLayout(new BoxLayout(qualitySection, BoxLayout.Y_AXIS));
            qualitySection.setOpaque(false);
            JLabel lblQualTag = new JLabel("Sleep Quality Rating");
            lblQualTag.setFont(ThemeConfig.FONT_SMALL);
            lblQualTag.setForeground(ThemeConfig.TEXT_SECONDARY);
            qualitySection.add(lblQualTag);
            qualitySection.add(Box.createVerticalStrut(4));

            JPanel starRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            starRow.setOpaque(false);
            starRow.add(createQualityChip("⭐ 1 Poor", 1));
            starRow.add(createQualityChip("⭐⭐ 2 Fair", 2));
            starRow.add(createQualityChip("⭐⭐⭐ 3 OK", 3));
            starRow.add(createQualityChip("⭐⭐⭐⭐ 4 Good", 4));
            starRow.add(createQualityChip("⭐⭐⭐⭐⭐ 5 Great", 5));
            qualitySection.add(starRow);
            main.add(qualitySection);
            main.add(Box.createVerticalStrut(12));

            // Section 5: Notes
            main.add(createFieldRow("Notes / Sleep Quality", txtNotes));
            main.add(Box.createVerticalGlue());
            main.add(Box.createVerticalStrut(16));

            // Section 6: Actions
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
            ModernButton btnSave = new ModernButton("Save Sleep Record", ModernButton.ButtonStyle.PRIMARY);
            btnSave.addActionListener(e -> saveSleep());
            btnRow.add(btnCancel);
            btnRow.add(btnSave);
            actions.add(btnRow, BorderLayout.EAST);
            main.add(actions);

            setContentPane(main);

            // Document listeners for live duration recalculation
            DocumentListener timeListener = new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { updateLiveDuration(); }
                @Override public void removeUpdate(DocumentEvent e) { updateLiveDuration(); }
                @Override public void changedUpdate(DocumentEvent e) { updateLiveDuration(); }
            };
            txtBedTime.getDocument().addDocumentListener(timeListener);
            txtWakeTime.getDocument().addDocumentListener(timeListener);
            updateLiveDuration();

            // Enter key shortcuts
            txtBedTime.addActionListener(e -> saveSleep());
            txtWakeTime.addActionListener(e -> saveSleep());
            txtDate.addActionListener(e -> saveSleep());
            txtNotes.addActionListener(e -> saveSleep());

            getRootPane().registerKeyboardAction(e -> dispose(),
                    KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);

            SwingUtilities.invokeLater(() -> {
                txtBedTime.requestFocusInWindow();
                txtBedTime.selectAll();
            });
        }

        private final JLabel lblLiveDuration = new JLabel("Duration: 7h 45m • Good Rest");

        private JButton createSleepScheduleChip(String label, String bed, String wake, int quality, String note) {
            JButton btn = new JButton(label);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btn.setForeground(ThemeConfig.TEXT_PRIMARY);
            btn.setBackground(ThemeConfig.BG_CARD);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));
            btn.addActionListener(e -> {
                txtBedTime.setText(bed);
                txtWakeTime.setText(wake);
                cmbQuality.setSelectedItem(quality);
                txtNotes.setText(note);
                updateLiveDuration();
            });
            return btn;
        }

        private JButton createQualityChip(String label, int val) {
            JButton btn = new JButton(label);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btn.setForeground(ThemeConfig.TEXT_PRIMARY);
            btn.setBackground(ThemeConfig.BG_CARD);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            btn.addActionListener(e -> cmbQuality.setSelectedItem(val));
            return btn;
        }

        private JButton createQuickPill(String text, boolean active) {
            JButton btn = new JButton(text);
            btn.setFont(ThemeConfig.FONT_SMALL);
            btn.setForeground(ThemeConfig.TEXT_PRIMARY);
            btn.setBackground(active ? ThemeConfig.PRIMARY_ACCENT : ThemeConfig.BG_CARD);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            return btn;
        }

        private void updateLiveDuration() {
            try {
                String[] b = txtBedTime.getText().trim().split(":");
                String[] w = txtWakeTime.getText().trim().split(":");
                int bh = Integer.parseInt(b[0]);
                int bm = Integer.parseInt(b[1]);
                int wh = Integer.parseInt(w[0]);
                int wm = Integer.parseInt(w[1]);
                int totalMins = (wh * 60 + wm) - (bh * 60 + bm);
                if (totalMins < 0) totalMins += 24 * 60;
                int hours = totalMins / 60;
                int mins = totalMins % 60;
                String qualText = totalMins >= 420 ? "Optimal Recovery" : (totalMins >= 360 ? "Moderate Sleep" : "Sleep Debt Warning");
                lblLiveDuration.setText(String.format("Calculated Duration: %dh %02dm • %s", hours, mins, qualText));
                lblLiveDuration.setForeground(totalMins >= 420 ? ThemeConfig.SUCCESS_EMERALD : ThemeConfig.WARNING);
            } catch (Exception ignored) {
                lblLiveDuration.setText("Enter valid HH:MM times (e.g. 23:15 and 07:00)");
                lblLiveDuration.setForeground(ThemeConfig.TEXT_MUTED);
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

        private void saveSleep() {
            LocalDate date = DateUtil.parseDateOrNull(txtDate.getText().trim());
            if (date == null) {
                JOptionPane.showMessageDialog(this, "Please enter a valid date.");
                return;
            }

            try {
                String[] bedParts = txtBedTime.getText().trim().split(":");
                String[] wakeParts = txtWakeTime.getText().trim().split(":");

                int bedH = Integer.parseInt(bedParts[0]);
                int bedM = Integer.parseInt(bedParts[1]);
                int wakeH = Integer.parseInt(wakeParts[0]);
                int wakeM = Integer.parseInt(wakeParts[1]);

                OffsetDateTime bed = OffsetDateTime.now()
                        .withYear(date.getYear()).withMonth(date.getMonthValue()).withDayOfMonth(date.getDayOfMonth())
                        .withHour(bedH).withMinute(bedM).withSecond(0);

                // If bed hour is late evening, wake is on next morning or same day
                OffsetDateTime wake = OffsetDateTime.now()
                        .withYear(date.getYear()).withMonth(date.getMonthValue()).withDayOfMonth(date.getDayOfMonth())
                        .withHour(wakeH).withMinute(wakeM).withSecond(0);

                if (!wake.isAfter(bed)) {
                    wake = wake.plusDays(1);
                }

                int quality = (Integer) cmbQuality.getSelectedItem();
                String notes = txtNotes.getText().trim();

                SleepRecord record = new SleepRecord(date, bed, wake, quality, notes);

                SwingWorker<Void, Void> worker = new SwingWorker<>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        sleepDAO.saveRecord(record);
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                            saved = true;
                            ToastNotification.showToast(WaterSleepPanel.this, "Sleep record saved!", ToastNotification.ToastType.SUCCESS);
                            dispose();
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(SleepDialog.this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                };
                worker.execute();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid times in HH:MM format (e.g. 23:30 and 07:15).");
            }
        }

        public boolean isSaved() {
            return saved;
        }
    }
}
