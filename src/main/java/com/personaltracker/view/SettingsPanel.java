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
    private final SupabaseAuthService authService = SupabaseAuthService.getInstance();
    private final AppConfig config = AppConfig.getInstance();

    // Hero Banner Elements
    private final JLabel lblAvatarInitials = new JLabel("AH", SwingConstants.CENTER);
    private final JLabel lblHeroName = new JLabel("Alex Hunter");
    private final JLabel lblHeroEmail = new JLabel("athlete@demo.com");
    private final JLabel lblHeroMeta = new JLabel("Athlete • Active Profile");
    private final JLabel lblStatusBadge = new JLabel("● Online", SwingConstants.CENTER);

    // Personal Info Display Labels (Clean, non-editable)
    private final JLabel valFullName = new JLabel("--");
    private final JLabel valEmail = new JLabel("--");
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

    // Cloud Connection Info
    private final JLabel lblConnectionStatus = new JLabel("● Checking status...");
    private final JLabel lblProjectUrl = new JLabel("https://xyz.supabase.co");

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
        rightCol.add(createCloudInfrastructureCard());
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
        ModernCard heroCard = new ModernCard(16, new Insets(18, 24, 18, 24));

        JPanel banner = new JPanel(new BorderLayout(18, 0));
        banner.setOpaque(false);

        // Avatar ring with initials
        JPanel avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(36, 36, 36));
                g2.fill(new Ellipse2D.Float(0, 0, 68, 68));
                g2.setColor(ThemeConfig.PRIMARY_ACCENT);
                g2.setStroke(new BasicStroke(2.8f));
                g2.draw(new Ellipse2D.Float(2, 2, 64, 64));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatarPanel.setPreferredSize(new Dimension(68, 68));
        avatarPanel.setOpaque(false);
        avatarPanel.setLayout(new BorderLayout());

        lblAvatarInitials.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblAvatarInitials.setForeground(Color.WHITE);
        avatarPanel.add(lblAvatarInitials, BorderLayout.CENTER);
        banner.add(avatarPanel, BorderLayout.WEST);

        // Center: Name & Metadata
        JPanel namePanel = new JPanel(new GridLayout(3, 1, 0, 2));
        namePanel.setOpaque(false);

        lblHeroName.setFont(ThemeConfig.FONT_TITLE_LARGE);
        lblHeroName.setForeground(ThemeConfig.TEXT_PRIMARY);

        lblHeroEmail.setFont(ThemeConfig.FONT_BODY);
        lblHeroEmail.setForeground(ThemeConfig.TEXT_SECONDARY);

        lblHeroMeta.setFont(ThemeConfig.FONT_SMALL);
        lblHeroMeta.setForeground(ThemeConfig.PRIMARY_ACCENT);

        namePanel.add(lblHeroName);
        namePanel.add(lblHeroEmail);
        namePanel.add(lblHeroMeta);
        banner.add(namePanel, BorderLayout.CENTER);

        // Right: Edit Profile Action Button
        JPanel actionBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        actionBox.setOpaque(false);

        ModernButton btnEditProfile = new ModernButton("✏️ Edit Profile Info", ModernButton.ButtonStyle.PRIMARY);
        btnEditProfile.setPreferredSize(new Dimension(180, 40));
        btnEditProfile.addActionListener(e -> openEditProfileDialog());
        actionBox.add(btnEditProfile);

        banner.add(actionBox, BorderLayout.EAST);
        heroCard.add(banner, BorderLayout.CENTER);
        return heroCard;
    }

    private JPanel createPersonalInfoCard() {
        ModernCard card = new ModernCard(14, new Insets(18, 20, 18, 20));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Personal Information");
        title.setFont(ThemeConfig.FONT_TITLE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        card.add(title);
        card.add(Box.createVerticalStrut(14));

        JPanel grid = new JPanel(new GridLayout(3, 2, 14, 14));
        grid.setOpaque(false);

        grid.add(createInfoTile("FULL NAME", valFullName, ThemeConfig.TEXT_PRIMARY));
        grid.add(createInfoTile("EMAIL ADDRESS", valEmail, ThemeConfig.PRIMARY_BLUE));
        grid.add(createInfoTile("GENDER", valGender, ThemeConfig.TEXT_PRIMARY));
        grid.add(createInfoTile("DATE OF BIRTH", valBirthDate, ThemeConfig.TEXT_SECONDARY));
        grid.add(createInfoTile("CURRENT AGE", valAge, ThemeConfig.PRIMARY_ACCENT));
        grid.add(createInfoTile("RECORDED HEIGHT", valHeight, ThemeConfig.SUCCESS_EMERALD));

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
        milestoneGrid.add(createMetricBox("TARGET GOAL", valTargetWeight, ThemeConfig.SUCCESS_EMERALD));
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
        grid.add(createMetricBox("BASAL METABOLIC RATE", valBmr, ThemeConfig.PRIMARY_BLUE));
        grid.add(createMetricBox("ESTIMATED TDEE", valTdee, ThemeConfig.SUCCESS_EMERALD));
        grid.add(createMetricBox("HEALTHY WEIGHT RANGE", valIdealRange, ThemeConfig.WARNING_AMBER));

        card.add(grid);
        return card;
    }

    private JPanel createCloudInfrastructureCard() {
        ModernCard card = new ModernCard(14, new Insets(18, 20, 18, 20));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Cloud Backend & Account Security");
        title.setFont(ThemeConfig.FONT_TITLE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        card.add(title);
        card.add(Box.createVerticalStrut(12));

        // Connection status
        lblConnectionStatus.setFont(ThemeConfig.FONT_BODY_BOLD);
        card.add(lblConnectionStatus);
        card.add(Box.createVerticalStrut(8));

        JPanel urlRow = new JPanel(new BorderLayout());
        urlRow.setOpaque(false);
        JLabel lblUrlTag = new JLabel("Project URL: ");
        lblUrlTag.setFont(ThemeConfig.FONT_SMALL);
        lblUrlTag.setForeground(ThemeConfig.TEXT_MUTED);
        urlRow.add(lblUrlTag, BorderLayout.WEST);

        lblProjectUrl.setFont(ThemeConfig.FONT_SMALL);
        lblProjectUrl.setForeground(ThemeConfig.TEXT_SECONDARY);
        urlRow.add(lblProjectUrl, BorderLayout.CENTER);
        card.add(urlRow);
        card.add(Box.createVerticalStrut(16));

        // Actions
        JPanel btnRow = new JPanel(new GridLayout(1, 3, 8, 0));
        btnRow.setOpaque(false);

        ModernButton btnConfig = new ModernButton("⚙️ Cloud Config", ModernButton.ButtonStyle.SECONDARY);
        btnConfig.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnConfig.addActionListener(e -> openCredentialsDialog());
        btnRow.add(btnConfig);

        ModernButton btnReset = new ModernButton("🔑 Reset Password", ModernButton.ButtonStyle.SECONDARY);
        btnReset.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnReset.addActionListener(e -> sendPasswordReset());
        btnRow.add(btnReset);

        ModernButton btnWizard = new ModernButton("✨ Setup Wizard", ModernButton.ButtonStyle.SECONDARY);
        btnWizard.setFont(new Font("Segoe UI", Font.BOLD, 11));
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

            if (profile.getCreatedAt() != null) {
                lblHeroMeta.setText("Athlete • Member since " + profile.getCreatedAt().getYear());
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
                        valGoalStatus.setText("🎯 Goal Achieved! Maintaining at " + String.format(Locale.US, "%.1f kg", currentWeight));
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

        // Cloud status
        if (config.isMockMode()) {
            lblConnectionStatus.setText("● Local In-Memory Demo Mode (Offline)");
            lblConnectionStatus.setForeground(ThemeConfig.WARNING_AMBER);
            lblProjectUrl.setText("Built-in Offline Data Store");
        } else {
            lblConnectionStatus.setText("● Connected to Supabase Cloud PostgreSQL & Auth");
            lblConnectionStatus.setForeground(ThemeConfig.SUCCESS_EMERALD);
            lblProjectUrl.setText(config.getSupabaseUrl());
        }
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
        dlg.setSize(480, 520);
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
        JComboBox<String> cmbGen = new JComboBox<>(new String[]{"Male", "Female", "Non-Binary", "Other", "Prefer not to say"});
        if (profile.getGender() != null) cmbGen.setSelectedItem(profile.getGender());

        ModernTextField txtDob = new ModernTextField(profile.getDateOfBirth() != null ? profile.getDateOfBirth().toString() : "2000-01-01");
        ModernTextField txtH = new ModernTextField(profile.getHeightCm() != null ? String.format(Locale.US, "%.1f", profile.getHeightCm()) : "175.0");
        ModernTextField txtW = new ModernTextField(profile.getTargetWeightKg() != null ? String.format(Locale.US, "%.1f", profile.getTargetWeightKg()) : "70.0");

        pnl.add(createFieldRow("Full Name", txtName));
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
        ModernButton btnSave = new ModernButton("Save Profile 💾", ModernButton.ButtonStyle.PRIMARY);
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
                        ToastNotification.showToast(SettingsPanel.this, "Profile updated successfully! ✨", ToastNotification.ToastType.SUCCESS);
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

    private void openCredentialsDialog() {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Configure Cloud Connection", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(460, 320);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.getContentPane().setBackground(ThemeConfig.BG_DARK);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("Supabase Cloud Credentials");
        title.setFont(ThemeConfig.FONT_TITLE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        p.add(title);
        p.add(Box.createVerticalStrut(14));

        ModernTextField txtUrl = new ModernTextField(config.getSupabaseUrl());
        ModernPasswordField txtKey = new ModernPasswordField(config.getSupabaseAnonKey());

        p.add(createFieldRow("Project URL (https://xyz.supabase.co)", txtUrl));
        p.add(Box.createVerticalStrut(10));
        p.add(createFieldRow("Anon / Public API Key", txtKey));
        p.add(Box.createVerticalStrut(20));

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnBar.setOpaque(false);

        ModernButton btnCancel = new ModernButton("Cancel", ModernButton.ButtonStyle.SECONDARY);
        btnCancel.addActionListener(e -> dlg.dispose());
        btnBar.add(btnCancel);

        ModernButton btnApply = new ModernButton("Apply Credentials", ModernButton.ButtonStyle.PRIMARY);
        btnApply.addActionListener(e -> {
            config.saveRuntimeCredentials(txtUrl.getText().trim(), new String(txtKey.getPassword()).trim());
            dlg.dispose();
            loadSettings();
            ToastNotification.showToast(SettingsPanel.this, "Supabase connection updated! ⚡", ToastNotification.ToastType.INFO);
        });
        btnBar.add(btnApply);

        p.add(btnBar);
        dlg.setContentPane(p);
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
}
