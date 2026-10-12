package com.personaltracker;

import com.personaltracker.config.ThemeConfig;
import com.personaltracker.service.SupabaseClient;
import com.personaltracker.view.AuthDialog;
import com.personaltracker.view.MainFrame;

import javax.swing.*;

/**
 * Personal Gain Tracking System - Desktop Application Entry Point.
 */
public class Main {

    public static void main(String[] args) {
        // Run application strictly on the Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            // 1. Initialize modern dark navy theme with FlatLaf
            ThemeConfig.initializeTheme();

            // 2. Initialize client instance
            SupabaseClient client = SupabaseClient.getInstance();

            // 3. Show Authentication Dialog
            AuthDialog authDialog = new AuthDialog(null);
            authDialog.setVisible(true);

            if (authDialog.isAuthenticated()) {
                // 4. Launch main desktop application
                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);
            } else {
                System.exit(0);
            }
        });
    }
}
