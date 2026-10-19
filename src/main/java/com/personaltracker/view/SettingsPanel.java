package com.personaltracker.view;

import com.personaltracker.components.ModernButton;
import com.personaltracker.components.ModernCard;
import com.personaltracker.components.ModernPasswordField;
import com.personaltracker.components.ModernProgressBar;
import com.personaltracker.components.ModernTextField;
import com.personaltracker.components.ToastNotification;
import com.personaltracker.config.AppConfig;
import com.personaltracker.config.ThemeConfig;
import com.personaltracker.dao.MeasurementDAO;
import com.personaltracker.dao.NutritionDAO;
import com.personaltracker.dao.ProfileDAO;
import com.personaltracker.dao.WorkoutDAO;
import com.personaltracker.model.BodyMeasurement;
import com.personaltracker.model.NutritionEntry;
import com.personaltracker.model.UserProfile;
import com.personaltracker.model.UserSession;
import com.personaltracker.model.Workout;
import com.personaltracker.service.SupabaseAuthService;
import com.personaltracker.service.SupabaseClient;
import com.personaltracker.util.CsvExporter;
import com.personaltracker.util.DateUtil;
import com.personaltracker.util.HealthCalculator;
import com.personaltracker.util.IconFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Locale;

/**
 * Personal Profile & Tracking Dashboard.
 * Non-editable tracking scene that displays personal health parameters,
 * journey progress, clinical metabolic calculations, and provides an option
 * to edit profile details in a clean dedicated dialog.
 */
