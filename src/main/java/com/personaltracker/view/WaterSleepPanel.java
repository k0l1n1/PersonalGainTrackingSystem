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
        lblWaterTotal.setForeground(ThemeConfig.PRIMARY_BLUE);
        center.add(lblWaterTotal);
        center.add(Box.createVerticalStrut(8));

        barWater.setColors(ThemeConfig.PRIMARY_BLUE, ThemeConfig.ACCENT_CYAN);
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
        innerSummary.setBackgroundColor(new Color(24, 33, 47));

        JPanel sumBox = new JPanel();
        sumBox.setLayout(new BoxLayout(sumBox, BoxLayout.Y_AXIS));
        sumBox.setOpaque(false);

        JLabel lblSumTitle = new JLabel("LAST NIGHT'S SLEEP");
        lblSumTitle.setFont(ThemeConfig.FONT_SMALL);
        lblSumTitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        sumBox.add(lblSumTitle);
        sumBox.add(Box.createVerticalStrut(4));

        lblSleepDuration.setFont(ThemeConfig.FONT_STAT_NUM);
        lblSleepDuration.setForeground(ThemeConfig.PURPLE_ACCENT);
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
        renderer.setSeriesPaint(0, ThemeConfig.PURPLE_ACCENT);
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
                loadData();
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
            setSize(420, 440);
            setLocationRelativeTo(owner);
            setResizable(false);
            getContentPane().setBackground(ThemeConfig.BG_DARK);

            JPanel main = new JPanel();
            main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
            main.setOpaque(false);
            main.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

            main.add(createFieldRow("Date (YYYY-MM-DD)", txtDate));
            main.add(Box.createVerticalStrut(10));

            JPanel r2 = new JPanel(new GridLayout(1, 2, 10, 0));
            r2.setOpaque(false);
            r2.add(createFieldRow("Bedtime (HH:MM 24h)", txtBedTime));
            r2.add(createFieldRow("Wake-up Time (HH:MM)", txtWakeTime));
            main.add(r2);
            main.add(Box.createVerticalStrut(10));

            main.add(createFieldRow("Quality Rating (1-Poor to 5-Great)", cmbQuality));
            main.add(Box.createVerticalStrut(10));

            main.add(createFieldRow("Notes / Sleep Quality", txtNotes));
            main.add(Box.createVerticalStrut(20));

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            actions.setOpaque(false);
            ModernButton btnCancel = new ModernButton("Cancel", ModernButton.ButtonStyle.GHOST);
            btnCancel.addActionListener(e -> dispose());
            ModernButton btnSave = new ModernButton("Save Sleep Record", ModernButton.ButtonStyle.PRIMARY);
            btnSave.addActionListener(e -> saveSleep());
            actions.add(btnCancel);
            actions.add(btnSave);
            main.add(actions);

            setContentPane(main);
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
                        saved = true;
                        ToastNotification.showToast(WaterSleepPanel.this, "Sleep record saved!", ToastNotification.ToastType.SUCCESS);
                        dispose();
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
