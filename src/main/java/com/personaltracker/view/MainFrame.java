package com.personaltracker.view;

import com.personaltracker.config.ThemeConfig;
import com.personaltracker.service.SupabaseAuthService;
import com.personaltracker.service.SupabaseClient;

import javax.swing.*;
import java.awt.*;

/**
 * Root desktop frame with fixed sidebar and dynamic CardLayout view switcher.
 */
public class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private final SidebarPanel sidebar;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentCards = new JPanel(cardLayout);

    // Feature Views
    private final DashboardPanel dashboardPanel = new DashboardPanel();
    private final WorkoutPanel workoutPanel = new WorkoutPanel();
    private final GoalPanel goalPanel = new GoalPanel();
    private final MeasurementPanel measurementPanel = new MeasurementPanel();
    private final NutritionPanel nutritionPanel = new NutritionPanel();
    private final WaterSleepPanel waterSleepPanel = new WaterSleepPanel();
    private final AnalyticsPanel analyticsPanel = new AnalyticsPanel();
    private final ReminderBadgePanel reminderBadgePanel = new ReminderBadgePanel();
    private final PhotoPanel photoPanel = new PhotoPanel();
    private final SettingsPanel settingsPanel = new SettingsPanel();

    public MainFrame() {
        super("Personal Gain Tracking System - Desktop Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1320, 840);
        setMinimumSize(new Dimension(1080, 680));
        setLocationRelativeTo(null);
        getContentPane().setBackground(ThemeConfig.BG_DARK);

        setLayout(new BorderLayout());

        // Fixed Sidebar
        sidebar = new SidebarPanel();
        add(sidebar, BorderLayout.WEST);

        // Content Area with CardLayout
        contentCards.setOpaque(false);
        contentCards.add(dashboardPanel, SidebarPanel.NavItem.DASHBOARD.name());
        contentCards.add(workoutPanel, SidebarPanel.NavItem.WORKOUTS.name());
        contentCards.add(goalPanel, SidebarPanel.NavItem.GOALS.name());
        contentCards.add(measurementPanel, SidebarPanel.NavItem.MEASUREMENTS.name());
        contentCards.add(nutritionPanel, SidebarPanel.NavItem.NUTRITION.name());
        contentCards.add(waterSleepPanel, SidebarPanel.NavItem.WATER_SLEEP.name());
        contentCards.add(analyticsPanel, SidebarPanel.NavItem.ANALYTICS.name());
        contentCards.add(reminderBadgePanel, SidebarPanel.NavItem.REMINDERS_BADGES.name());
        contentCards.add(photoPanel, SidebarPanel.NavItem.PHOTOS.name());
        contentCards.add(settingsPanel, SidebarPanel.NavItem.SETTINGS.name());

        add(contentCards, BorderLayout.CENTER);

        // Hook navigation
        sidebar.setOnNavigationListener(this::navigateTo);
        sidebar.setOnLogoutListener(this::handleLogout);

        dashboardPanel.setNavigationConsumer(this::navigateTo);
        settingsPanel.setOnProfileUpdatedCallback(() -> {
            sidebar.updateUserInfo();
            dashboardPanel.refreshDashboardData();
        });

        // Initial view
        navigateTo(SidebarPanel.NavItem.DASHBOARD);
    }

    public void navigateTo(SidebarPanel.NavItem item) {
        sidebar.setActiveItem(item);
        cardLayout.show(contentCards, item.name());

        // Refresh relevant data when page loads
        switch (item) {
            case DASHBOARD:
                dashboardPanel.refreshDashboardData();
                break;
            case WORKOUTS:
                workoutPanel.loadWorkouts();
                break;
            case GOALS:
                goalPanel.loadGoals();
                break;
            case MEASUREMENTS:
                measurementPanel.loadMeasurements();
                break;
            case NUTRITION:
                nutritionPanel.loadNutrition();
                break;
            case WATER_SLEEP:
                waterSleepPanel.loadData();
                break;
            case ANALYTICS:
                analyticsPanel.loadAnalytics();
                break;
            case REMINDERS_BADGES:
                reminderBadgePanel.loadData();
                break;
            case PHOTOS:
                photoPanel.loadPhotos();
                break;
            case SETTINGS:
                settingsPanel.loadSettings();
                break;
        }
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to sign out?",
                "Sign Out", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            SupabaseAuthService.getInstance().logout();
            setVisible(false);

            AuthDialog authDialog = new AuthDialog(this);
            authDialog.setVisible(true);

            if (authDialog.isAuthenticated()) {
                OnboardingDialog.showIfNewUser(this);
                sidebar.updateUserInfo();
                navigateTo(SidebarPanel.NavItem.DASHBOARD);
                setVisible(true);
            } else {
                System.exit(0);
            }
        }
    }
}
