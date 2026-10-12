package com.personaltracker.view;

import com.personaltracker.components.ModernButton;
import com.personaltracker.components.ModernCard;
import com.personaltracker.components.ModernTable;
import com.personaltracker.components.ModernTextField;
import com.personaltracker.components.ToastNotification;
import com.personaltracker.config.ThemeConfig;
import com.personaltracker.dao.AchievementDAO;
import com.personaltracker.dao.MeasurementDAO;
import com.personaltracker.model.BodyMeasurement;
import com.personaltracker.util.DateUtil;
import com.personaltracker.util.HealthCalculator;
import com.personaltracker.util.IconFactory;
import com.personaltracker.util.ValidationUtil;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Body Measurements and Weight progression tracking panel.
 */
public class MeasurementPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final MeasurementDAO measurementDAO = new MeasurementDAO();
    private final AchievementDAO achievementDAO = new AchievementDAO();

    private final JLabel lblCurrentWeight = new JLabel("-- kg");
    private final JLabel lblCurrentBmi = new JLabel("BMI: --");
    private final JLabel lblBmiCategory = new JLabel("Status: --");

    private final JPanel pnlChartContainer = new JPanel(new BorderLayout());

    private final DefaultTableModel tableModel;
    private final ModernTable table;

    private List<BodyMeasurement> currentMeasurements = new ArrayList<>();

    public MeasurementPanel() {
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
        JLabel title = new JLabel("Body Measurements & Weight");
        title.setFont(ThemeConfig.FONT_TITLE_LARGE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        titlePanel.add(title);

        JLabel subtitle = new JLabel("Track weight, BMI trends, and circumferences (cm)");
        subtitle.setFont(ThemeConfig.FONT_BODY);
        subtitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        titlePanel.add(subtitle);
        header.add(titlePanel, BorderLayout.WEST);

        ModernButton btnNew = new ModernButton("+ Log Measurement", ModernButton.ButtonStyle.PRIMARY);
        btnNew.addActionListener(e -> openMeasurementDialog());
        header.add(btnNew, BorderLayout.EAST);
        content.add(header);
        content.add(Box.createVerticalStrut(18));

        // Top Summary Cards Grid
        JPanel topGrid = new JPanel(new GridLayout(1, 3, 16, 0));
        topGrid.setOpaque(false);
        topGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        ModernCard cardWeight = new ModernCard(14, new Insets(16, 18, 16, 18));
        JLabel wTitle = new JLabel("CURRENT BODY WEIGHT");
        wTitle.setFont(ThemeConfig.FONT_SMALL);
        wTitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        lblCurrentWeight.setFont(ThemeConfig.FONT_STAT_NUM);
        lblCurrentWeight.setForeground(ThemeConfig.TEXT_PRIMARY);
        cardWeight.add(wTitle, BorderLayout.NORTH);
        cardWeight.add(lblCurrentWeight, BorderLayout.CENTER);
        topGrid.add(cardWeight);

        ModernCard cardBmi = new ModernCard(14, new Insets(16, 18, 16, 18));
        JLabel bTitle = new JLabel("CALCULATED BMI");
        bTitle.setFont(ThemeConfig.FONT_SMALL);
        bTitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        lblCurrentBmi.setFont(ThemeConfig.FONT_STAT_NUM);
        lblCurrentBmi.setForeground(ThemeConfig.PRIMARY_BLUE);
        cardBmi.add(bTitle, BorderLayout.NORTH);
        cardBmi.add(lblCurrentBmi, BorderLayout.CENTER);
        topGrid.add(cardBmi);

        ModernCard cardStatus = new ModernCard(14, new Insets(16, 18, 16, 18));
        JLabel sTitle = new JLabel("WHO CLASSIFICATION");
        sTitle.setFont(ThemeConfig.FONT_SMALL);
        sTitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        lblBmiCategory.setFont(ThemeConfig.FONT_SUBTITLE);
        lblBmiCategory.setForeground(ThemeConfig.SUCCESS_EMERALD);
        JLabel disclaimer = new JLabel("*BMI is an informational indicator");
        disclaimer.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        disclaimer.setForeground(ThemeConfig.TEXT_MUTED);
        cardStatus.add(sTitle, BorderLayout.NORTH);
        cardStatus.add(lblBmiCategory, BorderLayout.CENTER);
        cardStatus.add(disclaimer, BorderLayout.SOUTH);
        topGrid.add(cardStatus);

        content.add(topGrid);
        content.add(Box.createVerticalStrut(20));

        // Middle: Progression Line Chart
        ModernCard chartCard = new ModernCard(14, new Insets(16, 18, 16, 18));
        JLabel lblChartHead = new JLabel("Weight Progression History (kg)");
        lblChartHead.setFont(ThemeConfig.FONT_SUBTITLE);
        lblChartHead.setForeground(ThemeConfig.TEXT_PRIMARY);
        chartCard.add(lblChartHead, BorderLayout.NORTH);

        pnlChartContainer.setOpaque(false);
        pnlChartContainer.setPreferredSize(new Dimension(0, 240));
        chartCard.add(pnlChartContainer, BorderLayout.CENTER);
        chartCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        content.add(chartCard);
        content.add(Box.createVerticalStrut(20));

        // Bottom: Measurements History Table
        JLabel lblHistoryHead = new JLabel("Check-in Records");
        lblHistoryHead.setFont(ThemeConfig.FONT_SUBTITLE);
        lblHistoryHead.setForeground(ThemeConfig.TEXT_PRIMARY);
        content.add(lblHistoryHead);
        content.add(Box.createVerticalStrut(10));

        String[] cols = {"Date", "Weight (kg)", "Height (cm)", "BMI", "Waist (cm)", "Chest (cm)", "Arm (cm)", "Notes"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new ModernTable(tableModel);
        JScrollPane scrollTable = ModernTable.wrapInScrollPane(table);
        scrollTable.setPreferredSize(new Dimension(0, 220));
        content.add(scrollTable);
        content.add(Box.createVerticalStrut(12));

        // Delete button
        JPanel delBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        delBar.setOpaque(false);
        ModernButton btnDelete = new ModernButton("Delete Selected Record", ModernButton.ButtonStyle.DANGER);
        btnDelete.addActionListener(e -> deleteSelectedRecord());
        delBar.add(btnDelete);
        content.add(delBar);

        JScrollPane mainScroll = new JScrollPane(content);
        mainScroll.setBorder(null);
        mainScroll.setOpaque(false);
        mainScroll.getViewport().setOpaque(false);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(mainScroll, BorderLayout.CENTER);
    }

    public void loadMeasurements() {
        SwingWorker<List<BodyMeasurement>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<BodyMeasurement> doInBackground() throws Exception {
                return measurementDAO.getAllMeasurements();
            }

            @Override
            protected void done() {
                try {
                    currentMeasurements = get();
                    tableModel.setRowCount(0);

                    if (!currentMeasurements.isEmpty()) {
                        BodyMeasurement latest = currentMeasurements.get(0);
                        lblCurrentWeight.setText(String.format("%.1f kg", latest.getWeightKg()));
                        double bmi = latest.getBmi() != null ? latest.getBmi() : 0.0;
                        lblCurrentBmi.setText(String.format("%.1f", bmi));
                        lblBmiCategory.setText(HealthCalculator.getBmiCategory(bmi));
                    } else {
                        lblCurrentWeight.setText("-- kg");
                        lblCurrentBmi.setText("BMI: --");
                        lblBmiCategory.setText("Status: --");
                    }

                    for (BodyMeasurement m : currentMeasurements) {
                        tableModel.addRow(new Object[]{
                                DateUtil.formatDate(m.getRecordedDate()),
                                String.format("%.1f kg", m.getWeightKg()),
                                m.getHeightCm() != null ? String.format("%.1f cm", m.getHeightCm()) : "--",
                                m.getBmi() != null ? String.format("%.1f", m.getBmi()) : "--",
                                m.getWaistCm() != null ? String.format("%.1f cm", m.getWaistCm()) : "--",
                                m.getChestCm() != null ? String.format("%.1f cm", m.getChestCm()) : "--",
                                m.getArmCm() != null ? String.format("%.1f cm", m.getArmCm()) : "--",
                                m.getNotes() != null ? m.getNotes() : ""
                        });
                    }

                    updateChart();

                    if (currentMeasurements.size() >= 5) {
                        achievementDAO.unlockAchievement("BODY_TRACKER");
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load measurements: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void updateChart() {
        TimeSeries series = new TimeSeries("Weight (kg)");
        for (int i = currentMeasurements.size() - 1; i >= 0; i--) {
            BodyMeasurement m = currentMeasurements.get(i);
            if (m.getRecordedDate() != null) {
                LocalDate ld = m.getRecordedDate();
                series.addOrUpdate(new Day(ld.getDayOfMonth(), ld.getMonthValue(), ld.getYear()), m.getWeightKg());
            }
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection(series);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(null, null, null, dataset, false, false, false);
        DashboardPanel.applyDarkChartTheme(chart);

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);
        renderer.setSeriesPaint(0, ThemeConfig.PRIMARY_ACCENT);
        renderer.setSeriesStroke(0, new BasicStroke(2.4f));
        plot.setRenderer(renderer);

        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new java.text.SimpleDateFormat("dd MMM"));

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setOpaque(false);
        chartPanel.setBackground(new Color(0, 0, 0, 0));

        pnlChartContainer.removeAll();
        pnlChartContainer.add(chartPanel, BorderLayout.CENTER);
        pnlChartContainer.revalidate();
        pnlChartContainer.repaint();
    }

    private void openMeasurementDialog() {
        MeasurementDialog dialog = new MeasurementDialog(SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadMeasurements();
        }
    }

    private void deleteSelectedRecord() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a record to delete.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        BodyMeasurement m = currentMeasurements.get(modelRow);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete measurement recorded on " + DateUtil.formatDate(m.getRecordedDate()) + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    measurementDAO.deleteMeasurement(m.getId());
                    return null;
                }
                @Override
                protected void done() {
                    loadMeasurements();
                }
            };
            worker.execute();
        }
    }

    // Modal Measurement Dialog
    private class MeasurementDialog extends JDialog {
        private static final long serialVersionUID = 1L;
        private boolean saved = false;

        private final ModernTextField txtDate = new ModernTextField(LocalDate.now().toString());
        private final ModernTextField txtWeight = new ModernTextField("75.0");
        private final ModernTextField txtHeight = new ModernTextField("178.0");
        private final ModernTextField txtWaist = new ModernTextField("82.0");
        private final ModernTextField txtChest = new ModernTextField("100.0");
        private final ModernTextField txtArm = new ModernTextField("36.0");
        private final ModernTextField txtNotes = new ModernTextField("Morning check-in");

        public MeasurementDialog(Window owner) {
            super(owner, "Log Body Measurement Check-In", ModalityType.APPLICATION_MODAL);
            setSize(460, 490);
            setLocationRelativeTo(owner);
            setResizable(false);
            getContentPane().setBackground(ThemeConfig.BG_DARK);

            JPanel main = new JPanel();
            main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
            main.setOpaque(false);
            main.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

            main.add(createFieldRow("Date (YYYY-MM-DD)", txtDate));
            main.add(Box.createVerticalStrut(10));

            JPanel r2 = new JPanel(new GridLayout(1, 2, 12, 0));
            r2.setOpaque(false);
            r2.add(createFieldRow("Weight (kg)*", txtWeight));
            r2.add(createFieldRow("Height (cm)*", txtHeight));
            main.add(r2);
            main.add(Box.createVerticalStrut(10));

            JPanel r3 = new JPanel(new GridLayout(1, 3, 10, 0));
            r3.setOpaque(false);
            r3.add(createFieldRow("Waist (cm)", txtWaist));
            r3.add(createFieldRow("Chest (cm)", txtChest));
            r3.add(createFieldRow("Arm (cm)", txtArm));
            main.add(r3);
            main.add(Box.createVerticalStrut(10));

            main.add(createFieldRow("Notes / Comments", txtNotes));
            main.add(Box.createVerticalStrut(20));

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            actions.setOpaque(false);
            ModernButton btnCancel = new ModernButton("Cancel", ModernButton.ButtonStyle.GHOST);
            btnCancel.addActionListener(e -> dispose());
            ModernButton btnSave = new ModernButton("Save Check-In", ModernButton.ButtonStyle.PRIMARY);
            btnSave.addActionListener(e -> saveMeasurementData());
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

        private void saveMeasurementData() {
            LocalDate date = DateUtil.parseDateOrNull(txtDate.getText().trim());
            if (date == null) {
                JOptionPane.showMessageDialog(this, "Please enter a valid date.");
                return;
            }

            double weight;
            double height;
            try {
                weight = Double.parseDouble(txtWeight.getText().trim());
                height = Double.parseDouble(txtHeight.getText().trim());
                if (!ValidationUtil.isValidWeight(weight) || !ValidationUtil.isValidHeight(height)) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter a valid weight (20-400 kg) and height (50-270 cm).");
                return;
            }

            BodyMeasurement m = new BodyMeasurement(date, weight, height);
            try {
                if (!txtWaist.getText().trim().isEmpty()) m.setWaistCm(Double.parseDouble(txtWaist.getText().trim()));
                if (!txtChest.getText().trim().isEmpty()) m.setChestCm(Double.parseDouble(txtChest.getText().trim()));
                if (!txtArm.getText().trim().isEmpty()) m.setArmCm(Double.parseDouble(txtArm.getText().trim()));
            } catch (NumberFormatException ignored) {}
            m.setNotes(txtNotes.getText().trim());

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    measurementDAO.saveMeasurement(m);
                    return null;
                }
                @Override
                protected void done() {
                    saved = true;
                    ToastNotification.showToast(MeasurementPanel.this, "Check-in logged successfully!", ToastNotification.ToastType.SUCCESS);
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
