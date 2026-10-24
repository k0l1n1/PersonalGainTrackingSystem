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
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import java.text.DecimalFormat;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.personaltracker.model.UserProfile;
import com.personaltracker.service.SupabaseClient;

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
        content.setBorder(BorderFactory.createEmptyBorder(18, 24, 20, 24));

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

        ModernButton btnDeleteTop = new ModernButton("Delete Selected Record", ModernButton.ButtonStyle.DANGER);
        btnDeleteTop.setIcon(IconFactory.createIcon(IconFactory.IconType.TRASH, 13, Color.WHITE));
        btnDeleteTop.setIconTextGap(6);
        btnDeleteTop.addActionListener(e -> deleteSelectedRecord());

        ModernButton btnNew = new ModernButton("+ Log Measurement", ModernButton.ButtonStyle.PRIMARY);
        btnNew.addActionListener(e -> openMeasurementDialog());

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerActions.setOpaque(false);
        headerActions.add(btnDeleteTop);
        headerActions.add(btnNew);

        header.add(headerActions, BorderLayout.EAST);
        content.add(header);
        content.add(Box.createVerticalStrut(14));

        // Top Summary Cards Grid
        JPanel topGrid = new JPanel(new GridLayout(1, 3, 16, 0));
        topGrid.setOpaque(false);
        topGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 98));

        ModernCard cardWeight = new ModernCard(14, new Insets(12, 18, 12, 18));
        JLabel wTitle = new JLabel("CURRENT BODY WEIGHT");
        wTitle.setFont(ThemeConfig.FONT_SMALL);
        wTitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        lblCurrentWeight.setFont(ThemeConfig.FONT_STAT_NUM);
        lblCurrentWeight.setForeground(ThemeConfig.TEXT_PRIMARY);
        cardWeight.add(wTitle, BorderLayout.NORTH);
        cardWeight.add(lblCurrentWeight, BorderLayout.CENTER);
        topGrid.add(cardWeight);

        ModernCard cardBmi = new ModernCard(14, new Insets(12, 18, 12, 18));
        JLabel bTitle = new JLabel("CALCULATED BMI");
        bTitle.setFont(ThemeConfig.FONT_SMALL);
        bTitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        lblCurrentBmi.setFont(ThemeConfig.FONT_STAT_NUM);
        lblCurrentBmi.setForeground(ThemeConfig.PRIMARY_ACCENT);
        cardBmi.add(bTitle, BorderLayout.NORTH);
        cardBmi.add(lblCurrentBmi, BorderLayout.CENTER);
        topGrid.add(cardBmi);

        ModernCard cardStatus = new ModernCard(14, new Insets(12, 18, 12, 18));
        JLabel sTitle = new JLabel("WHO CLASSIFICATION");
        sTitle.setFont(ThemeConfig.FONT_SMALL);
        sTitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        lblBmiCategory.setFont(ThemeConfig.FONT_SUBTITLE);
        lblBmiCategory.setForeground(ThemeConfig.SUCCESS);
        JLabel disclaimer = new JLabel("*BMI is an informational indicator");
        disclaimer.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        disclaimer.setForeground(ThemeConfig.TEXT_MUTED);
        cardStatus.add(sTitle, BorderLayout.NORTH);
        cardStatus.add(lblBmiCategory, BorderLayout.CENTER);
        cardStatus.add(disclaimer, BorderLayout.SOUTH);
        topGrid.add(cardStatus);

        content.add(topGrid);
        content.add(Box.createVerticalStrut(14));

        // Middle: Progression Line Chart
        ModernCard chartCard = new ModernCard(14, new Insets(14, 18, 14, 18));
        JLabel lblChartHead = new JLabel("Weight Progression History (kg)");
        lblChartHead.setFont(ThemeConfig.FONT_SUBTITLE);
        lblChartHead.setForeground(ThemeConfig.TEXT_PRIMARY);
        chartCard.add(lblChartHead, BorderLayout.NORTH);

        pnlChartContainer.setOpaque(false);
        pnlChartContainer.setPreferredSize(new Dimension(0, 190));
        chartCard.add(pnlChartContainer, BorderLayout.CENTER);
        chartCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));
        content.add(chartCard);
        content.add(Box.createVerticalStrut(14));

        // Bottom: Measurements History Table
        JLabel lblHistoryHead = new JLabel("Check-in Records");
        lblHistoryHead.setFont(ThemeConfig.FONT_SUBTITLE);
        lblHistoryHead.setForeground(ThemeConfig.TEXT_PRIMARY);
        content.add(lblHistoryHead);
        content.add(Box.createVerticalStrut(8));

        String[] cols = {"Date", "Weight (kg)", "Height (cm)", "BMI", "Waist (cm)", "Chest (cm)", "Arm (cm)", "Notes"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new ModernTable(tableModel);
        JScrollPane scrollTable = ModernTable.wrapInScrollPane(table);
        scrollTable.setPreferredSize(new Dimension(0, 180));
        content.add(scrollTable);

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
        renderer.setSeriesStroke(0, new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        renderer.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-3.5, -3.5, 7, 7));
        renderer.setSeriesShapesFilled(0, true);
        renderer.setDrawOutlines(true);
        renderer.setUseFillPaint(true);
        renderer.setSeriesFillPaint(0, ThemeConfig.BG_CARD);
        renderer.setSeriesOutlinePaint(0, ThemeConfig.PRIMARY_ACCENT);
        renderer.setSeriesOutlineStroke(0, new BasicStroke(2.0f));
        plot.setRenderer(renderer);

        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new java.text.SimpleDateFormat("dd MMM"));
        axis.setLowerMargin(0.15);
        axis.setUpperMargin(0.15);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        if (rangeAxis != null) {
            rangeAxis.setAutoRangeIncludesZero(false);
            rangeAxis.setNumberFormatOverride(new DecimalFormat("#,##0.#"));
            rangeAxis.setAutoRangeMinimumSize(4.0);
            rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
            rangeAxis.setLowerMargin(0.15);
            rangeAxis.setUpperMargin(0.15);

            if (!currentMeasurements.isEmpty()) {
                double latestWeight = currentMeasurements.get(0).getWeightKg();
                boolean allSame = true;
                for (BodyMeasurement m : currentMeasurements) {
                    if (Math.abs(m.getWeightKg() - latestWeight) > 0.05) {
                        allSame = false;
                        break;
                    }
                }
                if (allSame) {
                    double pad = Math.max(3.0, latestWeight * 0.05);
                    rangeAxis.setRange(Math.max(0, latestWeight - pad), latestWeight + pad);
                }
            }
        }

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setOpaque(false);
        chartPanel.setBackground(new Color(0, 0, 0, 0));

        pnlChartContainer.removeAll();
        pnlChartContainer.add(chartPanel, BorderLayout.CENTER);
        pnlChartContainer.revalidate();
        pnlChartContainer.repaint();
    }

    private void openMeasurementDialog() {
        BodyMeasurement latest = !currentMeasurements.isEmpty() ? currentMeasurements.get(0) : null;
        MeasurementDialog dialog = new MeasurementDialog(SwingUtilities.getWindowAncestor(this), latest);
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
                    try {
                        get();
                        loadMeasurements();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(MeasurementPanel.this, "Failed to delete measurement: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    // Modern User-Friendly Measurement Dialog
    private class MeasurementDialog extends JDialog {
        private static final long serialVersionUID = 1L;
        private boolean saved = false;

        private final ModernTextField txtDate;
        private final ModernTextField txtWeight;
        private final ModernTextField txtHeight;
        private final ModernTextField txtWaist;
        private final ModernTextField txtChest;
        private final ModernTextField txtArm;
        private final ModernTextField txtNotes;
        private final JLabel lblLiveBmi = new JLabel("BMI: --");

        public MeasurementDialog(Window owner, BodyMeasurement lastCheckIn) {
            super(owner, "Log Body Measurement Check-In", ModalityType.APPLICATION_MODAL);
            setSize(490, 620);
            setLocationRelativeTo(owner);
            setResizable(false);
            getContentPane().setBackground(ThemeConfig.BG_DARK);

            // 1. Determine smart defaults from previous check-in or profile
            UserProfile profile = null;
            try {
                if (SupabaseClient.getInstance().getCurrentSession() != null) {
                    profile = SupabaseClient.getInstance().getCurrentSession().getProfile();
                }
            } catch (Exception ignored) {}

            double initialWeight = 75.0;
            if (lastCheckIn != null && lastCheckIn.getWeightKg() > 0) {
                initialWeight = lastCheckIn.getWeightKg();
            } else if (profile != null && profile.getTargetWeightKg() != null && profile.getTargetWeightKg() > 0) {
                initialWeight = profile.getTargetWeightKg();
            }

            double initialHeight = 178.0;
            if (lastCheckIn != null && lastCheckIn.getHeightCm() != null && lastCheckIn.getHeightCm() > 0) {
                initialHeight = lastCheckIn.getHeightCm();
            } else if (profile != null && profile.getHeightCm() != null && profile.getHeightCm() > 0) {
                initialHeight = profile.getHeightCm();
            }

            String initialWaist = (lastCheckIn != null && lastCheckIn.getWaistCm() != null) ? String.format(java.util.Locale.US, "%.1f", lastCheckIn.getWaistCm()) : "";
            String initialChest = (lastCheckIn != null && lastCheckIn.getChestCm() != null) ? String.format(java.util.Locale.US, "%.1f", lastCheckIn.getChestCm()) : "";
            String initialArm = (lastCheckIn != null && lastCheckIn.getArmCm() != null) ? String.format(java.util.Locale.US, "%.1f", lastCheckIn.getArmCm()) : "";

            txtDate = new ModernTextField(LocalDate.now().toString(), "YYYY-MM-DD");
            txtWeight = new ModernTextField(String.format(java.util.Locale.US, "%.1f", initialWeight), "e.g. 75.0");
            txtHeight = new ModernTextField(String.format(java.util.Locale.US, "%.1f", initialHeight), "e.g. 175.0");
            txtWaist = new ModernTextField(initialWaist, "e.g. 80.0");
            txtChest = new ModernTextField(initialChest, "e.g. 95.0");
            txtArm = new ModernTextField(initialArm, "e.g. 35.0");
            txtNotes = new ModernTextField("", "e.g. Morning check-in, feeling energetic");

            // Make weight field prominent
            txtWeight.setFont(new Font("Segoe UI", Font.BOLD, 17));

            JPanel main = new JPanel();
            main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
            main.setOpaque(false);
            main.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

            // Section 1: Quick Date Selection (Today / Yesterday / Custom)
            JPanel dateRow = new JPanel(new BorderLayout(8, 0));
            dateRow.setOpaque(false);
            JLabel lblDateTag = new JLabel("Check-In Date");
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

            // Section 2: Weight with Quick Stepper Buttons
            JPanel weightSection = new JPanel();
            weightSection.setLayout(new BoxLayout(weightSection, BoxLayout.Y_AXIS));
            weightSection.setOpaque(false);

            String autoFillHint = (lastCheckIn != null) ? " (auto-filled from last check-in)" : "";
            JLabel lblWeightTag = new JLabel("Weight (kg) *" + autoFillHint);
            lblWeightTag.setFont(ThemeConfig.FONT_SMALL);
            lblWeightTag.setForeground(ThemeConfig.TEXT_SECONDARY);
            weightSection.add(lblWeightTag);
            weightSection.add(Box.createVerticalStrut(4));
            weightSection.add(txtWeight);
            weightSection.add(Box.createVerticalStrut(6));

            // Quick stepper micro-adjust buttons
            JPanel stepperRow = new JPanel(new GridLayout(1, 6, 6, 0));
            stepperRow.setOpaque(false);
            stepperRow.add(createStepperButton("-1.0", -1.0));
            stepperRow.add(createStepperButton("-0.5", -0.5));
            stepperRow.add(createStepperButton("-0.1", -0.1));
            stepperRow.add(createStepperButton("+0.1", +0.1));
            stepperRow.add(createStepperButton("+0.5", +0.5));
            stepperRow.add(createStepperButton("+1.0", +1.0));
            weightSection.add(stepperRow);
            main.add(weightSection);
            main.add(Box.createVerticalStrut(14));

            // Section 3: Height & Live BMI Card
            JPanel r2 = new JPanel(new GridLayout(1, 2, 12, 0));
            r2.setOpaque(false);
            r2.add(createFieldRow("Height (cm) *", txtHeight));

            JPanel bmiBox = new JPanel(new BorderLayout());
            bmiBox.setBackground(ThemeConfig.BG_CARD);
            bmiBox.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
            ));
            JLabel bmiTitle = new JLabel("LIVE BMI CALCULATION");
            bmiTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
            bmiTitle.setForeground(ThemeConfig.TEXT_SECONDARY);
            lblLiveBmi.setFont(ThemeConfig.FONT_BODY_BOLD);
            bmiBox.add(bmiTitle, BorderLayout.NORTH);
            bmiBox.add(lblLiveBmi, BorderLayout.CENTER);
            r2.add(bmiBox);
            main.add(r2);
            main.add(Box.createVerticalStrut(14));

            // Section 4: Circumferences (Optional, pre-filled if previously measured)
            JLabel lblCircTag = new JLabel("Circumferences (cm, Optional)");
            lblCircTag.setFont(ThemeConfig.FONT_SMALL);
            lblCircTag.setForeground(ThemeConfig.TEXT_SECONDARY);
            main.add(lblCircTag);
            main.add(Box.createVerticalStrut(4));

            JPanel r3 = new JPanel(new GridLayout(1, 3, 10, 0));
            r3.setOpaque(false);
            r3.add(createFieldRow("Waist", txtWaist));
            r3.add(createFieldRow("Chest", txtChest));
            r3.add(createFieldRow("Arm", txtArm));
            main.add(r3);
            main.add(Box.createVerticalStrut(14));

            // Section 5: Notes & 1-Click Quick Chips
            JPanel notesPanel = new JPanel();
            notesPanel.setLayout(new BoxLayout(notesPanel, BoxLayout.Y_AXIS));
            notesPanel.setOpaque(false);

            JLabel lblNotesTag = new JLabel("Notes / Quick Tags");
            lblNotesTag.setFont(ThemeConfig.FONT_SMALL);
            lblNotesTag.setForeground(ThemeConfig.TEXT_SECONDARY);
            notesPanel.add(lblNotesTag);
            notesPanel.add(Box.createVerticalStrut(4));

            JPanel chipsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            chipsRow.setOpaque(false);
            chipsRow.add(createNoteChip("Morning Fasted"));
            chipsRow.add(createNoteChip("Post-Workout"));
            chipsRow.add(createNoteChip("Evening"));
            chipsRow.add(createNoteChip("Weekly Weigh-in"));
            notesPanel.add(chipsRow);
            notesPanel.add(Box.createVerticalStrut(6));
            notesPanel.add(txtNotes);
            main.add(notesPanel);

            main.add(Box.createVerticalGlue());
            main.add(Box.createVerticalStrut(16));

            // Section 6: Action Buttons
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
            ModernButton btnSave = new ModernButton("Save Check-In", ModernButton.ButtonStyle.PRIMARY);
            btnSave.addActionListener(e -> saveMeasurementData());
            btnRow.add(btnCancel);
            btnRow.add(btnSave);
            actions.add(btnRow, BorderLayout.EAST);
            main.add(actions);

            setContentPane(main);

            // Document listener for dynamic live BMI recalculation
            DocumentListener bmiListener = new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { updateLiveBmi(); }
                @Override public void removeUpdate(DocumentEvent e) { updateLiveBmi(); }
                @Override public void changedUpdate(DocumentEvent e) { updateLiveBmi(); }
            };
            txtWeight.getDocument().addDocumentListener(bmiListener);
            txtHeight.getDocument().addDocumentListener(bmiListener);
            updateLiveBmi();

            // Press Enter in any field to save immediately
            txtWeight.addActionListener(e -> saveMeasurementData());
            txtHeight.addActionListener(e -> saveMeasurementData());
            txtWaist.addActionListener(e -> saveMeasurementData());
            txtChest.addActionListener(e -> saveMeasurementData());
            txtArm.addActionListener(e -> saveMeasurementData());
            txtNotes.addActionListener(e -> saveMeasurementData());

            // Press Escape to dismiss
            getRootPane().registerKeyboardAction(e -> dispose(),
                    KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);

            // Automatically focus Weight input with text selected for instant typing
            SwingUtilities.invokeLater(() -> {
                txtWeight.requestFocusInWindow();
                txtWeight.selectAll();
            });
        }

        private JButton createStepperButton(String text, double delta) {
            JButton btn = new JButton(text);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btn.setForeground(ThemeConfig.TEXT_PRIMARY);
            btn.setBackground(ThemeConfig.BG_CARD);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true));
            btn.setPreferredSize(new Dimension(54, 28));
            btn.addActionListener(e -> adjustWeight(delta));
            return btn;
        }

        private void adjustWeight(double delta) {
            try {
                double current = Double.parseDouble(txtWeight.getText().trim());
                double updated = Math.max(20.0, Math.min(400.0, current + delta));
                txtWeight.setText(String.format(java.util.Locale.US, "%.1f", updated));
                txtWeight.selectAll();
                updateLiveBmi();
            } catch (Exception ignored) {}
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

        private JButton createNoteChip(String noteText) {
            JButton btn = new JButton(noteText);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btn.setForeground(ThemeConfig.TEXT_SECONDARY);
            btn.setBackground(ThemeConfig.BG_CARD);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            btn.addActionListener(e -> txtNotes.setText(noteText));
            return btn;
        }

        private void updateLiveBmi() {
            try {
                double w = Double.parseDouble(txtWeight.getText().trim());
                double h = Double.parseDouble(txtHeight.getText().trim());
                if (w > 0 && h > 0) {
                    double bmi = HealthCalculator.calculateBmi(w, h);
                    String cat = HealthCalculator.getBmiCategory(bmi);
                    lblLiveBmi.setText(String.format(java.util.Locale.US, "%.1f • %s", bmi, cat));
                    if ("Normal weight".equalsIgnoreCase(cat)) {
                        lblLiveBmi.setForeground(ThemeConfig.SUCCESS);
                    } else if (cat.contains("Obesity")) {
                        lblLiveBmi.setForeground(ThemeConfig.PRIMARY_ACCENT);
                    } else {
                        lblLiveBmi.setForeground(ThemeConfig.WARNING);
                    }
                    return;
                }
            } catch (Exception ignored) {}
            lblLiveBmi.setText("BMI: --");
            lblLiveBmi.setForeground(ThemeConfig.TEXT_SECONDARY);
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
                JOptionPane.showMessageDialog(this, "Please enter a valid date (YYYY-MM-DD).");
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
                    try {
                        get();
                        saved = true;
                        ToastNotification.showToast(MeasurementPanel.this, "Check-in logged successfully!", ToastNotification.ToastType.SUCCESS);
                        dispose();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(MeasurementDialog.this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
