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

    // Top Summary Stat Cards
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
    private final JLabel lblSleepQuality = new JLabel("-- / 5");
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
        content.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("Hydration & Sleep Tracking");
        title.setFont(ThemeConfig.FONT_TITLE_LARGE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(2));

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
        statsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        statsGrid.add(cardTodayWater);
        statsGrid.add(cardWaterStatus);
        statsGrid.add(cardLastNightSleep);
        statsGrid.add(cardAvgSleep);

        content.add(statsGrid);
        content.add(Box.createVerticalStrut(16));

        // Two Column Main Grid (Water on Left, Sleep on Right)
        JPanel mainGrid = new JPanel(new GridLayout(1, 2, 18, 0));
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
        ModernCard card = new ModernCard(14, new Insets(18, 20, 18, 20));

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
        center.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        // Hero Intake Display Card
        ModernCard heroCard = new ModernCard(10, new Insets(12, 16, 12, 16));
        heroCard.setBackgroundColor(ThemeConfig.BG_INPUT);
        heroCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 105));

        JPanel heroBox = new JPanel();
        heroBox.setLayout(new BoxLayout(heroBox, BoxLayout.Y_AXIS));
        heroBox.setOpaque(false);

        JPanel heroTopRow = new JPanel(new BorderLayout());
        heroTopRow.setOpaque(false);
        lblWaterTotal.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblWaterTotal.setForeground(ThemeConfig.PRIMARY_ACCENT);
        heroTopRow.add(lblWaterTotal, BorderLayout.WEST);

        lblWaterRemaining.setFont(ThemeConfig.FONT_SMALL);
        lblWaterRemaining.setForeground(ThemeConfig.TEXT_MUTED);
        heroTopRow.add(lblWaterRemaining, BorderLayout.EAST);
        heroBox.add(heroTopRow);
        heroBox.add(Box.createVerticalStrut(8));

        barWater.setColors(ThemeConfig.PRIMARY_ACCENT, ThemeConfig.ACCENT_HOVER);
        barWater.setPreferredSize(new Dimension(0, 12));
        barWater.setMaximumSize(new Dimension(Integer.MAX_VALUE, 12));
        heroBox.add(barWater);
        heroBox.add(Box.createVerticalStrut(6));

        lblWaterTarget.setFont(ThemeConfig.FONT_SMALL);
        lblWaterTarget.setForeground(ThemeConfig.TEXT_SECONDARY);
        heroBox.add(lblWaterTarget);

        heroCard.add(heroBox, BorderLayout.CENTER);
        center.add(heroCard);
        center.add(Box.createVerticalStrut(14));

        // Quick add buttons
        JLabel lblQuick = new JLabel("Quick Add Intake");
        lblQuick.setFont(ThemeConfig.FONT_SUBTITLE);
        lblQuick.setForeground(ThemeConfig.TEXT_SECONDARY);
        lblQuick.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(lblQuick);
        center.add(Box.createVerticalStrut(8));

        JPanel btnGrid = new JPanel(new GridLayout(2, 2, 8, 8));
        btnGrid.setOpaque(false);
        btnGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));
        btnGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        ModernButton b250 = new ModernButton("+ 250 ml (Glass)", ModernButton.ButtonStyle.SECONDARY);
        b250.setFont(ThemeConfig.FONT_SMALL);
        b250.setIcon(IconFactory.createIcon(IconFactory.IconType.WATER, 13, ThemeConfig.TEXT_SECONDARY));
        b250.addActionListener(e -> logWater(250));

        ModernButton b500 = new ModernButton("+ 500 ml (Bottle)", ModernButton.ButtonStyle.SECONDARY);
        b500.setFont(ThemeConfig.FONT_SMALL);
        b500.setIcon(IconFactory.createIcon(IconFactory.IconType.WATER, 13, ThemeConfig.TEXT_SECONDARY));
        b500.addActionListener(e -> logWater(500));

        ModernButton b750 = new ModernButton("+ 750 ml (Shaker)", ModernButton.ButtonStyle.SECONDARY);
        b750.setFont(ThemeConfig.FONT_SMALL);
        b750.setIcon(IconFactory.createIcon(IconFactory.IconType.WATER, 13, ThemeConfig.TEXT_SECONDARY));
        b750.addActionListener(e -> logWater(750));

        ModernButton bCustom = new ModernButton("+ Custom Amount", ModernButton.ButtonStyle.PRIMARY);
        bCustom.setFont(ThemeConfig.FONT_SMALL);
        bCustom.setIcon(IconFactory.createIcon(IconFactory.IconType.PLUS, 12, Color.WHITE));
        bCustom.addActionListener(e -> promptCustomWater());

        btnGrid.add(b250);
        btnGrid.add(b500);
        btnGrid.add(b750);
        btnGrid.add(bCustom);
        center.add(btnGrid);
        center.add(Box.createVerticalStrut(16));

        // History logs header
        JPanel logHeader = new JPanel(new BorderLayout());
        logHeader.setOpaque(false);
        logHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        logHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblLog = new JLabel("Today's Water Entries");
        lblLog.setFont(ThemeConfig.FONT_SUBTITLE);
        lblLog.setForeground(ThemeConfig.TEXT_SECONDARY);
        logHeader.add(lblLog, BorderLayout.WEST);

        lblWaterCount.setFont(ThemeConfig.FONT_SMALL);
        lblWaterCount.setForeground(ThemeConfig.TEXT_MUTED);
        logHeader.add(lblWaterCount, BorderLayout.EAST);
        center.add(logHeader);
        center.add(Box.createVerticalStrut(8));

        pnlWaterLogs.setLayout(new BoxLayout(pnlWaterLogs, BoxLayout.Y_AXIS));
        pnlWaterLogs.setOpaque(false);
        JScrollPane scrollLogs = new JScrollPane(pnlWaterLogs);
        scrollLogs.setOpaque(false);
        scrollLogs.getViewport().setOpaque(false);
        scrollLogs.setBorder(BorderFactory.createLineBorder(new Color(0x35, 0x35, 0x35, 120), 1, true));
        scrollLogs.setPreferredSize(new Dimension(0, 190));
        scrollLogs.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        scrollLogs.getVerticalScrollBar().setUnitIncrement(14);
        scrollLogs.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(scrollLogs);

        card.add(center, BorderLayout.CENTER);
        return card;
    }

    private ModernCard createSleepSection() {
        ModernCard card = new ModernCard(14, new Insets(18, 20, 18, 20));

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

        ModernButton btnLogSleep = new ModernButton("+ Log Sleep", ModernButton.ButtonStyle.PRIMARY);
        btnLogSleep.setFont(ThemeConfig.FONT_SMALL);
        btnLogSleep.setIcon(IconFactory.createIcon(IconFactory.IconType.PLUS, 12, Color.WHITE));
        btnLogSleep.setPreferredSize(new Dimension(115, 32));
        btnLogSleep.addActionListener(e -> openSleepDialog());
        top.add(btnLogSleep, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        // Latest summary card with modern segmented layout
        ModernCard innerSummary = new ModernCard(12, new Insets(14, 18, 14, 18));
        innerSummary.setBackgroundColor(ThemeConfig.BG_INPUT);
        innerSummary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        innerSummary.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel sumBox = new JPanel();
        sumBox.setLayout(new BoxLayout(sumBox, BoxLayout.Y_AXIS));
        sumBox.setOpaque(false);

        // 1. Header row
        JPanel sumTopRow = new JPanel(new BorderLayout());
        sumTopRow.setOpaque(false);

        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        titleBox.setOpaque(false);
        titleBox.add(new JLabel(IconFactory.createIcon(IconFactory.IconType.SLEEP, 14, ThemeConfig.PRIMARY_ACCENT)));
        JLabel lblSumTitle = new JLabel("LAST NIGHT'S SLEEP");
        lblSumTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblSumTitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        titleBox.add(lblSumTitle);
        sumTopRow.add(titleBox, BorderLayout.WEST);

        lblSleepTag.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblSleepTag.setForeground(ThemeConfig.SUCCESS);
        lblSleepTag.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x10, 0xB9, 0x81, 140), 1, true),
                BorderFactory.createEmptyBorder(2, 9, 2, 9)
        ));
        sumTopRow.add(lblSleepTag, BorderLayout.EAST);
        sumBox.add(sumTopRow);
        sumBox.add(Box.createVerticalStrut(10));

        // 2. Metrics & Micro-Cards Row
        JPanel metricsRow = new JPanel(new BorderLayout(16, 0));
        metricsRow.setOpaque(false);

        // Left: Hero Duration display
        JPanel heroDurationBox = new JPanel();
        heroDurationBox.setLayout(new BoxLayout(heroDurationBox, BoxLayout.Y_AXIS));
        heroDurationBox.setOpaque(false);

        lblSleepDuration.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblSleepDuration.setForeground(ThemeConfig.TEXT_PRIMARY);
        heroDurationBox.add(lblSleepDuration);
        heroDurationBox.add(Box.createVerticalStrut(2));

        lblSleepSub.setFont(ThemeConfig.FONT_SMALL);
        lblSleepSub.setForeground(ThemeConfig.TEXT_MUTED);
        heroDurationBox.add(lblSleepSub);
        metricsRow.add(heroDurationBox, BorderLayout.WEST);

        // Right: Schedule and Quality Pill Cards
        JPanel pillsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pillsPanel.setOpaque(false);

        // Pill 1: Schedule
        ModernCard pillSchedule = new ModernCard(8, new Insets(6, 12, 6, 12));
        pillSchedule.setBackgroundColor(ThemeConfig.BG_DARK);
        JPanel pSched = new JPanel();
        pSched.setLayout(new BoxLayout(pSched, BoxLayout.Y_AXIS));
        pSched.setOpaque(false);

        JPanel schedHead = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        schedHead.setOpaque(false);
        schedHead.add(new JLabel(IconFactory.createIcon(IconFactory.IconType.TIMER, 10, ThemeConfig.TEXT_MUTED)));
        JLabel lblSchedTitle = new JLabel("SCHEDULE");
        lblSchedTitle.setFont(new Font("Segoe UI", Font.BOLD, 9));
        lblSchedTitle.setForeground(ThemeConfig.TEXT_MUTED);
        schedHead.add(lblSchedTitle);
        pSched.add(schedHead);
        pSched.add(Box.createVerticalStrut(2));

        lblSleepTimes.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSleepTimes.setForeground(ThemeConfig.TEXT_PRIMARY);
        pSched.add(lblSleepTimes);
        pillSchedule.add(pSched);
        pillsPanel.add(pillSchedule);

        // Pill 2: Quality
        ModernCard pillQuality = new ModernCard(8, new Insets(6, 12, 6, 12));
        pillQuality.setBackgroundColor(ThemeConfig.BG_DARK);
        JPanel pQual = new JPanel();
        pQual.setLayout(new BoxLayout(pQual, BoxLayout.Y_AXIS));
        pQual.setOpaque(false);

        JPanel qualHead = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        qualHead.setOpaque(false);
        qualHead.add(new JLabel(IconFactory.createIcon(IconFactory.IconType.STAR, 10, ThemeConfig.WARNING)));
        JLabel lblQualTitle = new JLabel("QUALITY");
        lblQualTitle.setFont(new Font("Segoe UI", Font.BOLD, 9));
        lblQualTitle.setForeground(ThemeConfig.TEXT_MUTED);
        qualHead.add(lblQualTitle);
        pQual.add(qualHead);
        pQual.add(Box.createVerticalStrut(2));

        lblSleepQuality.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSleepQuality.setForeground(ThemeConfig.TEXT_PRIMARY);
        pQual.add(lblSleepQuality);
        pillQuality.add(pQual);
        pillsPanel.add(pillQuality);

        metricsRow.add(pillsPanel, BorderLayout.EAST);
        sumBox.add(metricsRow);

        // 3. Optional Sleep Note strip
        pnlSleepNotes.setOpaque(false);
        pnlSleepNotes.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0x35, 0x35, 0x35, 120)),
                BorderFactory.createEmptyBorder(8, 2, 0, 2)
        ));
        pnlSleepNotes.setVisible(false);

        JPanel notesInner = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        notesInner.setOpaque(false);
        notesInner.add(new JLabel(IconFactory.createIcon(IconFactory.IconType.INFO, 11, ThemeConfig.PRIMARY_ACCENT)));
        lblSleepNotes.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblSleepNotes.setForeground(ThemeConfig.TEXT_SECONDARY);
        notesInner.add(lblSleepNotes);
        pnlSleepNotes.add(notesInner, BorderLayout.WEST);

        sumBox.add(Box.createVerticalStrut(8));
        sumBox.add(pnlSleepNotes);

        innerSummary.add(sumBox, BorderLayout.CENTER);
        center.add(innerSummary);
        center.add(Box.createVerticalStrut(14));

        // Weekly sleep trend chart
        JPanel chartHeader = new JPanel(new BorderLayout());
        chartHeader.setOpaque(false);
        chartHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        chartHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblChartTitle = new JLabel("Weekly Sleep Duration (Hours)");
        lblChartTitle.setFont(ThemeConfig.FONT_SUBTITLE);
        lblChartTitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        chartHeader.add(lblChartTitle, BorderLayout.WEST);

        lblSleepAvgAnnotation.setFont(ThemeConfig.FONT_SMALL);
        lblSleepAvgAnnotation.setForeground(ThemeConfig.TEXT_MUTED);
        chartHeader.add(lblSleepAvgAnnotation, BorderLayout.EAST);
        center.add(chartHeader);
        center.add(Box.createVerticalStrut(8));

        pnlSleepChart.setOpaque(false);
        pnlSleepChart.setPreferredSize(new Dimension(0, 160));
        pnlSleepChart.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
        pnlSleepChart.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(pnlSleepChart);
        center.add(Box.createVerticalStrut(14));

        // Recent sleep records log
        JPanel sleepLogHeader = new JPanel(new BorderLayout());
        sleepLogHeader.setOpaque(false);
        sleepLogHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        sleepLogHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSleepLogsTitle = new JLabel("Recent Sleep Sessions");
        lblSleepLogsTitle.setFont(ThemeConfig.FONT_SUBTITLE);
        lblSleepLogsTitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        sleepLogHeader.add(lblSleepLogsTitle, BorderLayout.WEST);
        center.add(sleepLogHeader);
        center.add(Box.createVerticalStrut(8));

        pnlSleepLogs.setLayout(new BoxLayout(pnlSleepLogs, BoxLayout.Y_AXIS));
        pnlSleepLogs.setOpaque(false);
        JScrollPane scrollSleepLogs = new JScrollPane(pnlSleepLogs);
        scrollSleepLogs.setOpaque(false);
        scrollSleepLogs.getViewport().setOpaque(false);
        scrollSleepLogs.setBorder(BorderFactory.createLineBorder(new Color(0x35, 0x35, 0x35, 120), 1, true));
        scrollSleepLogs.setPreferredSize(new Dimension(0, 105));
        scrollSleepLogs.setMaximumSize(new Dimension(Integer.MAX_VALUE, 125));
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
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x35, 0x35, 0x35, 90)),
                    BorderFactory.createEmptyBorder(7, 10, 7, 10)
            ));

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            left.setOpaque(false);
            left.add(new JLabel(IconFactory.createIcon(IconFactory.IconType.WATER, 14, ThemeConfig.PRIMARY_ACCENT)));

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
            btnDel.setFont(new Font("Segoe UI", Font.BOLD, 15));
            btnDel.setForeground(ThemeConfig.TEXT_MUTED);
            btnDel.setContentAreaFilled(false);
            btnDel.setBorderPainted(false);
            btnDel.setFocusPainted(false);
            btnDel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnDel.addActionListener(e -> deleteWater(wi));
            row.add(btnDel, BorderLayout.EAST);

            pnlWaterLogs.add(row);
        }

        int count = todayWater.size();
        lblWaterCount.setText(count + (count == 1 ? " entry" : " entries"));

        if (todayWater.isEmpty()) {
            JPanel emptyRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 14));
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
        lblWaterRemaining.setText(remaining > 0 ? String.format("%,d ml remaining", remaining) : "Daily goal met! 🎉");

        cardTodayWater.setValue(String.format("%,d ml", totalMl));
        cardTodayWater.setSubtitle(pct + "% of " + String.format("%,d ml", target) + " goal");

        cardWaterStatus.setValue(pct + "%");
        cardWaterStatus.setSubtitle(remaining > 0 ? String.format("%,d ml to goal", remaining) : "Target achieved! 🎉");

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
            lblSleepDuration.setText(String.format(Locale.US, "%.1f Hours", latest.getDurationHours()));

            if (latest.getBedtime() != null && latest.getWakeTime() != null) {
                lblSleepTimes.setText(DateUtil.formatTime(latest.getBedtime()) + " → " + DateUtil.formatTime(latest.getWakeTime()));
            } else if (latest.getSleepDate() != null) {
                lblSleepTimes.setText(DateUtil.formatDate(latest.getSleepDate()));
            } else {
                lblSleepTimes.setText("-- → --");
            }

            int score = latest.getQualityScore() != null ? latest.getQualityScore() : 3;
            String scoreDesc;
            switch (score) {
                case 5: scoreDesc = "Excellent"; break;
                case 4: scoreDesc = "Restful"; break;
                case 3: scoreDesc = "Moderate"; break;
                case 2: scoreDesc = "Light"; break;
                default: scoreDesc = "Poor"; break;
            }
            lblSleepQuality.setText(score + " / 5 (" + scoreDesc + ")");

            if (latest.getDurationHours() >= 7.0 && latest.getDurationHours() <= 9.0) {
                lblSleepTag.setText("Optimal Recovery");
                lblSleepTag.setForeground(ThemeConfig.SUCCESS);
                lblSleepTag.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0x10, 0xB9, 0x81, 140), 1, true),
                        BorderFactory.createEmptyBorder(2, 9, 2, 9)
                ));
                lblSleepSub.setText("Within recommended 7.0 - 9.0 hr window");
            } else if (latest.getDurationHours() < 7.0) {
                lblSleepTag.setText(String.format(Locale.US, "Short Sleep (%.1fh)", latest.getDurationHours()));
                lblSleepTag.setForeground(ThemeConfig.WARNING);
                lblSleepTag.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xF5, 0x9E, 0x0B, 140), 1, true),
                        BorderFactory.createEmptyBorder(2, 9, 2, 9)
                ));
                lblSleepSub.setText(String.format(Locale.US, "%.1f hrs below 8.0 hr target", 8.0 - latest.getDurationHours()));
            } else {
                lblSleepTag.setText("Extended Rest");
                lblSleepTag.setForeground(ThemeConfig.PRIMARY_ACCENT);
                lblSleepTag.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xF0, 0x44, 0x44, 140), 1, true),
                        BorderFactory.createEmptyBorder(2, 9, 2, 9)
                ));
                lblSleepSub.setText("Above 9.0 hr standard target");
            }

            if (latest.getNotes() != null && !latest.getNotes().isBlank()) {
                lblSleepNotes.setText("\"" + latest.getNotes().trim() + "\"");
                pnlSleepNotes.setVisible(true);
            } else {
                pnlSleepNotes.setVisible(false);
            }

            cardLastNightSleep.setValue(String.format(Locale.US, "%.1f hrs", latest.getDurationHours()));
            cardLastNightSleep.setSubtitle("Score: " + score + "/5 · " + scoreDesc);

            // Calculate 7-day average
            double sum7 = 0.0;
            int count7 = 0;
            LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
            for (SleepRecord s : sleepRecords) {
                if (s.getSleepDate() != null && !s.getSleepDate().isBefore(sevenDaysAgo)) {
                    sum7 += s.getDurationHours();
                    count7++;
                }
            }
            double avg7 = count7 > 0 ? (sum7 / count7) : latest.getDurationHours();
            cardAvgSleep.setValue(String.format(Locale.US, "%.1f hrs", avg7));
            cardAvgSleep.setSubtitle(avg7 >= 7.0 && avg7 <= 9.0 ? "Optimal recovery range" : "Target: 7.0 - 9.0 hrs");
        } else {
            lblSleepDuration.setText("No sleep logged");
            lblSleepTimes.setText("-- → --");
            lblSleepQuality.setText("-- / 5");
            lblSleepTag.setText("Target: 8.0 hrs");
            lblSleepTag.setForeground(ThemeConfig.TEXT_MUTED);
            lblSleepTag.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(2, 9, 2, 9)
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
        renderer.setSeriesPaint(0, ThemeConfig.PRIMARY_ACCENT);
        renderer.setMaximumBarWidth(0.18);

        // 8h Target guideline line
        ValueMarker targetMarker = new ValueMarker(8.0);
        targetMarker.setPaint(new Color(240, 68, 68, 140));
        targetMarker.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[]{4.0f, 4.0f}, 0.0f));
        targetMarker.setLabel("8h Goal");
        targetMarker.setLabelFont(ThemeConfig.FONT_SMALL);
        targetMarker.setLabelPaint(ThemeConfig.TEXT_MUTED);
        plot.addRangeMarker(targetMarker);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        double maxLogged = 0.0;
        for (SleepRecord s : sleepRecords) {
            if (s.getDurationHours() > maxLogged) {
                maxLogged = s.getDurationHours();
            }
        }
        rangeAxis.setRange(0.0, Math.max(10.0, Math.ceil(maxLogged + 1.0)));

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
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x35, 0x35, 0x35, 90)),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)
            ));

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            left.setOpaque(false);
            left.add(new JLabel(IconFactory.createIcon(IconFactory.IconType.SLEEP, 13, ThemeConfig.PRIMARY_ACCENT)));

            String dateStr = s.getSleepDate() != null ? s.getSleepDate().format(DateTimeFormatter.ofPattern("MMM dd")) : "Past";
            JLabel lblDate = new JLabel(dateStr);
            lblDate.setFont(ThemeConfig.FONT_BODY_BOLD);
            lblDate.setForeground(ThemeConfig.TEXT_PRIMARY);
            left.add(lblDate);

            JLabel lblHours = new JLabel(String.format(Locale.US, "%.1f hrs", s.getDurationHours()));
            lblHours.setFont(ThemeConfig.FONT_BODY_BOLD);
            lblHours.setForeground(ThemeConfig.PRIMARY_ACCENT);
            left.add(lblHours);

            if (s.getBedtime() != null && s.getWakeTime() != null) {
                JLabel lblInterval = new JLabel("(" + DateUtil.formatTime(s.getBedtime()) + " → " + DateUtil.formatTime(s.getWakeTime()) + ")");
                lblInterval.setFont(ThemeConfig.FONT_SMALL);
                lblInterval.setForeground(ThemeConfig.TEXT_MUTED);
                left.add(lblInterval);
            }

            left.add(new JLabel(IconFactory.createIcon(IconFactory.IconType.STAR, 11, ThemeConfig.WARNING)));
            JLabel lblScore = new JLabel(s.getQualityScore() + "/5");
            lblScore.setFont(ThemeConfig.FONT_SMALL);
            lblScore.setForeground(ThemeConfig.TEXT_SECONDARY);
            left.add(lblScore);

            row.add(left, BorderLayout.WEST);

            JButton btnDel = new JButton("×");
            btnDel.setFont(new Font("Segoe UI", Font.BOLD, 15));
            btnDel.setForeground(ThemeConfig.TEXT_MUTED);
            btnDel.setContentAreaFilled(false);
            btnDel.setBorderPainted(false);
            btnDel.setFocusPainted(false);
            btnDel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
            lblLiveDuration.setForeground(ThemeConfig.SUCCESS);
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
                lblLiveDuration.setForeground(totalMins >= 420 ? ThemeConfig.SUCCESS : ThemeConfig.WARNING);
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
