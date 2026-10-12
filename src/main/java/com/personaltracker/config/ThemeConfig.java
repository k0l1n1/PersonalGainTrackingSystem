package com.personaltracker.config;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;

/**
 * Centralized theme configuration and visual design system.
 * Implements a strict Black + Crimson Red theme palette.
 */
public class ThemeConfig {

    // =========================================================================
    // EXACT BLACK + CRIMSON RED COLOR PALETTE
    // =========================================================================

    // Primary Background: #101010 — main windows and application backgrounds.
    public static final Color BG_DARK = Color.decode("#101010");
    public static final Color BG_PRIMARY = BG_DARK;

    // Secondary Background: #181818 — sidebars and secondary sections.
    public static final Color BG_SIDEBAR = Color.decode("#181818");
    public static final Color BG_SECONDARY = BG_SIDEBAR;

    // Card Background: #242424 — dashboard cards, panels, forms, and content containers.
    public static final Color BG_CARD = Color.decode("#242424");
    public static final Color BG_CARD_HOVER = Color.decode("#2E2E2E");

    // Primary Accent: #F04444 — primary buttons, selected navigation items, active tabs, highlights, and important actions.
    public static final Color PRIMARY_ACCENT = Color.decode("#F04444");
    public static final Color PRIMARY_BLUE = PRIMARY_ACCENT; // Backward compatibility alias

    // Accent Hover: #D93636 — button hover and active interaction states.
    public static final Color ACCENT_HOVER = Color.decode("#D93636");
    public static final Color PRIMARY_HOVER = ACCENT_HOVER; // Backward compatibility alias

    // Primary Text: #FFFFFF — headings, important labels, and primary content.
    public static final Color TEXT_PRIMARY = Color.decode("#FFFFFF");

    // Secondary Text: #B3B3B3 — descriptions, subtitles, hints, and secondary information.
    public static final Color TEXT_SECONDARY = Color.decode("#B3B3B3");
    public static final Color TEXT_MUTED = Color.decode("#B3B3B3");

    // Borders and Dividers: #353535 — card borders, separators, and subtle outlines.
    public static final Color BORDER_COLOR = Color.decode("#353535");
    public static final Color BORDER_FOCUS = PRIMARY_ACCENT; // Accent Focus border

    // Input Background: #1C1C1C — text fields, password fields, search boxes, and editable form controls.
    public static final Color BG_INPUT = Color.decode("#1C1C1C");

    // Input Border: #454545 — input outlines.
    public static final Color BORDER_INPUT = Color.decode("#454545");

    // Success: #4CAF50 — successful operations and completed goals.
    public static final Color SUCCESS = Color.decode("#4CAF50");
    public static final Color SUCCESS_EMERALD = SUCCESS; // Backward compatibility alias

    // Warning: #FFC107 — warnings and pending activities.
    public static final Color WARNING = Color.decode("#FFC107");
    public static final Color WARNING_AMBER = WARNING; // Backward compatibility alias

    // Error: #F04444 — validation errors and failed operations.
    public static final Color ERROR = Color.decode("#F04444");
    public static final Color DANGER_ROSE = ERROR; // Backward compatibility alias

    // Harmonious crimson accents replacing secondary colored highlights
    public static final Color ACCENT_CYAN = Color.decode("#D93636");
    public static final Color PURPLE_ACCENT = Color.decode("#F04444");

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

    /**
     * Initializes the modern FlatLaf dark look and feel with customized Black + Crimson Red properties.
     */
    public static void initializeTheme() {
        try {
            // Setup FlatDarkLaf as base
            FlatDarkLaf.setup();

            // Core Backgrounds & ScrollBars
            UIManager.put("Panel.background", BG_DARK);
            UIManager.put("ScrollPane.background", BG_DARK);
            UIManager.put("Viewport.background", BG_DARK);
            UIManager.put("ScrollBar.thumb", BORDER_COLOR);
            UIManager.put("ScrollBar.track", BG_DARK);
            UIManager.put("ScrollBar.width", 8);

            // Table styling (#242424 background, #181818 header, #353535 grid, #F04444 selection)
            UIManager.put("Table.background", BG_CARD);
            UIManager.put("Table.foreground", TEXT_PRIMARY);
            UIManager.put("Table.selectionBackground", new Color(0xF0, 0x44, 0x44, 120));
            UIManager.put("Table.selectionForeground", TEXT_PRIMARY);
            UIManager.put("Table.gridColor", new Color(0x35, 0x35, 0x35, 140));
            UIManager.put("TableHeader.background", BG_SIDEBAR);
            UIManager.put("TableHeader.foreground", TEXT_SECONDARY);
            UIManager.put("TableHeader.font", FONT_BODY_BOLD);
            UIManager.put("Table.rowHeight", 36);

            // Text components & Dropdowns (#1C1C1C input, #FFFFFF text, #454545 border)
            UIManager.put("TextField.background", BG_INPUT);
            UIManager.put("TextField.foreground", TEXT_PRIMARY);
            UIManager.put("TextField.caretForeground", TEXT_PRIMARY);
            UIManager.put("PasswordField.background", BG_INPUT);
            UIManager.put("PasswordField.foreground", TEXT_PRIMARY);
            UIManager.put("TextArea.background", BG_INPUT);
            UIManager.put("TextArea.foreground", TEXT_PRIMARY);
            UIManager.put("ComboBox.background", BG_INPUT);
            UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
            UIManager.put("ComboBox.selectionBackground", PRIMARY_ACCENT);
            UIManager.put("ComboBox.selectionForeground", TEXT_PRIMARY);

            // Progress bar
            UIManager.put("ProgressBar.foreground", PRIMARY_ACCENT);
            UIManager.put("ProgressBar.background", BG_INPUT);

            // TabbedPane & Dialogs
            UIManager.put("TabbedPane.selectedBackground", BG_CARD);
            UIManager.put("TabbedPane.selectedForeground", PRIMARY_ACCENT);
            UIManager.put("TabbedPane.foreground", TEXT_SECONDARY);
            UIManager.put("TabbedPane.background", BG_DARK);
            UIManager.put("TabbedPane.focusColor", PRIMARY_ACCENT);
            UIManager.put("TabbedPane.underlineColor", PRIMARY_ACCENT);

            UIManager.put("OptionPane.background", BG_DARK);
            UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);

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
