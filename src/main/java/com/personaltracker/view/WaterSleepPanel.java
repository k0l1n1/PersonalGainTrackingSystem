package com.personaltracker.view;

import com.personaltracker.components.ModernButton;
import com.personaltracker.components.ModernCard;
import com.personaltracker.components.ModernProgressBar;
import com.personaltracker.components.ModernTextField;
import com.personaltracker.components.StatCard;
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
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
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
import java.util.Locale;

/**
 * Water Hydration and Sleep Tracking panel.
 */
public class WaterSleepPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final WaterDAO waterDAO = new WaterDAO();
    private final SleepDAO sleepDAO = new SleepDAO();
    private final AchievementDAO achievementDAO = new AchievementDAO();

    // Top Summary Stat Cards (Full clarity and spacious presentation)
    private final StatCard cardTodayWater = new StatCard("TODAY'S INTAKE", "0 ml", "Target: 2,500 ml", IconFactory.IconType.WATER, ThemeConfig.PRIMARY_ACCENT);
    private final StatCard cardWaterStatus = new StatCard("HYDRATION GOAL", "0%", "Remaining: 2,500 ml", IconFactory.IconType.WATER, ThemeConfig.PRIMARY_ACCENT);
    private final StatCard cardLastNightSleep = new StatCard("LAST NIGHT'S SLEEP", "-- hrs", "Recovery: --", IconFactory.IconType.SLEEP, ThemeConfig.PRIMARY_ACCENT);
    private final StatCard cardAvgSleep = new StatCard("7-DAY SLEEP AVG", "-- hrs", "Target: 8.0 hrs/night", IconFactory.IconType.SLEEP, ThemeConfig.SUCCESS);

    // Water state
    private final JLabel lblWaterBadge = new JLabel("0% Goal");
    private final JLabel lblWaterTotal = new JLabel("0 ml");
    private final JLabel lblWaterTarget = new JLabel("Target: 2,500 ml (0%)");
    private final JLabel lblWaterRemaining = new JLabel("2,500 ml remaining");
    private final ModernProgressBar barWater = new ModernProgressBar(0, 2500);
    private final JLabel lblWaterCount = new JLabel("0 entries");
    private final JPanel pnlWaterLogs = new JPanel();


    // Sleep state
    private final JLabel lblSleepDuration = new JLabel("No sleep logged");
    private final JLabel lblSleepSub = new JLabel("Recommended: 7.0 - 9.0 hrs");
    private final JLabel lblSleepTag = new JLabel("Target: 8.0 hrs");
    private final JLabel lblSleepTimes = new JLabel("-- → --");
    private final JLabel lblSleepNotes = new JLabel("");
    private final JPanel pnlSleepNotes = new JPanel(new BorderLayout());
    private final JLabel lblSleepAvgAnnotation = new JLabel("Week Avg: --");
    private final JPanel pnlSleepChart = new JPanel(new BorderLayout());
    private final JPanel pnlSleepLogs = new JPanel();

    private List<WaterIntake> todayWater = new ArrayList<>();
    private List<SleepRecord> sleepRecords = new ArrayList<>();

    public WaterSleepPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(18, 24, 24, 24));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("Hydration & Sleep Tracking");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(3));

        JLabel subtitle = new JLabel("Monitor daily water consumption and sleep recovery trends");
        subtitle.setFont(ThemeConfig.FONT_BODY);
        subtitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        titlePanel.add(subtitle);
        header.add(titlePanel, BorderLayout.WEST);

        content.add(header);
        content.add(Box.createVerticalStrut(14));

        // Top 4 Stat Cards Grid
        JPanel statsGrid = new JPanel(new GridLayout(1, 4, 14, 0));
        statsGrid.setOpaque(false);
        statsGrid.setPreferredSize(new Dimension(0, 105));
        statsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));
        statsGrid.add(cardTodayWater);
        statsGrid.add(cardWaterStatus);
        statsGrid.add(cardLastNightSleep);
        statsGrid.add(cardAvgSleep);

        content.add(statsGrid);
        content.add(Box.createVerticalStrut(16));

        // Two Column Main Grid (Water on Left, Sleep on Right)
        JPanel mainGrid = new JPanel(new GridLayout(1, 2, 16, 0));
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
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private ModernCard createWaterSection() {
        ModernCard card = new ModernCard(14, new Insets(16, 18, 16, 18));

        // Top Header
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);

        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleBox.setOpaque(false);
        JLabel head = new JLabel("Daily Hydration (Water)");
        head.setFont(ThemeConfig.FONT_TITLE);
        head.setForeground(ThemeConfig.TEXT_PRIMARY);
        titleBox.add(head);

        JLabel icon = new JLabel(IconFactory.createIcon(IconFactory.IconType.WATER, 18, ThemeConfig.PRIMARY_ACCENT));
        titleBox.add(icon);
        top.add(titleBox, BorderLayout.WEST);

        lblWaterBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblWaterBadge.setForeground(ThemeConfig.PRIMARY_ACCENT);
        lblWaterBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.PRIMARY_ACCENT, 1, true),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        top.add(lblWaterBadge, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Hero Intake Display Card
        ModernCard heroCard = new ModernCard(10, new Insets(12, 16, 12, 16));
        heroCard.setBackgroundColor(ThemeConfig.BG_INPUT);
        heroCard.setPreferredSize(new Dimension(0, 90));
        heroCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        heroCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel heroBox = new JPanel();
        heroBox.setLayout(new BoxLayout(heroBox, BoxLayout.Y_AXIS));
        heroBox.setOpaque(false);

        JPanel heroTopRow = new JPanel(new BorderLayout());
        heroTopRow.setOpaque(false);
        lblWaterTotal.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblWaterTotal.setForeground(ThemeConfig.PRIMARY_ACCENT);
        heroTopRow.add(lblWaterTotal, BorderLayout.WEST);

        lblWaterRemaining.setFont(ThemeConfig.FONT_BODY);
        lblWaterRemaining.setForeground(ThemeConfig.TEXT_MUTED);
        heroTopRow.add(lblWaterRemaining, BorderLayout.EAST);
        heroBox.add(heroTopRow);
        heroBox.add(Box.createVerticalStrut(6));

        barWater.setColors(ThemeConfig.PRIMARY_ACCENT, ThemeConfig.ACCENT_HOVER);
        barWater.setPreferredSize(new Dimension(0, 10));
        barWater.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        heroBox.add(barWater);
        heroBox.add(Box.createVerticalStrut(6));

        lblWaterTarget.setFont(ThemeConfig.FONT_SMALL);
        lblWaterTarget.setForeground(ThemeConfig.TEXT_SECONDARY);
        heroBox.add(lblWaterTarget);

        heroCard.add(heroBox, BorderLayout.CENTER);
        center.add(heroCard);
        center.add(Box.createVerticalStrut(10));

        // Quick add buttons - 1 row of 4 buttons with comfortable height
        JLabel lblQuick = new JLabel("Quick Add Intake");
        lblQuick.setFont(ThemeConfig.FONT_BODY_BOLD);
        lblQuick.setForeground(ThemeConfig.TEXT_PRIMARY);
        lblQuick.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(lblQuick);
        center.add(Box.createVerticalStrut(6));

        JPanel btnGrid = new JPanel(new GridLayout(1, 4, 8, 0));
        btnGrid.setOpaque(false);
        btnGrid.setPreferredSize(new Dimension(0, 36));
        btnGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        ModernButton b250 = new ModernButton("+250 ml", ModernButton.ButtonStyle.SECONDARY,
                IconFactory.createIcon(IconFactory.IconType.WATER, 12, ThemeConfig.TEXT_SECONDARY));
        b250.setFont(ThemeConfig.FONT_BODY_BOLD);
        b250.setToolTipText("Glass (+250 ml)");
        b250.addActionListener(e -> logWater(250));

        ModernButton b500 = new ModernButton("+500 ml", ModernButton.ButtonStyle.SECONDARY,
                IconFactory.createIcon(IconFactory.IconType.WATER, 12, ThemeConfig.TEXT_SECONDARY));
        b500.setFont(ThemeConfig.FONT_BODY_BOLD);
        b500.setToolTipText("Bottle (+500 ml)");
        b500.addActionListener(e -> logWater(500));

        ModernButton b750 = new ModernButton("+750 ml", ModernButton.ButtonStyle.SECONDARY,
                IconFactory.createIcon(IconFactory.IconType.WATER, 12, ThemeConfig.TEXT_SECONDARY));
        b750.setFont(ThemeConfig.FONT_BODY_BOLD);
        b750.setToolTipText("Shaker (+750 ml)");
        b750.addActionListener(e -> logWater(750));

        ModernButton bCustom = new ModernButton("Custom", ModernButton.ButtonStyle.PRIMARY,
                IconFactory.createIcon(IconFactory.IconType.PLUS, 12, Color.WHITE));
        bCustom.setFont(ThemeConfig.FONT_BODY_BOLD);
        bCustom.setToolTipText("Custom Water Amount");
        bCustom.addActionListener(e -> promptCustomWater());

        btnGrid.add(b250);
        btnGrid.add(b500);
        btnGrid.add(b750);
        btnGrid.add(bCustom);
        center.add(btnGrid);
        center.add(Box.createVerticalStrut(10));



        // History logs header
        JPanel logHeader = new JPanel(new BorderLayout());
        logHeader.setOpaque(false);
        logHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        logHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblLog = new JLabel("Today's Water Entries");
        lblLog.setFont(ThemeConfig.FONT_BODY_BOLD);
        lblLog.setForeground(ThemeConfig.TEXT_PRIMARY);
        logHeader.add(lblLog, BorderLayout.WEST);

        lblWaterCount.setFont(ThemeConfig.FONT_SMALL);
        lblWaterCount.setForeground(ThemeConfig.TEXT_MUTED);
        logHeader.add(lblWaterCount, BorderLayout.EAST);
        center.add(logHeader);
        center.add(Box.createVerticalStrut(6));

        pnlWaterLogs.setLayout(new BoxLayout(pnlWaterLogs, BoxLayout.Y_AXIS));
        pnlWaterLogs.setOpaque(false);
        JScrollPane scrollLogs = new JScrollPane(pnlWaterLogs);
        scrollLogs.setOpaque(false);
        scrollLogs.getViewport().setOpaque(false);
        scrollLogs.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true));
        scrollLogs.setPreferredSize(new Dimension(0, 200));
        scrollLogs.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        scrollLogs.getVerticalScrollBar().setUnitIncrement(14);
        scrollLogs.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(scrollLogs);

        card.add(center, BorderLayout.CENTER);
        return card;
    }

    private ModernCard createSleepSection() {
        ModernCard card = new ModernCard(14, new Insets(16, 18, 16, 18));

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);

        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleBox.setOpaque(false);
        JLabel head = new JLabel("Sleep & Recovery");
        head.setFont(ThemeConfig.FONT_TITLE);
        head.setForeground(ThemeConfig.TEXT_PRIMARY);
        titleBox.add(head);

        JLabel icon = new JLabel(IconFactory.createIcon(IconFactory.IconType.SLEEP, 18, ThemeConfig.PRIMARY_ACCENT));
        titleBox.add(icon);
        top.add(titleBox, BorderLayout.WEST);

        ModernButton btnLogSleep = new ModernButton("Log Sleep", ModernButton.ButtonStyle.PRIMARY,
                IconFactory.createIcon(IconFactory.IconType.PLUS, 12, Color.WHITE));
        btnLogSleep.setFont(ThemeConfig.FONT_BODY_BOLD);
        btnLogSleep.setMargin(new Insets(5, 12, 5, 12));
        btnLogSleep.addActionListener(e -> openSleepDialog());
        top.add(btnLogSleep, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Latest summary card with modern segmented layout
        ModernCard innerSummary = new ModernCard(10, new Insets(12, 16, 12, 16));
        innerSummary.setBackgroundColor(ThemeConfig.BG_INPUT);
        innerSummary.setPreferredSize(new Dimension(0, 90));
        innerSummary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));
        innerSummary.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel sumBox = new JPanel();
        sumBox.setLayout(new BoxLayout(sumBox, BoxLayout.Y_AXIS));
        sumBox.setOpaque(false);

        // 1. Header row
        JPanel sumTopRow = new JPanel(new BorderLayout(8, 0));
        sumTopRow.setOpaque(false);
        sumTopRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        sumTopRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        JPanel sumTitleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        sumTitleBox.setOpaque(false);
        sumTitleBox.add(new JLabel(IconFactory.createIcon(IconFactory.IconType.SLEEP, 12, ThemeConfig.PRIMARY_ACCENT)));
        JLabel lblSumTitle = new JLabel("LAST NIGHT'S SLEEP");
        lblSumTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblSumTitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        sumTitleBox.add(lblSumTitle);
        sumTopRow.add(sumTitleBox, BorderLayout.WEST);

        lblSleepTag.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblSleepTag.setForeground(ThemeConfig.SUCCESS);
        lblSleepTag.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x10, 0xB9, 0x81, 140), 1, true),
                BorderFactory.createEmptyBorder(2, 7, 2, 7)
        ));
        sumTopRow.add(lblSleepTag, BorderLayout.EAST);
        sumBox.add(sumTopRow);
        sumBox.add(Box.createVerticalStrut(6));

        // 2. Metrics & Micro-Cards Row
        JPanel metricsRow = new JPanel(new BorderLayout(8, 0));
        metricsRow.setOpaque(false);
        metricsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        metricsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        // Left: Hero Duration display
        JPanel heroDurationBox = new JPanel();
        heroDurationBox.setLayout(new BoxLayout(heroDurationBox, BoxLayout.Y_AXIS));
        heroDurationBox.setOpaque(false);

        lblSleepDuration.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblSleepDuration.setForeground(ThemeConfig.TEXT_PRIMARY);
        heroDurationBox.add(lblSleepDuration);

        lblSleepSub.setFont(ThemeConfig.FONT_SMALL);
        lblSleepSub.setForeground(ThemeConfig.TEXT_MUTED);
        heroDurationBox.add(lblSleepSub);
        metricsRow.add(heroDurationBox, BorderLayout.WEST);

        // Right: Schedule Pill Card
        JPanel pillsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pillsPanel.setOpaque(false);

        // Schedule
        ModernCard pillSchedule = new ModernCard(6, new Insets(4, 10, 4, 10));
        pillSchedule.setBackgroundColor(ThemeConfig.BG_DARK);
        JPanel pSched = new JPanel();
        pSched.setLayout(new BoxLayout(pSched, BoxLayout.Y_AXIS));
        pSched.setOpaque(false);

        JPanel schedHead = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        schedHead.setOpaque(false);
        schedHead.add(new JLabel(IconFactory.createIcon(IconFactory.IconType.TIMER, 10, ThemeConfig.TEXT_MUTED)));
        JLabel lblSchedTitle = new JLabel("SCHEDULE");
        lblSchedTitle.setFont(new Font("Segoe UI", Font.BOLD, 9));
        lblSchedTitle.setForeground(ThemeConfig.TEXT_MUTED);
        schedHead.add(lblSchedTitle);
        pSched.add(schedHead);

        lblSleepTimes.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSleepTimes.setForeground(ThemeConfig.TEXT_PRIMARY);
        pSched.add(lblSleepTimes);
        pillSchedule.add(pSched);
        pillsPanel.add(pillSchedule);

        metricsRow.add(pillsPanel, BorderLayout.EAST);
        sumBox.add(metricsRow);

        // 3. Optional Sleep Note strip
        pnlSleepNotes.setOpaque(false);
        pnlSleepNotes.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlSleepNotes.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeConfig.BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 2, 0, 2)
        ));
        pnlSleepNotes.setVisible(false);

        JPanel notesInner = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        notesInner.setOpaque(false);
        notesInner.add(new JLabel(IconFactory.createIcon(IconFactory.IconType.INFO, 10, ThemeConfig.PRIMARY_ACCENT)));
        lblSleepNotes.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblSleepNotes.setForeground(ThemeConfig.TEXT_SECONDARY);
        notesInner.add(lblSleepNotes);
        pnlSleepNotes.add(notesInner, BorderLayout.WEST);

        sumBox.add(pnlSleepNotes);

        innerSummary.add(sumBox, BorderLayout.CENTER);
        center.add(innerSummary);
        center.add(Box.createVerticalStrut(12));

        // Weekly sleep trend chart
        JPanel chartHeader = new JPanel(new BorderLayout());
        chartHeader.setOpaque(false);
        chartHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        chartHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblChartTitle = new JLabel("Weekly Sleep Duration (Hours)");
        lblChartTitle.setFont(ThemeConfig.FONT_BODY_BOLD);
        lblChartTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
        chartHeader.add(lblChartTitle, BorderLayout.WEST);

        lblSleepAvgAnnotation.setFont(ThemeConfig.FONT_SMALL);
        lblSleepAvgAnnotation.setForeground(ThemeConfig.TEXT_MUTED);
        chartHeader.add(lblSleepAvgAnnotation, BorderLayout.EAST);
        center.add(chartHeader);
        center.add(Box.createVerticalStrut(6));

        pnlSleepChart.setOpaque(false);
        pnlSleepChart.setPreferredSize(new Dimension(0, 175));
        pnlSleepChart.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        pnlSleepChart.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(pnlSleepChart);
        center.add(Box.createVerticalStrut(12));

        // Recent sleep records log
        JPanel sleepLogHeader = new JPanel(new BorderLayout());
        sleepLogHeader.setOpaque(false);
        sleepLogHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        sleepLogHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSleepLogsTitle = new JLabel("Recent Sleep Sessions");
        lblSleepLogsTitle.setFont(ThemeConfig.FONT_BODY_BOLD);
        lblSleepLogsTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
        sleepLogHeader.add(lblSleepLogsTitle, BorderLayout.WEST);
        center.add(sleepLogHeader);
        center.add(Box.createVerticalStrut(6));

        pnlSleepLogs.setLayout(new BoxLayout(pnlSleepLogs, BoxLayout.Y_AXIS));
        pnlSleepLogs.setOpaque(false);
        JScrollPane scrollSleepLogs = new JScrollPane(pnlSleepLogs);
        scrollSleepLogs.setOpaque(false);
        scrollSleepLogs.getViewport().setOpaque(false);
        scrollSleepLogs.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true));
        scrollSleepLogs.setPreferredSize(new Dimension(0, 160));
        scrollSleepLogs.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
        scrollSleepLogs.getVerticalScrollBar().setUnitIncrement(14);
        scrollSleepLogs.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(scrollSleepLogs);

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

            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(true);
            row.setBackground(ThemeConfig.BG_CARD);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeConfig.BORDER_COLOR),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
            ));

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            left.setOpaque(false);
            left.add(new JLabel(IconFactory.createIcon(IconFactory.IconType.WATER, 15, ThemeConfig.PRIMARY_ACCENT)));

            JLabel lblAmount = new JLabel(wi.getAmountMl() + " ml");
            lblAmount.setFont(ThemeConfig.FONT_BODY_BOLD);
            lblAmount.setForeground(ThemeConfig.TEXT_PRIMARY);
            left.add(lblAmount);

            JLabel lblTime = new JLabel("at " + DateUtil.formatTime(wi.getRecordedTime()));
            lblTime.setFont(ThemeConfig.FONT_SMALL);
            lblTime.setForeground(ThemeConfig.TEXT_MUTED);
            left.add(lblTime);
            row.add(left, BorderLayout.WEST);

            JButton btnDel = new JButton("×");
            btnDel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            btnDel.setForeground(ThemeConfig.TEXT_MUTED);
            btnDel.setContentAreaFilled(false);
            btnDel.setBorderPainted(false);
            btnDel.setFocusPainted(false);
            btnDel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnDel.setToolTipText("Remove entry");
            btnDel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { btnDel.setForeground(ThemeConfig.PRIMARY_ACCENT); }
                @Override public void mouseExited(java.awt.event.MouseEvent e) { btnDel.setForeground(ThemeConfig.TEXT_MUTED); }
            });
            btnDel.addActionListener(e -> deleteWater(wi));
            row.add(btnDel, BorderLayout.EAST);

            pnlWaterLogs.add(row);
        }

        int count = todayWater.size();
        lblWaterCount.setText(count + (count == 1 ? " entry" : " entries"));

        if (todayWater.isEmpty()) {
            JPanel emptyRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 16));
            emptyRow.setOpaque(false);
            JLabel lbl = new JLabel("No water recorded today. Click a quick add button above!");
            lbl.setFont(ThemeConfig.FONT_SMALL);
            lbl.setForeground(ThemeConfig.TEXT_MUTED);
            emptyRow.add(lbl);
            pnlWaterLogs.add(emptyRow);
        }

        int target = 2500;
        UserSession sess = SupabaseClient.getInstance().getCurrentSession();
        if (sess != null && sess.getProfile() != null && sess.getProfile().getDailyWaterTargetMl() > 0) {
            target = sess.getProfile().getDailyWaterTargetMl();
        }

        barWater.setMaximum(target);
        barWater.setValue(totalMl);
        lblWaterTotal.setText(String.format("%,d ml", totalMl));
        int pct = (int) Math.round(((double) totalMl / target) * 100);
        int remaining = Math.max(0, target - totalMl);

        lblWaterBadge.setText(pct + "% Goal");
        lblWaterTarget.setText("Target: " + String.format("%,d ml", target) + " (" + pct + "%)");
        lblWaterRemaining.setText(remaining > 0 ? String.format("%,d ml remaining", remaining) : "Daily goal met!");

        cardTodayWater.setValue(String.format("%,d ml", totalMl));
        cardTodayWater.setSubtitle(pct + "% of " + String.format("%,d ml", target) + " goal");

        cardWaterStatus.setValue(pct + "%");
        cardWaterStatus.setSubtitle(remaining > 0 ? String.format("%,d ml to goal", remaining) : "Target achieved!");



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
            double latestHours = latest.getDurationHours();
            if (latestHours > 14.0) latestHours = latestHours % 24.0;
            lblSleepDuration.setText(String.format(Locale.US, "%.1f Hours", latestHours));

            if (latest.getBedtime() != null && latest.getWakeTime() != null) {
                lblSleepTimes.setText(DateUtil.formatTime(latest.getBedtime()) + " → " + DateUtil.formatTime(latest.getWakeTime()));
            } else if (latest.getSleepDate() != null) {
                lblSleepTimes.setText(DateUtil.formatDate(latest.getSleepDate()));
            } else {
                lblSleepTimes.setText("-- → --");
            }

            if (latestHours >= 7.0 && latestHours <= 9.0) {
                lblSleepTag.setText("Optimal Recovery");
                lblSleepTag.setForeground(ThemeConfig.SUCCESS);
                lblSleepTag.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0x10, 0xB9, 0x81, 140), 1, true),
                        BorderFactory.createEmptyBorder(2, 7, 2, 7)
                ));
                lblSleepSub.setText("Within recommended 7.0 - 9.0 hr window");
            } else if (latestHours < 7.0) {
                lblSleepTag.setText(String.format(Locale.US, "Short Sleep (%.1fh)", latestHours));
                lblSleepTag.setForeground(ThemeConfig.WARNING);
                lblSleepTag.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xF5, 0x9E, 0x0B, 140), 1, true),
                        BorderFactory.createEmptyBorder(2, 7, 2, 7)
                ));
                lblSleepSub.setText(String.format(Locale.US, "%.1f hrs below 8.0 hr target", 8.0 - latestHours));
            } else {
                lblSleepTag.setText("Extended Rest");
                lblSleepTag.setForeground(ThemeConfig.PRIMARY_ACCENT);
                lblSleepTag.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xF0, 0x44, 0x44, 140), 1, true),
                        BorderFactory.createEmptyBorder(2, 7, 2, 7)
                ));
                lblSleepSub.setText("Above 9.0 hr standard target");
            }

            if (latest.getNotes() != null && !latest.getNotes().isBlank()) {
                lblSleepNotes.setText("\"" + latest.getNotes().trim() + "\"");
                pnlSleepNotes.setVisible(true);
            } else {
                pnlSleepNotes.setVisible(false);
            }

            cardLastNightSleep.setValue(String.format(Locale.US, "%.1f hrs", latestHours));
            if (latest.getBedtime() != null && latest.getWakeTime() != null) {
                cardLastNightSleep.setSubtitle(DateUtil.formatTime(latest.getBedtime()) + " → " + DateUtil.formatTime(latest.getWakeTime()));
            } else {
                cardLastNightSleep.setSubtitle(latestHours >= 7.0 && latestHours <= 9.0 ? "Target 7-9h met" : "Target: 8.0 hrs/night");
            }

            // Calculate 7-day average
            double sum7 = 0.0;
            int count7 = 0;
            LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
            for (SleepRecord s : sleepRecords) {
                if (s.getSleepDate() != null && !s.getSleepDate().isBefore(sevenDaysAgo)) {
                    double h = s.getDurationHours();
                    if (h > 14.0) h = h % 24.0;
                    sum7 += h;
                    count7++;
                }
            }
            double avg7 = count7 > 0 ? (sum7 / count7) : latestHours;
            cardAvgSleep.setValue(String.format(Locale.US, "%.1f hrs", avg7));
            cardAvgSleep.setSubtitle(avg7 >= 7.0 && avg7 <= 9.0 ? "Optimal recovery range" : "Target: 7.0 - 9.0 hrs");
        } else {
            lblSleepDuration.setText("No sleep logged");
            lblSleepTimes.setText("-- → --");
            lblSleepTag.setText("Target: 8.0 hrs");
            lblSleepTag.setForeground(ThemeConfig.TEXT_MUTED);
            lblSleepTag.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(2, 7, 2, 7)
            ));
            lblSleepSub.setText("Log your sleep session to track recovery");
            pnlSleepNotes.setVisible(false);
            cardLastNightSleep.setValue("-- hrs");
            cardLastNightSleep.setSubtitle("No sleep logged yet");
            cardAvgSleep.setValue("-- hrs");
            cardAvgSleep.setSubtitle("Target: 8.0 hrs/night");
        }

        // Build 7-day sleep chart
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        LocalDate today = LocalDate.now();
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEE");

        double sumWeekHours = 0.0;
        int loggedDaysCount = 0;

        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            double hours = 0.0;
            for (SleepRecord s : sleepRecords) {
                if (s.getSleepDate() != null && s.getSleepDate().isEqual(d)) {
                    hours = s.getDurationHours();
                    if (hours > 14.0) hours = hours % 24.0;
                    break;
                }
            }
            if (hours > 0) {
                sumWeekHours += hours;
                loggedDaysCount++;
            }
            dataset.addValue(hours, "Sleep", d.format(dayFmt));
        }

        double weekAvg = loggedDaysCount > 0 ? (sumWeekHours / loggedDaysCount) : 0.0;
        lblSleepAvgAnnotation.setText(weekAvg > 0 ? String.format(Locale.US, "Week Avg: %.1f hrs", weekAvg) : "Week Avg: --");

        JFreeChart chart = ChartFactory.createBarChart(null, null, null, dataset, PlotOrientation.VERTICAL, false, false, false);
        DashboardPanel.applyDarkChartTheme(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, ThemeConfig.CHART_SLEEP);
        renderer.setBarPainter(new DashboardPanel.ModernRoundedBarPainter());
        renderer.setMaximumBarWidth(0.13);
        renderer.setShadowVisible(false);
        renderer.setDrawBarOutline(false);
        renderer.setItemMargin(0.04);

        // 8h Target guideline line
        ValueMarker targetMarker = new ValueMarker(8.0);
        targetMarker.setPaint(new Color(240, 68, 68, 210));
        targetMarker.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[]{4.0f, 4.0f}, 0.0f));
        targetMarker.setLabel("8h Target");
        targetMarker.setLabelFont(new Font("Segoe UI", Font.BOLD, 11));
        targetMarker.setLabelPaint(new Color(255, 120, 120));
        targetMarker.setLabelAnchor(org.jfree.chart.ui.RectangleAnchor.TOP_RIGHT);
        targetMarker.setLabelTextAnchor(org.jfree.chart.ui.TextAnchor.BOTTOM_RIGHT);
        plot.addRangeMarker(targetMarker);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        double maxLogged = 0.0;
        for (SleepRecord s : sleepRecords) {
            double h = s.getDurationHours();
            if (h > 14.0) h = h % 24.0;
            if (h > maxLogged) {
                maxLogged = h;
            }
        }
        rangeAxis.setRange(0.0, Math.max(10.0, Math.min(14.0, Math.ceil(maxLogged + 1.0))));

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setOpaque(false);
        chartPanel.setBackground(new Color(0, 0, 0, 0));

        pnlSleepChart.removeAll();
        pnlSleepChart.add(chartPanel, BorderLayout.CENTER);
        pnlSleepChart.revalidate();
        pnlSleepChart.repaint();

        // Populate Recent Sleep Sessions
        pnlSleepLogs.removeAll();
        int shown = 0;
        for (SleepRecord s : sleepRecords) {
            if (shown++ >= 6) break;
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(true);
            row.setBackground(ThemeConfig.BG_CARD);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeConfig.BORDER_COLOR),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
            ));

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            left.setOpaque(false);
            left.add(new JLabel(IconFactory.createIcon(IconFactory.IconType.SLEEP, 15, ThemeConfig.PRIMARY_ACCENT)));

            String dateStr = s.getSleepDate() != null ? s.getSleepDate().format(DateTimeFormatter.ofPattern("MMM dd")) : "Past";
            JLabel lblDate = new JLabel(dateStr);
            lblDate.setFont(ThemeConfig.FONT_BODY_BOLD);
            lblDate.setForeground(ThemeConfig.TEXT_PRIMARY);
            left.add(lblDate);

            double dispHours = s.getDurationHours();
            if (dispHours > 14.0) dispHours = dispHours % 24.0;
            JLabel lblHours = new JLabel(String.format(Locale.US, "%.1f hrs", dispHours));
            lblHours.setFont(ThemeConfig.FONT_BODY_BOLD);
            lblHours.setForeground(ThemeConfig.PRIMARY_ACCENT);
            left.add(lblHours);

            if (s.getBedtime() != null && s.getWakeTime() != null) {
                JLabel lblInterval = new JLabel("(" + DateUtil.formatTime(s.getBedtime()) + " → " + DateUtil.formatTime(s.getWakeTime()) + ")");
                lblInterval.setFont(ThemeConfig.FONT_SMALL);
                lblInterval.setForeground(ThemeConfig.TEXT_MUTED);
                left.add(lblInterval);
            }

            row.add(left, BorderLayout.WEST);

            JButton btnDel = new JButton("×");
            btnDel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            btnDel.setForeground(ThemeConfig.TEXT_MUTED);
            btnDel.setContentAreaFilled(false);
            btnDel.setBorderPainted(false);
            btnDel.setFocusPainted(false);
            btnDel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnDel.setToolTipText("Delete record");
            btnDel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { btnDel.setForeground(ThemeConfig.PRIMARY_ACCENT); }
                @Override public void mouseExited(java.awt.event.MouseEvent e) { btnDel.setForeground(ThemeConfig.TEXT_MUTED); }
            });
            btnDel.addActionListener(e -> deleteSleep(s));
            row.add(btnDel, BorderLayout.EAST);

            pnlSleepLogs.add(row);
        }

        if (sleepRecords.isEmpty()) {
            JPanel emptyRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 12));
            emptyRow.setOpaque(false);
            JLabel lbl = new JLabel("No sleep sessions recorded yet. Tap '+ Log Sleep' above.");
            lbl.setFont(ThemeConfig.FONT_SMALL);
            lbl.setForeground(ThemeConfig.TEXT_MUTED);
            emptyRow.add(lbl);
            pnlSleepLogs.add(emptyRow);
        }

        pnlSleepLogs.revalidate();
        pnlSleepLogs.repaint();
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

    private void deleteSleep(SleepRecord sr) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this sleep session?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                sleepDAO.deleteRecord(sr.getId());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    ToastNotification.showToast(WaterSleepPanel.this, "Sleep record deleted", ToastNotification.ToastType.INFO);
                    loadData();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(WaterSleepPanel.this, "Failed to delete sleep record: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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

    // Modal Sleep Log Dialog with Intuitive 12-Hour Pickers, Live Duration & Quality Rating
    private class SleepDialog extends JDialog {
        private static final long serialVersionUID = 1L;
        private boolean saved = false;

        private final ModernTextField txtDate = new ModernTextField(LocalDate.now().toString(), "YYYY-MM-DD");
        private final ModernTextField txtNotes = new ModernTextField("", "e.g. Slept deeply, woke up energized");

        // 12-Hour Time Pickers (No confusing 24h military time needed!)
        private static final String[] HOURS = {"12", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11"};
        private static final String[] MINUTES = {"00", "05", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55"};
        private static final String[] AM_PM = {"AM", "PM"};

        private final JComboBox<String> cmbBedHour = createStyledComboBox(HOURS);
        private final JComboBox<String> cmbBedMin = createStyledComboBox(MINUTES);
        private final JComboBox<String> cmbBedAmPm = createStyledComboBox(AM_PM);

        private final JComboBox<String> cmbWakeHour = createStyledComboBox(HOURS);
        private final JComboBox<String> cmbWakeMin = createStyledComboBox(MINUTES);
        private final JComboBox<String> cmbWakeAmPm = createStyledComboBox(AM_PM);

        private final JLabel lblDurationPreview = new JLabel("🌙 Total Sleep: 8h 00m • Optimal Recovery (Target 7-9 hrs)");
        private int selectedQuality = 4; // Default: Great (4)
        private final List<JButton> qualityButtons = new ArrayList<>();

        public SleepDialog(Window owner) {
            super(owner, "Log Sleep Session", ModalityType.APPLICATION_MODAL);
            setSize(560, 530);
            setMinimumSize(new Dimension(540, 500));
            setLocationRelativeTo(owner);
            setResizable(false);
            getContentPane().setBackground(ThemeConfig.BG_DARK);
            setLayout(new BorderLayout());

            JPanel form = new JPanel();
            form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
            form.setOpaque(false);
            form.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

            // Section 1: Quick Sleep Routine Chips (12-Hour Friendly)
            JLabel lblPresets = new JLabel("Quick 1-Click Sleep Schedules");
            lblPresets.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblPresets.setForeground(ThemeConfig.TEXT_SECONDARY);
            form.add(lblPresets);
            form.add(Box.createVerticalStrut(6));

            JPanel presetRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            presetRow.setOpaque(false);
            presetRow.add(createSleepScheduleChip("🌙 11 PM - 7 AM (8h)", "11", "00", "PM", "07", "00", "AM", "Felt well rested"));
            presetRow.add(createSleepScheduleChip("⚡ 11:30 PM - 7:30 AM (8h)", "11", "30", "PM", "07", "30", "AM", "Solid deep sleep"));
            presetRow.add(createSleepScheduleChip("🌅 10:30 PM - 6:30 AM (8h)", "10", "30", "PM", "06", "30", "AM", "Early to bed"));
            presetRow.add(createSleepScheduleChip("🛋️ 12 AM - 7 AM (7h)", "12", "00", "AM", "07", "00", "AM", "Normal sleep"));
            form.add(presetRow);
            form.add(Box.createVerticalStrut(12));

            // Section 2: Sleep Date
            JPanel dateRow = new JPanel(new BorderLayout(8, 0));
            dateRow.setOpaque(false);
            JLabel lblDateTag = new JLabel("Sleep Date");
            lblDateTag.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblDateTag.setForeground(ThemeConfig.TEXT_SECONDARY);
            dateRow.add(lblDateTag, BorderLayout.NORTH);

            JPanel dateControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            dateControls.setOpaque(false);
            JButton btnToday = createQuickPill("Today", false);
            JButton btnYesterday = createQuickPill("Yesterday", true);
            txtDate.setText(LocalDate.now().minusDays(1).toString()); // Defaults to yesterday (last night's sleep)

            btnToday.addActionListener(e -> {
                txtDate.setText(LocalDate.now().toString());
                ThemeConfig.styleQuickPill(btnToday, true);
                ThemeConfig.styleQuickPill(btnYesterday, false);
            });
            btnYesterday.addActionListener(e -> {
                txtDate.setText(LocalDate.now().minusDays(1).toString());
                ThemeConfig.styleQuickPill(btnYesterday, true);
                ThemeConfig.styleQuickPill(btnToday, false);
            });
            txtDate.setPreferredSize(new Dimension(130, 32));
            dateControls.add(btnYesterday);
            dateControls.add(btnToday);
            dateControls.add(txtDate);
            dateRow.add(dateControls, BorderLayout.CENTER);
            form.add(dateRow);
            form.add(Box.createVerticalStrut(12));

            // Section 3: Intuitive 12-Hour Bedtime & Wake-up Time Pickers
            // Set friendly defaults (11:00 PM to 07:00 AM)
            cmbBedHour.setSelectedItem("11");
            cmbBedMin.setSelectedItem("00");
            cmbBedAmPm.setSelectedItem("PM");

            cmbWakeHour.setSelectedItem("07");
            cmbWakeMin.setSelectedItem("00");
            cmbWakeAmPm.setSelectedItem("AM");

            JPanel timeGrid = new JPanel(new GridLayout(1, 2, 14, 0));
            timeGrid.setOpaque(false);

            timeGrid.add(createTimePickerRow("Bedtime (Fell asleep)", cmbBedHour, cmbBedMin, cmbBedAmPm));
            timeGrid.add(createTimePickerRow("Wake-up Time (Woke up)", cmbWakeHour, cmbWakeMin, cmbWakeAmPm));
            form.add(timeGrid);
            form.add(Box.createVerticalStrut(10));

            // Section 4: Live Calculated Duration & Recovery Indicator Box
            JPanel durationCard = new JPanel(new BorderLayout(8, 0));
            durationCard.setBackground(ThemeConfig.BG_CARD_HOVER);
            durationCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
            lblDurationPreview.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblDurationPreview.setForeground(ThemeConfig.SUCCESS);
            durationCard.add(lblDurationPreview, BorderLayout.CENTER);
            form.add(durationCard);
            form.add(Box.createVerticalStrut(12));

            // Hook live duration updates
            java.awt.event.ActionListener timeChange = e -> updateDurationPreview();
            cmbBedHour.addActionListener(timeChange);
            cmbBedMin.addActionListener(timeChange);
            cmbBedAmPm.addActionListener(timeChange);
            cmbWakeHour.addActionListener(timeChange);
            cmbWakeMin.addActionListener(timeChange);
            cmbWakeAmPm.addActionListener(timeChange);
            updateDurationPreview();

            // Section 5: Restfulness / Sleep Quality Rating (1 to 5)
            JLabel lblQual = new JLabel("Sleep Quality & Restfulness");
            lblQual.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblQual.setForeground(ThemeConfig.TEXT_SECONDARY);
            form.add(lblQual);
            form.add(Box.createVerticalStrut(6));

            JPanel qualRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            qualRow.setOpaque(false);
            qualRow.add(createQualityChip("😴 Poor", 1));
            qualRow.add(createQualityChip("😐 Fair", 2));
            qualRow.add(createQualityChip("😊 Good", 3));
            qualRow.add(createQualityChip("💪 Great", 4));
            qualRow.add(createQualityChip("⚡ Excellent", 5));
            form.add(qualRow);
            form.add(Box.createVerticalStrut(12));

            // Section 6: Notes (Optional)
            form.add(createFieldRow("Notes & Dream Recall (Optional)", txtNotes));
            form.add(Box.createVerticalStrut(10));

            JScrollPane scrollPane = new JScrollPane(form);
            scrollPane.setOpaque(false);
            scrollPane.getViewport().setOpaque(false);
            scrollPane.setBorder(null);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            add(scrollPane, BorderLayout.CENTER);

            // Dedicated Non-Clipping Pinned Actions Footer
            JPanel actions = new JPanel(new BorderLayout(12, 0));
            actions.setOpaque(true);
            actions.setBackground(ThemeConfig.BG_CARD);
            actions.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeConfig.BORDER_COLOR),
                    BorderFactory.createEmptyBorder(12, 24, 14, 24)
            ));

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
            add(actions, BorderLayout.SOUTH);

            // Enter key shortcuts
            txtDate.addActionListener(e -> saveSleep());
            txtNotes.addActionListener(e -> saveSleep());

            getRootPane().registerKeyboardAction(e -> dispose(),
                    KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);
        }

        private JPanel createTimePickerRow(String label, JComboBox<String> cmbH, JComboBox<String> cmbM, JComboBox<String> cmbA) {
            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.setOpaque(false);

            JLabel l = new JLabel(label);
            l.setFont(new Font("Segoe UI", Font.BOLD, 11));
            l.setForeground(ThemeConfig.TEXT_SECONDARY);
            p.add(l);
            p.add(Box.createVerticalStrut(4));

            JPanel picker = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            picker.setOpaque(false);

            cmbH.setPreferredSize(new Dimension(64, 32));
            cmbM.setPreferredSize(new Dimension(64, 32));
            cmbA.setPreferredSize(new Dimension(66, 32));

            JLabel colon = new JLabel(":");
            colon.setFont(new Font("Segoe UI", Font.BOLD, 14));
            colon.setForeground(ThemeConfig.TEXT_MUTED);

            picker.add(cmbH);
            picker.add(colon);
            picker.add(cmbM);
            picker.add(cmbA);
            p.add(picker);
            return p;
        }

        private <T> JComboBox<T> createStyledComboBox(T[] items) {
            JComboBox<T> box = new JComboBox<>(items);
            box.setFont(new Font("Segoe UI", Font.BOLD, 12));
            box.setForeground(ThemeConfig.TEXT_PRIMARY);
            box.setBackground(ThemeConfig.BG_CARD);
            box.setFocusable(false);
            box.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true));
            box.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    lbl.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                    if (isSelected) {
                        lbl.setBackground(ThemeConfig.PRIMARY_ACCENT);
                        lbl.setForeground(ThemeConfig.TEXT_ON_ACCENT);
                    } else {
                        lbl.setBackground(ThemeConfig.BG_CARD);
                        lbl.setForeground(ThemeConfig.TEXT_PRIMARY);
                    }
                    return lbl;
                }
            });
            return box;
        }

        private JButton createQualityChip(String label, int score) {
            JButton btn = new JButton(label);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            boolean isDefault = (score == selectedQuality);
            btn.setForeground(isDefault ? ThemeConfig.TEXT_ON_ACCENT : ThemeConfig.TEXT_PRIMARY);
            btn.setBackground(isDefault ? ThemeConfig.PRIMARY_ACCENT : ThemeConfig.BG_CARD);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(isDefault ? ThemeConfig.PRIMARY_ACCENT : ThemeConfig.BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)
            ));
            qualityButtons.add(btn);

            btn.addActionListener(e -> {
                selectedQuality = score;
                for (JButton b : qualityButtons) {
                    b.setBackground(ThemeConfig.BG_CARD);
                    b.setForeground(ThemeConfig.TEXT_PRIMARY);
                    b.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                            BorderFactory.createEmptyBorder(5, 10, 5, 10)
                    ));
                }
                btn.setBackground(ThemeConfig.PRIMARY_ACCENT);
                btn.setForeground(ThemeConfig.TEXT_ON_ACCENT);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeConfig.PRIMARY_ACCENT, 1, true),
                        BorderFactory.createEmptyBorder(5, 10, 5, 10)
                ));
            });
            return btn;
        }

        private void updateDurationPreview() {
            try {
                int bedH = get24Hour(Integer.parseInt((String) cmbBedHour.getSelectedItem()), (String) cmbBedAmPm.getSelectedItem());
                int bedM = Integer.parseInt((String) cmbBedMin.getSelectedItem());
                int wakeH = get24Hour(Integer.parseInt((String) cmbWakeHour.getSelectedItem()), (String) cmbWakeAmPm.getSelectedItem());
                int wakeM = Integer.parseInt((String) cmbWakeMin.getSelectedItem());

                LocalDate base = LocalDate.now();
                OffsetDateTime bed = OffsetDateTime.now()
                        .withYear(base.getYear()).withMonth(base.getMonthValue()).withDayOfMonth(base.getDayOfMonth())
                        .withHour(bedH).withMinute(bedM).withSecond(0);
                OffsetDateTime wake = OffsetDateTime.now()
                        .withYear(base.getYear()).withMonth(base.getMonthValue()).withDayOfMonth(base.getDayOfMonth())
                        .withHour(wakeH).withMinute(wakeM).withSecond(0);

                if (!wake.isAfter(bed)) {
                    wake = wake.plusDays(1);
                }

                long totalMinutes = java.time.Duration.between(bed, wake).toMinutes();
                long hrs = totalMinutes / 60;
                long mins = totalMinutes % 60;

                String durStr = hrs + "h " + (mins > 0 ? (mins + "m") : "00m");
                if (totalMinutes >= 420 && totalMinutes <= 570) {
                    lblDurationPreview.setText("🌙 Sleep Duration: " + durStr + " • Optimal Recovery (Target: 7-9 hrs)");
                    lblDurationPreview.setForeground(ThemeConfig.SUCCESS);
                } else if (totalMinutes < 420) {
                    lblDurationPreview.setText("⚠️ Sleep Duration: " + durStr + " • Short Sleep (Target 7+ hrs for recovery)");
                    lblDurationPreview.setForeground(ThemeConfig.WARNING);
                } else {
                    lblDurationPreview.setText("🛌 Sleep Duration: " + durStr + " • Extended Recovery");
                    lblDurationPreview.setForeground(ThemeConfig.PRIMARY_ACCENT);
                }
            } catch (Exception ignored) {}
        }

        private int get24Hour(int hour12, String amPm) {
            if ("AM".equalsIgnoreCase(amPm)) {
                return (hour12 == 12) ? 0 : hour12;
            } else {
                return (hour12 == 12) ? 12 : hour12 + 12;
            }
        }

        private JButton createSleepScheduleChip(String label, String bedH, String bedM, String bedAmPm,
                                                String wakeH, String wakeM, String wakeAmPm, String note) {
            JButton btn = new JButton(label);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btn.setForeground(ThemeConfig.TEXT_PRIMARY);
            btn.setBackground(ThemeConfig.BG_CARD);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(5, 8, 5, 8)
            ));
            btn.addActionListener(e -> {
                cmbBedHour.setSelectedItem(bedH);
                cmbBedMin.setSelectedItem(bedM);
                cmbBedAmPm.setSelectedItem(bedAmPm);

                cmbWakeHour.setSelectedItem(wakeH);
                cmbWakeMin.setSelectedItem(wakeM);
                cmbWakeAmPm.setSelectedItem(wakeAmPm);

                txtNotes.setText(note);
                updateDurationPreview();
            });
            return btn;
        }

        private JButton createQuickPill(String text, boolean active) {
            JButton btn = new JButton(text);
            ThemeConfig.styleQuickPill(btn, active);
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
            p.add(Box.createVerticalStrut(5));
            p.add(comp);
            return p;
        }

        private void saveSleep() {
            LocalDate date = DateUtil.parseDateOrNull(txtDate.getText().trim());
            if (date == null) {
                JOptionPane.showMessageDialog(this, "Please enter a valid date in YYYY-MM-DD format.", "Invalid Date", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                int bedH = get24Hour(Integer.parseInt((String) cmbBedHour.getSelectedItem()), (String) cmbBedAmPm.getSelectedItem());
                int bedM = Integer.parseInt((String) cmbBedMin.getSelectedItem());

                int wakeH = get24Hour(Integer.parseInt((String) cmbWakeHour.getSelectedItem()), (String) cmbWakeAmPm.getSelectedItem());
                int wakeM = Integer.parseInt((String) cmbWakeMin.getSelectedItem());

                OffsetDateTime bed = OffsetDateTime.now()
                        .withYear(date.getYear()).withMonth(date.getMonthValue()).withDayOfMonth(date.getDayOfMonth())
                        .withHour(bedH).withMinute(bedM).withSecond(0);

                OffsetDateTime wake = OffsetDateTime.now()
                        .withYear(date.getYear()).withMonth(date.getMonthValue()).withDayOfMonth(date.getDayOfMonth())
                        .withHour(wakeH).withMinute(wakeM).withSecond(0);

                if (!wake.isAfter(bed)) {
                    wake = wake.plusDays(1);
                }

                int quality = selectedQuality;
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
                JOptionPane.showMessageDialog(this, "Could not process selected times: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        public boolean isSaved() {
            return saved;
        }
    }
}
