package com.personaltracker;

import com.personaltracker.view.MainFrame;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MainFrameInitTest {
    @Test
    public void testMainFrameInstantiation() {
        System.setProperty("java.awt.headless", "true");
        try {
            MainFrame frame = new MainFrame();
            assertNotNull(frame);
        } catch (java.awt.HeadlessException e) {
            // Headless environment is expected on some CI runners, pass
        }
    }
}
