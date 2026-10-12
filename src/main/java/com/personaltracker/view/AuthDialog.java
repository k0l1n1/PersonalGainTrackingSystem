package com.personaltracker.view;

import com.personaltracker.components.ModernButton;
import com.personaltracker.components.ModernPasswordField;
import com.personaltracker.components.ModernTextField;
import com.personaltracker.config.ThemeConfig;
import com.personaltracker.model.UserSession;
import com.personaltracker.service.SupabaseAuthService;
import com.personaltracker.util.IconFactory;
import com.personaltracker.util.ValidationUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Authentication Dialog supporting Login, Registration, and Password Reset.
 */
public class AuthDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final SupabaseAuthService authService = SupabaseAuthService.getInstance();
    private boolean authenticated = false;

    private final ModernTextField txtEmail = new ModernTextField("alex@example.com");
    private final ModernPasswordField txtPassword = new ModernPasswordField("••••••••");
    private final ModernTextField txtFullName = new ModernTextField("Alex Hunter");

    private final JLabel lblError = new JLabel("");
    private final JLabel lblStatus = new JLabel("");
    private final ModernButton btnPrimary = new ModernButton("Sign In", ModernButton.ButtonStyle.PRIMARY);
    private final ModernButton btnDemo = new ModernButton("Continue in Demo Mode", ModernButton.ButtonStyle.SECONDARY);

    private boolean isRegisterMode = false;
    private boolean isResetMode = false;

    private final JLabel lblSwitchMode = new JLabel("Don't have an account? Sign Up");
    private final JLabel lblForgotPwd = new JLabel("Forgot password?");

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
        JLabel iconLbl = new JLabel(IconFactory.createIcon(IconFactory.IconType.WORKOUT, 32, ThemeConfig.PRIMARY_BLUE));
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
        lblError.setForeground(ThemeConfig.DANGER_ROSE);
        lblError.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblError.setVisible(false);
        mainPanel.add(lblError);

        lblStatus.setFont(ThemeConfig.FONT_SMALL);
        lblStatus.setForeground(ThemeConfig.SUCCESS_EMERALD);
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblStatus.setVisible(false);
        mainPanel.add(lblStatus);

        mainPanel.add(Box.createVerticalStrut(8));

        // Form Fields
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setOpaque(false);

        // Full Name (only in Register mode)
        JLabel lblNameTag = new JLabel("Full Name");
        lblNameTag.setFont(ThemeConfig.FONT_SMALL);
        lblNameTag.setForeground(ThemeConfig.TEXT_SECONDARY);
        lblNameTag.setVisible(false);
        txtFullName.setVisible(false);

        JLabel lblEmailTag = new JLabel("Email Address");
        lblEmailTag.setFont(ThemeConfig.FONT_SMALL);
        lblEmailTag.setForeground(ThemeConfig.TEXT_SECONDARY);

        JLabel lblPassTag = new JLabel("Password");
        lblPassTag.setFont(ThemeConfig.FONT_SMALL);
        lblPassTag.setForeground(ThemeConfig.TEXT_SECONDARY);

        formPanel.add(lblNameTag);
        formPanel.add(Box.createVerticalStrut(4));
        formPanel.add(txtFullName);
        formPanel.add(Box.createVerticalStrut(10));

        formPanel.add(lblEmailTag);
        formPanel.add(Box.createVerticalStrut(4));
        formPanel.add(txtEmail);
        formPanel.add(Box.createVerticalStrut(10));

        formPanel.add(lblPassTag);
        formPanel.add(Box.createVerticalStrut(4));
        formPanel.add(txtPassword);

        mainPanel.add(formPanel);

        // Forgot Password link
        lblForgotPwd.setFont(ThemeConfig.FONT_SMALL);
        lblForgotPwd.setForeground(ThemeConfig.PRIMARY_BLUE);
        lblForgotPwd.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblForgotPwd.setAlignmentX(Component.RIGHT_ALIGNMENT);
        lblForgotPwd.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                switchToResetMode();
            }
        });
        mainPanel.add(Box.createVerticalStrut(6));
        mainPanel.add(lblForgotPwd);

        mainPanel.add(Box.createVerticalStrut(16));

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
    }

    private void switchToLoginMode() {
        isRegisterMode = false;
        isResetMode = false;
        setTitle("Sign In - Personal Gain Tracking System");
        txtFullName.setVisible(false);
        txtPassword.setVisible(true);
        lblForgotPwd.setVisible(true);
        btnPrimary.setText("Sign In");
        lblSwitchMode.setText("Don't have an account? Sign Up");
        clearMessages();
        revalidate();
        repaint();
    }

    private void switchToRegisterMode() {
        isRegisterMode = true;
        isResetMode = false;
        setTitle("Sign Up - Personal Gain Tracking System");
        txtFullName.setVisible(true);
        txtPassword.setVisible(true);
        lblForgotPwd.setVisible(false);
        btnPrimary.setText("Create Account");
        lblSwitchMode.setText("Already have an account? Sign In");
        clearMessages();
        revalidate();
        repaint();
    }

    private void switchToResetMode() {
        isResetMode = true;
        isRegisterMode = false;
        setTitle("Reset Password - Personal Gain Tracking System");
        txtFullName.setVisible(false);
        txtPassword.setVisible(false);
        lblForgotPwd.setVisible(false);
        btnPrimary.setText("Send Reset Link");
        lblSwitchMode.setText("Back to Sign In");
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
                    return authService.register(email, password, fullName);
                }

                @Override
                protected void done() {
                    btnPrimary.setEnabled(true);
                    btnPrimary.setText("Create Account");
                    try {
                        UserSession session = get();
                        if (session != null) {
                            authenticated = true;
                            dispose();
                        }
                    } catch (Exception ex) {
                        showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
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
