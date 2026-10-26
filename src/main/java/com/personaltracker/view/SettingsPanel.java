package com.personaltracker.view;

import com.personaltracker.components.ModernButton;
import com.personaltracker.components.ModernCard;
import com.personaltracker.components.ModernPasswordField;
import com.personaltracker.components.ModernProgressBar;
import com.personaltracker.components.ModernTextField;
import com.personaltracker.components.ToastNotification;
import com.personaltracker.config.AppConfig;
import com.personaltracker.config.ThemeConfig;
import com.personaltracker.dao.GoalDAO;
import com.personaltracker.dao.MeasurementDAO;
import com.personaltracker.dao.NutritionDAO;
import com.personaltracker.dao.ProfileDAO;
import com.personaltracker.dao.SleepDAO;
import com.personaltracker.dao.WaterDAO;
import com.personaltracker.dao.WorkoutDAO;
import com.personaltracker.model.BodyMeasurement;
import com.personaltracker.model.Goal;
import com.personaltracker.model.NutritionEntry;
import com.personaltracker.model.SleepRecord;
import com.personaltracker.model.UserProfile;
import com.personaltracker.model.UserSession;
import com.personaltracker.model.WaterIntake;
import com.personaltracker.model.Workout;
import com.personaltracker.service.SupabaseAuthService;
import com.personaltracker.service.SupabaseClient;
import com.personaltracker.util.CsvExporter;
import com.personaltracker.util.DataSyncManager;
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
    private final WaterDAO waterDAO = new WaterDAO();
    private final SleepDAO sleepDAO = new SleepDAO();
    private final GoalDAO goalDAO = new GoalDAO();
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
    private java.util.function.Consumer<SidebarPanel.NavItem> navigationConsumer;

    public void setNavigationConsumer(java.util.function.Consumer<SidebarPanel.NavItem> consumer) {
        this.navigationConsumer = consumer;
    }

    public SettingsPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());

        DataSyncManager.registerListener(type -> {
            if (type == DataSyncManager.DataType.PROFILE ||
                type == DataSyncManager.DataType.WEIGHT_MEASUREMENT ||
                type == DataSyncManager.DataType.GOALS ||
                type == DataSyncManager.DataType.WORKOUTS ||
                type == DataSyncManager.DataType.WATER ||
                type == DataSyncManager.DataType.SLEEP) {
                loadSettings();
            }
        });

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        // 1. Hero Profile Header Bar
        content.add(createHeroBanner(), BorderLayout.NORTH);

        // 2. Main 2x2 Dashboard Grid (Personal Info, Weight Journey, Fitness Reports, Metabolic Health)
        JPanel grid = new JPanel(new GridLayout(2, 2, 14, 14));
        grid.setOpaque(false);
        grid.setPreferredSize(new Dimension(1000, 580));

        grid.add(createPersonalInfoCard());
        grid.add(createWeightJourneyCard());
        grid.add(createDataReportsCard());
        grid.add(createMetabolicHealthCard());
        content.add(grid, BorderLayout.CENTER);

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
    // UI BUILDERS (NON-EDITABLE TRACKING SCENE - ALL-IN-ONE FRAME)
    // =========================================================================

    private JPanel createHeroBanner() {
        ModernCard heroCard = new ModernCard(12, new Insets(10, 16, 10, 16));
        heroCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        heroCard.setPreferredSize(new Dimension(1000, 68));

        JPanel banner = new JPanel(new BorderLayout(12, 0));
        banner.setOpaque(false);

        // Avatar ring with mathematically centered initials
        avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int size = 44;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                g2.setColor(new Color(28, 28, 28));
                g2.fill(new Ellipse2D.Float(x, y, size, size));

                g2.setColor(ThemeConfig.PRIMARY_ACCENT);
                g2.setStroke(new BasicStroke(2.0f));
                g2.draw(new Ellipse2D.Float(x + 1f, y + 1f, size - 2f, size - 2f));

                String initials = lblAvatarInitials.getText();
                if (initials == null || initials.isBlank()) initials = "PG";
                g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int tx = x + (size - fm.stringWidth(initials)) / 2;
                int ty = y + (size - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(initials, tx, ty);
                g2.dispose();
            }
        };
        avatarPanel.setPreferredSize(new Dimension(46, 46));
        avatarPanel.setMinimumSize(new Dimension(46, 46));
        avatarPanel.setOpaque(false);
        banner.add(avatarPanel, BorderLayout.WEST);

        // Center: Name & Metadata
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS));
        namePanel.setOpaque(false);

        // Top line: Name + Active badge
        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        nameRow.setOpaque(false);
        lblHeroName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblHeroName.setForeground(ThemeConfig.TEXT_PRIMARY);
        nameRow.add(lblHeroName);

        lblStatusBadge.setFont(new Font("Segoe UI", Font.BOLD, 9));
        lblStatusBadge.setForeground(ThemeConfig.SUCCESS);
        lblStatusBadge.setText("● Active Session");
        lblStatusBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x10, 0xB9, 0x81, 100), 1, true),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        nameRow.add(lblStatusBadge);
        namePanel.add(nameRow);
        namePanel.add(Box.createVerticalStrut(2));

        // Middle line: Email + metadata
        JPanel metaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        metaRow.setOpaque(false);
        JLabel emailIcon = new JLabel(IconFactory.createIcon(IconFactory.IconType.USER, 11, ThemeConfig.TEXT_MUTED));
        metaRow.add(emailIcon);
        lblHeroEmail.setFont(ThemeConfig.FONT_SMALL);
        lblHeroEmail.setForeground(ThemeConfig.TEXT_SECONDARY);
        metaRow.add(lblHeroEmail);

        JLabel dot1 = new JLabel("•");
        dot1.setFont(ThemeConfig.FONT_SMALL);
        dot1.setForeground(ThemeConfig.TEXT_MUTED);
        metaRow.add(dot1);

        lblHeroMeta.setFont(ThemeConfig.FONT_SMALL);
        lblHeroMeta.setForeground(ThemeConfig.PRIMARY_ACCENT);
        metaRow.add(lblHeroMeta);

        namePanel.add(metaRow);
        banner.add(namePanel, BorderLayout.CENTER);

        // Right Action Buttons: Reset Password, Setup Wizard, Edit Profile
        JPanel actionBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionBox.setOpaque(false);

        ModernButton btnReset = new ModernButton("Reset Password", ModernButton.ButtonStyle.SECONDARY);
        btnReset.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnReset.setPreferredSize(new Dimension(120, 32));
        btnReset.addActionListener(e -> sendPasswordReset());
        actionBox.add(btnReset);

        ModernButton btnWizard = new ModernButton("Setup Wizard", ModernButton.ButtonStyle.SECONDARY);
        btnWizard.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnWizard.setPreferredSize(new Dimension(110, 32));
        btnWizard.addActionListener(e -> launchOnboardingWizard());
        actionBox.add(btnWizard);

        ModernButton btnTheme = new ModernButton(ThemeConfig.isDarkMode() ? "Light Mode" : "Dark Mode", ModernButton.ButtonStyle.SECONDARY);
        btnTheme.setIcon(IconFactory.createIcon(ThemeConfig.isDarkMode() ? IconFactory.IconType.SUN : IconFactory.IconType.MOON, 12, ThemeConfig.TEXT_PRIMARY));
        btnTheme.setIconTextGap(6);
        btnTheme.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnTheme.setPreferredSize(new Dimension(115, 32));
        btnTheme.addActionListener(e -> ThemeConfig.toggleTheme());
        actionBox.add(btnTheme);

        ModernButton btnEditProfile = new ModernButton("Edit Profile", ModernButton.ButtonStyle.PRIMARY);
        btnEditProfile.setIcon(IconFactory.createIcon(IconFactory.IconType.EDIT, 12, Color.WHITE));
        btnEditProfile.setIconTextGap(6);
        btnEditProfile.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnEditProfile.setPreferredSize(new Dimension(115, 32));
        btnEditProfile.addActionListener(e -> openEditProfileDialog());
        actionBox.add(btnEditProfile);

        banner.add(actionBox, BorderLayout.EAST);
        heroCard.add(banner, BorderLayout.CENTER);
        return heroCard;
    }

    private JPanel createPersonalInfoCard() {
        ModernCard card = new ModernCard(12, new Insets(14, 16, 14, 16));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Personal Information & Unique ID");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(8));

        // Unique Member / Student Code Banner - Fixed height so it NEVER stretches
        JPanel codeBanner = new JPanel(new BorderLayout(8, 0));
        codeBanner.setBackground(ThemeConfig.BG_CARD_HOVER);
        codeBanner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(4, 12, 4, 10)
        ));
        codeBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        codeBanner.setPreferredSize(new Dimension(450, 38));
        codeBanner.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel codeLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        codeLeft.setOpaque(false);
        JLabel lblCodeTitle = new JLabel("ATHLETE ID:");
        lblCodeTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblCodeTitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        codeLeft.add(lblCodeTitle);

        valUserCode.setFont(new Font("Segoe UI", Font.BOLD, 14));
        valUserCode.setForeground(ThemeConfig.TEXT_PRIMARY);
        codeLeft.add(valUserCode);
        codeBanner.add(codeLeft, BorderLayout.CENTER);

        JPanel copyWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        copyWrapper.setOpaque(false);
        JButton btnCopyCode = new JButton("Copy Code");
        btnCopyCode.setIcon(IconFactory.createIcon(IconFactory.IconType.COPY, 11, ThemeConfig.TEXT_PRIMARY));
        btnCopyCode.setIconTextGap(4);
        btnCopyCode.setHorizontalTextPosition(SwingConstants.RIGHT);
        btnCopyCode.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btnCopyCode.setForeground(ThemeConfig.TEXT_PRIMARY);
        btnCopyCode.setBackground(ThemeConfig.BG_CARD);
        btnCopyCode.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        btnCopyCode.setFocusPainted(false);
        btnCopyCode.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCopyCode.addActionListener(e -> {
            String code = valUserCode.getText();
            java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(code);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            ToastNotification.showToast(SettingsPanel.this, "Copied code: " + code, ToastNotification.ToastType.SUCCESS);
        });
        copyWrapper.add(btnCopyCode);
        codeBanner.add(copyWrapper, BorderLayout.EAST);

        card.add(codeBanner);
        card.add(Box.createVerticalStrut(10));

        JPanel grid = new JPanel(new GridLayout(3, 2, 8, 8));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        grid.add(createInfoTile("FULL NAME", valFullName, ThemeConfig.TEXT_PRIMARY));
        grid.add(createInfoTile("ACCOUNT ROLE", valRole, ThemeConfig.PRIMARY_ACCENT));
        grid.add(createInfoTile("GENDER", valGender, ThemeConfig.TEXT_PRIMARY));
        grid.add(createInfoTile("DATE OF BIRTH", valBirthDate, ThemeConfig.TEXT_SECONDARY));
        grid.add(createInfoTile("CURRENT AGE", valAge, ThemeConfig.PRIMARY_ACCENT));
        grid.add(createInfoTile("RECORDED HEIGHT", valHeight, ThemeConfig.SUCCESS));

        card.add(grid);
        card.add(Box.createVerticalGlue());
        return card;
    }

    private JPanel createWeightJourneyCard() {
        ModernCard card = new ModernCard(12, new Insets(14, 16, 14, 16));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        // Header Row: Title on left, Adjust Target button on right in FlowLayout wrapper
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Weight Journey & Progress");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        headerRow.add(title, BorderLayout.WEST);

        JPanel btnWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnWrapper.setOpaque(false);

        ModernButton btnAdjust = new ModernButton("Adjust Target", ModernButton.ButtonStyle.SECONDARY);
        btnAdjust.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btnAdjust.setIcon(IconFactory.createIcon(IconFactory.IconType.EDIT, 11, ThemeConfig.TEXT_SECONDARY));
        btnAdjust.setIconTextGap(4);
        btnAdjust.setPreferredSize(new Dimension(120, 26));
        btnAdjust.addActionListener(e -> openQuickTargetDialog());
        btnWrapper.add(btnAdjust);

        headerRow.add(btnWrapper, BorderLayout.EAST);

        card.add(headerRow);
        card.add(Box.createVerticalStrut(10));

        // 3 Milestone Cards: Starting -> Current -> Target
        JPanel milestoneGrid = new JPanel(new GridLayout(1, 3, 8, 0));
        milestoneGrid.setOpaque(false);
        milestoneGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        milestoneGrid.setPreferredSize(new Dimension(450, 66));
        milestoneGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        milestoneGrid.add(createMetricBox("STARTING WEIGHT", valStartingWeight, ThemeConfig.TEXT_SECONDARY));
        milestoneGrid.add(createMetricBox("CURRENT WEIGHT", valCurrentWeight, ThemeConfig.PRIMARY_ACCENT));
        milestoneGrid.add(createMetricBox("TARGET GOAL", valTargetWeight, ThemeConfig.SUCCESS));
        card.add(milestoneGrid);
        card.add(Box.createVerticalStrut(12));

        // Goal Status text
        JPanel statusRow = new JPanel(new BorderLayout());
        statusRow.setOpaque(false);
        statusRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        statusRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        valGoalStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        valGoalStatus.setForeground(ThemeConfig.TEXT_PRIMARY);
        statusRow.add(valGoalStatus, BorderLayout.WEST);

        lblProgressPercent.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblProgressPercent.setForeground(ThemeConfig.PRIMARY_ACCENT);
        statusRow.add(lblProgressPercent, BorderLayout.EAST);
        card.add(statusRow);
        card.add(Box.createVerticalStrut(6));

        // Modern Progress Bar
        barWeightProgress.setPreferredSize(new Dimension(Integer.MAX_VALUE, 8));
        barWeightProgress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        barWeightProgress.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(barWeightProgress);
        card.add(Box.createVerticalStrut(10));

        // Contextual Footer Row
        JPanel footerRow = new JPanel(new BorderLayout());
        footerRow.setOpaque(false);
        footerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        footerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel hint = new JLabel("Baseline check-in → Latest check-in");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(ThemeConfig.TEXT_MUTED);
        footerRow.add(hint, BorderLayout.WEST);

        JLabel lnkCheckIns = new JLabel("Manage Check-ins →");
        lnkCheckIns.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lnkCheckIns.setForeground(ThemeConfig.PRIMARY_ACCENT);
        lnkCheckIns.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lnkCheckIns.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (navigationConsumer != null) {
                    navigationConsumer.accept(SidebarPanel.NavItem.MEASUREMENTS);
                }
            }
        });
        footerRow.add(lnkCheckIns, BorderLayout.EAST);
        card.add(footerRow);
        card.add(Box.createVerticalGlue());

        return card;
    }

    private JPanel createMetabolicHealthCard() {
        ModernCard card = new ModernCard(12, new Insets(14, 16, 14, 16));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Metabolic Health & Clinical Benchmarks");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(4));

        JLabel sub = new JLabel("Automated Mifflin-St Jeor & WHO clinical calculations.");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(ThemeConfig.TEXT_MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(sub);
        card.add(Box.createVerticalStrut(10));

        JPanel grid = new JPanel(new GridLayout(2, 2, 8, 8));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        grid.add(createMetricBox("BODY MASS INDEX", valBmi, ThemeConfig.PRIMARY_ACCENT));
        grid.add(createMetricBox("BASAL METABOLIC RATE", valBmr, ThemeConfig.PRIMARY_ACCENT));
        grid.add(createMetricBox("ESTIMATED TDEE", valTdee, ThemeConfig.SUCCESS));
        grid.add(createMetricBox("HEALTHY WEIGHT RANGE", valIdealRange, ThemeConfig.WARNING));

        card.add(grid);
        card.add(Box.createVerticalGlue());
        return card;
    }

    private JPanel createInfoTile(String header, JLabel valueLbl, Color valueColor) {
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBackground(ThemeConfig.BG_DARK);
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        JLabel hLbl = new JLabel(header);
        hLbl.setFont(new Font("Segoe UI", Font.BOLD, 9));
        hLbl.setForeground(ThemeConfig.TEXT_MUTED);
        tile.add(hLbl);
        tile.add(Box.createVerticalStrut(3));

        valueLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        valueLbl.setForeground(valueColor);
        tile.add(valueLbl);
        return tile;
    }

    private JPanel createMetricBox(String header, JLabel valueLbl, Color accent) {
        JPanel box = new JPanel(new BorderLayout(0, 4));
        box.setBackground(ThemeConfig.BG_DARK);
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JLabel hLbl = new JLabel(header);
        hLbl.setFont(new Font("Segoe UI", Font.BOLD, 9));
        hLbl.setForeground(ThemeConfig.TEXT_MUTED);

        valueLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
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

        // Fetch body measurement history and goals to calculate current vs starting weight journey
        final UserProfile userProf = profile;
        SwingWorker<Object[], Void> worker = new SwingWorker<>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                List<BodyMeasurement> measurements = measurementDAO.getAllMeasurements();
                List<Goal> goals = List.of();
                try {
                    goals = goalDAO.getAllGoals();
                } catch (Exception ignored) {}
                return new Object[]{measurements, goals};
            }

            @Override
            protected void done() {
                try {
                    Object[] res = get();
                    @SuppressWarnings("unchecked")
                    List<BodyMeasurement> measurements = (List<BodyMeasurement>) res[0];
                    @SuppressWarnings("unchecked")
                    List<Goal> goals = (List<Goal>) res[1];

                    double currentWeight = 75.0;
                    double startingWeight = 75.0;
                    double targetWeight = (userProf != null && userProf.getTargetWeightKg() != null && userProf.getTargetWeightKg() > 0)
                            ? userProf.getTargetWeightKg() : 70.0;
                    double height = (userProf != null && userProf.getHeightCm() != null) ? userProf.getHeightCm() : 175.0;
                    String gender = (userProf != null && userProf.getGender() != null) ? userProf.getGender() : "Male";
                    int age = (userProf != null && userProf.getDateOfBirth() != null) ? Period.between(userProf.getDateOfBirth(), LocalDate.now()).getYears() : 25;

                    // Check if an explicit active weight goal exists
                    Goal activeWeightGoal = null;
                    if (goals != null) {
                        for (Goal g : goals) {
                            if ("Weight".equalsIgnoreCase(g.getCategory()) ||
                                    (g.getTitle() != null && g.getTitle().toLowerCase().contains("weight"))) {
                                activeWeightGoal = g;
                                break;
                            }
                        }
                    }

                    if (measurements != null && !measurements.isEmpty()) {
                        currentWeight = measurements.get(0).getWeightKg();
                        startingWeight = measurements.get(measurements.size() - 1).getWeightKg();
                    } else if (activeWeightGoal != null && activeWeightGoal.getCurrentValue() > 0) {
                        currentWeight = activeWeightGoal.getCurrentValue();
                        startingWeight = currentWeight;
                    } else if (userProf != null && userProf.getTargetWeightKg() != null) {
                        currentWeight = userProf.getTargetWeightKg();
                        startingWeight = currentWeight;
                    }

                    if (userProf == null || userProf.getTargetWeightKg() == null || userProf.getTargetWeightKg() <= 0) {
                        if (activeWeightGoal != null && activeWeightGoal.getTargetValue() > 0) {
                            targetWeight = activeWeightGoal.getTargetValue();
                        }
                    }

                    valStartingWeight.setText(String.format(Locale.US, "%.1f kg", startingWeight));
                    valCurrentWeight.setText(String.format(Locale.US, "%.1f kg", currentWeight));
                    valTargetWeight.setText(String.format(Locale.US, "%.1f kg", targetWeight));

                    // Accurate weight journey progress calculation
                    double deltaFromTarget = currentWeight - targetWeight;
                    double journeyDelta = startingWeight - targetWeight;

                    if (Math.abs(deltaFromTarget) <= 0.3) {
                        // User has achieved target maintenance!
                        valGoalStatus.setText(String.format(Locale.US, "Goal Achieved! Maintaining at %.1f kg", currentWeight));
                        valGoalStatus.setForeground(ThemeConfig.SUCCESS);
                        barWeightProgress.setValue(100);
                        barWeightProgress.setProgressColor(ThemeConfig.SUCCESS);
                        lblProgressPercent.setText("100% Completed");
                        lblProgressPercent.setForeground(ThemeConfig.SUCCESS);
                    } else if (journeyDelta > 0.5) {
                        // Weight loss goal (startingWeight > targetWeight)
                        double totalToLose = startingWeight - targetWeight;
                        double lost = startingWeight - currentWeight;
                        double remaining = currentWeight - targetWeight;

                        if (remaining <= 0) {
                            valGoalStatus.setText(String.format(Locale.US, "Goal Reached! Target was %.1f kg (Current: %.1f kg)", targetWeight, currentWeight));
                            valGoalStatus.setForeground(ThemeConfig.SUCCESS);
                            barWeightProgress.setValue(100);
                            barWeightProgress.setProgressColor(ThemeConfig.SUCCESS);
                            lblProgressPercent.setText("100% Completed");
                            lblProgressPercent.setForeground(ThemeConfig.SUCCESS);
                        } else {
                            int percent = (int) Math.round((lost / totalToLose) * 100.0);
                            percent = Math.min(100, Math.max(0, percent));
                            valGoalStatus.setText(String.format(Locale.US, "%.1f kg remaining to reach target (%.1f kg)", remaining, targetWeight));
                            valGoalStatus.setForeground(ThemeConfig.TEXT_PRIMARY);
                            barWeightProgress.setValue(percent);
                            barWeightProgress.setProgressColor(ThemeConfig.PRIMARY_ACCENT);
                            lblProgressPercent.setText(percent + "% Completed");
                            lblProgressPercent.setForeground(ThemeConfig.PRIMARY_ACCENT);
                        }
                    } else if (journeyDelta < -0.5) {
                        // Muscle / Weight gain goal (startingWeight < targetWeight)
                        double totalToGain = targetWeight - startingWeight;
                        double gained = currentWeight - startingWeight;
                        double remaining = targetWeight - currentWeight;

                        if (remaining <= 0) {
                            valGoalStatus.setText(String.format(Locale.US, "Goal Reached! Target was %.1f kg (Current: %.1f kg)", targetWeight, currentWeight));
                            valGoalStatus.setForeground(ThemeConfig.SUCCESS);
                            barWeightProgress.setValue(100);
                            barWeightProgress.setProgressColor(ThemeConfig.SUCCESS);
                            lblProgressPercent.setText("100% Completed");
                            lblProgressPercent.setForeground(ThemeConfig.SUCCESS);
                        } else {
                            int percent = (int) Math.round((gained / totalToGain) * 100.0);
                            percent = Math.min(100, Math.max(0, percent));
                            valGoalStatus.setText(String.format(Locale.US, "%.1f kg remaining to reach target (%.1f kg)", remaining, targetWeight));
                            valGoalStatus.setForeground(ThemeConfig.TEXT_PRIMARY);
                            barWeightProgress.setValue(percent);
                            barWeightProgress.setProgressColor(ThemeConfig.PRIMARY_ACCENT);
                            lblProgressPercent.setText(percent + "% Completed");
                            lblProgressPercent.setForeground(ThemeConfig.PRIMARY_ACCENT);
                        }
                    } else {
                        // startingWeight == targetWeight (within 0.5kg), but currentWeight != targetWeight
                        if (currentWeight < targetWeight) {
                            double toGain = targetWeight - currentWeight;
                            valGoalStatus.setText(String.format(Locale.US, "%.1f kg to gain to reach target (%.1f kg)", toGain, targetWeight));
                            valGoalStatus.setForeground(ThemeConfig.TEXT_PRIMARY);
                            int percent = (int) Math.min(100, Math.max(0, Math.round((currentWeight / targetWeight) * 100.0)));
                            barWeightProgress.setValue(percent);
                            barWeightProgress.setProgressColor(ThemeConfig.PRIMARY_ACCENT);
                            lblProgressPercent.setText(String.format(Locale.US, "%d%% • %.1f kg to gain", percent, toGain));
                            lblProgressPercent.setForeground(ThemeConfig.PRIMARY_ACCENT);
                        } else {
                            double toLose = currentWeight - targetWeight;
                            valGoalStatus.setText(String.format(Locale.US, "%.1f kg to lose to reach target (%.1f kg)", toLose, targetWeight));
                            valGoalStatus.setForeground(ThemeConfig.TEXT_PRIMARY);
                            int percent = (int) Math.min(100, Math.max(0, Math.round((targetWeight / currentWeight) * 100.0)));
                            barWeightProgress.setValue(percent);
                            barWeightProgress.setProgressColor(ThemeConfig.PRIMARY_ACCENT);
                            lblProgressPercent.setText(String.format(Locale.US, "%d%% • %.1f kg to lose", percent, toLose));
                            lblProgressPercent.setForeground(ThemeConfig.PRIMARY_ACCENT);
                        }
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

    /**
     * Opens a quick modal dialog to adjust or set target weight.
     */
    private void openQuickTargetDialog() {
        UserSession session = SupabaseClient.getInstance().getCurrentSession();
        if (session == null) return;
        UserProfile profile = session.getProfile();
        if (profile == null) return;

        double curTarget = (profile.getTargetWeightKg() != null && profile.getTargetWeightKg() > 0)
                ? profile.getTargetWeightKg() : 70.0;

        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Adjust Target Weight Goal", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(400, 290);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.getContentPane().setBackground(ThemeConfig.BG_DARK);

        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
        pnl.setOpaque(false);
        pnl.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("Adjust Target Weight Goal");
        title.setFont(ThemeConfig.FONT_TITLE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        pnl.add(title);
        pnl.add(Box.createVerticalStrut(6));

        JLabel sub = new JLabel("Set your desired target goal weight in kilograms:");
        sub.setFont(ThemeConfig.FONT_SMALL);
        sub.setForeground(ThemeConfig.TEXT_MUTED);
        pnl.add(sub);
        pnl.add(Box.createVerticalStrut(14));

        ModernTextField txtTarget = new ModernTextField(String.format(Locale.US, "%.1f", curTarget), "e.g. 75.0");
        pnl.add(txtTarget);
        pnl.add(Box.createVerticalStrut(8));

        // Stepper micro-adjust buttons
        JPanel stepRow = new JPanel(new GridLayout(1, 4, 6, 0));
        stepRow.setOpaque(false);
        stepRow.add(createAdjustButton("-1.0", () -> adjustTargetField(txtTarget, -1.0)));
        stepRow.add(createAdjustButton("-0.5", () -> adjustTargetField(txtTarget, -0.5)));
        stepRow.add(createAdjustButton("+0.5", () -> adjustTargetField(txtTarget, +0.5)));
        stepRow.add(createAdjustButton("+1.0", () -> adjustTargetField(txtTarget, +1.0)));
        pnl.add(stepRow);
        pnl.add(Box.createVerticalStrut(18));

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnBar.setOpaque(false);

        ModernButton btnCancel = new ModernButton("Cancel", ModernButton.ButtonStyle.SECONDARY);
        btnCancel.addActionListener(e -> dlg.dispose());
        btnBar.add(btnCancel);

        ModernButton btnSave = new ModernButton("Save Target", ModernButton.ButtonStyle.PRIMARY);
        btnSave.addActionListener(e -> {
            try {
                double newTarget = Double.parseDouble(txtTarget.getText().trim());
                if (newTarget < 20 || newTarget > 350) {
                    JOptionPane.showMessageDialog(dlg, "Please enter a realistic target weight (20-350 kg).");
                    return;
                }
                profile.setTargetWeightKg(newTarget);
                btnSave.setEnabled(false);
                btnSave.setText("Saving...");

                SwingWorker<Void, Void> sw = new SwingWorker<>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        profileDAO.updateProfile(profile);
                        try {
                            List<Goal> goals = goalDAO.getAllGoals();
                            for (Goal g : goals) {
                                if ("Weight".equalsIgnoreCase(g.getCategory()) ||
                                        (g.getTitle() != null && g.getTitle().toLowerCase().contains("weight"))) {
                                    g.setTargetValue(newTarget);
                                    goalDAO.updateGoal(g);
                                    break;
                                }
                            }
                        } catch (Exception ignored) {}
                        return null;
                    }

                    @Override
                    protected void done() {
                        dlg.dispose();
                        ToastNotification.showToast(SettingsPanel.this, "Target goal updated to " + String.format(Locale.US, "%.1f kg", newTarget), ToastNotification.ToastType.SUCCESS);
                        loadSettings();
                        if (onProfileUpdatedCallback != null) onProfileUpdatedCallback.run();
                        DataSyncManager.fireDataChanged(
                                DataSyncManager.DataType.PROFILE,
                                DataSyncManager.DataType.GOALS,
                                DataSyncManager.DataType.WEIGHT_MEASUREMENT
                        );
                    }
                };
                sw.execute();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dlg, "Please enter a valid numeric target weight.");
            }
        });
        btnBar.add(btnSave);
        pnl.add(btnBar);

        dlg.setContentPane(pnl);
        dlg.setVisible(true);
    }

    private void adjustTargetField(ModernTextField field, double delta) {
        try {
            double current = Double.parseDouble(field.getText().trim());
            double updated = Math.max(20.0, current + delta);
            field.setText(String.format(Locale.US, "%.1f", updated));
        } catch (Exception ignored) {}
    }

    private JButton createAdjustButton(String text, Runnable action) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btn.setForeground(ThemeConfig.TEXT_PRIMARY);
        btn.setBackground(ThemeConfig.BG_CARD);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> action.run());
        return btn;
    }

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
                        // Also synchronize active weight goal target value if target weight was edited
                        if (targetProfile.getTargetWeightKg() != null && targetProfile.getTargetWeightKg() > 0) {
                            try {
                                List<Goal> goals = goalDAO.getAllGoals();
                                for (Goal g : goals) {
                                    if (GoalDAO.isWeightGoal(g)) {
                                        g.setTargetValue(targetProfile.getTargetWeightKg());
                                        g.checkStatus();
                                        goalDAO.updateGoal(g);
                                        break;
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        btnSave.setEnabled(true);
                        dlg.dispose();
                        ToastNotification.showToast(SettingsPanel.this, "Profile updated successfully!", ToastNotification.ToastType.SUCCESS);
                        loadSettings();
                        if (onProfileUpdatedCallback != null) onProfileUpdatedCallback.run();
                        DataSyncManager.fireDataChanged(
                                DataSyncManager.DataType.PROFILE,
                                DataSyncManager.DataType.GOALS,
                                DataSyncManager.DataType.WEIGHT_MEASUREMENT
                        );
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
        ModernCard card = new ModernCard(12, new Insets(14, 16, 14, 16));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        // Header: Title on left, Badge on right in a FlowLayout wrapper so it NEVER stretches
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Fitness & Health Reports");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);

        // Badge wrapped in FlowLayout so it stays compact and never stretches vertically
        JPanel badgeWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        badgeWrapper.setOpaque(false);

        JLabel badge = new JLabel("CSV EXPORT");
        badge.setFont(new Font("Segoe UI", Font.BOLD, 9));
        badge.setForeground(ThemeConfig.PRIMARY_ACCENT);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xF0, 0x44, 0x44, 120), 1, true),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        badgeWrapper.add(badge);
        header.add(badgeWrapper, BorderLayout.EAST);
        card.add(header);
        card.add(Box.createVerticalStrut(4));

        JLabel sub = new JLabel("Export full logs or individual datasets for coaching & backups.");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(ThemeConfig.TEXT_MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        card.add(sub);
        card.add(Box.createVerticalStrut(10));

        // Full Comprehensive Archive Button (prominent)
        ModernButton btnFullExport = new ModernButton("Export Complete Fitness Archive (All 5 Datasets)", ModernButton.ButtonStyle.PRIMARY);
        btnFullExport.setIcon(IconFactory.createIcon(IconFactory.IconType.COPY, 12, Color.WHITE));
        btnFullExport.setIconTextGap(7);
        btnFullExport.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnFullExport.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnFullExport.setPreferredSize(new Dimension(400, 36));
        btnFullExport.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnFullExport.addActionListener(e -> exportFullPackage());
        card.add(btnFullExport);
        card.add(Box.createVerticalStrut(10));

        // 5 Individual Report Buttons in 2 compact rows
        JPanel row1 = new JPanel(new GridLayout(1, 3, 8, 0));
        row1.setOpaque(false);
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        row1.setPreferredSize(new Dimension(400, 32));

        row1.add(createQuickExportBtn("Workouts", IconFactory.IconType.WORKOUT, () -> exportReport("Workouts")));
        row1.add(createQuickExportBtn("Measurements", IconFactory.IconType.MEASUREMENT, () -> exportReport("Measurements")));
        row1.add(createQuickExportBtn("Nutrition", IconFactory.IconType.NUTRITION, () -> exportReport("Nutrition")));
        card.add(row1);
        card.add(Box.createVerticalStrut(8));

        JPanel row2 = new JPanel(new GridLayout(1, 3, 8, 0));
        row2.setOpaque(false);
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        row2.setPreferredSize(new Dimension(400, 32));

        row2.add(createQuickExportBtn("Water Intake", IconFactory.IconType.WATER, () -> exportReport("Water")));
        row2.add(createQuickExportBtn("Sleep & Recovery", IconFactory.IconType.SLEEP, () -> exportReport("Sleep")));
        row2.add(createQuickExportBtn("Custom Dialog", IconFactory.IconType.ANALYTICS, this::promptExportReport));
        card.add(row2);
        card.add(Box.createVerticalStrut(8));

        // Local privacy note at bottom
        JLabel note = new JLabel("• Exports are saved directly as clean CSV spreadsheets.");
        note.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        note.setForeground(ThemeConfig.TEXT_MUTED);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        note.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        card.add(note);
        card.add(Box.createVerticalGlue());

        return card;
    }

    private ModernButton createQuickExportBtn(String label, IconFactory.IconType iconType, Runnable action) {
        ModernButton btn = new ModernButton(label, ModernButton.ButtonStyle.SECONDARY);
        btn.setIcon(IconFactory.createIcon(iconType, 12, ThemeConfig.TEXT_SECONDARY));
        btn.setIconTextGap(5);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private void promptExportReport() {
        String[] reportOptions = {
                "Workouts Report",
                "Body Measurements Report",
                "Nutrition Entries Report",
                "Water Intake Report",
                "Sleep & Recovery Report",
                "All Data (Combined Archive)"
        };
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
        } else if ("Water Intake Report".equals(selected)) {
            exportReport("Water");
        } else if ("Sleep & Recovery Report".equals(selected)) {
            exportReport("Sleep");
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
        } else if ("Water".equalsIgnoreCase(type)) {
            defaultFileName = "PersonalTracker_Water_" + LocalDate.now() + ".csv";
            fileChooser.setDialogTitle("Export Water Intake Report to CSV");
        } else if ("Sleep".equalsIgnoreCase(type)) {
            defaultFileName = "PersonalTracker_Sleep_" + LocalDate.now() + ".csv";
            fileChooser.setDialogTitle("Export Sleep & Recovery Report to CSV");
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
                    } else if ("Water".equalsIgnoreCase(type)) {
                        List<WaterIntake> list = waterDAO.getAllEntries();
                        CsvExporter.exportWater(list, file);
                    } else if ("Sleep".equalsIgnoreCase(type)) {
                        List<SleepRecord> list = sleepDAO.getAllRecords();
                        CsvExporter.exportSleep(list, file);
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
                    java.io.File fWater = new java.io.File(dir, "PersonalTracker_Water_" + dateStr + ".csv");
                    java.io.File fSleep = new java.io.File(dir, "PersonalTracker_Sleep_" + dateStr + ".csv");

                    List<Workout> workouts = workoutDAO.getAllWorkouts();
                    List<BodyMeasurement> measurements = measurementDAO.getAllMeasurements();
                    List<NutritionEntry> nutrition = nutritionDAO.getAllEntries();
                    List<WaterIntake> water = waterDAO.getAllEntries();
                    List<SleepRecord> sleep = sleepDAO.getAllRecords();

                    CsvExporter.exportWorkouts(workouts, fWorkouts);
                    CsvExporter.exportMeasurements(measurements, fMeasurements);
                    CsvExporter.exportNutrition(nutrition, fNutrition);
                    CsvExporter.exportWater(water, fWater);
                    CsvExporter.exportSleep(sleep, fSleep);
                    return 5;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        ToastNotification.showToast(SettingsPanel.this, "All 5 reports exported successfully!", ToastNotification.ToastType.SUCCESS);
                        JOptionPane.showMessageDialog(SettingsPanel.this,
                                "All 5 fitness reports exported successfully to folder:\n" + dir.getAbsolutePath() +
                                "\n• PersonalTracker_Workouts_" + LocalDate.now() + ".csv" +
                                "\n• PersonalTracker_Measurements_" + LocalDate.now() + ".csv" +
                                "\n• PersonalTracker_Nutrition_" + LocalDate.now() + ".csv" +
                                "\n• PersonalTracker_Water_" + LocalDate.now() + ".csv" +
                                "\n• PersonalTracker_Sleep_" + LocalDate.now() + ".csv",
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