public class SettingsPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final ProfileDAO profileDAO = new ProfileDAO();
    private final MeasurementDAO measurementDAO = new MeasurementDAO();
    private final WorkoutDAO workoutDAO = new WorkoutDAO();
    private final NutritionDAO nutritionDAO = new NutritionDAO();
    private final SupabaseAuthService authService = SupabaseAuthService.getInstance();
    private final AppConfig config = AppConfig.getInstance();

    // Hero Banner Elements
    private final JLabel lblAvatarInitials = new JLabel("AH", SwingConstants.CENTER);
    private final JLabel lblHeroName = new JLabel("Alex Hunter");
    private final JLabel lblHeroEmail = new JLabel("athlete@demo.com");
    private final JLabel lblHeroMeta = new JLabel("Athlete • Active Profile");
    private final JLabel lblStatusBadge = new JLabel("● ACTIVE SESSION", SwingConstants.CENTER);
    private JPanel avatarPanel;

    // Personal Info Display Labels (Clean, non-editable)
    private final JLabel valFullName = new JLabel("--");
    private final JLabel valEmail = new JLabel("--");
    private final JLabel valRole = new JLabel("--");
    private final JLabel valUserCode = new JLabel("--");
    private final JLabel valGender = new JLabel("--");
    private final JLabel valBirthDate = new JLabel("--");
    private final JLabel valAge = new JLabel("--");
    private final JLabel valHeight = new JLabel("--");

    // Weight Journey Tracking Labels
    private final JLabel valStartingWeight = new JLabel("-- kg");
    private final JLabel valCurrentWeight = new JLabel("-- kg");
    private final JLabel valTargetWeight = new JLabel("-- kg");
    private final JLabel valGoalStatus = new JLabel("Goal: --");
    private final ModernProgressBar barWeightProgress = new ModernProgressBar(0, 100);
    private final JLabel lblProgressPercent = new JLabel("0% Completed");

    // Metabolic & Clinical Health Labels
    private final JLabel valBmi = new JLabel("--");
    private final JLabel valBmr = new JLabel("-- kcal / day");
    private final JLabel valTdee = new JLabel("-- kcal / day");
    private final JLabel valIdealRange = new JLabel("-- kg");

    // Account & Sync Info
    private final JLabel lblConnectionStatus = new JLabel("Cloud Synced • Active Session");
    private final JLabel lblAccountEmail = new JLabel("--");

    private Runnable onProfileUpdatedCallback;

    public SettingsPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(20, 28, 28, 28));

        // 1. Hero Athlete Header Card with Edit Button
        content.add(createHeroBanner());
        content.add(Box.createVerticalStrut(20));

        // 2. Main Two-Column Tracking Grid
        JPanel grid = new JPanel(new GridLayout(1, 2, 20, 0));
        grid.setOpaque(false);

        // Left Column: Personal Identity & Weight Journey Tracker
        JPanel leftCol = new JPanel();
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
        leftCol.setOpaque(false);
        leftCol.add(createPersonalInfoCard());
        leftCol.add(Box.createVerticalStrut(16));
        leftCol.add(createWeightJourneyCard());
        grid.add(leftCol);

        // Right Column: Clinical Metabolic Health & Cloud Backend
        JPanel rightCol = new JPanel();
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
        rightCol.setOpaque(false);
        rightCol.add(createMetabolicHealthCard());
        rightCol.add(Box.createVerticalStrut(16));
        rightCol.add(createAccountSecurityCard());
        rightCol.add(Box.createVerticalStrut(16));
        rightCol.add(createDataReportsCard());
        grid.add(rightCol);

        content.add(grid);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setOnProfileUpdatedCallback(Runnable callback) {
        this.onProfileUpdatedCallback = callback;
    }

    // =========================================================================
    // UI BUILDERS (NON-EDITABLE TRACKING SCENE)
    // =========================================================================

    private JPanel createHeroBanner() {
        ModernCard heroCard = new ModernCard(16, new Insets(20, 24, 20, 24));

        JPanel banner = new JPanel(new BorderLayout(20, 0));
        banner.setOpaque(false);

        // Avatar ring with mathematically centered initials
        avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int size = 70;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                // Subtle dark circular fill
                g2.setColor(new Color(28, 28, 28));
                g2.fill(new Ellipse2D.Float(x, y, size, size));

                // Crimson accent outer ring
                g2.setColor(ThemeConfig.PRIMARY_ACCENT);
                g2.setStroke(new BasicStroke(2.6f));
                g2.draw(new Ellipse2D.Float(x + 1.5f, y + 1.5f, size - 3f, size - 3f));

                // Centered initials
                String initials = lblAvatarInitials.getText();
                if (initials == null || initials.isBlank()) initials = "PG";
                g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int tx = x + (size - fm.stringWidth(initials)) / 2;
                int ty = y + (size - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(initials, tx, ty);

                g2.dispose();
            }
        };
        avatarPanel.setPreferredSize(new Dimension(76, 76));
        avatarPanel.setMinimumSize(new Dimension(76, 76));
        avatarPanel.setOpaque(false);
        banner.add(avatarPanel, BorderLayout.WEST);

        // Center: Name & Metadata
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS));
        namePanel.setOpaque(false);

        // Top line: Name + Active badge
        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        nameRow.setOpaque(false);
        lblHeroName.setFont(ThemeConfig.FONT_TITLE_LARGE);
        lblHeroName.setForeground(ThemeConfig.TEXT_PRIMARY);
        nameRow.add(lblHeroName);

        lblStatusBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblStatusBadge.setForeground(ThemeConfig.SUCCESS);
        lblStatusBadge.setText("● ACTIVE SESSION");
        lblStatusBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x10, 0xB9, 0x81, 120), 1, true),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        nameRow.add(lblStatusBadge);
        namePanel.add(nameRow);
        namePanel.add(Box.createVerticalStrut(4));

        // Middle line: Email
        JPanel emailRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        emailRow.setOpaque(false);
        JLabel emailIcon = new JLabel(IconFactory.createIcon(IconFactory.IconType.USER, 13, ThemeConfig.TEXT_MUTED));
        emailRow.add(emailIcon);
        lblHeroEmail.setFont(ThemeConfig.FONT_BODY);
        lblHeroEmail.setForeground(ThemeConfig.TEXT_SECONDARY);
        emailRow.add(lblHeroEmail);
        namePanel.add(emailRow);
        namePanel.add(Box.createVerticalStrut(6));

        // Bottom line: Role & Code metadata
        JPanel metaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        metaRow.setOpaque(false);
        lblHeroMeta.setFont(ThemeConfig.FONT_SMALL);
        lblHeroMeta.setForeground(ThemeConfig.PRIMARY_ACCENT);
        metaRow.add(lblHeroMeta);
        namePanel.add(metaRow);

        banner.add(namePanel, BorderLayout.CENTER);

        // Right: Export Report & Edit Profile Action Buttons
        JPanel actionBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionBox.setOpaque(false);

        ModernButton btnExportReport = new ModernButton("Export Report", ModernButton.ButtonStyle.SECONDARY);
        btnExportReport.setIcon(IconFactory.createIcon(IconFactory.IconType.ANALYTICS, 13, ThemeConfig.TEXT_SECONDARY));
        btnExportReport.setIconTextGap(6);
        btnExportReport.setPreferredSize(new Dimension(145, 42));
        btnExportReport.addActionListener(e -> promptExportReport());
        actionBox.add(btnExportReport);

        ModernButton btnEditProfile = new ModernButton("Edit Profile Info", ModernButton.ButtonStyle.PRIMARY);
        btnEditProfile.setIcon(IconFactory.createIcon(IconFactory.IconType.EDIT, 13, Color.WHITE));
        btnEditProfile.setIconTextGap(8);
        btnEditProfile.setPreferredSize(new Dimension(175, 42));
        btnEditProfile.addActionListener(e -> openEditProfileDialog());
        actionBox.add(btnEditProfile);

        banner.add(actionBox, BorderLayout.EAST);
        heroCard.add(banner, BorderLayout.CENTER);
        return heroCard;
    }

    private JPanel createPersonalInfoCard() {
        ModernCard card = new ModernCard(14, new Insets(18, 20, 18, 20));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Personal Information & Unique ID");
        title.setFont(ThemeConfig.FONT_TITLE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        card.add(title);
        card.add(Box.createVerticalStrut(14));

        // Unique Member / Student Code Banner
        JPanel codeBanner = new JPanel(new BorderLayout(12, 0));
        codeBanner.setBackground(ThemeConfig.BG_DARK);
        codeBanner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.PRIMARY_ACCENT, 1, true),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));

        JPanel codeText = new JPanel(new GridLayout(2, 1, 0, 2));
        codeText.setOpaque(false);
        JLabel lblCodeTitle = new JLabel("UNIQUE STUDENT / USER ID CODE");
        lblCodeTitle.setFont(new Font("Segoe UI", Font.BOLD, 9));
        lblCodeTitle.setForeground(ThemeConfig.PRIMARY_ACCENT);
        codeText.add(lblCodeTitle);

        valUserCode.setFont(new Font("Segoe UI", Font.BOLD, 18));
        valUserCode.setForeground(Color.WHITE);
        codeText.add(valUserCode);
        codeBanner.add(codeText, BorderLayout.CENTER);

        JButton btnCopyCode = new JButton("Copy Code");
        btnCopyCode.setIcon(IconFactory.createIcon(IconFactory.IconType.COPY, 13, Color.WHITE));
        btnCopyCode.setIconTextGap(6);
        btnCopyCode.setHorizontalTextPosition(SwingConstants.RIGHT);
        btnCopyCode.setFont(ThemeConfig.FONT_SMALL);
        btnCopyCode.setForeground(Color.WHITE);
        btnCopyCode.setBackground(ThemeConfig.BG_CARD);
        btnCopyCode.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        btnCopyCode.setFocusPainted(false);
        btnCopyCode.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCopyCode.addActionListener(e -> {
            String code = valUserCode.getText();
            java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(code);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            ToastNotification.showToast(SettingsPanel.this, "Copied code: " + code + " to clipboard!", ToastNotification.ToastType.SUCCESS);
        });
        codeBanner.add(btnCopyCode, BorderLayout.EAST);

        card.add(codeBanner);
        card.add(Box.createVerticalStrut(10));

        JLabel lblCodeHint = new JLabel("Share this unique code with your trainer so they can monitor your progress in their roster.");
        lblCodeHint.setIcon(IconFactory.createIcon(IconFactory.IconType.INFO, 12, ThemeConfig.TEXT_MUTED));
        lblCodeHint.setIconTextGap(6);
        lblCodeHint.setFont(ThemeConfig.FONT_SMALL);
        lblCodeHint.setForeground(ThemeConfig.TEXT_MUTED);
        card.add(lblCodeHint);
        card.add(Box.createVerticalStrut(14));

        JPanel grid = new JPanel(new GridLayout(3, 2, 14, 14));
        grid.setOpaque(false);

        grid.add(createInfoTile("FULL NAME", valFullName, ThemeConfig.TEXT_PRIMARY));
        grid.add(createInfoTile("ACCOUNT ROLE", valRole, ThemeConfig.PRIMARY_ACCENT));
        grid.add(createInfoTile("GENDER", valGender, ThemeConfig.TEXT_PRIMARY));
        grid.add(createInfoTile("DATE OF BIRTH", valBirthDate, ThemeConfig.TEXT_SECONDARY));
        grid.add(createInfoTile("CURRENT AGE", valAge, ThemeConfig.PRIMARY_ACCENT));
        grid.add(createInfoTile("RECORDED HEIGHT", valHeight, ThemeConfig.SUCCESS));

        card.add(grid);
        return card;
    }

    private JPanel createWeightJourneyCard() {
        ModernCard card = new ModernCard(14, new Insets(18, 20, 18, 20));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Weight Journey & Goal Progress");
        title.setFont(ThemeConfig.FONT_TITLE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        card.add(title);
        card.add(Box.createVerticalStrut(14));

        // 3 Milestone Cards: Starting -> Current -> Target
        JPanel milestoneGrid = new JPanel(new GridLayout(1, 3, 10, 0));
        milestoneGrid.setOpaque(false);

        milestoneGrid.add(createMetricBox("STARTING WEIGHT", valStartingWeight, ThemeConfig.TEXT_SECONDARY));
        milestoneGrid.add(createMetricBox("CURRENT WEIGHT", valCurrentWeight, ThemeConfig.PRIMARY_ACCENT));
        milestoneGrid.add(createMetricBox("TARGET GOAL", valTargetWeight, ThemeConfig.SUCCESS));
        card.add(milestoneGrid);
        card.add(Box.createVerticalStrut(16));

        // Goal Status text
        JPanel statusRow = new JPanel(new BorderLayout());
        statusRow.setOpaque(false);
        valGoalStatus.setFont(ThemeConfig.FONT_BODY_BOLD);
        valGoalStatus.setForeground(ThemeConfig.TEXT_PRIMARY);
        statusRow.add(valGoalStatus, BorderLayout.WEST);

        lblProgressPercent.setFont(ThemeConfig.FONT_BODY_BOLD);
        lblProgressPercent.setForeground(ThemeConfig.PRIMARY_ACCENT);
        statusRow.add(lblProgressPercent, BorderLayout.EAST);
        card.add(statusRow);
        card.add(Box.createVerticalStrut(8));

        // Modern Progress Bar
        barWeightProgress.setPreferredSize(new Dimension(Integer.MAX_VALUE, 12));
        card.add(barWeightProgress);

        return card;
    }

    private JPanel createMetabolicHealthCard() {
        ModernCard card = new ModernCard(14, new Insets(18, 20, 18, 20));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Metabolic Profile & Clinical Benchmarks");
        title.setFont(ThemeConfig.FONT_TITLE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        card.add(title);
        card.add(Box.createVerticalStrut(6));

        JLabel sub = new JLabel("Automated calculations based on clinical Mifflin-St Jeor & WHO standards.");
        sub.setFont(ThemeConfig.FONT_SMALL);
        sub.setForeground(ThemeConfig.TEXT_MUTED);
        card.add(sub);
        card.add(Box.createVerticalStrut(14));

        JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
        grid.setOpaque(false);

        grid.add(createMetricBox("BODY MASS INDEX", valBmi, ThemeConfig.PRIMARY_ACCENT));
        grid.add(createMetricBox("BASAL METABOLIC RATE", valBmr, ThemeConfig.PRIMARY_ACCENT));
        grid.add(createMetricBox("ESTIMATED TDEE", valTdee, ThemeConfig.SUCCESS));
        grid.add(createMetricBox("HEALTHY WEIGHT RANGE", valIdealRange, ThemeConfig.WARNING));

        card.add(grid);
        return card;
    }

    private JPanel createAccountSecurityCard() {
        ModernCard card = new ModernCard(14, new Insets(18, 20, 18, 20));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Account & Security");
        title.setFont(ThemeConfig.FONT_TITLE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        card.add(title);
        card.add(Box.createVerticalStrut(12));

        // Account status & email strip
        JPanel statusStrip = new JPanel(new BorderLayout(8, 0));
        statusStrip.setOpaque(false);

        lblConnectionStatus.setFont(ThemeConfig.FONT_BODY_BOLD);
        statusStrip.add(lblConnectionStatus, BorderLayout.WEST);

        lblAccountEmail.setFont(ThemeConfig.FONT_SMALL);
        lblAccountEmail.setForeground(ThemeConfig.TEXT_SECONDARY);
        statusStrip.add(lblAccountEmail, BorderLayout.EAST);
        card.add(statusStrip);
        card.add(Box.createVerticalStrut(8));

        JLabel lblSecurityHint = new JLabel("Your personal health metrics and training history are safely synchronized.");
        lblSecurityHint.setFont(ThemeConfig.FONT_SMALL);
        lblSecurityHint.setForeground(ThemeConfig.TEXT_MUTED);
        card.add(lblSecurityHint);
        card.add(Box.createVerticalStrut(16));

        // Actions: Reset Password & Setup Wizard
        JPanel btnRow = new JPanel(new GridLayout(1, 2, 10, 0));
        btnRow.setOpaque(false);

        ModernButton btnReset = new ModernButton("Reset Password", ModernButton.ButtonStyle.SECONDARY);
        btnReset.setFont(ThemeConfig.FONT_SMALL);
        btnReset.setIcon(IconFactory.createIcon(IconFactory.IconType.USER, 13, ThemeConfig.TEXT_SECONDARY));
        btnReset.setIconTextGap(6);
        btnReset.addActionListener(e -> sendPasswordReset());
        btnRow.add(btnReset);

        ModernButton btnWizard = new ModernButton("Setup Wizard", ModernButton.ButtonStyle.SECONDARY);
        btnWizard.setFont(ThemeConfig.FONT_SMALL);
        btnWizard.setIcon(IconFactory.createIcon(IconFactory.IconType.REFRESH, 13, ThemeConfig.TEXT_SECONDARY));
        btnWizard.setIconTextGap(6);
        btnWizard.addActionListener(e -> launchOnboardingWizard());
        btnRow.add(btnWizard);

        card.add(btnRow);
        return card;
    }

    private JPanel createInfoTile(String header, JLabel valueLbl, Color valueColor) {
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBackground(ThemeConfig.BG_DARK);
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JLabel hLbl = new JLabel(header);
        hLbl.setFont(new Font("Segoe UI", Font.BOLD, 9));
        hLbl.setForeground(ThemeConfig.TEXT_MUTED);
        tile.add(hLbl);
        tile.add(Box.createVerticalStrut(4));

        valueLbl.setFont(ThemeConfig.FONT_BODY_BOLD);
        valueLbl.setForeground(valueColor);
        tile.add(valueLbl);
        return tile;
    }

    private JPanel createMetricBox(String header, JLabel valueLbl, Color accent) {
        JPanel box = new JPanel(new BorderLayout(0, 4));
        box.setBackground(ThemeConfig.BG_DARK);
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JLabel hLbl = new JLabel(header);
        hLbl.setFont(new Font("Segoe UI", Font.BOLD, 9));
        hLbl.setForeground(ThemeConfig.TEXT_MUTED);

        valueLbl.setFont(ThemeConfig.FONT_BODY_BOLD);
        valueLbl.setForeground(accent);

        box.add(hLbl, BorderLayout.NORTH);
        box.add(valueLbl, BorderLayout.CENTER);
        return box;
    }

    // =========================================================================
    // DATA LOADING & REFRESH
    // =========================================================================

    public void loadSettings() {
        UserSession session = SupabaseClient.getInstance().getCurrentSession();
        if (session == null) return;

        UserProfile profile = session.getProfile();
        if (profile == null) {
            try {
                profile = profileDAO.getProfile(session.getUserId());
            } catch (Exception ignored) {}
        }

        if (profile != null) {
            String name = profile.getDisplayName();
            lblHeroName.setText(name);
            valFullName.setText(profile.getFullName() != null && !profile.getFullName().isBlank() ? profile.getFullName() : name);
            lblAvatarInitials.setText(getInitials(name));

            String email = session.getEmail() != null ? session.getEmail() : profile.getEmail();
            lblHeroEmail.setText(email != null ? email : "athlete@personaltracker.com");
            valEmail.setText(email != null ? email : "--");

            valGender.setText(profile.getGender() != null && !profile.getGender().isBlank() ? profile.getGender() : "Not specified");

            if (profile.getDateOfBirth() != null) {
                LocalDate dob = profile.getDateOfBirth();
                valBirthDate.setText(DateUtil.formatDate(dob));
                int age = Period.between(dob, LocalDate.now()).getYears();
                valAge.setText(age + " years old");
            } else {
                valBirthDate.setText("Not specified");
                valAge.setText("--");
            }

            if (profile.getHeightCm() != null && profile.getHeightCm() > 0) {
                valHeight.setText(String.format(Locale.US, "%.1f cm", profile.getHeightCm()));
            } else {
                valHeight.setText("-- cm");
            }

            if (profile.getTargetWeightKg() != null && profile.getTargetWeightKg() > 0) {
                valTargetWeight.setText(String.format(Locale.US, "%.1f kg", profile.getTargetWeightKg()));
            } else {
                valTargetWeight.setText("-- kg");
            }

            String roleDisplay = profile.isTrainer() ? "Personal Trainer" : "Member / Athlete";
            valRole.setText(roleDisplay);
            valUserCode.setText(profile.getUserCode());

            if (profile.getCreatedAt() != null) {
                lblHeroMeta.setText(roleDisplay + " • Code: " + profile.getUserCode() + " • Member since " + profile.getCreatedAt().getYear());
            } else {
                lblHeroMeta.setText(roleDisplay + " • Code: " + profile.getUserCode());
            }
            if (avatarPanel != null) {
                avatarPanel.repaint();
            }
        }

        // Fetch body measurement history to calculate current vs starting weight journey
        final UserProfile userProf = profile;
        SwingWorker<List<BodyMeasurement>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<BodyMeasurement> doInBackground() throws Exception {
                return measurementDAO.getAllMeasurements();
            }

            @Override
            protected void done() {
                try {
                    List<BodyMeasurement> measurements = get();
                    double currentWeight = 75.0;
                    double startingWeight = 75.0;
                    double targetWeight = (userProf != null && userProf.getTargetWeightKg() != null) ? userProf.getTargetWeightKg() : 70.0;
                    double height = (userProf != null && userProf.getHeightCm() != null) ? userProf.getHeightCm() : 175.0;
                    String gender = (userProf != null && userProf.getGender() != null) ? userProf.getGender() : "Male";
                    int age = (userProf != null && userProf.getDateOfBirth() != null) ? Period.between(userProf.getDateOfBirth(), LocalDate.now()).getYears() : 25;

                    if (!measurements.isEmpty()) {
                        currentWeight = measurements.get(0).getWeightKg();
                        startingWeight = measurements.get(measurements.size() - 1).getWeightKg();
                    } else if (userProf != null && userProf.getTargetWeightKg() != null) {
                        currentWeight = userProf.getTargetWeightKg();
                        startingWeight = currentWeight;
                    }

                    valStartingWeight.setText(String.format(Locale.US, "%.1f kg", startingWeight));
                    valCurrentWeight.setText(String.format(Locale.US, "%.1f kg", currentWeight));
                    valTargetWeight.setText(String.format(Locale.US, "%.1f kg", targetWeight));

                    // Calculate weight journey progress
                    double totalDeltaToLose = startingWeight - targetWeight;
                    double deltaRemaining = currentWeight - targetWeight;

                    if (Math.abs(deltaRemaining) < 0.2) {
                        valGoalStatus.setText("Goal Achieved! Maintaining at " + String.format(Locale.US, "%.1f kg", currentWeight));
                        barWeightProgress.setValue(100);
                        lblProgressPercent.setText("100% Completed");
                    } else if (totalDeltaToLose > 0.5) {
                        // Weight loss goal
                        double lost = startingWeight - currentWeight;
                        int percent = (int) Math.min(100, Math.max(0, (lost / totalDeltaToLose) * 100));
                        valGoalStatus.setText(String.format(Locale.US, "%.1f kg remaining to reach target", Math.max(0, deltaRemaining)));
                        barWeightProgress.setValue(percent);
                        lblProgressPercent.setText(percent + "% Completed");
                    } else if (totalDeltaToLose < -0.5) {
                        // Muscle gain goal
                        double gained = currentWeight - startingWeight;
                        double totalToGain = targetWeight - startingWeight;
                        int percent = (int) Math.min(100, Math.max(0, (gained / totalToGain) * 100));
                        valGoalStatus.setText(String.format(Locale.US, "%.1f kg remaining to reach target", Math.max(0, -deltaRemaining)));
                        barWeightProgress.setValue(percent);
                        lblProgressPercent.setText(percent + "% Completed");
                    } else {
                        valGoalStatus.setText("Weight maintenance in progress");
                        barWeightProgress.setValue(100);
                        lblProgressPercent.setText("Maintained");
                    }

                    // Clinical calculations
                    if (height > 50 && currentWeight > 20) {
                        double bmi = HealthCalculator.calculateBmi(currentWeight, height);
                        String category = HealthCalculator.getBmiCategory(bmi);
                        valBmi.setText(String.format(Locale.US, "%.1f • %s", bmi, category));

                        // BMR Mifflin-St Jeor
                        double baseBmr = (10.0 * currentWeight) + (6.25 * height) - (5.0 * age);
                        double bmr = "Female".equalsIgnoreCase(gender) ? (baseBmr - 161.0) : (baseBmr + 5.0);
                        int bmrInt = (int) Math.round(bmr);
                        valBmr.setText(String.format(Locale.US, "%,d kcal / day", bmrInt));

                        int tdeeInt = (int) Math.round(bmr * 1.375);
                        valTdee.setText(String.format(Locale.US, "%,d kcal / day", tdeeInt));

                        double hm = height / 100.0;
                        double minW = 18.5 * (hm * hm);
                        double maxW = 24.9 * (hm * hm);
                        valIdealRange.setText(String.format(Locale.US, "%.1f kg - %.1f kg", minW, maxW));
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();

        // Account Sync status
        if (session.getEmail() != null) {
            lblAccountEmail.setText(session.getEmail());
        }
        if (config.isMockMode()) {
            lblConnectionStatus.setText("Demo Mode (Offline)");
            lblConnectionStatus.setForeground(ThemeConfig.WARNING);
            lblConnectionStatus.setIcon(IconFactory.createIcon(IconFactory.IconType.INFO, 12, ThemeConfig.WARNING));
        } else {
            lblConnectionStatus.setText("Cloud Synced • Active Session");
            lblConnectionStatus.setForeground(ThemeConfig.SUCCESS);
            lblConnectionStatus.setIcon(IconFactory.createIcon(IconFactory.IconType.CHECK, 12, ThemeConfig.SUCCESS));
        }
        lblConnectionStatus.setIconTextGap(6);
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "PG";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    // =========================================================================
    // DIALOGS & ACTIONS
    // =========================================================================

    /**
     * Opens the dedicated, elegant Edit Profile modal dialog.
     */
    private void openEditProfileDialog() {
        UserSession session = SupabaseClient.getInstance().getCurrentSession();
        if (session == null) return;

        UserProfile profile = session.getProfile();
        if (profile == null) {
            profile = new UserProfile(session.getUserId(), session.getEmail(), "Athlete");
            session.setProfile(profile);
        }

        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Edit Personal Profile", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(480, 580);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.getContentPane().setBackground(ThemeConfig.BG_DARK);

        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
        pnl.setOpaque(false);
        pnl.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("Edit Personal Profile Info");
        title.setFont(ThemeConfig.FONT_TITLE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        pnl.add(title);
        pnl.add(Box.createVerticalStrut(14));

        ModernTextField txtName = new ModernTextField(profile.getFullName());
        JComboBox<String> cmbRole = new JComboBox<>(new String[]{"Normal Member", "Personal Trainer"});
        cmbRole.setSelectedIndex(profile.isTrainer() ? 1 : 0);

        JComboBox<String> cmbGen = new JComboBox<>(new String[]{"Male", "Female", "Non-Binary", "Other", "Prefer not to say"});
        if (profile.getGender() != null) cmbGen.setSelectedItem(profile.getGender());

        ModernTextField txtDob = new ModernTextField(profile.getDateOfBirth() != null ? profile.getDateOfBirth().toString() : "", "YYYY-MM-DD");
        ModernTextField txtH = new ModernTextField(profile.getHeightCm() != null && profile.getHeightCm() > 0 ? String.format(Locale.US, "%.1f", profile.getHeightCm()) : "", "e.g. 175.0");
        ModernTextField txtW = new ModernTextField(profile.getTargetWeightKg() != null && profile.getTargetWeightKg() > 0 ? String.format(Locale.US, "%.1f", profile.getTargetWeightKg()) : "", "e.g. 70.0");

        pnl.add(createFieldRow("Full Name", txtName));
        pnl.add(Box.createVerticalStrut(10));
        pnl.add(createFieldRow("Account Role", cmbRole));
        pnl.add(Box.createVerticalStrut(10));
        pnl.add(createFieldRow("Gender", cmbGen));
        pnl.add(Box.createVerticalStrut(10));
        pnl.add(createFieldRow("Date of Birth (YYYY-MM-DD)", txtDob));
        pnl.add(Box.createVerticalStrut(10));

        JPanel hw = new JPanel(new GridLayout(1, 2, 10, 0));
        hw.setOpaque(false);
        hw.add(createFieldRow("Height (cm)", txtH));
        hw.add(createFieldRow("Target Weight (kg)", txtW));
        pnl.add(hw);
        pnl.add(Box.createVerticalStrut(20));

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnBar.setOpaque(false);

        ModernButton btnCancel = new ModernButton("Cancel", ModernButton.ButtonStyle.SECONDARY);
        btnCancel.addActionListener(e -> dlg.dispose());
        btnBar.add(btnCancel);

        final UserProfile targetProfile = profile;
        ModernButton btnSave = new ModernButton("Save Profile", ModernButton.ButtonStyle.PRIMARY);
        btnSave.addActionListener(e -> {
            String nameVal = txtName.getText().trim();
            if (nameVal.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Please enter a valid name.");
                return;
            }

            try {
                double heightVal = Double.parseDouble(txtH.getText().trim());
                double targetWeightVal = Double.parseDouble(txtW.getText().trim());
                LocalDate dobVal = DateUtil.parseDateOrNull(txtDob.getText().trim());

                targetProfile.setFullName(nameVal);
                targetProfile.setRole(cmbRole.getSelectedIndex() == 1 ? UserProfile.ROLE_TRAINER : UserProfile.ROLE_MEMBER);
                targetProfile.setGender((String) cmbGen.getSelectedItem());
                if (dobVal != null) targetProfile.setDateOfBirth(dobVal);
                targetProfile.setHeightCm(heightVal);
                targetProfile.setTargetWeightKg(targetWeightVal);

                btnSave.setEnabled(false);
                btnSave.setText("Saving...");

                SwingWorker<Void, Void> saveWorker = new SwingWorker<>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        profileDAO.updateProfile(targetProfile);
                        return null;
                    }

                    @Override
                    protected void done() {
                        btnSave.setEnabled(true);
                        dlg.dispose();
                        ToastNotification.showToast(SettingsPanel.this, "Profile updated successfully!", ToastNotification.ToastType.SUCCESS);
                        loadSettings();
                        if (onProfileUpdatedCallback != null) onProfileUpdatedCallback.run();
                    }
                };
                saveWorker.execute();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Please check numeric values for height and weight.");
            }
        });
        btnBar.add(btnSave);

        pnl.add(btnBar);
        dlg.setContentPane(pnl);
        dlg.setVisible(true);
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

    private JPanel createDataReportsCard() {
        ModernCard card = new ModernCard(14, new Insets(18, 20, 18, 20));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Fitness & Health Reports");
        title.setFont(ThemeConfig.FONT_TITLE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);

        JLabel badge = new JLabel("CSV EXPORT");
        badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        badge.setForeground(ThemeConfig.PRIMARY_ACCENT);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xF0, 0x44, 0x44, 120), 1, true),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        header.add(badge, BorderLayout.EAST);
        card.add(header);
        card.add(Box.createVerticalStrut(6));

        JLabel desc = new JLabel("Export your historical workout sessions, body measurements, and nutrition logs.");
        desc.setFont(ThemeConfig.FONT_SMALL);
        desc.setForeground(ThemeConfig.TEXT_MUTED);
        card.add(desc);
        card.add(Box.createVerticalStrut(14));

        // Quick Export Buttons Grid
        JPanel btnGrid = new JPanel(new GridLayout(1, 3, 10, 0));
        btnGrid.setOpaque(false);

        ModernButton btnWorkoutsReport = new ModernButton("Workouts", ModernButton.ButtonStyle.SECONDARY);
        btnWorkoutsReport.setIcon(IconFactory.createIcon(IconFactory.IconType.WORKOUT, 12, ThemeConfig.TEXT_SECONDARY));
        btnWorkoutsReport.setIconTextGap(5);
        btnWorkoutsReport.setFont(ThemeConfig.FONT_SMALL);
        btnWorkoutsReport.addActionListener(e -> exportReport("Workouts"));

        ModernButton btnBodyReport = new ModernButton("Measurements", ModernButton.ButtonStyle.SECONDARY);
        btnBodyReport.setIcon(IconFactory.createIcon(IconFactory.IconType.MEASUREMENT, 12, ThemeConfig.TEXT_SECONDARY));
        btnBodyReport.setIconTextGap(5);
        btnBodyReport.setFont(ThemeConfig.FONT_SMALL);
        btnBodyReport.addActionListener(e -> exportReport("Measurements"));

        ModernButton btnNutritionReport = new ModernButton("Nutrition", ModernButton.ButtonStyle.SECONDARY);
        btnNutritionReport.setIcon(IconFactory.createIcon(IconFactory.IconType.NUTRITION, 12, ThemeConfig.TEXT_SECONDARY));
        btnNutritionReport.setIconTextGap(5);
        btnNutritionReport.setFont(ThemeConfig.FONT_SMALL);
        btnNutritionReport.addActionListener(e -> exportReport("Nutrition"));

        btnGrid.add(btnWorkoutsReport);
        btnGrid.add(btnBodyReport);
        btnGrid.add(btnNutritionReport);
        card.add(btnGrid);
        card.add(Box.createVerticalStrut(12));

        // Full Comprehensive Export Button
        ModernButton btnFullExport = new ModernButton("📥 Export Complete Fitness Archive (All Data)", ModernButton.ButtonStyle.PRIMARY);
        btnFullExport.setFont(ThemeConfig.FONT_SMALL);
        btnFullExport.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnFullExport.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnFullExport.addActionListener(e -> exportFullPackage());
        card.add(btnFullExport);

        return card;
    }

    private void promptExportReport() {
        String[] reportOptions = {"Workouts Report", "Body Measurements Report", "Nutrition Entries Report", "All Data (Combined Archive)"};
        String selected = (String) JOptionPane.showInputDialog(
                this,
                "Select data report to export as CSV:",
                "Export Fitness Report",
                JOptionPane.QUESTION_MESSAGE,
                null,
                reportOptions,
                reportOptions[0]
        );
        if (selected == null) return;

        if ("Workouts Report".equals(selected)) {
            exportReport("Workouts");
        } else if ("Body Measurements Report".equals(selected)) {
            exportReport("Measurements");
        } else if ("Nutrition Entries Report".equals(selected)) {
            exportReport("Nutrition");
        } else {
            exportFullPackage();
        }
    }

    private void exportReport(String type) {
        JFileChooser fileChooser = new JFileChooser();
        String defaultFileName;
        if ("Measurements".equalsIgnoreCase(type)) {
            defaultFileName = "PersonalTracker_Measurements_" + LocalDate.now() + ".csv";
            fileChooser.setDialogTitle("Export Body Measurements Report to CSV");
        } else if ("Nutrition".equalsIgnoreCase(type)) {
            defaultFileName = "PersonalTracker_Nutrition_" + LocalDate.now() + ".csv";
            fileChooser.setDialogTitle("Export Nutrition Report to CSV");
        } else {
            defaultFileName = "PersonalTracker_Workouts_" + LocalDate.now() + ".csv";
            fileChooser.setDialogTitle("Export Workouts Report to CSV");
        }
        fileChooser.setSelectedFile(new java.io.File(defaultFileName));

        int res = fileChooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    if ("Measurements".equalsIgnoreCase(type)) {
                        List<BodyMeasurement> list = measurementDAO.getAllMeasurements();
                        CsvExporter.exportMeasurements(list, file);
                    } else if ("Nutrition".equalsIgnoreCase(type)) {
                        List<NutritionEntry> list = nutritionDAO.getAllEntries();
                        CsvExporter.exportNutrition(list, file);
                    } else {
                        List<Workout> list = workoutDAO.getAllWorkouts();
                        CsvExporter.exportWorkouts(list, file);
                    }
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        ToastNotification.showToast(SettingsPanel.this, "Report saved: " + file.getName(), ToastNotification.ToastType.SUCCESS);
                        JOptionPane.showMessageDialog(SettingsPanel.this,
                                type + " report exported successfully to:\n" + file.getAbsolutePath(),
                                "Export Complete",
                                JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(SettingsPanel.this,
                                "Failed to export report: " + ex.getMessage(),
                                "Export Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void exportFullPackage() {
        JFileChooser dirChooser = new JFileChooser();
        dirChooser.setDialogTitle("Select Folder to Save All Fitness Reports");
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int res = dirChooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            java.io.File dir = dirChooser.getSelectedFile();
            new SwingWorker<Integer, Void>() {
                @Override
                protected Integer doInBackground() throws Exception {
                    String dateStr = LocalDate.now().toString();
                    java.io.File fWorkouts = new java.io.File(dir, "PersonalTracker_Workouts_" + dateStr + ".csv");
                    java.io.File fMeasurements = new java.io.File(dir, "PersonalTracker_Measurements_" + dateStr + ".csv");
                    java.io.File fNutrition = new java.io.File(dir, "PersonalTracker_Nutrition_" + dateStr + ".csv");

                    List<Workout> workouts = workoutDAO.getAllWorkouts();
                    List<BodyMeasurement> measurements = measurementDAO.getAllMeasurements();
                    List<NutritionEntry> nutrition = nutritionDAO.getAllEntries();

                    CsvExporter.exportWorkouts(workouts, fWorkouts);
                    CsvExporter.exportMeasurements(measurements, fMeasurements);
                    CsvExporter.exportNutrition(nutrition, fNutrition);
                    return 3;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        ToastNotification.showToast(SettingsPanel.this, "All 3 reports exported successfully!", ToastNotification.ToastType.SUCCESS);
                        JOptionPane.showMessageDialog(SettingsPanel.this,
                                "All 3 fitness reports exported successfully to folder:\n" + dir.getAbsolutePath() +
                                "\n• PersonalTracker_Workouts_" + LocalDate.now() + ".csv" +
                                "\n• PersonalTracker_Measurements_" + LocalDate.now() + ".csv" +
                                "\n• PersonalTracker_Nutrition_" + LocalDate.now() + ".csv",
                                "Comprehensive Export Complete",
                                JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(SettingsPanel.this,
                                "Failed to export data package: " + ex.getMessage(),
                                "Export Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }
}
