package com.personaltracker.config;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Centralized theme configuration and visual design system.
 * Implements seamless dynamic switching between Classic Light Mode and Dark Mode.
 * Crafted for maximum clarity, readability, and visual ergonomics in both themes.
 */
public class ThemeConfig {

    private static boolean isDarkMode = false;
    private static final List<Runnable> themeListeners = new CopyOnWriteArrayList<>();

    // =========================================================================
    // DYNAMIC PALETTE FIELDS (SWITCH BETWEEN LIGHT AND DARK)
    // =========================================================================

    // Primary Canvas Background
    public static Color BG_DARK;
    public static Color BG_PRIMARY;

    // Secondary / Sidebar Background
    public static Color BG_SIDEBAR;
    public static Color BG_SECONDARY;

    // Card Surface Background
    public static Color BG_CARD;
    public static Color BG_CARD_HOVER;

    // Primary Accent (Black in Light mode, Bright Crisp Silver-White in Dark mode)
    public static Color PRIMARY_ACCENT;
    public static Color PRIMARY_BLUE; // Compatibility alias

    // Accent Hover
    public static Color ACCENT_HOVER;
    public static Color PRIMARY_HOVER; // Compatibility alias

    // Text hierarchy
    public static Color TEXT_PRIMARY;
    public static Color TEXT_SECONDARY;
    public static Color TEXT_MUTED;
    public static Color TEXT_ON_ACCENT;

    // Borders & Dividers
    public static Color BORDER_COLOR;
    public static Color BORDER_FOCUS;

    // Text Inputs
    public static Color BG_INPUT;
    public static Color BORDER_INPUT;

    // =========================================================================
    // VIBRANT METRIC & GRAPH ACCENTS (PRESERVED UNCHANGED IN BOTH MODES)
    // =========================================================================
    public static final Color SUCCESS = Color.decode("#10B981"); // Vibrant Emerald Green
    public static final Color SUCCESS_EMERALD = SUCCESS;

    public static final Color WARNING = Color.decode("#F59E0B"); // Vibrant Amber / Orange
    public static final Color WARNING_AMBER = WARNING;

    public static final Color ERROR = Color.decode("#EF4444"); // Vibrant Red / Coral
    public static final Color DANGER_ROSE = ERROR;

    // Dedicated Chart & Metric Colors
    public static final Color CHART_WORKOUT = Color.decode("#2563EB"); // Vibrant Royal Blue
    public static final Color CHART_WEIGHT = SUCCESS;                  // Vibrant Emerald Green
    public static final Color CHART_CALORIES = WARNING;                // Vibrant Amber
    public static final Color CHART_SLEEP = Color.decode("#8B5CF6");   // Vibrant Violet / Purple
    public static final Color CHART_WATER = Color.decode("#06B6D4");   // Vibrant Cyan

    // =========================================================================
    // TYPOGRAPHY (PRESERVED UNCHANGED)
    // =========================================================================
    public static final Font FONT_TITLE_LARGE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_STAT_NUM = new Font("Segoe UI", Font.BOLD, 26);

    static {
        // Initialize with default light mode
        applyPalette(false);
    }

    /**
     * Applies the color tokens for either dark or light mode.
     */
    public static void applyPalette(boolean dark) {
        isDarkMode = dark;
        if (dark) {
            // DARK MODE: High-contrast, easy-to-see dark theme with clear visual depth
            BG_DARK = Color.decode("#111115");       // Sleek dark canvas
            BG_PRIMARY = BG_DARK;

            BG_SIDEBAR = Color.decode("#17171C");    // Clearly defined sidebar
            BG_SECONDARY = BG_SIDEBAR;

            BG_CARD = Color.decode("#1D1D24");       // Elevated, distinct card surface
            BG_CARD_HOVER = Color.decode("#272732"); // Distinct card hover

            PRIMARY_ACCENT = Color.decode("#E4E4E7"); // Bright crisp silver-white for icons & highlights
            PRIMARY_BLUE = PRIMARY_ACCENT;

            ACCENT_HOVER = Color.decode("#F4F4F5");
            PRIMARY_HOVER = ACCENT_HOVER;

            TEXT_PRIMARY = Color.decode("#FFFFFF");   // Pure white for maximum clarity & legibility
            TEXT_SECONDARY = Color.decode("#D4D4D8"); // High-contrast light silver
            TEXT_MUTED = Color.decode("#A1A1AA");     // Clearly readable neutral gray
            TEXT_ON_ACCENT = Color.decode("#111115"); // Deep rich black for maximum contrast on light silver-white accent

            BORDER_COLOR = Color.decode("#32323D");   // Crisp, clearly defined borders
            BORDER_FOCUS = Color.decode("#FFFFFF");

            BG_INPUT = Color.decode("#181820");       // Distinct input field background
            BORDER_INPUT = Color.decode("#3E3E4C");
        } else {
            // LIGHT MODE: Crisp white canvas, clean cards, solid black accents and text
            BG_DARK = Color.decode("#F8F9FA");
            BG_PRIMARY = BG_DARK;

            BG_SIDEBAR = Color.decode("#FFFFFF");
            BG_SECONDARY = BG_SIDEBAR;

            BG_CARD = Color.decode("#FFFFFF");
            BG_CARD_HOVER = Color.decode("#F3F4F6");

            PRIMARY_ACCENT = Color.decode("#18181B");
            PRIMARY_BLUE = PRIMARY_ACCENT;

            ACCENT_HOVER = Color.decode("#27272A");
            PRIMARY_HOVER = ACCENT_HOVER;

            TEXT_PRIMARY = Color.decode("#111827");
            TEXT_SECONDARY = Color.decode("#6B7280");
            TEXT_MUTED = Color.decode("#9CA3AF");
            TEXT_ON_ACCENT = Color.WHITE;             // Crisp white on black primary accent

            BORDER_COLOR = Color.decode("#E5E7EB");
            BORDER_FOCUS = Color.decode("#18181B");

            BG_INPUT = Color.decode("#FFFFFF");
            BORDER_INPUT = Color.decode("#D1D5DB");
        }
    }

