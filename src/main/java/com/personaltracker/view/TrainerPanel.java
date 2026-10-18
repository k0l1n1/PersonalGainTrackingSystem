package com.personaltracker.view;

import com.personaltracker.components.ModernButton;
import com.personaltracker.components.ModernCard;
import com.personaltracker.components.ModernProgressBar;
import com.personaltracker.components.ModernTextField;
import com.personaltracker.components.ToastNotification;
import com.personaltracker.config.ThemeConfig;
import com.personaltracker.dao.TrainerStudentDAO;
import com.personaltracker.model.*;
import com.personaltracker.service.SupabaseClient;
import com.personaltracker.util.DateUtil;
import com.personaltracker.util.HealthCalculator;
import com.personaltracker.util.IconFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dedicated Trainer Portal View.
 * Enables personal trainers to manage their student roster, enroll new clients
 * via unique student ID codes, and inspect each student's workout, nutrition,
 * and goal progress in real time.
 */
public class TrainerPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final TrainerStudentDAO dao = new TrainerStudentDAO();
    private final JPanel rosterContainer = new JPanel();

    // Summary Metric Labels
    private final JLabel lblTotalStudents = new JLabel("0");
    private final JLabel lblTotalWorkouts = new JLabel("0");
    private final JLabel lblActiveGoals = new JLabel("0");

    private List<TrainerStudent> currentStudents = new ArrayList<>();

    public TrainerPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(20, 28, 28, 28));

        // 1. Header with Enroll Action
        root.add(createHeaderPanel());
        root.add(Box.createVerticalStrut(18));

        // 2. Summary KPI Strip
        root.add(createSummaryKpiStrip());
        root.add(Box.createVerticalStrut(22));

        // 3. Roster Section Header
        JPanel rosterHeader = new JPanel(new BorderLayout());
        rosterHeader.setOpaque(false);
        JLabel lblRoster = new JLabel("Enrolled Athletes & Clients");
        lblRoster.setFont(ThemeConfig.FONT_TITLE);
        lblRoster.setForeground(ThemeConfig.TEXT_PRIMARY);
        rosterHeader.add(lblRoster, BorderLayout.WEST);

        JLabel lblRosterSub = new JLabel("Click 'View Full Progress' to inspect workouts, weight journey & meal logs.");
        lblRosterSub.setFont(ThemeConfig.FONT_SMALL);
        lblRosterSub.setForeground(ThemeConfig.TEXT_MUTED);
        rosterHeader.add(lblRosterSub, BorderLayout.EAST);
        root.add(rosterHeader);
        root.add(Box.createVerticalStrut(12));

        // 4. Roster Container
        rosterContainer.setLayout(new BoxLayout(rosterContainer, BoxLayout.Y_AXIS));
        rosterContainer.setOpaque(false);
        root.add(rosterContainer);

        JScrollPane scrollPane = new JScrollPane(root);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        ModernCard card = new ModernCard(16, new Insets(16, 22, 16, 22));
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 4));
        left.setOpaque(false);

        JLabel title = new JLabel("TRAINER MANAGEMENT PORTAL");
        title.setFont(ThemeConfig.FONT_TITLE_LARGE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        left.add(title);

        JLabel sub = new JLabel("Supervise your athletes, enroll students by unique code, and guide their fitness journeys.");
        sub.setFont(ThemeConfig.FONT_SMALL);
        sub.setForeground(ThemeConfig.TEXT_MUTED);
        left.add(sub);
        header.add(left, BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        btnBar.setOpaque(false);

        ModernButton btnRefresh = new ModernButton("🔄 Refresh", ModernButton.ButtonStyle.SECONDARY);
        btnRefresh.setPreferredSize(new Dimension(110, 38));
        btnRefresh.addActionListener(e -> loadData());
        btnBar.add(btnRefresh);

        ModernButton btnEnroll = new ModernButton("➕ Enroll Student by Code", ModernButton.ButtonStyle.PRIMARY);
        btnEnroll.setPreferredSize(new Dimension(210, 38));
        btnEnroll.addActionListener(e -> openEnrollStudentDialog());
        btnBar.add(btnEnroll);

        header.add(btnBar, BorderLayout.EAST);
        card.add(header, BorderLayout.CENTER);
        return card;
    }

    private JPanel createSummaryKpiStrip() {
        JPanel strip = new JPanel(new GridLayout(1, 3, 16, 0));
        strip.setOpaque(false);

        strip.add(createKpiCard("ACTIVE STUDENTS", lblTotalStudents, "Athletes under coaching", ThemeConfig.PRIMARY_ACCENT));
        strip.add(createKpiCard("TOTAL WORKOUTS TRACKED", lblTotalWorkouts, "Logged by your roster", ThemeConfig.PRIMARY_BLUE));
        strip.add(createKpiCard("GOALS IN PROGRESS", lblActiveGoals, "Active fitness targets", ThemeConfig.SUCCESS_EMERALD));

        return strip;
    }

    private JPanel createKpiCard(String header, JLabel valueLbl, String sub, Color accent) {
        ModernCard card = new ModernCard(14, new Insets(14, 18, 14, 18));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel hLbl = new JLabel(header);
        hLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        hLbl.setForeground(ThemeConfig.TEXT_MUTED);
        card.add(hLbl);
        card.add(Box.createVerticalStrut(6));

        valueLbl.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valueLbl.setForeground(accent);
        card.add(valueLbl);
        card.add(Box.createVerticalStrut(4));

        JLabel sLbl = new JLabel(sub);
        sLbl.setFont(ThemeConfig.FONT_SMALL);
        sLbl.setForeground(ThemeConfig.TEXT_SECONDARY);
        card.add(sLbl);

        return card;
    }

    public void loadData() {
        UserSession session = SupabaseClient.getInstance().getCurrentSession();
        if (session == null) return;

        String trainerId = session.getUserId();

        SwingWorker<List<TrainerStudent>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<TrainerStudent> doInBackground() throws Exception {
                return dao.getEnrolledStudents(trainerId);
            }

            @Override
            protected void done() {
                try {
                    currentStudents = get();
                    renderRoster(currentStudents);
                } catch (Exception ex) {
                    renderRoster(new ArrayList<>());
                }
            }
        };
        worker.execute();
    }

    private void renderRoster(List<TrainerStudent> students) {
        rosterContainer.removeAll();

        int totalWorkouts = 0;
        int totalGoals = 0;

        for (TrainerStudent ts : students) {
            totalWorkouts += ts.getTotalWorkouts();
            totalGoals += ts.getActiveGoals();
        }

        lblTotalStudents.setText(String.valueOf(students.size()));
        lblTotalWorkouts.setText(String.valueOf(totalWorkouts));
        lblActiveGoals.setText(String.valueOf(totalGoals));

        if (students.isEmpty()) {
            ModernCard emptyCard = new ModernCard(16, new Insets(32, 28, 32, 28));
            emptyCard.setLayout(new BoxLayout(emptyCard, BoxLayout.Y_AXIS));

            JLabel icon = new JLabel(IconFactory.createIcon(IconFactory.IconType.STUDENTS, 44, ThemeConfig.TEXT_MUTED));
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyCard.add(icon);
            emptyCard.add(Box.createVerticalStrut(12));

            JLabel title = new JLabel("No Students Enrolled Yet");
            title.setFont(ThemeConfig.FONT_TITLE);
            title.setForeground(ThemeConfig.TEXT_PRIMARY);
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyCard.add(title);
            emptyCard.add(Box.createVerticalStrut(6));

            JLabel desc = new JLabel("Ask your student for their unique ID code (e.g., PG-XXXXXX, found on their Profile & Settings) to add them to your roster.");
            desc.setFont(ThemeConfig.FONT_SMALL);
            desc.setForeground(ThemeConfig.TEXT_MUTED);
            desc.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyCard.add(desc);
            emptyCard.add(Box.createVerticalStrut(10));

            JLabel hint = new JLabel("💡 Quick Demo: You can enroll pre-seeded students using code PG-SARAH7 or PG-MARCUS.");
            hint.setFont(ThemeConfig.FONT_SMALL);
            hint.setForeground(ThemeConfig.PRIMARY_ACCENT);
            hint.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyCard.add(hint);
            emptyCard.add(Box.createVerticalStrut(16));

            ModernButton btnAddNow = new ModernButton("➕ Enroll Student Now", ModernButton.ButtonStyle.PRIMARY);
            btnAddNow.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnAddNow.setPreferredSize(new Dimension(180, 36));
            btnAddNow.addActionListener(e -> openEnrollStudentDialog());
            emptyCard.add(btnAddNow);

            rosterContainer.add(emptyCard);
        } else {
            for (TrainerStudent ts : students) {
                rosterContainer.add(createStudentCard(ts));
                rosterContainer.add(Box.createVerticalStrut(14));
            }
        }

        rosterContainer.revalidate();
        rosterContainer.repaint();
    }

    private JPanel createStudentCard(TrainerStudent ts) {
        ModernCard card = new ModernCard(14, new Insets(16, 20, 16, 20));
        card.setLayout(new BorderLayout(18, 0));

        UserProfile p = ts.getStudentProfile();
        String name = p != null ? p.getDisplayName() : "Student";
        String email = p != null && p.getEmail() != null ? p.getEmail() : "No email";
        String code = p != null && p.getUserCode() != null ? p.getUserCode() : "PG-UNKNOWN";

        // Left: Avatar with initials
        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(36, 36, 36));
                g2.fill(new Ellipse2D.Float(0, 0, 52, 52));
                g2.setColor(ThemeConfig.PRIMARY_ACCENT);
                g2.setStroke(new BasicStroke(2.2f));
                g2.draw(new Ellipse2D.Float(1, 1, 50, 50));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatar.setPreferredSize(new Dimension(52, 52));
        avatar.setOpaque(false);
        avatar.setLayout(new BorderLayout());
        JLabel lblInitials = new JLabel(getInitials(name), SwingConstants.CENTER);
        lblInitials.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblInitials.setForeground(Color.WHITE);
        avatar.add(lblInitials, BorderLayout.CENTER);
        card.add(avatar, BorderLayout.WEST);

        // Center: Info and metrics
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        // Name and Code row
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        topRow.setOpaque(false);

        JLabel lblName = new JLabel(name);
        lblName.setFont(ThemeConfig.FONT_TITLE);
        lblName.setForeground(ThemeConfig.TEXT_PRIMARY);
        topRow.add(lblName);

        JLabel lblCode = new JLabel(" " + code + " ");
        lblCode.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblCode.setForeground(ThemeConfig.PRIMARY_ACCENT);
        lblCode.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.PRIMARY_ACCENT, 1, true),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        topRow.add(lblCode);

        JLabel lblEnrolled = new JLabel("• Enrolled: " + DateUtil.formatDate(ts.getEnrolledDate()));
        lblEnrolled.setFont(ThemeConfig.FONT_SMALL);
        lblEnrolled.setForeground(ThemeConfig.TEXT_MUTED);
        topRow.add(lblEnrolled);

        center.add(topRow);
        center.add(Box.createVerticalStrut(4));

        JLabel lblEmail = new JLabel(email);
        lblEmail.setFont(ThemeConfig.FONT_SMALL);
        lblEmail.setForeground(ThemeConfig.TEXT_SECONDARY);
        center.add(lblEmail);
        center.add(Box.createVerticalStrut(10));

        // Vitals Grid
        JPanel vitals = new JPanel(new GridLayout(1, 4, 10, 0));
        vitals.setOpaque(false);

        double curW = ts.getLatestWeightKg() != null ? ts.getLatestWeightKg() : (p != null && p.getTargetWeightKg() != null ? p.getTargetWeightKg() : 70.0);
        double tgtW = p != null && p.getTargetWeightKg() != null ? p.getTargetWeightKg() : 70.0;
        double h = p != null && p.getHeightCm() != null ? p.getHeightCm() : 175.0;

        vitals.add(createMiniTile("CURRENT WEIGHT", String.format(Locale.US, "%.1f kg", curW), ThemeConfig.PRIMARY_ACCENT));
        vitals.add(createMiniTile("TARGET GOAL", String.format(Locale.US, "%.1f kg", tgtW), ThemeConfig.SUCCESS_EMERALD));

        double bmi = HealthCalculator.calculateBmi(curW, h);
        vitals.add(createMiniTile("BMI", String.format(Locale.US, "%.1f", bmi), ThemeConfig.PRIMARY_BLUE));
        vitals.add(createMiniTile("WORKOUTS", ts.getTotalWorkouts() + " logged", ThemeConfig.TEXT_PRIMARY));

        center.add(vitals);
        card.add(center, BorderLayout.CENTER);

        // Right: Action buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 16));
        actions.setOpaque(false);

        ModernButton btnView = new ModernButton("📊 View Full Progress", ModernButton.ButtonStyle.PRIMARY);
        btnView.setPreferredSize(new Dimension(170, 38));
        btnView.addActionListener(e -> openStudentProgressInspector(ts.getStudentId()));
        actions.add(btnView);

        JButton btnRemove = new JButton("✕");
        btnRemove.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnRemove.setForeground(ThemeConfig.ERROR);
        btnRemove.setBackground(ThemeConfig.BG_DARK);
        btnRemove.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.ERROR, 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        btnRemove.setFocusPainted(false);
        btnRemove.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRemove.setToolTipText("Remove from roster");
        btnRemove.addActionListener(e -> confirmRemoveStudent(ts));
        actions.add(btnRemove);

        card.add(actions, BorderLayout.EAST);
        return card;
    }

    private JPanel createMiniTile(String header, String val, Color color) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 1));
        p.setBackground(ThemeConfig.BG_DARK);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        JLabel h = new JLabel(header);
        h.setFont(new Font("Segoe UI", Font.BOLD, 8));
        h.setForeground(ThemeConfig.TEXT_MUTED);
        p.add(h);

        JLabel v = new JLabel(val);
        v.setFont(new Font("Segoe UI", Font.BOLD, 12));
        v.setForeground(color);
        p.add(v);
        return p;
    }

    private void openEnrollStudentDialog() {
        UserSession session = SupabaseClient.getInstance().getCurrentSession();
        if (session == null) return;

        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Enroll Student by Unique Code", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(440, 290);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.getContentPane().setBackground(ThemeConfig.BG_DARK);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("Enroll New Student");
        title.setFont(ThemeConfig.FONT_TITLE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        p.add(title);
        p.add(Box.createVerticalStrut(4));

        JLabel sub = new JLabel("Enter the unique ID code from your student's Profile & Settings view.");
        sub.setFont(ThemeConfig.FONT_SMALL);
        sub.setForeground(ThemeConfig.TEXT_MUTED);
        p.add(sub);
        p.add(Box.createVerticalStrut(14));

        ModernTextField txtCode = new ModernTextField("PG-SARAH7");
        p.add(txtCode);
        p.add(Box.createVerticalStrut(8));

        JLabel hint = new JLabel("Format: PG-XXXXXX (case-insensitive). Demo: PG-SARAH7 or PG-MARCUS");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        hint.setForeground(ThemeConfig.TEXT_MUTED);
        p.add(hint);
        p.add(Box.createVerticalStrut(20));

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnBar.setOpaque(false);

        ModernButton btnCancel = new ModernButton("Cancel", ModernButton.ButtonStyle.SECONDARY);
        btnCancel.addActionListener(e -> dlg.dispose());
        btnBar.add(btnCancel);

        ModernButton btnAdd = new ModernButton("Enroll Student 🚀", ModernButton.ButtonStyle.PRIMARY);
        btnAdd.addActionListener(e -> {
            String code = txtCode.getText().trim();
            if (code.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Please enter a valid student code.");
                return;
            }

            btnAdd.setEnabled(false);
            btnAdd.setText("Enrolling...");

            SwingWorker<TrainerStudent, Void> worker = new SwingWorker<>() {
                @Override
                protected TrainerStudent doInBackground() throws Exception {
                    return dao.enrollStudentByCode(session.getUserId(), code);
                }

                @Override
                protected void done() {
                    btnAdd.setEnabled(true);
                    btnAdd.setText("Enroll Student 🚀");
                    try {
                        TrainerStudent ts = get();
                        dlg.dispose();
                        ToastNotification.showToast(TrainerPanel.this, "Successfully enrolled " + (ts.getStudentProfile() != null ? ts.getStudentProfile().getDisplayName() : "student") + "! 🎉", ToastNotification.ToastType.SUCCESS);
                        loadData();
                    } catch (Exception ex) {
                        String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                        JOptionPane.showMessageDialog(dlg, msg, "Enrollment Failed", JOptionPane.WARNING_MESSAGE);
                    }
                }
            };
            worker.execute();
        });
        btnBar.add(btnAdd);

        p.add(btnBar);
        dlg.setContentPane(p);
        dlg.setVisible(true);
    }

    private void openStudentProgressInspector(String studentId) {
        SwingWorker<StudentProgressDetail, Void> worker = new SwingWorker<>() {
            @Override
            protected StudentProgressDetail doInBackground() throws Exception {
                return dao.getStudentProgressDetail(studentId);
            }

            @Override
            protected void done() {
                try {
                    StudentProgressDetail detail = get();
                    StudentDetailDialog dlg = new StudentDetailDialog(SwingUtilities.getWindowAncestor(TrainerPanel.this), detail);
                    dlg.setVisible(true);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(TrainerPanel.this, "Failed to load student progress: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void confirmRemoveStudent(TrainerStudent ts) {
        UserSession session = SupabaseClient.getInstance().getCurrentSession();
        if (session == null) return;

        String name = ts.getStudentProfile() != null ? ts.getStudentProfile().getDisplayName() : "this student";
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to remove " + name + " from your student roster?",
                "Remove Student",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    dao.removeStudent(session.getUserId(), ts.getStudentId());
                    return null;
                }

                @Override
                protected void done() {
                    ToastNotification.showToast(TrainerPanel.this, "Removed " + name + " from roster.", ToastNotification.ToastType.INFO);
                    loadData();
                }
            };
            worker.execute();
        }
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "ST";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }
}
