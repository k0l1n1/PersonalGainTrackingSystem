package com.personaltracker.config;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;

/**
 * Theme configuration and visual design system.
 * Implements a modern dark navy, blue, white, and grey aesthetic.
 */
public class ThemeConfig {

    // Primary Brand & Background Colors
    public static final Color BG_DARK = new Color(15, 23, 42);         // #0F172A (Deep Navy)
    public static final Color BG_CARD = new Color(30, 41, 59);         // #1E293B (Card/Surface)
    public static final Color BG_CARD_HOVER = new Color(39, 53, 76);   // #27354C (Card Hover)
    public static final Color BG_SIDEBAR = new Color(11, 17, 32);      // #0B1120 (Sidebar Dark)
    public static final Color BG_INPUT = new Color(24, 33, 47);        // #18212F (Input Fields)

    // Accent & Status Colors
    public static final Color PRIMARY_BLUE = new Color(59, 130, 246);  // #3B82F6 (Vibrant Blue)
    public static final Color PRIMARY_HOVER = new Color(37, 99, 235);  // #2563EB
    public static final Color ACCENT_CYAN = new Color(6, 182, 212);    // #06B6D4
    public static final Color SUCCESS_EMERALD = new Color(16, 185, 129);// #10B981 (Green)
    public static final Color WARNING_AMBER = new Color(245, 158, 11); // #F59E0B (Orange/Amber)
    public static final Color DANGER_ROSE = new Color(239, 68, 68);    // #EF4444 (Red/Rose)
    public static final Color PURPLE_ACCENT = new Color(139, 92, 246); // #8B5CF6

    // Text Colors
    public static final Color TEXT_PRIMARY = new Color(248, 250, 252); // #F8FAFC (White/Lightest Slate)
    public static final Color TEXT_SECONDARY = new Color(148, 163, 184);// #94A3B8 (Muted Slate)
    public static final Color TEXT_MUTED = new Color(100, 116, 139);   // #64748B (Dim Slate)

    // Border & Separator Colors
    public static final Color BORDER_COLOR = new Color(51, 65, 85);    // #334155 (Subtle border)
    public static final Color BORDER_FOCUS = new Color(59, 130, 246);   // Accent Focus border

    // Typography
    public static final Font FONT_TITLE_LARGE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_STAT_NUM = new Font("Segoe UI", Font.BOLD, 26);

    /**
     * Initializes the modern FlatLaf dark look and feel with customized properties.
     */
    public static void initializeTheme() {
        try {
            // Setup FlatDarkLaf
            FlatDarkLaf.setup();

            // Custom UI Defaults
            UIManager.put("Panel.background", BG_DARK);
            UIManager.put("ScrollPane.background", BG_DARK);
            UIManager.put("Viewport.background", BG_DARK);
            UIManager.put("ScrollBar.thumb", new Color(51, 65, 85));
            UIManager.put("ScrollBar.track", BG_DARK);
            UIManager.put("ScrollBar.width", 8);

            // Table styling
            UIManager.put("Table.background", BG_CARD);
            UIManager.put("Table.foreground", TEXT_PRIMARY);
            UIManager.put("Table.selectionBackground", new Color(59, 130, 246, 120));
            UIManager.put("Table.selectionForeground", TEXT_PRIMARY);
            UIManager.put("Table.gridColor", new Color(51, 65, 85, 100));
            UIManager.put("TableHeader.background", new Color(20, 30, 46));
            UIManager.put("TableHeader.foreground", TEXT_SECONDARY);
            UIManager.put("TableHeader.font", FONT_BODY_BOLD);
            UIManager.put("Table.rowHeight", 36);

            // Text components
            UIManager.put("TextField.background", BG_INPUT);
            UIManager.put("TextField.foreground", TEXT_PRIMARY);
            UIManager.put("TextField.caretForeground", TEXT_PRIMARY);
            UIManager.put("PasswordField.background", BG_INPUT);
            UIManager.put("PasswordField.foreground", TEXT_PRIMARY);
            UIManager.put("TextArea.background", BG_INPUT);
            UIManager.put("TextArea.foreground", TEXT_PRIMARY);
            UIManager.put("ComboBox.background", BG_INPUT);
            UIManager.put("ComboBox.foreground", TEXT_PRIMARY);

            // Progress bar
            UIManager.put("ProgressBar.foreground", PRIMARY_BLUE);
            UIManager.put("ProgressBar.background", new Color(30, 41, 59));

            // Set system font default
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
}
