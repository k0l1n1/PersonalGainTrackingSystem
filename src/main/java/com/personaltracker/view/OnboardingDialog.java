package com.personaltracker.view;

import com.personaltracker.components.ModernButton;
import com.personaltracker.components.ModernTextField;
import com.personaltracker.config.ThemeConfig;
import com.personaltracker.dao.AchievementDAO;
import com.personaltracker.dao.GoalDAO;
import com.personaltracker.dao.MeasurementDAO;
import com.personaltracker.dao.ProfileDAO;
import com.personaltracker.model.BodyMeasurement;
import com.personaltracker.model.Goal;
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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

/**
 * Onboarding Profile Setup Dialog for new login / registered users.
 * Automatically triggered when a user's profile is incomplete (height/target weight missing).
 * Provides a low-friction, beautiful Black + Crimson Red setup experience with steppers and presets.
 */
public class OnboardingDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final UserProfile profile;
    private final ProfileDAO profileDAO = new ProfileDAO();
    private final MeasurementDAO measurementDAO = new MeasurementDAO();
    private final GoalDAO goalDAO = new GoalDAO();
    private final AchievementDAO achievementDAO = new AchievementDAO();

    // Form inputs
    private final ModernTextField txtFullName;
    private final JComboBox<String> cmbDobMonth;
    private final JComboBox<String> cmbDobDay;
    private final JComboBox<String> cmbDobYear;
    private final JLabel lblDobAge = new JLabel("Age: --");
    private final ModernTextField txtHeight;
    private final ModernTextField txtWeight;
    private final ModernTextField txtTargetWeight;
    private final ModernTextField txtCalorieTarget;
    private final ModernTextField txtWaterTarget;

    private String selectedGender = "Male";
    private final JPanel pnlGenderPills = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));

    private String selectedRole = com.personaltracker.model.UserProfile.ROLE_MEMBER;
    private final JPanel pnlRolePills = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));

    // Live feedback indicators
    private final JLabel lblLiveBmi = new JLabel("Initial BMI: --");
    private final JLabel lblTargetGap = new JLabel("Goal Delta: --");
    private final JLabel lblError = new JLabel("");
    private final ModernButton btnComplete = new ModernButton("Complete Setup & Enter Dashboard", ModernButton.ButtonStyle.PRIMARY);

    /**
     * Checks if the given profile needs initial onboarding.
     */
    public static boolean isNewUser(UserProfile profile) {
        if (profile == null) return true;
        // Incomplete profile criteria: missing height or target weight
        boolean missingHeight = (profile.getHeightCm() == null || profile.getHeightCm() <= 0);
        boolean missingTarget = (profile.getTargetWeightKg() == null || profile.getTargetWeightKg() <= 0);
        return missingHeight || missingTarget;
    }

    /**
     * Checks the current session and displays the onboarding dialog if the user is new.
     * If the user has already created an account, bypasses the filling process and
     * directly loads their account profile.
     */
    public static void showIfNewUser(Window owner) {
        UserSession session = SupabaseClient.getInstance().getCurrentSession();
        if (session == null) return;

        // If user already created an account (not a brand new registration),
        // directly bypass onboarding form filling and ensure their existing profile is loaded.
        if (!session.isNewRegistration()) {
            if (session.getProfile() == null || session.getProfile().getHeightCm() == null) {
                try {
                    ProfileDAO dao = new ProfileDAO();
                    UserProfile p = dao.getProfile(session.getUserId());
                    if (p != null) {
                        session.setProfile(p);
                    }
                } catch (Exception ignored) {}
            }
            return;
        }

        UserProfile profile = session.getProfile();
        if (profile == null || profile.getHeightCm() == null) {
            try {
                ProfileDAO dao = new ProfileDAO();
                UserProfile remote = dao.getProfile(session.getUserId());
                if (remote != null) {
                    profile = remote;
                    session.setProfile(profile);
                }
            } catch (Exception ignored) {}
        }

        // Double check: if profile already has height and target weight filled,
        // it is an existing account with completed data - do not force onboarding!
        if (profile != null && !isNewUser(profile)) {
            session.setNewRegistration(false);
            return;
        }

        if (profile == null) {
            profile = new UserProfile(session.getUserId(), session.getEmail(), "");
            session.setProfile(profile);
        }

        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        // A new person created an account: make them do the data filling process!
        OnboardingDialog dialog = new OnboardingDialog(owner, profile, true);
        dialog.setVisible(true);
    }

    private final boolean mandatory;

    public boolean isMandatory() {
        return mandatory;
    }

    public OnboardingDialog(Window owner, UserProfile userProfile) {
        this(owner, userProfile, false);
    }

    public OnboardingDialog(Window owner, UserProfile userProfile, boolean mandatory) {
        super(owner, "Welcome to Personal Gain - Profile Setup", ModalityType.APPLICATION_MODAL);
        this.profile = userProfile != null ? userProfile : new UserProfile();
        this.mandatory = mandatory;

        if (mandatory) {
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    int choice = JOptionPane.showConfirmDialog(
                            OnboardingDialog.this,
                            "Initial profile setup is required to configure your fitness metrics.\n" +
                            "Would you like to sign out and exit?",
                            "Setup Incomplete",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE
                    );
                    if (choice == JOptionPane.YES_OPTION) {
                        SupabaseAuthService.getInstance().logout();
                        dispose();
                        System.exit(0);
                    }
                }
            });
        } else {
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        }

        setSize(580, 720);
        setLocationRelativeTo(owner);
        setResizable(false);
        getContentPane().setBackground(ThemeConfig.BG_DARK);

        // Pre-fill initial fields without auto-filling dummy data
        String initialName = profile.getFullName();
        if ((initialName == null || initialName.isBlank()) && profile.getEmail() != null && profile.getEmail().contains("@")) {
            initialName = profile.getEmail().substring(0, profile.getEmail().indexOf('@'));
            // Capitalize first letter
            if (!initialName.isEmpty()) {
                initialName = Character.toUpperCase(initialName.charAt(0)) + initialName.substring(1);
            }
        }
        txtFullName = new ModernTextField(initialName != null ? initialName : "", "e.g. Alex Hunter");

        if (profile.getRole() != null && !profile.getRole().isBlank()) {
            selectedRole = profile.getRole();
        }

        if (profile.getGender() != null && !profile.getGender().isBlank()) {
            selectedGender = profile.getGender();
        }

        // Structured DOB selector (Month, Day, Year) with live age feedback
        String[] months = {
            "Month", "01 - Jan", "02 - Feb", "03 - Mar", "04 - Apr", "05 - May", "06 - Jun",
            "07 - Jul", "08 - Aug", "09 - Sep", "10 - Oct", "11 - Nov", "12 - Dec"
        };
        cmbDobMonth = createStyledComboBox(months);

        String[] days = new String[32];
        days[0] = "Day";
        for (int i = 1; i <= 31; i++) {
            days[i] = String.format("%02d", i);
        }
        cmbDobDay = createStyledComboBox(days);

        int curYear = LocalDate.now().getYear();
        int minYear = 1940;
        int countYears = curYear - minYear + 1;
        String[] years = new String[countYears + 1];
        years[0] = "Year";
        for (int i = 0; i < countYears; i++) {
            years[i + 1] = String.valueOf(curYear - i);
        }
        cmbDobYear = createStyledComboBox(years);

        cmbDobMonth.addActionListener(e -> updateDobAgeDisplay());
        cmbDobDay.addActionListener(e -> updateDobAgeDisplay());
        cmbDobYear.addActionListener(e -> updateDobAgeDisplay());

        if (profile.getDateOfBirth() != null) {
            LocalDate existingDob = profile.getDateOfBirth();
            cmbDobMonth.setSelectedIndex(existingDob.getMonthValue());
            cmbDobDay.setSelectedItem(String.format("%02d", existingDob.getDayOfMonth()));
            cmbDobYear.setSelectedItem(String.valueOf(existingDob.getYear()));
            updateDobAgeDisplay();
        } else {
            updateDobAgeDisplay();
        }

        // Height: leave empty with translucent placeholder unless already saved in profile
        String initialHeight = (profile.getHeightCm() != null && profile.getHeightCm() > 0)
                ? String.format(Locale.US, "%.1f", profile.getHeightCm())
                : "";
        txtHeight = new ModernTextField(initialHeight, "e.g. 175.0");

        // Starting Weight: leave empty with translucent placeholder
        txtWeight = new ModernTextField("", "e.g. 75.0");

        // Goal Weight: leave empty with translucent placeholder unless already saved in profile
        String initialTargetWeight = (profile.getTargetWeightKg() != null && profile.getTargetWeightKg() > 0)
                ? String.format(Locale.US, "%.1f", profile.getTargetWeightKg())
                : "";
        txtTargetWeight = new ModernTextField(initialTargetWeight, "e.g. 70.0");

        // Daily Calorie Target: leave empty with translucent placeholder unless already saved
        String initialCalories = profile.getDailyCalorieTarget() > 0
                ? String.valueOf(profile.getDailyCalorieTarget())
                : "";
        txtCalorieTarget = new ModernTextField(initialCalories, "e.g. 2200");

        // Daily Water Target: leave empty with translucent placeholder unless already saved
        String initialWater = profile.getDailyWaterTargetMl() > 0
                ? String.valueOf(profile.getDailyWaterTargetMl())
                : "";
        txtWaterTarget = new ModernTextField(initialWater, "e.g. 2500");

        // Build UI
        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);

        // Header
        JPanel headerPanel = createHeaderPanel();
        root.add(headerPanel, BorderLayout.NORTH);

        // Scrollable content
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 26, 16, 26));

        // Section 1: Identity & Demographics
        contentPanel.add(createIdentityCard());
        contentPanel.add(Box.createVerticalStrut(14));

        // Section 2: Body Baseline & Realtime Metrics
        contentPanel.add(createBodyMetricsCard());
        contentPanel.add(Box.createVerticalStrut(14));

        // Section 3: Goals & Daily Targets
        contentPanel.add(createTargetsCard());
        contentPanel.add(Box.createVerticalStrut(16));

        // Error message
        lblError.setFont(ThemeConfig.FONT_SMALL);
        lblError.setForeground(ThemeConfig.ERROR);
        lblError.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblError.setVisible(false);
        contentPanel.add(lblError);
        contentPanel.add(Box.createVerticalStrut(10));

        // Action Buttons
        contentPanel.add(createActionButtonsPanel());

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scrollPane, BorderLayout.CENTER);

        setContentPane(root);

        // Setup real-time calculations
        attachCalculationListeners();
        updateCalculations();
        attachEnterKey();
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(20, 26, 12, 26));

        JLabel iconLbl = new JLabel(IconFactory.createIcon(IconFactory.IconType.WORKOUT, 36, ThemeConfig.PRIMARY_ACCENT));
        header.add(iconLbl, BorderLayout.WEST);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 3));
        textPanel.setOpaque(false);

        JLabel titleLbl = new JLabel("WELCOME TO PERSONAL GAIN");
        titleLbl.setFont(ThemeConfig.FONT_TITLE);
        titleLbl.setForeground(ThemeConfig.TEXT_PRIMARY);

        JLabel subLbl = new JLabel("Set up your profile once to unlock personalized tracking & targets.");
        subLbl.setFont(ThemeConfig.FONT_SMALL);
        subLbl.setForeground(ThemeConfig.TEXT_MUTED);

        textPanel.add(titleLbl);
        textPanel.add(subLbl);
        header.add(textPanel, BorderLayout.CENTER);

        return header;
    }

    private JPanel createIdentityCard() {
        JPanel card = createStyledCard("1. Personal Details");

        // Role row with pills
        card.add(createFieldHeader("Account Role"));
        card.add(Box.createVerticalStrut(4));
        pnlRolePills.setOpaque(false);
        refreshRolePills();
        card.add(pnlRolePills);
        card.add(Box.createVerticalStrut(10));

        // Full Name row
        card.add(createFieldHeader("Full Name"));
        card.add(Box.createVerticalStrut(4));
        card.add(txtFullName);
        card.add(Box.createVerticalStrut(10));

        // Gender row with pills
        card.add(createFieldHeader("Gender"));
        card.add(Box.createVerticalStrut(4));
        pnlGenderPills.setOpaque(false);
        refreshGenderPills();
        card.add(pnlGenderPills);
        card.add(Box.createVerticalStrut(10));

        // Date of birth with live age badge and quick age pills
        JPanel dobHeader = new JPanel(new BorderLayout());
        dobHeader.setOpaque(false);

        JPanel dobTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        dobTitlePanel.setOpaque(false);
        dobTitlePanel.add(createFieldHeader("Date of Birth"));
        dobTitlePanel.add(lblDobAge);
        dobHeader.add(dobTitlePanel, BorderLayout.WEST);

        JPanel ageChips = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        ageChips.setOpaque(false);
        ageChips.add(createAgeChip("20", 20));
        ageChips.add(createAgeChip("25", 25));
        ageChips.add(createAgeChip("30", 30));
        ageChips.add(createAgeChip("35", 35));
        dobHeader.add(ageChips, BorderLayout.EAST);

        card.add(dobHeader);
        card.add(Box.createVerticalStrut(6));

        JPanel dobPickerRow = new JPanel(new GridLayout(1, 3, 8, 0));
        dobPickerRow.setOpaque(false);
        dobPickerRow.add(cmbDobMonth);
        dobPickerRow.add(cmbDobDay);
        dobPickerRow.add(cmbDobYear);
        card.add(dobPickerRow);

        return card;
    }

    private JButton createAgeChip(String label, int age) {
        JButton btn = new JButton("Age " + label);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btn.setForeground(ThemeConfig.TEXT_SECONDARY);
        btn.setBackground(ThemeConfig.BG_DARK);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> setDobByAge(age));
        return btn;
    }

    private void setDobByAge(int age) {
        int birthYear = LocalDate.now().getYear() - age;
        cmbDobYear.setSelectedItem(String.valueOf(birthYear));
        cmbDobMonth.setSelectedIndex(1); // 01 - Jan
        cmbDobDay.setSelectedItem("01");
        updateDobAgeDisplay();
    }

    private void refreshGenderPills() {
        pnlGenderPills.removeAll();
        String[] options = {"Male", "Female", "Non-Binary", "Other", "Prefer not to say"};
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
            });
            pnlGenderPills.add(pill);
        }
    }

    private void refreshRolePills() {
        pnlRolePills.removeAll();
        String[][] roles = {
            {com.personaltracker.model.UserProfile.ROLE_MEMBER, "Normal Member (Workout & Health)"},
            {com.personaltracker.model.UserProfile.ROLE_TRAINER, "Personal Trainer (Manage Students)"}
        };
        for (String[] r : roles) {
            String roleVal = r[0];
            String roleLabel = r[1];
            boolean isSelected = roleVal.equalsIgnoreCase(selectedRole);
            JButton pill = new JButton(roleLabel);
            pill.setFont(ThemeConfig.FONT_SMALL);
            if (roleVal.equalsIgnoreCase(UserProfile.ROLE_MEMBER)) {
                pill.setIcon(IconFactory.createIcon(IconFactory.IconType.USER, 13, isSelected ? Color.WHITE : ThemeConfig.TEXT_SECONDARY));
            } else {
                pill.setIcon(IconFactory.createIcon(IconFactory.IconType.STUDENTS, 13, isSelected ? Color.WHITE : ThemeConfig.TEXT_SECONDARY));
            }
            pill.setIconTextGap(6);
            pill.setForeground(isSelected ? Color.WHITE : ThemeConfig.TEXT_SECONDARY);
            pill.setBackground(isSelected ? ThemeConfig.PRIMARY_ACCENT : ThemeConfig.BG_DARK);
            pill.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(isSelected ? ThemeConfig.PRIMARY_ACCENT : ThemeConfig.BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(4, 10, 4, 10)
            ));
            pill.setFocusPainted(false);
            pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            pill.addActionListener(e -> {
                selectedRole = roleVal;
                refreshRolePills();
                pnlRolePills.revalidate();
                pnlRolePills.repaint();
            });
            pnlRolePills.add(pill);
        }
    }

    private JPanel createBodyMetricsCard() {
        JPanel card = createStyledCard("2. Body Baseline & Target");

        // Height Row with steppers
        JPanel heightRow = new JPanel(new BorderLayout(8, 0));
        heightRow.setOpaque(false);
        heightRow.add(createFieldHeader("Height (cm)"), BorderLayout.WEST);

        JPanel heightSteppers = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        heightSteppers.setOpaque(false);
        heightSteppers.add(createAdjustButton("-5", () -> adjustDoubleField(txtHeight, -5.0, 175.0)));
        heightSteppers.add(createAdjustButton("-1", () -> adjustDoubleField(txtHeight, -1.0, 175.0)));
        heightSteppers.add(createAdjustButton("+1", () -> adjustDoubleField(txtHeight, +1.0, 175.0)));
        heightSteppers.add(createAdjustButton("+5", () -> adjustDoubleField(txtHeight, +5.0, 175.0)));
        heightRow.add(heightSteppers, BorderLayout.EAST);

        card.add(heightRow);
        card.add(Box.createVerticalStrut(4));
        card.add(txtHeight);
        card.add(Box.createVerticalStrut(10));

        // Weight & Target Weight in side-by-side layout
        JPanel weightsGrid = new JPanel(new GridLayout(1, 2, 14, 0));
        weightsGrid.setOpaque(false);

        // Current Weight
        JPanel colWeight = new JPanel();
        colWeight.setLayout(new BoxLayout(colWeight, BoxLayout.Y_AXIS));
        colWeight.setOpaque(false);

        JPanel weightHeader = new JPanel(new BorderLayout());
        weightHeader.setOpaque(false);
        weightHeader.add(createFieldHeader("Starting Weight (kg)"), BorderLayout.WEST);
        JPanel wSteppers = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        wSteppers.setOpaque(false);
        wSteppers.add(createAdjustButton("-1", () -> adjustDoubleField(txtWeight, -1.0, 75.0)));
        wSteppers.add(createAdjustButton("+1", () -> adjustDoubleField(txtWeight, +1.0, 75.0)));
        weightHeader.add(wSteppers, BorderLayout.EAST);

        colWeight.add(weightHeader);
        colWeight.add(Box.createVerticalStrut(4));
        colWeight.add(txtWeight);
        weightsGrid.add(colWeight);

        // Target Weight
        JPanel colTarget = new JPanel();
        colTarget.setLayout(new BoxLayout(colTarget, BoxLayout.Y_AXIS));
        colTarget.setOpaque(false);

        JPanel targetHeader = new JPanel(new BorderLayout());
        targetHeader.setOpaque(false);
        targetHeader.add(createFieldHeader("Goal Weight (kg)"), BorderLayout.WEST);
        JPanel tSteppers = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        tSteppers.setOpaque(false);
        tSteppers.add(createAdjustButton("-1", () -> adjustDoubleField(txtTargetWeight, -1.0, getBaseWeightForTarget())));
        tSteppers.add(createAdjustButton("+1", () -> adjustDoubleField(txtTargetWeight, +1.0, getBaseWeightForTarget())));
        targetHeader.add(tSteppers, BorderLayout.EAST);

        colTarget.add(targetHeader);
        colTarget.add(Box.createVerticalStrut(4));
        colTarget.add(txtTargetWeight);
        weightsGrid.add(colTarget);

        card.add(weightsGrid);
        card.add(Box.createVerticalStrut(8));

        // Quick goal presets (Cut -5kg, Maintain, Bulk +5kg)
        JPanel goalPresets = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        goalPresets.setOpaque(false);
        goalPresets.add(createGoalPresetButton("-5kg Cut", -5.0));
        goalPresets.add(createGoalPresetButton("Maintain", 0.0));
        goalPresets.add(createGoalPresetButton("+5kg Bulk", +5.0));
        card.add(goalPresets);
        card.add(Box.createVerticalStrut(10));

        // Live calculation display
        JPanel liveCalcBox = new JPanel(new GridLayout(1, 2, 8, 0));
        liveCalcBox.setBackground(ThemeConfig.BG_DARK);
        liveCalcBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        lblLiveBmi.setFont(ThemeConfig.FONT_BODY_BOLD);
        lblLiveBmi.setForeground(ThemeConfig.PRIMARY_ACCENT);
        liveCalcBox.add(lblLiveBmi);

        lblTargetGap.setFont(ThemeConfig.FONT_BODY_BOLD);
        lblTargetGap.setForeground(ThemeConfig.PRIMARY_ACCENT);
        liveCalcBox.add(lblTargetGap);

        card.add(liveCalcBox);

        return card;
    }

    private JButton createGoalPresetButton(String text, double offsetKg) {
        JButton btn = new JButton(text);
        btn.setFont(ThemeConfig.FONT_SMALL);
        btn.setForeground(ThemeConfig.TEXT_SECONDARY);
        btn.setBackground(ThemeConfig.BG_DARK);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            try {
                double currentW = getBaseWeightForTarget();
                double targetW = Math.max(30.0, currentW + offsetKg);
                txtTargetWeight.setText(String.format(Locale.US, "%.1f", targetW));
                updateCalculations();
            } catch (Exception ignored) {}
        });
        return btn;
    }

    private JPanel createTargetsCard() {
        JPanel card = createStyledCard("3. Fitness Focus & Daily Targets");

        // Quick Focus Presets
        JLabel lblFocus = createFieldHeader("Select Your Focus to Auto-Configure Targets:");
        card.add(lblFocus);
        card.add(Box.createVerticalStrut(6));

        JPanel focusRow = new JPanel(new GridLayout(1, 3, 8, 0));
        focusRow.setOpaque(false);
        focusRow.add(createFocusButton("Fat Loss", 1850, 3000));
        focusRow.add(createFocusButton("Maintain", 2200, 2500));
        focusRow.add(createFocusButton("Muscle Gain", 2700, 3200));
        card.add(focusRow);
        card.add(Box.createVerticalStrut(12));

        // Calorie & Water row
        JPanel targetsGrid = new JPanel(new GridLayout(1, 2, 14, 0));
        targetsGrid.setOpaque(false);

        // Daily Calorie Target
        JPanel colCal = new JPanel();
        colCal.setLayout(new BoxLayout(colCal, BoxLayout.Y_AXIS));
        colCal.setOpaque(false);
        colCal.add(createFieldHeader("Daily Calories (kcal)"));
        colCal.add(Box.createVerticalStrut(4));
        colCal.add(txtCalorieTarget);

        JPanel calPills = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        calPills.setOpaque(false);
        calPills.add(createPresetPill("1800", () -> txtCalorieTarget.setText("1800")));
        calPills.add(createPresetPill("2200", () -> txtCalorieTarget.setText("2200")));
        calPills.add(createPresetPill("2600", () -> txtCalorieTarget.setText("2600")));
        colCal.add(calPills);
        targetsGrid.add(colCal);

        // Daily Water Target
        JPanel colWater = new JPanel();
        colWater.setLayout(new BoxLayout(colWater, BoxLayout.Y_AXIS));
        colWater.setOpaque(false);
        colWater.add(createFieldHeader("Daily Water (ml)"));
        colWater.add(Box.createVerticalStrut(4));
        colWater.add(txtWaterTarget);

        JPanel waterPills = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        waterPills.setOpaque(false);
        waterPills.add(createPresetPill("2000ml", () -> txtWaterTarget.setText("2000")));
        waterPills.add(createPresetPill("2500ml", () -> txtWaterTarget.setText("2500")));
        waterPills.add(createPresetPill("3000ml", () -> txtWaterTarget.setText("3000")));
        colWater.add(waterPills);
        targetsGrid.add(colWater);

        card.add(targetsGrid);

        return card;
    }

    private JButton createFocusButton(String title, int calories, int waterMl) {
        JButton btn = new JButton(title);
        btn.setFont(ThemeConfig.FONT_BODY_BOLD);
        btn.setForeground(Color.WHITE);
        btn.setBackground(ThemeConfig.BG_DARK);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(8, 6, 8, 6)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            txtCalorieTarget.setText(String.valueOf(calories));
            txtWaterTarget.setText(String.valueOf(waterMl));
        });
        return btn;
    }

    private JButton createPresetPill(String label, Runnable onClick) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btn.setForeground(ThemeConfig.TEXT_SECONDARY);
        btn.setBackground(ThemeConfig.BG_DARK);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> onClick.run());
        return btn;
    }

    private JPanel createActionButtonsPanel() {
        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setOpaque(false);

        btnComplete.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btnComplete.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnComplete.addActionListener(e -> saveAndComplete());
        actions.add(btnComplete);

        actions.add(Box.createVerticalStrut(8));

        ModernButton btnSkip = new ModernButton("Skip Setup & Use Standard Defaults", ModernButton.ButtonStyle.SECONDARY);
        btnSkip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        btnSkip.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSkip.addActionListener(e -> applyDefaultsAndComplete());
        actions.add(btnSkip);

        return actions;
    }

    private JPanel createStyledCard(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ThemeConfig.BG_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(ThemeConfig.FONT_SUBTITLE);
        titleLbl.setForeground(ThemeConfig.PRIMARY_ACCENT);
        panel.add(titleLbl);
        panel.add(Box.createVerticalStrut(10));

        return panel;
    }

    private JLabel createFieldHeader(String text) {
        JLabel l = new JLabel(text);
        l.setFont(ThemeConfig.FONT_SMALL);
        l.setForeground(ThemeConfig.TEXT_SECONDARY);
        return l;
    }

    private JButton createAdjustButton(String label, Runnable action) {
        JButton b = new JButton(label);
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

    private double getBaseWeightForTarget() {
        try {
            String wStr = txtWeight.getText().trim();
            if (!wStr.isEmpty()) {
                return Double.parseDouble(wStr);
            }
        } catch (Exception ignored) {}
        return 75.0;
    }

    private void adjustDoubleField(ModernTextField field, double delta, double defaultBase) {
        try {
            String text = field.getText().trim();
            double base;
            if (text.isEmpty()) {
                base = defaultBase + delta;
            } else {
                base = Double.parseDouble(text) + delta;
            }
            double updated = Math.max(10.0, base);
            field.setText(String.format(Locale.US, "%.1f", updated));
            updateCalculations();
        } catch (Exception ignored) {}
    }

    private void updateDobAgeDisplay() {
        LocalDate dob = getSelectedDateOfBirth();
        if (dob != null) {
            int age = Period.between(dob, LocalDate.now()).getYears();
            if (age >= 0 && age <= 120) {
                lblDobAge.setText("Age: " + age + " yrs");
                lblDobAge.setForeground(ThemeConfig.PRIMARY_ACCENT);
                lblDobAge.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeConfig.PRIMARY_ACCENT, 1, true),
                        BorderFactory.createEmptyBorder(2, 6, 2, 6)
                ));
                return;
            }
        }
        lblDobAge.setText("Age: --");
        lblDobAge.setForeground(ThemeConfig.TEXT_MUTED);
        lblDobAge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
    }

    private LocalDate getSelectedDateOfBirth() {
        int mIdx = cmbDobMonth.getSelectedIndex();
        int dIdx = cmbDobDay.getSelectedIndex();
        int yIdx = cmbDobYear.getSelectedIndex();
        if (mIdx <= 0 || dIdx <= 0 || yIdx <= 0) {
            return null;
        }
        try {
            int m = mIdx;
            int d = Integer.parseInt((String) cmbDobDay.getSelectedItem());
            int y = Integer.parseInt((String) cmbDobYear.getSelectedItem());
            YearMonth ym = YearMonth.of(y, m);
            int maxDays = ym.lengthOfMonth();
            if (d > maxDays) {
                d = maxDays;
                cmbDobDay.setSelectedItem(String.format("%02d", d));
            }
            return LocalDate.of(y, m, d);
        } catch (Exception e) {
            return null;
        }
    }

    private <T> JComboBox<T> createStyledComboBox(T[] items) {
        JComboBox<T> box = new JComboBox<>(items);
        box.setFont(ThemeConfig.FONT_BODY);
        box.setForeground(ThemeConfig.TEXT_PRIMARY);
        box.setBackground(ThemeConfig.BG_INPUT);
        box.setOpaque(true);
        box.setFocusable(false);
        box.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setFont(ThemeConfig.FONT_BODY);
                lbl.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
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

    private void attachCalculationListeners() {
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updateCalculations(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updateCalculations(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updateCalculations(); }
        };
        txtHeight.getDocument().addDocumentListener(listener);
        txtWeight.getDocument().addDocumentListener(listener);
        txtTargetWeight.getDocument().addDocumentListener(listener);
    }

    private void updateCalculations() {
        try {
            String hText = txtHeight.getText().trim();
            String wText = txtWeight.getText().trim();
            String twText = txtTargetWeight.getText().trim();

            if (!hText.isEmpty() && !wText.isEmpty()) {
                double h = Double.parseDouble(hText);
                double w = Double.parseDouble(wText);
                if (h > 50 && w > 20) {
                    double bmi = HealthCalculator.calculateBmi(w, h);
                    String category = HealthCalculator.getBmiCategory(bmi);
                    lblLiveBmi.setText(String.format(Locale.US, "BMI: %.1f (%s)", bmi, category));
                } else {
                    lblLiveBmi.setText("Initial BMI: --");
                }
            } else {
                lblLiveBmi.setText("Initial BMI: --");
            }

            if (!wText.isEmpty() && !twText.isEmpty()) {
                double w = Double.parseDouble(wText);
                double tw = Double.parseDouble(twText);
                if (w > 20 && tw > 20) {
                    double diff = tw - w;
                    String direction = diff > 0.05 ? String.format(Locale.US, "+%.1f kg (Bulk)", diff) :
                                       diff < -0.05 ? String.format(Locale.US, "%.1f kg (Cut)", diff) : "Maintain";
                    lblTargetGap.setText("Goal: " + direction);
                } else {
                    lblTargetGap.setText("Goal Delta: --");
                }
            } else {
                lblTargetGap.setText("Goal Delta: --");
            }
        } catch (Exception e) {
            lblLiveBmi.setText("Initial BMI: --");
            lblTargetGap.setText("Goal Delta: --");
        }
    }

    private void attachEnterKey() {
        KeyAdapter enterListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    saveAndComplete();
                }
            }
        };
        txtFullName.addKeyListener(enterListener);
        txtHeight.addKeyListener(enterListener);
        txtWeight.addKeyListener(enterListener);
        txtTargetWeight.addKeyListener(enterListener);
        txtCalorieTarget.addKeyListener(enterListener);
        txtWaterTarget.addKeyListener(enterListener);
    }

    private void applyDefaultsAndComplete() {
        if (txtFullName.getText().trim().isEmpty()) {
            txtFullName.setText("Athlete");
        }
        txtHeight.setText("175.0");
        txtWeight.setText("75.0");
        txtTargetWeight.setText("72.0");
        txtCalorieTarget.setText("2200");
        txtWaterTarget.setText("2500");
        setDobByAge(25);
        saveAndComplete();
    }

    private void saveAndComplete() {
        lblError.setVisible(false);

        String fullName = txtFullName.getText().trim();
        if (fullName.isEmpty()) {
            showError("Please enter your full name.");
            txtFullName.requestFocus();
            return;
        }

        String hStr = txtHeight.getText().trim();
        String wStr = txtWeight.getText().trim();
        String twStr = txtTargetWeight.getText().trim();
        String calStr = txtCalorieTarget.getText().trim();
        String waterStr = txtWaterTarget.getText().trim();

        if (hStr.isEmpty()) {
            showError("Please enter your height in cm (e.g. 175.0).");
            txtHeight.requestFocus();
            return;
        }
        if (wStr.isEmpty()) {
            showError("Please enter your starting weight in kg (e.g. 75.0).");
            txtWeight.requestFocus();
            return;
        }
        if (twStr.isEmpty()) {
            showError("Please enter your goal weight in kg (e.g. 70.0).");
            txtTargetWeight.requestFocus();
            return;
        }

        double height;
        double weight;
        double targetWeight;
        int calories;
        int water;

        try {
            height = Double.parseDouble(hStr);
            weight = Double.parseDouble(wStr);
            targetWeight = Double.parseDouble(twStr);
            calories = calStr.isEmpty() ? 2200 : Integer.parseInt(calStr);
            water = waterStr.isEmpty() ? 2500 : Integer.parseInt(waterStr);

            if (height < 80 || height > 260) {
                showError("Please enter a realistic height between 80 cm and 260 cm.");
                return;
            }
            if (weight < 25 || weight > 350) {
                showError("Please enter a realistic starting weight between 25 kg and 350 kg.");
                return;
            }
            if (targetWeight < 25 || targetWeight > 350) {
                showError("Please enter a realistic goal weight between 25 kg and 350 kg.");
                return;
            }
        } catch (NumberFormatException ex) {
            showError("Please check that height, weight, calories, and water are valid numbers.");
            return;
        }

        LocalDate dob = getSelectedDateOfBirth();
        if (dob != null && dob.isAfter(LocalDate.now())) {
            showError("Date of birth cannot be in the future.");
            return;
        }

        btnComplete.setEnabled(false);
        btnComplete.setText("Setting up your dashboard...");

        // Populate profile model
        profile.setFullName(fullName);
        profile.setGender(selectedGender);
        profile.setRole(selectedRole);
        if (profile.getUserCode() == null || profile.getUserCode().isBlank()) {
            profile.setUserCode(com.personaltracker.model.UserProfile.generateUniqueUserCode());
        }
        if (dob != null) profile.setDateOfBirth(dob);
        profile.setHeightCm(height);
        profile.setTargetWeightKg(targetWeight);
        profile.setDailyCalorieTarget(calories);
        profile.setDailyWaterTargetMl(water);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                // 1. Save user profile
                profileDAO.updateProfile(profile);

                // 2. Save Day-1 initial body measurement so charts & metrics have baseline data immediately
                try {
                    List<BodyMeasurement> existingMeasurements = measurementDAO.getAllMeasurements();
                    boolean hasToday = false;
                    if (existingMeasurements != null) {
                        for (BodyMeasurement m : existingMeasurements) {
                            if (LocalDate.now().equals(m.getRecordedDate())) {
                                hasToday = true;
                                break;
                            }
                        }
                    }
                    if (!hasToday) {
                        BodyMeasurement baseline = new BodyMeasurement(LocalDate.now(), weight, height);
                        measurementDAO.saveMeasurement(baseline);
                    }
                } catch (Exception ex) {
                    System.err.println("Notice: Could not log initial measurement: " + ex.getMessage());
                }

                // 3. Save or update initial goal (Target Weight) - avoids creating duplicate goals
                try {
                    List<Goal> existingGoals = goalDAO.getAllGoals();
                    Goal existingWeightGoal = null;
                    if (existingGoals != null) {
                        for (Goal g : existingGoals) {
                            if ("Weight".equalsIgnoreCase(g.getCategory()) ||
                                    (g.getTitle() != null && g.getTitle().equalsIgnoreCase("Reach Target Weight"))) {
                                existingWeightGoal = g;
                                break;
                            }
                        }
                    }

                    if (existingWeightGoal != null) {
                        // Update existing weight goal target and current values
                        existingWeightGoal.setTargetValue(targetWeight);
                        existingWeightGoal.setCurrentValue(weight);
                        existingWeightGoal.setTargetDate(LocalDate.now().plusMonths(3));
                        goalDAO.updateGoal(existingWeightGoal);
                    } else {
                        Goal weightGoal = new Goal("Reach Target Weight", "Weight", targetWeight, weight, "kg", LocalDate.now().plusMonths(3));
                        goalDAO.saveGoal(weightGoal);
                    }
                } catch (Exception ex) {
                    System.err.println("Notice: Could not log or update initial goal: " + ex.getMessage());
                }

                // 4. Try unlocking welcome achievement
                try {
                    achievementDAO.unlockAchievement("FIRST_WORKOUT");
                } catch (Exception ignored) {}

                return null;
            }

            @Override
            protected void done() {
                btnComplete.setEnabled(true);
                btnComplete.setText("Complete Setup & Enter Dashboard");
                try {
                    get();
                    // Successfully saved, update in session
                    UserSession session = SupabaseClient.getInstance().getCurrentSession();
                    if (session != null) {
                        session.setProfile(profile);
                        session.setNewRegistration(false);
                    }
                    dispose();
                } catch (Exception e) {
                    showError(e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void showError(String msg) {
        lblError.setText("<html><center>" + msg + "</center></html>");
        lblError.setVisible(true);
    }
}
