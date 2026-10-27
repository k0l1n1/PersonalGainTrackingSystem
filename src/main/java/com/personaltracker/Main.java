package com.personaltracker;

import com.personaltracker.config.ThemeConfig;
import com.personaltracker.model.UserSession;
import com.personaltracker.service.SupabaseAuthService;
import com.personaltracker.service.SupabaseClient;
import com.personaltracker.view.AuthDialog;
import com.personaltracker.view.MainFrame;
import com.personaltracker.view.OnboardingDialog;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Personal Gain Tracking System - Desktop Application Entry Point.
 */
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // Run application strictly on the Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            try {
                // 1. Initialize modern dark navy theme with FlatLaf
                ThemeConfig.initializeTheme();

                // 2. Initialize client instance
                SupabaseClient.getInstance();

                // 2.5. Direct Login for Existing Users:
                // If user has already created an account and signed in previously,
                // directly restore their account session and launch the dashboard without
                // forcing them through the login or onboarding filling process again.
                UserSession savedSession = SupabaseAuthService.getInstance().restoreSession();
                if (savedSession != null && savedSession.isAuthenticated()) {
                    OnboardingDialog.showIfNewUser(null);
                    MainFrame mainFrame = new MainFrame();
                    mainFrame.setVisible(true);
                    return;
                }

                // 3. Show Authentication Dialog
                AuthDialog authDialog = new AuthDialog(null);
                authDialog.setVisible(true);

                if (authDialog.isAuthenticated()) {
                    // 3.5. Onboarding Form Filling Session (Only for new users)
                    OnboardingDialog.showIfNewUser(null);

                    // 4. Launch main desktop application
                    MainFrame mainFrame = new MainFrame();
                    mainFrame.setVisible(true);
                } else {
                    System.exit(0);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to start Personal Gain Tracker: " + e.getMessage(), e);
                JOptionPane.showMessageDialog(null,
                        "Failed to start Personal Gain Tracker:\n" + (e.getMessage() != null ? e.getMessage() : e.toString()),
                        "Startup Error",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
