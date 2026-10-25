package com.personaltracker.view;

import com.personaltracker.components.ModernButton;
import com.personaltracker.components.ModernCard;
import com.personaltracker.components.ModernTextField;
import com.personaltracker.components.ToastNotification;
import com.personaltracker.config.ThemeConfig;
import com.personaltracker.dao.TrainerAssignmentDAO;
import com.personaltracker.model.*;
import com.personaltracker.service.SupabaseClient;
import com.personaltracker.util.IconFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern modal dialog enabling Personal Trainers and Coaches to assign fitness goals,
 * schedule tailored workout routines, or dispatch coaching tasks directly to an enrolled student.
 */
public class AssignTaskDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final UserProfile studentProfile;
    private final Runnable onSuccessCallback;
    private final TrainerAssignmentDAO assignmentDAO = new TrainerAssignmentDAO();

    // Mode Selector
    private enum AssignmentMode {
        WORKOUT,
        GOAL,
        SCHEDULE
    }
    private AssignmentMode currentMode = AssignmentMode.WORKOUT;

    private final JButton btnTabWorkout = new JButton("Schedule Workout");
    private final JButton btnTabGoal = new JButton("Assign Goal");
    private final JButton btnTabSchedule = new JButton("Schedule Task");

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel pnlCards = new JPanel(cardLayout);

    // Common Coach Notes
    private final JTextArea txtCoachNotes = new JTextArea(3, 20);

    // --- WORKOUT FIELDS ---
    private final ModernTextField txtWorkoutTitle = new ModernTextField("", "e.g. Upper Body Hypertrophy & Arms");
    private final JComboBox<String> cmbWorkoutType = new JComboBox<>(new String[]{"Strength", "Cardio", "HIIT", "Endurance", "Yoga", "Flexibility", "Other"});
    private final ModernTextField txtWorkoutDate = new ModernTextField(LocalDate.now().plusDays(1).toString(), "YYYY-MM-DD");
    private final ModernTextField txtWorkoutDuration = new ModernTextField("50", "Duration (mins)");
    private final ModernTextField txtWorkoutCalories = new ModernTextField("420", "Target Burn (kcal)");
    private final DefaultTableModel exerciseTableModel;
    private final JTable tblExercises;
    private final List<WorkoutExercise> plannedExercises = new ArrayList<>();
    private JLabel lblExCount;

    // --- GOAL FIELDS ---
    private final ModernTextField txtGoalTitle = new ModernTextField("", "e.g. Sub-25 Minute 5K Run");
    private final JComboBox<String> cmbGoalCategory = new JComboBox<>(new String[]{"Weight", "Workout Frequency", "Strength", "Endurance", "Nutrition", "Habit"});
    private final ModernTextField txtGoalTargetVal = new ModernTextField("60.0", "Target number");
    private final ModernTextField txtGoalCurrentVal = new ModernTextField("63.5", "Current number");
    private final ModernTextField txtGoalUnit = new ModernTextField("kg", "e.g. kg, reps, mins, km");
    private final ModernTextField txtGoalTargetDate = new ModernTextField(LocalDate.now().plusMonths(1).toString(), "YYYY-MM-DD");

    // --- SCHEDULE / TASK FIELDS ---
    private final ModernTextField txtScheduleTitle = new ModernTextField("", "e.g. Weekly Body Composition & Weigh-In");
    private final ModernTextField txtScheduleDate = new ModernTextField(LocalDate.now().plusDays(2).toString(), "YYYY-MM-DD");
    private final ModernTextField txtScheduleTime = new ModernTextField("09:00", "HH:mm");

    private final ModernButton btnDispatch;

    public AssignTaskDialog(Window owner, UserProfile studentProfile, Runnable onSuccessCallback) {
        super(owner, "Assign Coaching Task - " + (studentProfile != null ? studentProfile.getDisplayName() : "Student"), ModalityType.APPLICATION_MODAL);
        this.studentProfile = studentProfile;
        this.onSuccessCallback = onSuccessCallback;

        setSize(820, 740);
        setMinimumSize(new Dimension(740, 660));
        setLocationRelativeTo(owner);
        getContentPane().setBackground(ThemeConfig.BG_DARK);
        setLayout(new BorderLayout());

        // Table Model for Exercises
        exerciseTableModel = new DefaultTableModel(new Object[]{"Exercise Name", "Sets", "Reps", "Weight (kg)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblExercises = new JTable(exerciseTableModel);
        setupExerciseTable();

        // 1. Top Header Banner
        add(createHeaderBanner(), BorderLayout.NORTH);

        // 2. Center Content with Tab Switcher & Dynamic Forms
        JPanel centerContainer = new JPanel();
        centerContainer.setLayout(new BoxLayout(centerContainer, BoxLayout.Y_AXIS));
        centerContainer.setOpaque(false);
        centerContainer.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));

        // Tab switcher row
        JPanel tabSwitcher = createTabSwitcher();
        tabSwitcher.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerContainer.add(tabSwitcher);
        centerContainer.add(Box.createVerticalStrut(12));

        // Cards
        pnlCards.setOpaque(false);
        pnlCards.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlCards.add(createWorkoutForm(), AssignmentMode.WORKOUT.name());
        pnlCards.add(createGoalForm(), AssignmentMode.GOAL.name());
        pnlCards.add(createScheduleForm(), AssignmentMode.SCHEDULE.name());
        centerContainer.add(pnlCards);
        centerContainer.add(Box.createVerticalStrut(12));

        // Coach Notes & Advice Card
        JPanel coachNotes = createCoachNotesSection();
        coachNotes.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerContainer.add(coachNotes);

        JScrollPane scrollCenter = new JScrollPane(centerContainer);
        scrollCenter.setOpaque(false);
        scrollCenter.getViewport().setOpaque(false);
        scrollCenter.setBorder(null);
        scrollCenter.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollCenter.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollCenter, BorderLayout.CENTER);

        // 3. Bottom Action Bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 14));
        bottomBar.setBackground(ThemeConfig.BG_CARD);
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeConfig.BORDER_COLOR));

        ModernButton btnCancel = new ModernButton("Cancel", ModernButton.ButtonStyle.SECONDARY);
        btnCancel.setPreferredSize(new Dimension(110, 38));
        btnCancel.addActionListener(e -> dispose());
        bottomBar.add(btnCancel);

        String studentName = studentProfile != null ? studentProfile.getDisplayName() : "Student";
        btnDispatch = new ModernButton("Dispatch Assignment to " + studentName, ModernButton.ButtonStyle.PRIMARY);
        btnDispatch.setIcon(IconFactory.createIcon(IconFactory.IconType.CHECK, 14, Color.WHITE));
        btnDispatch.setIconTextGap(8);
        btnDispatch.setPreferredSize(new Dimension(280, 38));
        btnDispatch.addActionListener(e -> executeDispatch());
        bottomBar.add(btnDispatch);

        add(bottomBar, BorderLayout.SOUTH);

        // Initialize with default template & show workout mode
        switchMode(AssignmentMode.WORKOUT);
        loadWorkoutTemplate("Push");
    }

    private JPanel createHeaderBanner() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(ThemeConfig.BG_CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeConfig.BORDER_COLOR),
                BorderFactory.createEmptyBorder(16, 24, 16, 24)
        ));

        // Avatar
        String initials = studentProfile != null ? getInitials(studentProfile.getDisplayName()) : "ST";
        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(36, 36, 36));
                g2.fill(new Ellipse2D.Float(0, 0, 50, 50));
                g2.setColor(ThemeConfig.PRIMARY_ACCENT);
                g2.setStroke(new BasicStroke(2.2f));
                g2.draw(new Ellipse2D.Float(1, 1, 48, 48));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatar.setPreferredSize(new Dimension(50, 50));
        avatar.setOpaque(false);
        avatar.setLayout(new BorderLayout());
        JLabel lblInitials = new JLabel(initials, SwingConstants.CENTER);
        lblInitials.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblInitials.setForeground(Color.WHITE);
        avatar.add(lblInitials, BorderLayout.CENTER);
        header.add(avatar, BorderLayout.WEST);

        // Text details
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        String name = studentProfile != null ? studentProfile.getDisplayName() : "Student";
        JLabel lblName = new JLabel("Assign Fitness Task to " + name);
        lblName.setFont(ThemeConfig.FONT_SUBTITLE);
        lblName.setForeground(ThemeConfig.TEXT_PRIMARY);
        textPanel.add(lblName);
        textPanel.add(Box.createVerticalStrut(3));

        String code = studentProfile != null && studentProfile.getUserCode() != null ? studentProfile.getUserCode() : "N/A";
        String email = studentProfile != null && studentProfile.getEmail() != null ? studentProfile.getEmail() : "";
        JLabel lblSub = new JLabel("Student ID: " + code + "  •  " + email + "  •  Coaching Portal");
        lblSub.setFont(ThemeConfig.FONT_SMALL);
        lblSub.setForeground(ThemeConfig.TEXT_MUTED);
        textPanel.add(lblSub);

        header.add(textPanel, BorderLayout.CENTER);

        return header;
    }

    private JPanel createTabSwitcher() {
        JPanel bar = new JPanel(new GridLayout(1, 3, 10, 0));
        bar.setOpaque(false);
        bar.setMaximumSize(new Dimension(32767, 42));
        bar.setPreferredSize(new Dimension(720, 42));

        btnTabWorkout.setText("Schedule Workout");
        btnTabWorkout.setIconTextGap(8);

        btnTabGoal.setText("Assign Goal");
        btnTabGoal.setIconTextGap(8);

        btnTabSchedule.setText("Schedule Task");
        btnTabSchedule.setIconTextGap(8);

        btnTabWorkout.addActionListener(e -> switchMode(AssignmentMode.WORKOUT));
        btnTabGoal.addActionListener(e -> switchMode(AssignmentMode.GOAL));
        btnTabSchedule.addActionListener(e -> switchMode(AssignmentMode.SCHEDULE));

        bar.add(btnTabWorkout);
        bar.add(btnTabGoal);
        bar.add(btnTabSchedule);

        return bar;
    }

    private void styleTabButton(JButton btn, boolean active, IconFactory.IconType iconType) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (active) {
            btn.setForeground(ThemeConfig.TEXT_ON_ACCENT);
            btn.setBackground(ThemeConfig.PRIMARY_ACCENT);
            btn.setIcon(IconFactory.createIcon(iconType, 15, ThemeConfig.TEXT_ON_ACCENT));
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeConfig.PRIMARY_ACCENT, 1, true),
                    BorderFactory.createEmptyBorder(8, 14, 8, 14)
            ));
        } else {
            btn.setForeground(ThemeConfig.TEXT_SECONDARY);
            btn.setBackground(ThemeConfig.BG_CARD);
            btn.setIcon(IconFactory.createIcon(iconType, 15, ThemeConfig.TEXT_SECONDARY));
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(8, 14, 8, 14)
            ));
        }
    }

    private void switchMode(AssignmentMode mode) {
        this.currentMode = mode;
        styleTabButton(btnTabWorkout, mode == AssignmentMode.WORKOUT, IconFactory.IconType.WORKOUT);
        styleTabButton(btnTabGoal, mode == AssignmentMode.GOAL, IconFactory.IconType.GOAL);
        styleTabButton(btnTabSchedule, mode == AssignmentMode.SCHEDULE, IconFactory.IconType.REMINDERS);
        cardLayout.show(pnlCards, mode.name());
        pnlCards.revalidate();
        pnlCards.repaint();
    }

    // =========================================================================
    // 1. WORKOUT FORM
    // =========================================================================
    private JPanel createWorkoutForm() {
        ModernCard card = new ModernCard(12, new Insets(16, 20, 16, 20));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        // Quick Routine Template Chips
        JPanel pnlChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pnlChips.setOpaque(false);
        JLabel lblTpl = new JLabel("Quick Templates:");
        lblTpl.setFont(ThemeConfig.FONT_SMALL);
        lblTpl.setForeground(ThemeConfig.TEXT_MUTED);
        pnlChips.add(lblTpl);

        pnlChips.add(createChip("Push Day", () -> loadWorkoutTemplate("Push")));
        pnlChips.add(createChip("Pull & Back", () -> loadWorkoutTemplate("Pull")));
        pnlChips.add(createChip("Leg Hypertrophy", () -> loadWorkoutTemplate("Legs")));
        pnlChips.add(createChip("5K Cardio Run", () -> loadWorkoutTemplate("Cardio")));
        pnlChips.add(createChip("Core & HIIT", () -> loadWorkoutTemplate("HIIT")));

        card.add(pnlChips);
        card.add(Box.createVerticalStrut(12));

        // Row 1: Workout Title & Category
        JPanel r1 = new JPanel(new GridLayout(1, 2, 16, 0));
        r1.setOpaque(false);

        JPanel pnlTitle = createFieldWrapper("Workout Routine Title *", txtWorkoutTitle);
        JPanel pnlType = createFieldWrapper("Workout Type", cmbWorkoutType);
        styleComboBox(cmbWorkoutType);
        r1.add(pnlTitle);
        r1.add(pnlType);
        card.add(r1);
        card.add(Box.createVerticalStrut(10));

        // Row 2: Scheduled Date, Duration, Target Burn
        JPanel r2 = new JPanel(new GridLayout(1, 3, 14, 0));
        r2.setOpaque(false);
        r2.add(createFieldWrapper("Scheduled Date (YYYY-MM-DD) *", txtWorkoutDate));
        r2.add(createFieldWrapper("Est. Duration (mins)", txtWorkoutDuration));
        r2.add(createFieldWrapper("Target Calories (kcal)", txtWorkoutCalories));
        card.add(r2);
        card.add(Box.createVerticalStrut(14));

        // Exercises Section Header with "+ Add Exercise" button
        JPanel exHeader = new JPanel(new BorderLayout());
        exHeader.setOpaque(false);
        lblExCount = new JLabel("Assigned Exercise Routine (" + plannedExercises.size() + " exercises)");
        lblExCount.setFont(ThemeConfig.FONT_BODY_BOLD);
        lblExCount.setForeground(ThemeConfig.TEXT_PRIMARY);
        exHeader.add(lblExCount, BorderLayout.WEST);

        ModernButton btnAddEx = new ModernButton("+ Add Exercise", ModernButton.ButtonStyle.SECONDARY);
        btnAddEx.setPreferredSize(new Dimension(130, 28));
        btnAddEx.addActionListener(e -> openAddExerciseDialog());
        exHeader.add(btnAddEx, BorderLayout.EAST);
        card.add(exHeader);
        card.add(Box.createVerticalStrut(8));

        // Table
        JScrollPane scrollEx = new JScrollPane(tblExercises);
        scrollEx.setPreferredSize(new Dimension(700, 130));
        scrollEx.setMaximumSize(new Dimension(32767, 130));
        scrollEx.setOpaque(false);
        scrollEx.getViewport().setOpaque(false);
        scrollEx.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true));
        scrollEx.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        card.add(scrollEx);

        return card;
    }

    private void setupExerciseTable() {
        tblExercises.setBackground(ThemeConfig.BG_DARK);
        tblExercises.setForeground(ThemeConfig.TEXT_PRIMARY);
        tblExercises.setRowHeight(28);
        tblExercises.setShowGrid(false);
        tblExercises.setIntercellSpacing(new Dimension(0, 0));
        tblExercises.getTableHeader().setBackground(ThemeConfig.BG_CARD);
        tblExercises.getTableHeader().setForeground(ThemeConfig.TEXT_SECONDARY);
        tblExercises.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 1; i < 4; i++) {
            tblExercises.getColumnModel().getColumn(i).setCellRenderer(center);
        }
    }

    private void loadWorkoutTemplate(String type) {
        plannedExercises.clear();
        exerciseTableModel.setRowCount(0);

        if ("Push".equalsIgnoreCase(type)) {
            txtWorkoutTitle.setText("Chest, Shoulders & Triceps Hypertrophy");
            cmbWorkoutType.setSelectedItem("Strength");
            txtWorkoutDuration.setText("55");
            txtWorkoutCalories.setText("440");
            addExerciseToPlan("Barbell Bench Press", 4, 8, 80.0);
            addExerciseToPlan("Incline Dumbbell Press", 3, 10, 26.0);
            addExerciseToPlan("Overhead Dumbbell Press", 3, 10, 18.0);
            addExerciseToPlan("Cable Tricep Pushdown", 3, 12, 25.0);
            txtCoachNotes.setText("Focus on controlled 3-second eccentrics on the bench press. Keep rest under 90s.");
        } else if ("Pull".equalsIgnoreCase(type)) {
            txtWorkoutTitle.setText("Heavy Pull - Deadlifts & Lats");
            cmbWorkoutType.setSelectedItem("Strength");
            txtWorkoutDuration.setText("60");
            txtWorkoutCalories.setText("480");
            addExerciseToPlan("Conventional Deadlift", 4, 5, 130.0);
            addExerciseToPlan("Lat Pulldown (Wide Grip)", 4, 10, 65.0);
            addExerciseToPlan("Barbell Bent-Over Row", 3, 8, 75.0);
            addExerciseToPlan("Incline Bicep Curls", 3, 12, 14.0);
            txtCoachNotes.setText("Lock out firmly on deadlifts. Drive through midfoot and protect lumbar spine.");
        } else if ("Legs".equalsIgnoreCase(type)) {
            txtWorkoutTitle.setText("Leg Power & Glute Hypertrophy");
            cmbWorkoutType.setSelectedItem("Strength");
            txtWorkoutDuration.setText("50");
            txtWorkoutCalories.setText("460");
            addExerciseToPlan("Barbell Back Squat", 4, 8, 100.0);
            addExerciseToPlan("Romanian Deadlift", 3, 10, 80.0);
            addExerciseToPlan("Bulgarian Split Squat", 3, 10, 16.0);
            addExerciseToPlan("Standing Calf Raises", 4, 15, 60.0);
            txtCoachNotes.setText("Hit parallel depth on every squat rep. Hydrate well prior to session.");
        } else if ("Cardio".equalsIgnoreCase(type)) {
            txtWorkoutTitle.setText("5K Tempo Interval Run");
            cmbWorkoutType.setSelectedItem("Cardio");
            txtWorkoutDuration.setText("35");
            txtWorkoutCalories.setText("320");
            addExerciseToPlan("Warm-Up Jog", 1, 1, 0.0);
            addExerciseToPlan("5x 400m Tempo Sprints", 5, 1, 0.0);
            addExerciseToPlan("Cool-Down Jog & Stretch", 1, 1, 0.0);
            txtCoachNotes.setText("Target pace 4:55/km on tempo sprints. Focus on rhythmic nasal breathing.");
        } else if ("HIIT".equalsIgnoreCase(type)) {
            txtWorkoutTitle.setText("Full-Body Functional HIIT Burner");
            cmbWorkoutType.setSelectedItem("HIIT");
            txtWorkoutDuration.setText("30");
            txtWorkoutCalories.setText("350");
            addExerciseToPlan("Kettlebell Swings", 4, 20, 20.0);
            addExerciseToPlan("Burpees", 4, 15, 0.0);
            addExerciseToPlan("Box Jumps", 4, 12, 0.0);
            addExerciseToPlan("Plank with Shoulder Taps", 3, 20, 0.0);
            txtCoachNotes.setText("30 seconds work, 15 seconds rest. Keep heart rate elevated.");
        }
        if (lblExCount != null) {
            lblExCount.setText("Assigned Exercise Routine (" + plannedExercises.size() + " exercises)");
        }
    }

    private void addExerciseToPlan(String name, int sets, int reps, double weight) {
        WorkoutExercise ex = new WorkoutExercise(name, sets, reps, weight);
        plannedExercises.add(ex);
        exerciseTableModel.addRow(new Object[]{name, sets, reps, String.format(java.util.Locale.US, "%.1f", weight)});
        if (lblExCount != null) {
            lblExCount.setText("Assigned Exercise Routine (" + plannedExercises.size() + " exercises)");
        }
    }

    private void openAddExerciseDialog() {
        ModernTextField txtExName = new ModernTextField("", "Exercise Name (e.g. Lat Pulldown)");
        ModernTextField txtSets = new ModernTextField("3", "Sets");
        ModernTextField txtReps = new ModernTextField("10", "Reps");
        ModernTextField txtWeight = new ModernTextField("40.0", "Weight (kg)");

        JPanel panel = new JPanel(new GridLayout(4, 2, 8, 8));
        panel.add(new JLabel("Exercise Name:"));
        panel.add(txtExName);
        panel.add(new JLabel("Sets:"));
        panel.add(txtSets);
        panel.add(new JLabel("Reps:"));
        panel.add(txtReps);
        panel.add(new JLabel("Weight (kg):"));
        panel.add(txtWeight);

        int res = JOptionPane.showConfirmDialog(this, panel, "Add Exercise to Routine", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION && !txtExName.getText().trim().isEmpty()) {
            try {
                int sets = Integer.parseInt(txtSets.getText().trim());
                int reps = Integer.parseInt(txtReps.getText().trim());
                double weight = Double.parseDouble(txtWeight.getText().trim());
                addExerciseToPlan(txtExName.getText().trim(), sets, reps, weight);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid numeric sets, reps, and weight.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // =========================================================================
    // 2. GOAL FORM
    // =========================================================================
    private JPanel createGoalForm() {
        ModernCard card = new ModernCard(12, new Insets(16, 20, 16, 20));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        // Quick Goal Template Chips
        JPanel pnlChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pnlChips.setOpaque(false);
        JLabel lblTpl = new JLabel("Quick Goal Presets:");
        lblTpl.setFont(ThemeConfig.FONT_SMALL);
        lblTpl.setForeground(ThemeConfig.TEXT_MUTED);
        pnlChips.add(lblTpl);

        pnlChips.add(createChip("Target Weight 60kg", () -> loadGoalTemplate("Weight", "Target Weight Milestone", 60.0, 63.5, "kg", 60)));
        pnlChips.add(createChip("16 Workouts / Month", () -> loadGoalTemplate("Workout Frequency", "16 Monthly Workouts", 16.0, 0.0, "workouts", 30)));
        pnlChips.add(createChip("Deadlift 160kg PR", () -> loadGoalTemplate("Strength", "Deadlift 160kg 1RM Target", 160.0, 140.0, "kg", 45)));
        pnlChips.add(createChip("Sub-25m 5K Run", () -> loadGoalTemplate("Endurance", "5K Sub-25 Minute Milestone", 25.0, 27.5, "mins", 45)));
        pnlChips.add(createChip("160g+ Daily Protein", () -> loadGoalTemplate("Nutrition", "Daily Protein Target 160g", 160.0, 120.0, "grams", 30)));

        card.add(pnlChips);
        card.add(Box.createVerticalStrut(14));

        // Row 1: Goal Title & Category
        JPanel r1 = new JPanel(new GridLayout(1, 2, 16, 0));
        r1.setOpaque(false);
        r1.add(createFieldWrapper("Goal Title *", txtGoalTitle));
        r1.add(createFieldWrapper("Category", cmbGoalCategory));
        styleComboBox(cmbGoalCategory);
        card.add(r1);
        card.add(Box.createVerticalStrut(12));

        // Row 2: Target Value, Current Value, Unit
        JPanel r2 = new JPanel(new GridLayout(1, 3, 14, 0));
        r2.setOpaque(false);
        r2.add(createFieldWrapper("Target Value *", txtGoalTargetVal));
        r2.add(createFieldWrapper("Starting / Current Value", txtGoalCurrentVal));
        r2.add(createFieldWrapper("Unit (kg, reps, etc.)", txtGoalUnit));
        card.add(r2);
        card.add(Box.createVerticalStrut(12));

        // Row 3: Target Completion Date
        JPanel r3 = new JPanel(new GridLayout(1, 2, 16, 0));
        r3.setOpaque(false);
        r3.add(createFieldWrapper("Target Completion Deadline (YYYY-MM-DD) *", txtGoalTargetDate));
        card.add(r3);

        return card;
    }

    private void loadGoalTemplate(String category, String title, double target, double current, String unit, int days) {
        cmbGoalCategory.setSelectedItem(category);
        txtGoalTitle.setText(title);
        txtGoalTargetVal.setText(String.format(java.util.Locale.US, "%.1f", target));
        txtGoalCurrentVal.setText(String.format(java.util.Locale.US, "%.1f", current));
        txtGoalUnit.setText(unit);
        txtGoalTargetDate.setText(LocalDate.now().plusDays(days).toString());
        txtCoachNotes.setText("Track your check-ins consistently. We will review this milestone every Sunday!");
    }

    // =========================================================================
    // 3. SCHEDULE / TASK FORM
    // =========================================================================
    private JPanel createScheduleForm() {
        ModernCard card = new ModernCard(12, new Insets(16, 20, 16, 20));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        // Quick Task Template Chips
        JPanel pnlChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pnlChips.setOpaque(false);
        JLabel lblTpl = new JLabel("Task Presets:");
        lblTpl.setFont(ThemeConfig.FONT_SMALL);
        lblTpl.setForeground(ThemeConfig.TEXT_MUTED);
        pnlChips.add(lblTpl);

        pnlChips.add(createChip("Sunday Weigh-In & Measurements", () -> {
            txtScheduleTitle.setText("Sunday Body Composition & Weigh-In");
            txtScheduleTime.setText("08:30");
            txtCoachNotes.setText("Step on the scale fasting before breakfast. Record weight, waist, and chest measurements.");
        }));
        pnlChips.add(createChip("Pre-Workout Nutrition Check", () -> {
            txtScheduleTitle.setText("Pre-Workout Fuel & Carbs Check");
            txtScheduleTime.setText("16:00");
            txtCoachNotes.setText("Have 40g complex carbs + 20g protein 90 mins prior to training.");
        }));
        pnlChips.add(createChip("3-Liter Daily Hydration Goal", () -> {
            txtScheduleTitle.setText("Hydration Check: Hit 3,000ml Water");
            txtScheduleTime.setText("14:00");
            txtCoachNotes.setText("Ensure you have consumed at least 2 liters of water by mid-afternoon.");
        }));

        card.add(pnlChips);
        card.add(Box.createVerticalStrut(14));

        // Row 1: Task Title
        card.add(createFieldWrapper("Schedule Task / Reminder Title *", txtScheduleTitle));
        card.add(Box.createVerticalStrut(12));

        // Row 2: Date & Time
        JPanel r2 = new JPanel(new GridLayout(1, 2, 16, 0));
        r2.setOpaque(false);
        r2.add(createFieldWrapper("Scheduled Date (YYYY-MM-DD) *", txtScheduleDate));
        r2.add(createFieldWrapper("Scheduled Time (HH:mm)", txtScheduleTime));
        card.add(r2);

        return card;
    }

    private JPanel createCoachNotesSection() {
        ModernCard card = new ModernCard(12, new Insets(14, 20, 14, 20));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel lblNotes = new JLabel("Trainer Coaching Notes & Direct Instructions (Sent in Notification)");
        lblNotes.setFont(ThemeConfig.FONT_SMALL);
        lblNotes.setForeground(ThemeConfig.TEXT_SECONDARY);
        card.add(lblNotes);
        card.add(Box.createVerticalStrut(6));

        txtCoachNotes.setBackground(ThemeConfig.BG_DARK);
        txtCoachNotes.setForeground(ThemeConfig.TEXT_PRIMARY);
        txtCoachNotes.setCaretColor(ThemeConfig.TEXT_PRIMARY);
        txtCoachNotes.setFont(ThemeConfig.FONT_BODY);
        txtCoachNotes.setLineWrap(true);
        txtCoachNotes.setWrapStyleWord(true);
        txtCoachNotes.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JScrollPane scrollNotes = new JScrollPane(txtCoachNotes);
        scrollNotes.setPreferredSize(new Dimension(700, 75));
        scrollNotes.setMaximumSize(new Dimension(32767, 75));
        scrollNotes.setBorder(null);
        scrollNotes.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        card.add(scrollNotes);

        return card;
    }

    private JPanel createFieldWrapper(String label, JComponent comp) {
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

    private JButton createChip(String text, Runnable action) {
        JButton btn = new JButton("+ " + text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btn.setForeground(ThemeConfig.PRIMARY_ACCENT);
        btn.setBackground(ThemeConfig.BG_DARK);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.PRIMARY_ACCENT, 1, true),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private void styleComboBox(JComboBox<?> box) {
        box.setBackground(ThemeConfig.BG_DARK);
        box.setForeground(ThemeConfig.TEXT_PRIMARY);
        box.setFont(ThemeConfig.FONT_BODY);
        box.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true));
        box.setPreferredSize(new Dimension(box.getPreferredSize().width, 38));
    }

    // =========================================================================
    // EXECUTION & DISPATCH
    // =========================================================================
    private void executeDispatch() {
        UserSession currentSession = SupabaseClient.getInstance().getCurrentSession();
        String trainerId = currentSession != null ? currentSession.getUserId() : "trainer-coach-99";
        String trainerName = currentSession != null && currentSession.getProfile() != null
                ? currentSession.getProfile().getDisplayName() : "Coach David Miller";

        String studentId = studentProfile != null ? studentProfile.getId() : null;
        if (studentId == null || studentId.isBlank()) {
            JOptionPane.showMessageDialog(this, "Target student profile is invalid.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String notes = txtCoachNotes.getText().trim();

        btnDispatch.setEnabled(false);
        btnDispatch.setText("Dispatching to " + (studentProfile != null ? studentProfile.getDisplayName() : "Student") + "...");

        SwingWorker<TrainerAssignment, Void> worker = new SwingWorker<>() {
            @Override
            protected TrainerAssignment doInBackground() throws Exception {
                switch (currentMode) {
                    case WORKOUT -> {
                        String title = txtWorkoutTitle.getText().trim();
                        if (title.isEmpty()) throw new IllegalArgumentException("Workout title cannot be empty.");
                        String type = (String) cmbWorkoutType.getSelectedItem();
                        LocalDate date = LocalDate.parse(txtWorkoutDate.getText().trim());
                        int dur = Integer.parseInt(txtWorkoutDuration.getText().trim());
                        int cal = Integer.parseInt(txtWorkoutCalories.getText().trim());

                        Workout w = new Workout(title, type, date, dur, cal);
                        w.setExercises(new ArrayList<>(plannedExercises));
                        return assignmentDAO.assignWorkoutToStudent(trainerId, trainerName, studentId, w, notes);
                    }
                    case GOAL -> {
                        String title = txtGoalTitle.getText().trim();
                        if (title.isEmpty()) throw new IllegalArgumentException("Goal title cannot be empty.");
                        String cat = (String) cmbGoalCategory.getSelectedItem();
                        double target = Double.parseDouble(txtGoalTargetVal.getText().trim());
                        double current = Double.parseDouble(txtGoalCurrentVal.getText().trim());
                        String unit = txtGoalUnit.getText().trim();
                        LocalDate targetDate = LocalDate.parse(txtGoalTargetDate.getText().trim());

                        Goal g = new Goal(title, cat, target, current, unit, targetDate);
                        return assignmentDAO.assignGoalToStudent(trainerId, trainerName, studentId, g, notes);
                    }
                    case SCHEDULE -> {
                        String title = txtScheduleTitle.getText().trim();
                        if (title.isEmpty()) throw new IllegalArgumentException("Task title cannot be empty.");
                        LocalDate date = LocalDate.parse(txtScheduleDate.getText().trim());
                        LocalTime time = LocalTime.parse(txtScheduleTime.getText().trim());

                        return assignmentDAO.assignScheduleToStudent(trainerId, trainerName, studentId, title, notes, date, time);
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    TrainerAssignment created = get();
                    if (created != null) {
                        Window parentWindow = getOwner();
                        if (parentWindow != null) {
                            ToastNotification.showToast(parentWindow,
                                    "Successfully assigned " + created.getAssignmentType().toLowerCase() + " to " +
                                            (studentProfile != null ? studentProfile.getDisplayName() : "student") + "!",
                                    ToastNotification.ToastType.SUCCESS);
                        }
                        if (onSuccessCallback != null) {
                            onSuccessCallback.run();
                        }
                        dispose();
                    }
                } catch (Exception ex) {
                    btnDispatch.setEnabled(true);
                    btnDispatch.setText("Dispatch Assignment");
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    JOptionPane.showMessageDialog(AssignTaskDialog.this,
                            "Failed to dispatch assignment:\n" + msg, "Dispatch Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "ST";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
