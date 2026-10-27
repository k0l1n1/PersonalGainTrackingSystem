package com.personaltracker.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;

import com.personaltracker.components.ModernButton;
import com.personaltracker.components.ModernPasswordField;
import com.personaltracker.components.ModernTextField;
import com.personaltracker.config.AppConfig;
import com.personaltracker.config.ThemeConfig;
import com.personaltracker.model.UserSession;
import com.personaltracker.service.SupabaseAuthService;
import com.personaltracker.service.SupabaseClient;
import com.personaltracker.util.IconFactory;
import com.personaltracker.util.ValidationUtil;

/**
 * Authentication Dialog supporting Login, Registration, and Password Reset.
 */
public class AuthDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final SupabaseAuthService authService = SupabaseAuthService.getInstance();
    private boolean authenticated = false;

    private final ModernTextField txtEmail = new ModernTextField("", "e.g. name@example.com");
    private final ModernPasswordField txtPassword = new ModernPasswordField("", "Enter your password");
    private final ModernTextField txtFullName = new ModernTextField("", "e.g. Alex Hunter");

    private String selectedRole = com.personaltracker.model.UserProfile.ROLE_MEMBER;
    private final JLabel lblRoleTag = new JLabel("Account Type");
    private final JPanel pnlRoleSelector = new JPanel(new GridLayout(1, 2, 8, 0));
    private final JButton btnRoleMember = new JButton("Normal Member");
    private final JButton btnRoleTrainer = new JButton("Trainer / Coach");

    private final JLabel lblNameTag = new JLabel("Full Name");
    private final JLabel lblPassTag = new JLabel("Password");

    private final JPanel pnlRoleRow = new JPanel();
    private final JPanel pnlNameRow = new JPanel();
    private final JPanel pnlEmailRow = new JPanel();
    private final JPanel pnlPassRow = new JPanel();

    private final JLabel lblError = new JLabel("");
    private final JLabel lblStatus = new JLabel("");
    private final ModernButton btnPrimary = new ModernButton("Sign In", ModernButton.ButtonStyle.PRIMARY);
    private final ModernButton btnDemo = new ModernButton("Continue in Demo Mode", ModernButton.ButtonStyle.SECONDARY);

    private boolean isRegisterMode = false;
    private boolean isResetMode = false;

    private final JLabel lblSwitchMode = new JLabel("Don't have an account? Sign Up");
    private final JLabel lblForgotPwd = new JLabel("Forgot password?");
    private final JCheckBox chkRememberMe = new JCheckBox("Remember me", true);
    private final JPanel pnlOptions = new JPanel(new BorderLayout());

    public AuthDialog(Frame owner) {
        super(owner, "Sign In - Personal Gain Tracking System", true);
        setSize(420, 520);
        setLocationRelativeTo(owner);
        setResizable(false);
        getContentPane().setBackground(ThemeConfig.BG_DARK);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));
        mainPanel.setBackground(ThemeConfig.BG_DARK);

        // Header logo & title
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        headerPanel.setOpaque(false);
        JLabel iconLbl = new JLabel(IconFactory.createIcon(IconFactory.IconType.WORKOUT, 32, ThemeConfig.PRIMARY_ACCENT));
        headerPanel.add(iconLbl);
        JLabel brandLbl = new JLabel("PERSONAL GAIN");
        brandLbl.setFont(ThemeConfig.FONT_TITLE);
        brandLbl.setForeground(ThemeConfig.TEXT_PRIMARY);
        headerPanel.add(brandLbl);
        mainPanel.add(headerPanel);

        mainPanel.add(Box.createVerticalStrut(6));

        JLabel subTitle = new JLabel("Track Workouts, Goals, Nutrition & Health");
        subTitle.setFont(ThemeConfig.FONT_SMALL);
        subTitle.setForeground(ThemeConfig.TEXT_MUTED);
        subTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(subTitle);

        mainPanel.add(Box.createVerticalStrut(22));

        // Error / Status banner
        lblError.setFont(ThemeConfig.FONT_SMALL);
        lblError.setForeground(ThemeConfig.ERROR);
        lblError.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblError.setVisible(false);
        mainPanel.add(lblError);

        lblStatus.setFont(ThemeConfig.FONT_SMALL);
        lblStatus.setForeground(ThemeConfig.SUCCESS);
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblStatus.setVisible(false);
        mainPanel.add(lblStatus);

        mainPanel.add(Box.createVerticalStrut(8));

        // Form Fields Container
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setOpaque(false);

        // Account Role selection row (only in Register mode)
        pnlRoleRow.setLayout(new BoxLayout(pnlRoleRow, BoxLayout.Y_AXIS));
        pnlRoleRow.setOpaque(false);
        lblRoleTag.setFont(ThemeConfig.FONT_SMALL);
        lblRoleTag.setForeground(ThemeConfig.TEXT_SECONDARY);
        pnlRoleRow.add(lblRoleTag);
        pnlRoleRow.add(Box.createVerticalStrut(4));

        pnlRoleSelector.setOpaque(false);
        btnRoleMember.setFont(ThemeConfig.FONT_SMALL);
        btnRoleMember.setFocusPainted(false);
        btnRoleMember.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRoleMember.setIconTextGap(8);
        btnRoleMember.addActionListener(e -> {
            selectedRole = com.personaltracker.model.UserProfile.ROLE_MEMBER;
            updateRoleButtonStyles();
        });

        btnRoleTrainer.setFont(ThemeConfig.FONT_SMALL);
        btnRoleTrainer.setFocusPainted(false);
        btnRoleTrainer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRoleTrainer.setIconTextGap(8);
        btnRoleTrainer.addActionListener(e -> {
            selectedRole = com.personaltracker.model.UserProfile.ROLE_TRAINER;
            updateRoleButtonStyles();
        });

        pnlRoleSelector.add(btnRoleMember);
        pnlRoleSelector.add(btnRoleTrainer);
        updateRoleButtonStyles();
        pnlRoleRow.add(pnlRoleSelector);
        pnlRoleRow.add(Box.createVerticalStrut(10));
        pnlRoleRow.setVisible(false);
        formPanel.add(pnlRoleRow);

        // Full Name row (only in Register mode)
        pnlNameRow.setLayout(new BoxLayout(pnlNameRow, BoxLayout.Y_AXIS));
        pnlNameRow.setOpaque(false);
        lblNameTag.setFont(ThemeConfig.FONT_SMALL);
        lblNameTag.setForeground(ThemeConfig.TEXT_SECONDARY);
        pnlNameRow.add(lblNameTag);
        pnlNameRow.add(Box.createVerticalStrut(4));
        pnlNameRow.add(txtFullName);
        pnlNameRow.add(Box.createVerticalStrut(10));
        pnlNameRow.setVisible(false);
        formPanel.add(pnlNameRow);

        // Email row
        pnlEmailRow.setLayout(new BoxLayout(pnlEmailRow, BoxLayout.Y_AXIS));
        pnlEmailRow.setOpaque(false);
        JLabel lblEmailTag = new JLabel("Email Address");
        lblEmailTag.setFont(ThemeConfig.FONT_SMALL);
        lblEmailTag.setForeground(ThemeConfig.TEXT_SECONDARY);
        pnlEmailRow.add(lblEmailTag);
        pnlEmailRow.add(Box.createVerticalStrut(4));
        pnlEmailRow.add(txtEmail);
        pnlEmailRow.add(Box.createVerticalStrut(10));
        formPanel.add(pnlEmailRow);

        // Password row
        pnlPassRow.setLayout(new BoxLayout(pnlPassRow, BoxLayout.Y_AXIS));
        pnlPassRow.setOpaque(false);
        lblPassTag.setFont(ThemeConfig.FONT_SMALL);
        lblPassTag.setForeground(ThemeConfig.TEXT_SECONDARY);
        pnlPassRow.add(lblPassTag);
        pnlPassRow.add(Box.createVerticalStrut(4));
        pnlPassRow.add(txtPassword);
        formPanel.add(pnlPassRow);

        mainPanel.add(formPanel);

        // Options row: Remember Me on left, Forgot Password on right
        pnlOptions.setOpaque(false);
        pnlOptions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        chkRememberMe.setFont(ThemeConfig.FONT_SMALL);
        chkRememberMe.setForeground(ThemeConfig.TEXT_SECONDARY);
        chkRememberMe.setOpaque(false);
        chkRememberMe.setFocusPainted(false);
        chkRememberMe.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pnlOptions.add(chkRememberMe, BorderLayout.WEST);

        // Forgot Password link
        lblForgotPwd.setFont(ThemeConfig.FONT_SMALL);
        lblForgotPwd.setForeground(ThemeConfig.PRIMARY_ACCENT);
        lblForgotPwd.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblForgotPwd.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                switchToResetMode();
            }
        });
        pnlOptions.add(lblForgotPwd, BorderLayout.EAST);

        mainPanel.add(Box.createVerticalStrut(8));
        mainPanel.add(pnlOptions);

        mainPanel.add(Box.createVerticalStrut(14));

        // Primary action button
        btnPrimary.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnPrimary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnPrimary.addActionListener(e -> executePrimaryAction());
        mainPanel.add(btnPrimary);

        mainPanel.add(Box.createVerticalStrut(10));

        // Demo mode button
        btnDemo.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnDemo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnDemo.addActionListener(e -> {
            SupabaseClient.getInstance().enableDemoMode();
            UserSession demoSession = SupabaseClient.getInstance().getCurrentSession();
            if (demoSession != null) {
                demoSession.setNewRegistration(false);
                if (chkRememberMe.isSelected()) {
                    authService.saveSession(demoSession);
                }
            }
            authenticated = true;
            dispose();
        });
        mainPanel.add(btnDemo);

        mainPanel.add(Box.createVerticalStrut(16));

        // Switch mode link (Sign in <-> Sign up)
        lblSwitchMode.setFont(ThemeConfig.FONT_SMALL);
        lblSwitchMode.setForeground(ThemeConfig.TEXT_SECONDARY);
        lblSwitchMode.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblSwitchMode.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblSwitchMode.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (isResetMode) {
                    switchToLoginMode();
                } else if (isRegisterMode) {
                    switchToLoginMode();
                } else {
                    switchToRegisterMode();
                }
            }
        });
        mainPanel.add(lblSwitchMode);

        setContentPane(mainPanel);

        // Pressing Enter in text fields triggers primary action
        txtEmail.addActionListener(e -> executePrimaryAction());
        txtFullName.addActionListener(e -> executePrimaryAction());
        txtPassword.addActionListener(e -> executePrimaryAction());
        getRootPane().setDefaultButton(btnPrimary);

        // Escape key closes dialog
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void updateRoleButtonStyles() {
        boolean isMember = com.personaltracker.model.UserProfile.ROLE_MEMBER.equalsIgnoreCase(selectedRole);
        btnRoleMember.setIcon(IconFactory.createIcon(IconFactory.IconType.USER, 14, isMember ? Color.WHITE : ThemeConfig.TEXT_SECONDARY));
        btnRoleMember.setBackground(isMember ? ThemeConfig.PRIMARY_ACCENT : ThemeConfig.BG_CARD);
        btnRoleMember.setForeground(isMember ? Color.WHITE : ThemeConfig.TEXT_SECONDARY);
        btnRoleMember.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isMember ? ThemeConfig.PRIMARY_ACCENT : ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        btnRoleTrainer.setIcon(IconFactory.createIcon(IconFactory.IconType.STUDENTS, 14, !isMember ? Color.WHITE : ThemeConfig.TEXT_SECONDARY));
        btnRoleTrainer.setBackground(!isMember ? ThemeConfig.PRIMARY_ACCENT : ThemeConfig.BG_CARD);
        btnRoleTrainer.setForeground(!isMember ? Color.WHITE : ThemeConfig.TEXT_SECONDARY);
        btnRoleTrainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(!isMember ? ThemeConfig.PRIMARY_ACCENT : ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
    }

    private void switchToLoginMode() {
        isRegisterMode = false;
        isResetMode = false;
        setTitle("Sign In - Personal Gain Tracking System");
        pnlRoleRow.setVisible(false);
        pnlNameRow.setVisible(false);
        pnlPassRow.setVisible(true);
        pnlOptions.setVisible(true);
        chkRememberMe.setVisible(true);
        lblForgotPwd.setVisible(true);
        btnPrimary.setText("Sign In");
        lblSwitchMode.setText("Don't have an account? Sign Up");
        setSize(420, 520);
        setLocationRelativeTo(getOwner());
        clearMessages();
        revalidate();
        repaint();
    }

    private void switchToRegisterMode() {
        isRegisterMode = true;
        isResetMode = false;
        setTitle("Sign Up - Personal Gain Tracking System");
        pnlRoleRow.setVisible(true);
        pnlNameRow.setVisible(true);
        pnlPassRow.setVisible(true);
        pnlOptions.setVisible(true);
        chkRememberMe.setVisible(true);
        lblForgotPwd.setVisible(false);
        btnPrimary.setText("Create Account");
        lblSwitchMode.setText("Already have an account? Sign In");
        setSize(430, 620);
        setLocationRelativeTo(getOwner());
        clearMessages();
        revalidate();
        repaint();
    }

    private void switchToResetMode() {
        isResetMode = true;
        isRegisterMode = false;
        setTitle("Reset Password - Personal Gain Tracking System");
        pnlRoleRow.setVisible(false);
        pnlNameRow.setVisible(false);
        pnlPassRow.setVisible(false);
        pnlOptions.setVisible(false);
        btnPrimary.setText("Send Reset Link");
        lblSwitchMode.setText("Back to Sign In");
        setSize(420, 440);
        setLocationRelativeTo(getOwner());
        clearMessages();
        revalidate();
        repaint();
    }

    private void clearMessages() {
        lblError.setVisible(false);
        lblStatus.setVisible(false);
    }

    private void executePrimaryAction() {
        String email = txtEmail.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();
        String fullName = txtFullName.getText().trim();

        clearMessages();

        if (isResetMode) {
            if (!ValidationUtil.isValidEmail(email)) {
                showError("Please enter a valid email address.");
                return;
            }
            btnPrimary.setEnabled(false);
            btnPrimary.setText("Sending...");

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    authService.resetPassword(email);
                    return null;
                }

                @Override
                protected void done() {
                    btnPrimary.setEnabled(true);
                    btnPrimary.setText("Send Reset Link");
                    try {
                        get();
                        showStatus("Password reset email sent! Check your inbox.");
                    } catch (Exception ex) {
                        showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                    }
                }
            };
            worker.execute();

        } else if (isRegisterMode) {
            if (fullName.isEmpty()) {
                showError("Please enter your full name.");
                return;
            }
            if (!ValidationUtil.isValidEmail(email)) {
                showError("Please enter a valid email address.");
                return;
            }
            if (!ValidationUtil.isValidPassword(password)) {
                showError("Password must be at least 6 characters.");
                return;
            }

            btnPrimary.setEnabled(false);
            btnPrimary.setText("Registering...");

            SwingWorker<UserSession, Void> worker = new SwingWorker<>() {
                @Override
                protected UserSession doInBackground() throws Exception {
                    return authService.register(email, password, fullName, selectedRole);
                }

                @Override
                protected void done() {
                    btnPrimary.setEnabled(true);
                    btnPrimary.setText("Create Account");
                    try {
                        UserSession session = get();
                        if (session != null) {
                            if (chkRememberMe.isSelected()) {
                                authService.saveSession(session);
                            } else {
                                authService.clearSavedSession();
                            }
                            if (AppConfig.getInstance().isSupabaseConfigured()) {
                                AppConfig.getInstance().setMockMode(false);
                            }
                            authenticated = true;
                            dispose();
                        }
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        String errMsg = cause.getMessage() != null ? cause.getMessage() : "";
                        // If user has already created an account, directly log into their account!
                        if (SupabaseAuthService.isUserAlreadyExistsError(errMsg, "")) {
                            attemptDirectLogin(email, password);
                        } else {
                            showError(errMsg);
                        }
                    }
                }
            };
            worker.execute();

        } else {
            // Login
            if (!ValidationUtil.isValidEmail(email)) {
                showError("Please enter a valid email address.");
                return;
            }
            if (!ValidationUtil.isValidPassword(password)) {
                showError("Password must be at least 6 characters.");
                return;
            }

            btnPrimary.setEnabled(false);
            btnPrimary.setText("Signing In...");

            SwingWorker<UserSession, Void> worker = new SwingWorker<>() {
                @Override
                protected UserSession doInBackground() throws Exception {
                    return authService.login(email, password);
                }

                @Override
                protected void done() {
                    btnPrimary.setEnabled(true);
                    btnPrimary.setText("Sign In");
                    try {
                        UserSession session = get();
                        if (session != null) {
                            if (chkRememberMe.isSelected()) {
                                authService.saveSession(session);
                            } else {
                                authService.clearSavedSession();
                            }
                            if (AppConfig.getInstance().isSupabaseConfigured()) {
                                AppConfig.getInstance().setMockMode(false);
                            }
                            authenticated = true;
                            dispose();
                        }
                    } catch (Exception ex) {
                        showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                    }
                }
            };
            worker.execute();
        }
    }

    private void attemptDirectLogin(String email, String password) {
        btnPrimary.setEnabled(false);
        btnPrimary.setText("Signing into existing account...");

        SwingWorker<UserSession, Void> worker = new SwingWorker<>() {
            @Override
            protected UserSession doInBackground() throws Exception {
                UserSession session = authService.login(email, password);
                if (session != null) {
                    session.setNewRegistration(false);
                }
                return session;
            }

            @Override
            protected void done() {
                btnPrimary.setEnabled(true);
                btnPrimary.setText(isRegisterMode ? "Create Account" : "Sign In");
                try {
                    UserSession session = get();
                    if (session != null) {
                        if (chkRememberMe.isSelected()) {
                            authService.saveSession(session);
                        } else {
                            authService.clearSavedSession();
                        }
                        if (AppConfig.getInstance().isSupabaseConfigured()) {
                            AppConfig.getInstance().setMockMode(false);
                        }
                        authenticated = true;
                        dispose();
                    }
                } catch (Exception ex) {
                    showError("An account already exists for this email, but the password entered was incorrect.");
                }
            }
        };
        worker.execute();
    }

    private void showError(String msg) {
        lblError.setText("<html><center>" + msg + "</center></html>");
        lblError.setVisible(true);
    }

    private void showStatus(String msg) {
        lblStatus.setText("<html><center>" + msg + "</center></html>");
        lblStatus.setVisible(true);
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}
