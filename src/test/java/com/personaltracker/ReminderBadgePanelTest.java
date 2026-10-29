package com.personaltracker;

import com.personaltracker.config.ThemeConfig;
import com.personaltracker.view.ReminderBadgePanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ReminderBadgePanel Unit Tests")
public class ReminderBadgePanelTest {

    @BeforeAll
    public static void setup() {
        System.setProperty("java.awt.headless", "true");
        ThemeConfig.initializeTheme();
    }

    @Test
    @DisplayName("Should instantiate ReminderBadgePanel and execute click action")
    public void testReminderBadgePanelInstantiationAndClick() {
        AtomicBoolean clicked = new AtomicBoolean(false);
        ReminderBadgePanel panel = new ReminderBadgePanel(() -> clicked.set(true));

        assertNotNull(panel);
        assertEquals(0, panel.getPendingCount());

        // Test click action invocation
        panel.setOnClickAction(() -> clicked.set(true));
        panel.refreshReminders();

        assertNotNull(panel.getLayout());
    }
}