    public static boolean isDarkMode() {
        return isDarkMode;
    }

    /**
     * Toggles between Light Mode and Dark Mode.
     */
    public static void toggleTheme() {
        setDarkMode(!isDarkMode);
    }

    /**
     * Activates either Dark Mode or Light Mode dynamically across all open windows.
     */
    public static void setDarkMode(boolean dark) {
        applyPalette(dark);
        try {
            if (dark) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
            updateUIManager();
            FlatLaf.updateUI();
        } catch (Exception e) {
            System.err.println("Failed to update FlatLaf theme: " + e.getMessage());
        }
        fireThemeChanged();
    }

    public static void addThemeChangeListener(Runnable listener) {
        themeListeners.add(listener);
    }

    public static void removeThemeChangeListener(Runnable listener) {
        themeListeners.remove(listener);
    }

    private static void fireThemeChanged() {
        for (Runnable r : themeListeners) {
            try {
                r.run();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Synchronizes FlatLaf UIManager defaults with the active palette.
     */
    public static void updateUIManager() {
        // Core Backgrounds & ScrollBars
        UIManager.put("Panel.background", BG_DARK);
        UIManager.put("ScrollPane.background", BG_DARK);
        UIManager.put("Viewport.background", BG_DARK);
        UIManager.put("ScrollBar.thumb", isDarkMode ? new Color(0x4B, 0x4B, 0x58) : new Color(0xC0, 0xC4, 0xCC));
        UIManager.put("ScrollBar.track", BG_DARK);
        UIManager.put("ScrollBar.width", 8);

        // Table styling
        UIManager.put("Table.background", BG_CARD);
        UIManager.put("Table.foreground", TEXT_PRIMARY);
        UIManager.put("Table.selectionBackground", isDarkMode ? new Color(0x3B, 0x42, 0x52, 160) : new Color(0x18, 0x18, 0x1B, 22));
        UIManager.put("Table.selectionForeground", TEXT_PRIMARY);
        UIManager.put("Table.gridColor", BORDER_COLOR);
        UIManager.put("TableHeader.background", isDarkMode ? Color.decode("#252530") : Color.decode("#F1F3F5"));
        UIManager.put("TableHeader.foreground", isDarkMode ? Color.WHITE : TEXT_SECONDARY);
        UIManager.put("TableHeader.font", FONT_BODY_BOLD);
        UIManager.put("Table.rowHeight", 36);

        // Text components & Dropdowns
        UIManager.put("TextField.background", BG_INPUT);
        UIManager.put("TextField.foreground", TEXT_PRIMARY);
        UIManager.put("TextField.caretForeground", TEXT_PRIMARY);
        UIManager.put("PasswordField.background", BG_INPUT);
        UIManager.put("PasswordField.foreground", TEXT_PRIMARY);
        UIManager.put("TextArea.background", BG_INPUT);
        UIManager.put("TextArea.foreground", TEXT_PRIMARY);
        UIManager.put("ComboBox.background", BG_INPUT);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("ComboBox.selectionBackground", isDarkMode ? new Color(0x3B, 0x42, 0x52, 160) : new Color(0x18, 0x18, 0x1B, 22));
        UIManager.put("ComboBox.selectionForeground", TEXT_PRIMARY);

        // Progress bar
        UIManager.put("ProgressBar.foreground", isDarkMode ? Color.decode("#2563EB") : PRIMARY_ACCENT);
        UIManager.put("ProgressBar.background", BORDER_COLOR);

        // TabbedPane & Dialogs
        UIManager.put("TabbedPane.selectedBackground", BG_CARD);
        UIManager.put("TabbedPane.selectedForeground", isDarkMode ? Color.WHITE : PRIMARY_ACCENT);
        UIManager.put("TabbedPane.foreground", TEXT_SECONDARY);
        UIManager.put("TabbedPane.background", BG_DARK);
        UIManager.put("TabbedPane.focusColor", isDarkMode ? Color.WHITE : PRIMARY_ACCENT);
        UIManager.put("TabbedPane.underlineColor", isDarkMode ? Color.WHITE : PRIMARY_ACCENT);

        UIManager.put("OptionPane.background", BG_CARD);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
    }

    /**
     * Initializes the modern FlatLaf look and feel with customized properties.
     */
    public static void initializeTheme() {
        try {
            if (isDarkMode) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
            updateUIManager();
            setUIFont(new FontUIResource("Segoe UI", Font.PLAIN, 13));
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf theme: " + e.getMessage());
        }
    }

    private static void setUIFont(FontUIResource f) {
        java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, f);
            }
        }
    }

    /**
     * Styles a segmented quick-pill toggle button (e.g. Today / Yesterday) with high-contrast text and border.
     */
    public static void styleQuickPill(JButton btn, boolean active) {
        btn.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 11));
        btn.setBackground(active ? PRIMARY_ACCENT : BG_CARD);
        btn.setForeground(active ? TEXT_ON_ACCENT : TEXT_PRIMARY);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(active ? PRIMARY_ACCENT : BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(5, 11, 5, 11)
        ));
    }
}
