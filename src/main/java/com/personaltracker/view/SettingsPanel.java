package com.personaltracker.view;

import com.personaltracker.components.ModernButton;
import com.personaltracker.components.ModernCard;
import com.personaltracker.components.ModernPasswordField;
import com.personaltracker.components.ModernTextField;
import com.personaltracker.components.ToastNotification;
import com.personaltracker.config.AppConfig;
import com.personaltracker.config.ThemeConfig;
import com.personaltracker.dao.ProfileDAO;
import com.personaltracker.model.UserProfile;
import com.personaltracker.model.UserSession;
import com.personaltracker.service.SupabaseAuthService;
import com.personaltracker.service.SupabaseClient;
import com.personaltracker.util.DateUtil;
import com.personaltracker.util.IconFactory;
import com.personaltracker.util.ValidationUtil;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

/**
 * User Profile and Supabase Connection Settings panel.
 */
public class SettingsPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final ProfileDAO profileDAO = new ProfileDAO();
    private final SupabaseAuthService authService = SupabaseAuthService.getInstance();
    private final AppConfig config = AppConfig.getInstance();

    // Profile form
    private final ModernTextField txtFullName = new ModernTextField();
    private final JComboBox<String> cmbGender = new JComboBox<>(new String[]{"Prefer not to say", "Male", "Female", "Non-Binary", "Other"});
    private final ModernTextField txtBirthDate = new ModernTextField();
    private final ModernTextField txtHeight = new ModernTextField();
    private final ModernTextField txtTargetWeight = new ModernTextField();
    private final ModernTextField txtCalorieTarget = new ModernTextField();
    private final ModernTextField txtWaterTarget = new ModernTextField();

    // Connection config form
    private final JLabel lblConnectionStatus = new JLabel();
    private final ModernTextField txtSupabaseUrl = new ModernTextField("https://your-project-id.supabase.co");
    private final ModernPasswordField txtAnonKey = new ModernPasswordField("eyJhbGciOiJIUzI1Ni...");

    private Runnable onProfileUpdatedCallback;

    public SettingsPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        titlePanel.setOpaque(false);
        JLabel title = new JLabel("Profile & System Settings");
        title.setFont(ThemeConfig.FONT_TITLE_LARGE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        titlePanel.add(title);

        JLabel subtitle = new JLabel("Manage personal fitness parameters and Supabase cloud connection");
        subtitle.setFont(ThemeConfig.FONT_BODY);
        subtitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        titlePanel.add(subtitle);
        header.add(titlePanel, BorderLayout.WEST);

        content.add(header);
        content.add(Box.createVerticalStrut(20));

        // Two Column Grid: Left Profile Form, Right Cloud Connection Settings
        JPanel grid = new JPanel(new GridLayout(1, 2, 20, 0));
        grid.setOpaque(false);

        // 1. User Profile Card
        ModernCard cardProfile = new ModernCard(14, new Insets(20, 22, 20, 22));
        JLabel pHead = new JLabel("Personal Fitness Profile");
        pHead.setFont(ThemeConfig.FONT_TITLE);
        pHead.setForeground(ThemeConfig.TEXT_PRIMARY);
        cardProfile.add(pHead, BorderLayout.NORTH);

        JPanel pForm = new JPanel();
        pForm.setLayout(new BoxLayout(pForm, BoxLayout.Y_AXIS));
        pForm.setOpaque(false);
        pForm.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        pForm.add(createFieldRow("Full Name", txtFullName));
        pForm.add(Box.createVerticalStrut(10));

        JPanel r2 = new JPanel(new GridLayout(1, 2, 10, 0));
        r2.setOpaque(false);
        r2.add(createFieldRow("Gender", cmbGender));
        r2.add(createFieldRow("Date of Birth (YYYY-MM-DD)", txtBirthDate));
        pForm.add(r2);
        pForm.add(Box.createVerticalStrut(10));

        JPanel r3 = new JPanel(new GridLayout(1, 2, 10, 0));
        r3.setOpaque(false);
        r3.add(createFieldRow("Height (cm)", txtHeight));
        r3.add(createFieldRow("Target Weight (kg)", txtTargetWeight));
        pForm.add(r3);
        pForm.add(Box.createVerticalStrut(10));

        JPanel r4 = new JPanel(new GridLayout(1, 2, 10, 0));
        r4.setOpaque(false);
        r4.add(createFieldRow("Daily Calorie Target (kcal)", txtCalorieTarget));
        r4.add(createFieldRow("Daily Water Target (ml)", txtWaterTarget));
        pForm.add(r4);
        pForm.add(Box.createVerticalStrut(20));

        ModernButton btnSaveProfile = new ModernButton("Save Profile Details", ModernButton.ButtonStyle.PRIMARY);
        btnSaveProfile.addActionListener(e -> saveProfileDetails());
        pForm.add(btnSaveProfile);

        cardProfile.add(pForm, BorderLayout.CENTER);
        grid.add(cardProfile);

        // 2. Supabase Backend Connection Card
        ModernCard cardBackend = new ModernCard(14, new Insets(20, 22, 20, 22));
        JLabel bHead = new JLabel("Supabase Cloud Backend");
        bHead.setFont(ThemeConfig.FONT_TITLE);
        bHead.setForeground(ThemeConfig.TEXT_PRIMARY);
        cardBackend.add(bHead, BorderLayout.NORTH);

        JPanel bForm = new JPanel();
        bForm.setLayout(new BoxLayout(bForm, BoxLayout.Y_AXIS));
        bForm.setOpaque(false);
        bForm.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        // Status badge
        lblConnectionStatus.setFont(ThemeConfig.FONT_BODY_BOLD);
        bForm.add(lblConnectionStatus);
        bForm.add(Box.createVerticalStrut(14));

        bForm.add(createFieldRow("Supabase Project URL (e.g. https://xyz.supabase.co)", txtSupabaseUrl));
        bForm.add(Box.createVerticalStrut(10));

        bForm.add(createFieldRow("Supabase Anon / Public API Key", txtAnonKey));
        bForm.add(Box.createVerticalStrut(14));

        ModernButton btnSaveConn = new ModernButton("Update & Apply Credentials", ModernButton.ButtonStyle.PRIMARY);
        btnSaveConn.addActionListener(e -> saveCredentials());
        bForm.add(btnSaveConn);
        bForm.add(Box.createVerticalStrut(20));

        // Password reset & Account action
        JLabel accTitle = new JLabel("Security & Password Reset");
        accTitle.setFont(ThemeConfig.FONT_SUBTITLE);
        accTitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        bForm.add(accTitle);
        bForm.add(Box.createVerticalStrut(8));

        ModernButton btnResetPwd = new ModernButton("Request Password Reset Email", ModernButton.ButtonStyle.SECONDARY);
        btnResetPwd.addActionListener(e -> sendPasswordReset());
        bForm.add(btnResetPwd);

        cardBackend.add(bForm, BorderLayout.CENTER);
        grid.add(cardBackend);

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

    public void loadSettings() {
        UserSession session = SupabaseClient.getInstance().getCurrentSession();
        if (session != null && session.getProfile() != null) {
            UserProfile p = session.getProfile();
            txtFullName.setText(p.getFullName());
            if (p.getGender() != null) cmbGender.setSelectedItem(p.getGender());
            txtBirthDate.setText(p.getDateOfBirth() != null ? p.getDateOfBirth().toString() : "");
            txtHeight.setText(p.getHeightCm() != null ? String.valueOf(p.getHeightCm()) : "");
            txtTargetWeight.setText(p.getTargetWeightKg() != null ? String.valueOf(p.getTargetWeightKg()) : "");
            txtCalorieTarget.setText(String.valueOf(p.getDailyCalorieTarget()));
            txtWaterTarget.setText(String.valueOf(p.getDailyWaterTargetMl()));
        }

        // Connection status
        txtSupabaseUrl.setText(config.getSupabaseUrl());
        txtAnonKey.setText(config.getSupabaseAnonKey());

        if (config.isMockMode()) {
            lblConnectionStatus.setText("● Local In-Memory Demo Mode (Offline)");
            lblConnectionStatus.setForeground(ThemeConfig.WARNING_AMBER);
        } else {
            lblConnectionStatus.setText("● Connected to Supabase PostgreSQL & Auth");
            lblConnectionStatus.setForeground(ThemeConfig.SUCCESS_EMERALD);
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

    private void saveProfileDetails() {
        UserSession session = SupabaseClient.getInstance().getCurrentSession();
        if (session == null) return;

        UserProfile p = session.getProfile();
        if (p == null) p = new UserProfile(session.getUserId(), session.getEmail(), "");

        p.setFullName(txtFullName.getText().trim());
        p.setGender((String) cmbGender.getSelectedItem());

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
                ToastNotification.showToast(SettingsPanel.this, "Profile updated successfully!", ToastNotification.ToastType.SUCCESS);
                if (onProfileUpdatedCallback != null) onProfileUpdatedCallback.run();
            }
        };
        worker.execute();
    }

    private void saveCredentials() {
        String url = txtSupabaseUrl.getText().trim();
        String anon = new String(txtAnonKey.getPassword()).trim();

        config.saveRuntimeCredentials(url, anon);
        loadSettings();
        ToastNotification.showToast(this, "Supabase connection updated!", ToastNotification.ToastType.INFO);
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
                    ToastNotification.showToast(SettingsPanel.this, "Password reset link sent to: " + session.getEmail(), ToastNotification.ToastType.SUCCESS);
                }
            };
            worker.execute();
        }
    }
}
