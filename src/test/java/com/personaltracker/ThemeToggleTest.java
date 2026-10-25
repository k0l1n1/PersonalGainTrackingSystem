package com.personaltracker;

import com.personaltracker.config.ThemeConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class ThemeToggleTest {

    @BeforeEach
    public void setup() {
        ThemeConfig.setDarkMode(false);
    }

    @AfterEach
    public void tearDown() {
        ThemeConfig.setDarkMode(false);
    }

    @Test
    public void testLightModePalette() {
        assertFalse(ThemeConfig.isDarkMode());
        assertEquals(Color.decode("#FFFFFF"), ThemeConfig.BG_CARD);
        assertEquals(Color.decode("#18181B"), ThemeConfig.PRIMARY_ACCENT);
        assertEquals(Color.decode("#111827"), ThemeConfig.TEXT_PRIMARY);
        assertEquals(Color.decode("#F8F9FA"), ThemeConfig.BG_DARK);
        assertEquals(Color.WHITE, ThemeConfig.TEXT_ON_ACCENT);

        javax.swing.JButton btn = new javax.swing.JButton("Today");
        ThemeConfig.styleQuickPill(btn, true);
        assertEquals(ThemeConfig.PRIMARY_ACCENT, btn.getBackground());
        assertEquals(ThemeConfig.TEXT_ON_ACCENT, btn.getForeground());
    }

    @Test
    public void testDarkModePalette() {
        ThemeConfig.setDarkMode(true);
        assertTrue(ThemeConfig.isDarkMode());

        // In Dark Mode: Crisp, high-contrast, easy-to-see theme
        assertEquals(Color.decode("#1D1D24"), ThemeConfig.BG_CARD);
        assertEquals(Color.decode("#E4E4E7"), ThemeConfig.PRIMARY_ACCENT);
        assertEquals(Color.decode("#FFFFFF"), ThemeConfig.TEXT_PRIMARY);
        assertEquals(Color.decode("#111115"), ThemeConfig.BG_DARK);
        assertEquals(Color.decode("#111115"), ThemeConfig.TEXT_ON_ACCENT);

        // Chart colors remain vibrant
        assertEquals(Color.decode("#10B981"), ThemeConfig.SUCCESS);
        assertEquals(Color.decode("#2563EB"), ThemeConfig.CHART_WORKOUT);

        javax.swing.JButton btn = new javax.swing.JButton("Today");
        ThemeConfig.styleQuickPill(btn, true);
        assertEquals(ThemeConfig.PRIMARY_ACCENT, btn.getBackground());
        assertEquals(ThemeConfig.TEXT_ON_ACCENT, btn.getForeground());
        // Verify active text is dark on silver background for maximum contrast
        assertEquals(Color.decode("#111115"), btn.getForeground());
    }

    @Test
    public void testToggleThemeAndListener() {
        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);

        ThemeConfig.addThemeChangeListener(listener);
        try {
            assertFalse(ThemeConfig.isDarkMode());
            ThemeConfig.toggleTheme();
            assertTrue(ThemeConfig.isDarkMode());
            assertTrue(notified.get());

            notified.set(false);
            ThemeConfig.toggleTheme();
            assertFalse(ThemeConfig.isDarkMode());
            assertTrue(notified.get());
        } finally {
            ThemeConfig.removeThemeChangeListener(listener);
        }
    }
}
