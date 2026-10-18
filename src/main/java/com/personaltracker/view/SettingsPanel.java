package com.personaltracker.view;

import com.personaltracker.components.ModernButton;
import com.personaltracker.components.ModernCard;
import com.personaltracker.components.ModernPasswordField;
import com.personaltracker.components.ModernTextField;
import com.personaltracker.components.ToastNotification;
import com.personaltracker.config.AppConfig;
import com.personaltracker.config.ThemeConfig;
import com.personaltracker.dao.MeasurementDAO;
import com.personaltracker.dao.ProfileDAO;
import com.personaltracker.model.BodyMeasurement;
import com.personaltracker.model.UserProfile;
import com.personaltracker.model.UserSession;
import com.personaltracker.service.SupabaseAuthService;
import com.personaltracker.service.SupabaseClient;
import com.personaltracker.util.DateUtil;
import com.personaltracker.util.HealthCalculator;
import com.personaltracker.util.IconFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Locale;

/**
 * Enhanced Profile & System Settings View.
 * Provides a rich athlete profile summary, real-time metabolic insights (BMR, TDEE, BMI),
 * customizable targets with macro blueprints, and cloud infrastructure management.
 */
public class SettingsPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final ProfileDAO profileDAO = new ProfileDAO();
    private final MeasurementDAO measurementDAO = new MeasurementDAO();
    private final SupabaseAuthService authService = SupabaseAuthService.getInstance();
    private final AppConfig config = AppConfig.getInstance();

    // Profile Inputs
    private final ModernTextField txtFullName = new ModernTextField();
    private final ModernTextField txtBirthDate = new ModernTextField();
    private final ModernTextField txtHeight = new ModernTextField();
    private final ModernTextField txtTargetWeight = new ModernTextField();
    private final ModernTextField txtCalorieTarget = new ModernTextField();
    private final ModernTextField txtWaterTarget = new ModernTextField();

    // Gender selection state
    private String selectedGender = "Male";
    private final JPanel pnlGenderPills = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));

    // Hero Banner Elements
    private final JLabel lblAvatarInitials = new JLabel("AH", SwingConstants.CENTER);
    private final JLabel lblHeroName = new JLabel("Alex Hunter");
    private final JLabel lblHeroEmail = new JLabel("athlete@demo.com");
    private final JLabel lblHeroMeta = new JLabel("Athlete • Active Session");
    private final JLabel lblHeroBmiChip = new JLabel("BMI: --");
    private final JLabel lblHeroBmrChip = new JLabel("BMR: --");
    private final JLabel lblHeroWeightChip = new JLabel("Goal: --");
    private final JLabel lblHeroFuelChip = new JLabel("Fuel: --");

    // Dynamic Metabolic & Health Insight Labels
    private final JLabel lblAgeInsight = new JLabel("Age: --");
    private final JLabel lblBmiInsight = new JLabel("BMI: --");
    private final JLabel lblBmrInsight = new JLabel("BMR: -- kcal / day");
    private final JLabel lblTdeeInsight = new JLabel("TDEE: -- kcal / day");
    private final JLabel lblHealthyRange = new JLabel("Ideal Weight: --");

    // Dynamic Macro Blueprint Labels
    private final JLabel lblMacroProtein = new JLabel("Protein: -- g (30%)");
    private final JLabel lblMacroCarbs = new JLabel("Carbs: -- g (40%)");
    private final JLabel lblMacroFat = new JLabel("Fat: -- g (30%)");
    private final JProgressBar barMacroProtein = new JProgressBar(0, 100);
    private final JProgressBar barMacroCarbs = new JProgressBar(0, 100);
    private final JProgressBar barMacroFat = new JProgressBar(0, 100);

    // Supabase Cloud Form
    private final JLabel lblConnectionStatus = new JLabel();
    private final ModernTextField txtSupabaseUrl = new ModernTextField();
    private final ModernPasswordField txtAnonKey = new ModernPasswordField("••••••••");

    private double latestLoggedWeight = 75.0;
    private Runnable onProfileUpdatedCallback;

    public SettingsPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(20, 26, 26, 26));

        // 1. Hero Athlete Profile Header
        content.add(createHeroProfileBanner());
        content.add(Box.createVerticalStrut(20));

        // 2. Main Two-Column Grid
        JPanel grid = new JPanel(new GridLayout(1, 2, 20, 0));
        grid.setOpaque(false);

        // Left Column: Personal Identity & Metabolic Insights
        JPanel leftCol = new JPanel();
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
        leftCol.setOpaque(false);
        leftCol.add(createIdentityCard());
        leftCol.add(Box.createVerticalStrut(16));
        leftCol.add(createMetabolicInsightsCard());
        grid.add(leftCol);

        // Right Column: Nutritional Blueprint & Cloud Infrastructure
        JPanel rightCol = new JPanel();
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
        rightCol.setOpaque(false);
        rightCol.add(createTargetsAndMacrosCard());
        rightCol.add(Box.createVerticalStrut(16));
        rightCol.add(createCloudBackendCard());
        grid.add(rightCol);

        content.add(grid);
        content.add(Box.createVerticalStrut(20));

        // 3. Bottom Action Bar
        content.add(createBottomActionBar());

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Wire calculation listeners
        attachCalculationListeners();
    }

    public void setOnProfileUpdatedCallback(Runnable callback) {
        this.onProfileUpdatedCallback = callback;
    }

    // =========================================================================
    // UI BUILDERS
    // =========================================================================

    private JPanel createHeroProfileBanner() {
        ModernCard heroCard = new ModernCard(16, new Insets(18, 22, 18, 22));

        JPanel banner = new JPanel(new BorderLayout(18, 0));
        banner.setOpaque(false);

        // Avatar circle
        JPanel avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Background circle
                g2.setColor(new Color(36, 36, 36));
                g2.fill(new Ellipse2D.Float(0, 0, 64, 64));
                // Accent border ring
                g2.setColor(ThemeConfig.PRIMARY_ACCENT);
                g2.setStroke(new BasicStroke(2.5f));
                g2.draw(new Ellipse2D.Float(1.5f, 1.5f, 61, 61));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatarPanel.setPreferredSize(new Dimension(64, 64));
        avatarPanel.setOpaque(false);
        avatarPanel.setLayout(new BorderLayout());

        lblAvatarInitials.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblAvatarInitials.setForeground(Color.WHITE);
        avatarPanel.add(lblAvatarInitials, BorderLayout.CENTER);
        banner.add(avatarPanel, BorderLayout.WEST);

        // User Identity Details
        JPanel idPanel = new JPanel(new GridLayout(3, 1, 0, 3));
        idPanel.setOpaque(false);

        lblHeroName.setFont(ThemeConfig.FONT_TITLE_LARGE);
        lblHeroName.setForeground(ThemeConfig.TEXT_PRIMARY);

        lblHeroEmail.setFont(ThemeConfig.FONT_BODY);
        lblHeroEmail.setForeground(ThemeConfig.TEXT_SECONDARY);

        lblHeroMeta.setFont(ThemeConfig.FONT_SMALL);
        lblHeroMeta.setForeground(ThemeConfig.PRIMARY_ACCENT);

        idPanel.add(lblHeroName);
        idPanel.add(lblHeroEmail);
        idPanel.add(lblHeroMeta);
        banner.add(idPanel, BorderLayout.CENTER);

        // Quick Snapshot Badges (Right)
        JPanel statsBadgeGrid = new JPanel(new GridLayout(2, 2, 8, 8));
        statsBadgeGrid.setOpaque(false);

        statsBadgeGrid.add(createMiniStatChip("BMI Status", lblHeroBmiChip, ThemeConfig.PRIMARY_ACCENT));
        statsBadgeGrid.add(createMiniStatChip("Resting BMR", lblHeroBmrChip, ThemeConfig.PRIMARY_BLUE));
        statsBadgeGrid.add(createMiniStatChip("Target Weight", lblHeroWeightChip, ThemeConfig.SUCCESS_EMERALD));
        statsBadgeGrid.add(createMiniStatChip("Daily Target", lblHeroFuelChip, ThemeConfig.WARNING_AMBER));

        banner.add(statsBadgeGrid, BorderLayout.EAST);
        heroCard.add(banner, BorderLayout.CENTER);
        return heroCard;
    }

    private JPanel createMiniStatChip(String title, JLabel valueLbl, Color accentColor) {
        JPanel chip = new JPanel(new GridLayout(2, 1, 0, 2));
        chip.setBackground(ThemeConfig.BG_DARK);
        chip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        JLabel tLbl = new JLabel(title.toUpperCase());
        tLbl.setFont(new Font("Segoe UI", Font.BOLD, 9));
        tLbl.setForeground(ThemeConfig.TEXT_MUTED);

        valueLbl.setFont(ThemeConfig.FONT_BODY_BOLD);
        valueLbl.setForeground(accentColor);

        chip.add(tLbl);
        chip.add(valueLbl);
        return chip;
    }

    private JPanel createIdentityCard() {
        ModernCard card = new ModernCard(14, new Insets(18, 20, 18, 20));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("1. Personal & Physical Parameters");
        title.setFont(ThemeConfig.FONT_TITLE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        card.add(title);
        card.add(Box.createVerticalStrut(14));

        // Full Name
        card.add(createFieldHeader("Full Name"));
        card.add(Box.createVerticalStrut(4));
        card.add(txtFullName);
        card.add(Box.createVerticalStrut(12));

        // Gender Pills
        card.add(createFieldHeader("Gender"));
        card.add(Box.createVerticalStrut(4));
        pnlGenderPills.setOpaque(false);
        refreshGenderPills();
        card.add(pnlGenderPills);
        card.add(Box.createVerticalStrut(12));

        // Date of Birth & Live Age Pill
        JPanel dobHeader = new JPanel(new BorderLayout());
        dobHeader.setOpaque(false);
        dobHeader.add(createFieldHeader("Date of Birth (YYYY-MM-DD)"), BorderLayout.WEST);
        lblAgeInsight.setFont(ThemeConfig.FONT_SMALL);
        lblAgeInsight.setForeground(ThemeConfig.PRIMARY_BLUE);
        dobHeader.add(lblAgeInsight, BorderLayout.EAST);
        card.add(dobHeader);
        card.add(Box.createVerticalStrut(4));
        card.add(txtBirthDate);
        card.add(Box.createVerticalStrut(12));

        // Height & Target Weight Row
        JPanel hwGrid = new JPanel(new GridLayout(1, 2, 12, 0));
        hwGrid.setOpaque(false);

        // Height with steppers
        JPanel pnlH = new JPanel();
        pnlH.setLayout(new BoxLayout(pnlH, BoxLayout.Y_AXIS));
        pnlH.setOpaque(false);

        JPanel hHead = new JPanel(new BorderLayout());
        hHead.setOpaque(false);
        hHead.add(createFieldHeader("Height (cm)"), BorderLayout.WEST);
        JPanel hSteps = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        hSteps.setOpaque(false);
        hSteps.add(createStepper("-5", () -> adjustDoubleField(txtHeight, -5.0)));
        hSteps.add(createStepper("+5", () -> adjustDoubleField(txtHeight, +5.0)));
        hHead.add(hSteps, BorderLayout.EAST);

        pnlH.add(hHead);
        pnlH.add(Box.createVerticalStrut(4));
        pnlH.add(txtHeight);
        hwGrid.add(pnlH);

        // Target Weight with steppers
        JPanel pnlW = new JPanel();
        pnlW.setLayout(new BoxLayout(pnlW, BoxLayout.Y_AXIS));
        pnlW.setOpaque(false);

        JPanel wHead = new JPanel(new BorderLayout());
        wHead.setOpaque(false);
        wHead.add(createFieldHeader("Target Weight (kg)"), BorderLayout.WEST);
        JPanel wSteps = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        wSteps.setOpaque(false);
        wSteps.add(createStepper("-1", () -> adjustDoubleField(txtTargetWeight, -1.0)));
        wSteps.add(createStepper("+1", () -> adjustDoubleField(txtTargetWeight, +1.0)));
        wHead.add(wSteps, BorderLayout.EAST);

        pnlW.add(wHead);
        pnlW.add(Box.createVerticalStrut(4));
        pnlW.add(txtTargetWeight);
        hwGrid.add(pnlW);

        card.add(hwGrid);
        return card;
    }

    private JPanel createMetabolicInsightsCard() {
        ModernCard card = new ModernCard(14, new Insets(18, 20, 18, 20));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("2. Smart Metabolic Insights (Mifflin-St Jeor)");
        title.setFont(ThemeConfig.FONT_TITLE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        card.add(title);
        card.add(Box.createVerticalStrut(6));

        JLabel subtitle = new JLabel("Calculated in real-time using clinical metabolic expenditure formulas.");
        subtitle.setFont(ThemeConfig.FONT_SMALL);
        subtitle.setForeground(ThemeConfig.TEXT_MUTED);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(14));

        // Metric Badges Grid
        JPanel metricBox = new JPanel(new GridLayout(2, 2, 10, 10));
        metricBox.setOpaque(false);

        metricBox.add(createInsightBox("BODY MASS INDEX", lblBmiInsight, ThemeConfig.PRIMARY_ACCENT, "World Health Organization standard"));
        metricBox.add(createInsightBox("BASAL METABOLIC RATE", lblBmrInsight, ThemeConfig.PRIMARY_BLUE, "Resting daily caloric burn"));
        metricBox.add(createInsightBox("ESTIMATED TDEE", lblTdeeInsight, ThemeConfig.SUCCESS_EMERALD, "Active maintenance expenditure"));
        metricBox.add(createInsightBox("HEALTHY WEIGHT RANGE", lblHealthyRange, ThemeConfig.WARNING_AMBER, "Normal BMI (18.5 - 24.9)"));

        card.add(metricBox);
        return card;
    }

    private JPanel createInsightBox(String header, JLabel valueLbl, Color accent, String footer) {
        JPanel box = new JPanel(new BorderLayout(0, 4));
        box.setBackground(ThemeConfig.BG_DARK);
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JLabel hLbl = new JLabel(header);
        hLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        hLbl.setForeground(ThemeConfig.TEXT_MUTED);

        valueLbl.setFont(ThemeConfig.FONT_BODY_BOLD);
        valueLbl.setForeground(accent);

        JLabel fLbl = new JLabel(footer);
        fLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        fLbl.setForeground(ThemeConfig.TEXT_MUTED);

        box.add(hLbl, BorderLayout.NORTH);
        box.add(valueLbl, BorderLayout.CENTER);
        box.add(fLbl, BorderLayout.SOUTH);
        return box;
    }

    private JPanel createTargetsAndMacrosCard() {
        ModernCard card = new ModernCard(14, new Insets(18, 20, 18, 20));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("3. Daily Targets & Macro Blueprint");
        title.setFont(ThemeConfig.FONT_TITLE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        card.add(title);
        card.add(Box.createVerticalStrut(14));

        // Calories & Water inputs
        JPanel cwGrid = new JPanel(new GridLayout(1, 2, 12, 0));
        cwGrid.setOpaque(false);

        // Daily Calories
        JPanel pnlCal = new JPanel();
        pnlCal.setLayout(new BoxLayout(pnlCal, BoxLayout.Y_AXIS));
        pnlCal.setOpaque(false);
        pnlCal.add(createFieldHeader("Daily Calories (kcal)"));
        pnlCal.add(Box.createVerticalStrut(4));
        pnlCal.add(txtCalorieTarget);

        JPanel calChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        calChips.setOpaque(false);
        calChips.add(createPresetChip("1800", () -> txtCalorieTarget.setText("1800")));
        calChips.add(createPresetChip("2200", () -> txtCalorieTarget.setText("2200")));
        calChips.add(createPresetChip("2600", () -> txtCalorieTarget.setText("2600")));
        pnlCal.add(calChips);
        cwGrid.add(pnlCal);

        // Daily Water
        JPanel pnlWat = new JPanel();
        pnlWat.setLayout(new BoxLayout(pnlWat, BoxLayout.Y_AXIS));
        pnlWat.setOpaque(false);
        pnlWat.add(createFieldHeader("Daily Water (ml)"));
        pnlWat.add(Box.createVerticalStrut(4));
        pnlWat.add(txtWaterTarget);

        JPanel watChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        watChips.setOpaque(false);
        watChips.add(createPresetChip("2000ml", () -> txtWaterTarget.setText("2000")));
        watChips.add(createPresetChip("2500ml", () -> txtWaterTarget.setText("2500")));
        watChips.add(createPresetChip("3000ml", () -> txtWaterTarget.setText("3000")));
        pnlWat.add(watChips);
        cwGrid.add(pnlWat);

        card.add(cwGrid);
        card.add(Box.createVerticalStrut(14));

        // Suggested Macro Blueprint Breakdown
        JLabel lblMacroTitle = createFieldHeader("Calculated Macro Split (30% Protein / 40% Carbs / 30% Fat):");
        card.add(lblMacroTitle);
        card.add(Box.createVerticalStrut(8));

        JPanel macroPanel = new JPanel(new GridLayout(3, 1, 0, 6));
        macroPanel.setBackground(ThemeConfig.BG_DARK);
        macroPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        lblMacroProtein.setFont(ThemeConfig.FONT_SMALL);
        lblMacroProtein.setForeground(ThemeConfig.PRIMARY_ACCENT);
        macroPanel.add(lblMacroProtein);

        lblMacroCarbs.setFont(ThemeConfig.FONT_SMALL);
        lblMacroCarbs.setForeground(ThemeConfig.PRIMARY_BLUE);
        macroPanel.add(lblMacroCarbs);

        lblMacroFat.setFont(ThemeConfig.FONT_SMALL);
        lblMacroFat.setForeground(ThemeConfig.WARNING_AMBER);
        macroPanel.add(lblMacroFat);

        card.add(macroPanel);
        return card;
    }

    private JPanel createCloudBackendCard() {
        ModernCard card = new ModernCard(14, new Insets(18, 20, 18, 20));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("4. Supabase Cloud & Account Security");
        title.setFont(ThemeConfig.FONT_TITLE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        card.add(title);
        card.add(Box.createVerticalStrut(10));

        // Connection status pill
        lblConnectionStatus.setFont(ThemeConfig.FONT_BODY_BOLD);
        card.add(lblConnectionStatus);
        card.add(Box.createVerticalStrut(10));

        card.add(createFieldHeader("Supabase Project URL"));
        card.add(Box.createVerticalStrut(4));
        card.add(txtSupabaseUrl);
        card.add(Box.createVerticalStrut(10));

        card.add(createFieldHeader("Supabase Anon / Public API Key"));
        card.add(Box.createVerticalStrut(4));
        card.add(txtAnonKey);
        card.add(Box.createVerticalStrut(12));

        // Action Buttons Row
        JPanel btnRow = new JPanel(new GridLayout(1, 3, 8, 0));
        btnRow.setOpaque(false);

        ModernButton btnSaveConn = new ModernButton("Save Connection", ModernButton.ButtonStyle.PRIMARY);
        btnSaveConn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnSaveConn.addActionListener(e -> saveCredentials());
        btnRow.add(btnSaveConn);

        ModernButton btnResetPwd = new ModernButton("Reset Password", ModernButton.ButtonStyle.SECONDARY);
        btnResetPwd.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnResetPwd.addActionListener(e -> sendPasswordReset());
        btnRow.add(btnResetPwd);

        ModernButton btnLaunchWizard = new ModernButton("✨ Setup Wizard", ModernButton.ButtonStyle.SECONDARY);
        btnLaunchWizard.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnLaunchWizard.addActionListener(e -> launchOnboardingWizard());
        btnRow.add(btnLaunchWizard);

        card.add(btnRow);
        return card;
    }

    private JPanel createBottomActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        bar.setOpaque(false);

        ModernButton btnReset = new ModernButton("Revert to Saved", ModernButton.ButtonStyle.SECONDARY);
        btnReset.setPreferredSize(new Dimension(150, 40));
        btnReset.addActionListener(e -> loadSettings());
        bar.add(btnReset);

        ModernButton btnSave = new ModernButton("Save All Changes 💾", ModernButton.ButtonStyle.PRIMARY);
        btnSave.setPreferredSize(new Dimension(200, 40));
        btnSave.addActionListener(e -> saveProfileDetails());
        bar.add(btnSave);

        return bar;
    }

    // =========================================================================
    // HELPERS & DYNAMIC CALCULATIONS
    // =========================================================================

    private JLabel createFieldHeader(String text) {
        JLabel l = new JLabel(text);
        l.setFont(ThemeConfig.FONT_SMALL);
        l.setForeground(ThemeConfig.TEXT_SECONDARY);
        return l;
    }

    private JButton createStepper(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 10));
        b.setForeground(ThemeConfig.TEXT_PRIMARY);
        b.setBackground(ThemeConfig.BG_DARK);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        return b;
    }

    private JButton createPresetChip(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        b.setForeground(ThemeConfig.TEXT_SECONDARY);
        b.setBackground(ThemeConfig.BG_DARK);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> {
            action.run();
            updateCalculations();
        });
        return b;
    }

    private void adjustDoubleField(ModernTextField field, double delta) {
        try {
            double current = Double.parseDouble(field.getText().trim());
            double updated = Math.max(10.0, current + delta);
            field.setText(String.format(Locale.US, "%.1f", updated));
            updateCalculations();
        } catch (Exception ignored) {}
    }

    private void refreshGenderPills() {
        pnlGenderPills.removeAll();
        String[] options = {"Male", "Female", "Non-Binary", "Other"};
        for (String opt : options) {
            boolean isSelected = opt.equalsIgnoreCase(selectedGender);
            JButton pill = new JButton(opt);
            pill.setFont(ThemeConfig.FONT_SMALL);
            pill.setForeground(isSelected ? Color.WHITE : ThemeConfig.TEXT_SECONDARY);
            pill.setBackground(isSelected ? ThemeConfig.PRIMARY_ACCENT : ThemeConfig.BG_DARK);
            pill.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(isSelected ? ThemeConfig.PRIMARY_ACCENT : ThemeConfig.BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(4, 10, 4, 10)
            ));
            pill.setFocusPainted(false);
            pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            pill.addActionListener(e -> {
                selectedGender = opt;
                refreshGenderPills();
                pnlGenderPills.revalidate();
                pnlGenderPills.repaint();
                updateCalculations();
            });
            pnlGenderPills.add(pill);
        }
    }

    private void attachCalculationListeners() {
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updateCalculations(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updateCalculations(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updateCalculations(); }
        };
        txtBirthDate.getDocument().addDocumentListener(listener);
        txtHeight.getDocument().addDocumentListener(listener);
        txtTargetWeight.getDocument().addDocumentListener(listener);
        txtCalorieTarget.getDocument().addDocumentListener(listener);
        txtFullName.getDocument().addDocumentListener(listener);
    }

    private void updateCalculations() {
        // Full name banner
        String name = txtFullName.getText().trim();
        if (!name.isEmpty()) {
            lblHeroName.setText(name);
            String initials = getInitials(name);
            lblAvatarInitials.setText(initials);
        }

        double h = parseDouble(txtHeight.getText().trim(), 175.0);
        double targetW = parseDouble(txtTargetWeight.getText().trim(), 70.0);
        double activeWeight = latestLoggedWeight > 20.0 ? latestLoggedWeight : targetW;

        // 1. Age Calculation
        LocalDate dob = DateUtil.parseDateOrNull(txtBirthDate.getText().trim());
        int age = 26; // Default fallback age
        if (dob != null) {
            age = Period.between(dob, LocalDate.now()).getYears();
            lblAgeInsight.setText(String.format("Age: %d yrs (Born %d)", age, dob.getYear()));
        } else {
            lblAgeInsight.setText("Age: --");
        }

        // 2. BMI Calculation
        if (h > 50 && activeWeight > 20) {
            double bmi = HealthCalculator.calculateBmi(activeWeight, h);
            String category = HealthCalculator.getBmiCategory(bmi);
            lblBmiInsight.setText(String.format(Locale.US, "%.1f • %s", bmi, category));
            lblHeroBmiChip.setText(String.format(Locale.US, "%.1f (%s)", bmi, category));
        } else {
            lblBmiInsight.setText("BMI: --");
            lblHeroBmiChip.setText("BMI: --");
        }

        // 3. Basal Metabolic Rate (BMR) - Mifflin-St Jeor
        if (h > 50 && activeWeight > 20 && age > 10) {
            double baseBmr = (10.0 * activeWeight) + (6.25 * h) - (5.0 * age);
            double bmr = "Female".equalsIgnoreCase(selectedGender) ? (baseBmr - 161.0) : (baseBmr + 5.0);
            int bmrInt = (int) Math.round(bmr);
            lblBmrInsight.setText(String.format(Locale.US, "%s kcal / day", NumberFormatUtil(bmrInt)));
            lblHeroBmrChip.setText(String.format(Locale.US, "%d kcal", bmrInt));

            // 4. Estimated TDEE (Sedentary / Light Active 1.375)
            int tdeeInt = (int) Math.round(bmr * 1.375);
            lblTdeeInsight.setText(String.format(Locale.US, "%s kcal / day (Light-Mod)", NumberFormatUtil(tdeeInt)));
        } else {
            lblBmrInsight.setText("BMR: --");
            lblHeroBmrChip.setText("BMR: --");
            lblTdeeInsight.setText("TDEE: --");
        }

        // 5. Healthy Weight Range for Height (BMI 18.5 - 24.9)
        if (h > 50) {
            double hm = h / 100.0;
            double minWeight = 18.5 * (hm * hm);
            double maxWeight = 24.9 * (hm * hm);
            lblHealthyRange.setText(String.format(Locale.US, "%.1f kg - %.1f kg", minWeight, maxWeight));
        } else {
            lblHealthyRange.setText("Ideal Weight: --");
        }

        // 6. Target Weight Chip
        lblHeroWeightChip.setText(String.format(Locale.US, "%.1f kg", targetW));

        // 7. Macro Blueprint Split based on Calorie Target
        int calories = parseInt(txtCalorieTarget.getText().trim(), 2000);
        lblHeroFuelChip.setText(String.format(Locale.US, "%d kcal", calories));

        double proteinG = (calories * 0.30) / 4.0;
        double carbsG = (calories * 0.40) / 4.0;
        double fatG = (calories * 0.30) / 9.0;

        lblMacroProtein.setText(String.format(Locale.US, "🥩 Protein (30%%): %.0f g  (~%.0f kcal)", proteinG, proteinG * 4));
        lblMacroCarbs.setText(String.format(Locale.US, "🍚 Carbohydrates (40%%): %.0f g  (~%.0f kcal)", carbsG, carbsG * 4));
        lblMacroFat.setText(String.format(Locale.US, "🥑 Healthy Fats (30%%): %.0f g  (~%.0f kcal)", fatG, fatG * 9));
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "PG";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    private String NumberFormatUtil(int number) {
        return String.format(Locale.US, "%,d", number);
    }

    private double parseDouble(String str, double def) {
        try {
            return Double.parseDouble(str);
        } catch (Exception e) {
            return def;
        }
    }

    private int parseInt(String str, int def) {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            return def;
        }
    }

    // =========================================================================
    // DATA LOADING & PERSISTENCE
    // =========================================================================

    public void loadSettings() {
        UserSession session = SupabaseClient.getInstance().getCurrentSession();
        if (session != null) {
            lblHeroEmail.setText(session.getEmail() != null ? session.getEmail() : "athlete@personaltracker.com");

            UserProfile p = session.getProfile();
            if (p != null) {
                txtFullName.setText(p.getFullName());
                if (p.getGender() != null && !p.getGender().isBlank()) {
                    selectedGender = p.getGender();
                    refreshGenderPills();
                }
                txtBirthDate.setText(p.getDateOfBirth() != null ? p.getDateOfBirth().toString() : "");
                txtHeight.setText(p.getHeightCm() != null ? String.format(Locale.US, "%.1f", p.getHeightCm()) : "");
                txtTargetWeight.setText(p.getTargetWeightKg() != null ? String.format(Locale.US, "%.1f", p.getTargetWeightKg()) : "");
                txtCalorieTarget.setText(String.valueOf(p.getDailyCalorieTarget()));
                txtWaterTarget.setText(String.valueOf(p.getDailyWaterTargetMl()));

                if (p.getCreatedAt() != null) {
                    lblHeroMeta.setText("Athlete • Member since " + p.getCreatedAt().getYear());
                }
            }
        }

        // Fetch latest measurement weight asynchronously
        SwingWorker<Double, Void> weightWorker = new SwingWorker<>() {
            @Override
            protected Double doInBackground() throws Exception {
                List<BodyMeasurement> list = measurementDAO.getAllMeasurements();
                if (list != null && !list.isEmpty()) {
                    return list.get(0).getWeightKg();
                }
                return 75.0;
            }

            @Override
            protected void done() {
                try {
                    latestLoggedWeight = get();
                    updateCalculations();
                } catch (Exception ignored) {}
            }
        };
        weightWorker.execute();

        // Supabase Connection status
        txtSupabaseUrl.setText(config.getSupabaseUrl());
        txtAnonKey.setText(config.getSupabaseAnonKey());

        if (config.isMockMode()) {
            lblConnectionStatus.setText("● Local In-Memory Demo Mode (Offline)");
            lblConnectionStatus.setForeground(ThemeConfig.WARNING_AMBER);
        } else {
            lblConnectionStatus.setText("● Connected to Supabase Cloud PostgreSQL & Auth");
            lblConnectionStatus.setForeground(ThemeConfig.SUCCESS_EMERALD);
        }

        updateCalculations();
    }

    private void saveProfileDetails() {
        UserSession session = SupabaseClient.getInstance().getCurrentSession();
        if (session == null) return;

        UserProfile p = session.getProfile();
        if (p == null) p = new UserProfile(session.getUserId(), session.getEmail(), "");

        p.setFullName(txtFullName.getText().trim());
        p.setGender(selectedGender);

        LocalDate dob = DateUtil.parseDateOrNull(txtBirthDate.getText().trim());
        if (dob != null) p.setDateOfBirth(dob);

        try {
            if (!txtHeight.getText().trim().isEmpty()) p.setHeightCm(Double.parseDouble(txtHeight.getText().trim()));
            if (!txtTargetWeight.getText().trim().isEmpty()) p.setTargetWeightKg(Double.parseDouble(txtTargetWeight.getText().trim()));
            if (!txtCalorieTarget.getText().trim().isEmpty()) p.setDailyCalorieTarget(Integer.parseInt(txtCalorieTarget.getText().trim()));
            if (!txtWaterTarget.getText().trim().isEmpty()) p.setDailyWaterTargetMl(Integer.parseInt(txtWaterTarget.getText().trim()));
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please verify numeric values for height, weight, and targets.");
            return;
        }

        final UserProfile target = p;
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                profileDAO.updateProfile(target);
                return null;
            }

            @Override
            protected void done() {
                ToastNotification.showToast(SettingsPanel.this, "Profile updated successfully! 🚀", ToastNotification.ToastType.SUCCESS);
                if (onProfileUpdatedCallback != null) onProfileUpdatedCallback.run();
                loadSettings();
            }
        };
        worker.execute();
    }

    private void saveCredentials() {
        String url = txtSupabaseUrl.getText().trim();
        String anon = new String(txtAnonKey.getPassword()).trim();

        config.saveRuntimeCredentials(url, anon);
        loadSettings();
        ToastNotification.showToast(this, "Supabase connection updated! ⚡", ToastNotification.ToastType.INFO);
    }

    private void sendPasswordReset() {
        UserSession session = SupabaseClient.getInstance().getCurrentSession();
        if (session != null && session.getEmail() != null) {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    authService.resetPassword(session.getEmail());
                    return null;
                }
                @Override
                protected void done() {
                    ToastNotification.showToast(SettingsPanel.this, "Password recovery link dispatched to: " + session.getEmail(), ToastNotification.ToastType.SUCCESS);
                }
            };
            worker.execute();
        }
    }

    private void launchOnboardingWizard() {
        UserSession session = SupabaseClient.getInstance().getCurrentSession();
        if (session != null) {
            UserProfile p = session.getProfile();
            OnboardingDialog dialog = new OnboardingDialog(SwingUtilities.getWindowAncestor(this), p);
            dialog.setVisible(true);
            loadSettings();
            if (onProfileUpdatedCallback != null) onProfileUpdatedCallback.run();
        }
    }
}
