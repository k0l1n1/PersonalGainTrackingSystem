package com.personaltracker.components;

import com.personaltracker.config.ThemeConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern progress bar with rounded corners and gradient fill.
 */
public class ModernProgressBar extends JProgressBar {
    private static final long serialVersionUID = 1L;
    private Color progressColor = ThemeConfig.PRIMARY_ACCENT;
    private Color secondaryColor = ThemeConfig.ACCENT_HOVER;
    private int cornerRadius = 6;

    public ModernProgressBar(int min, int max) {
        super(min, max);
        setOpaque(false);
        setBorderPainted(false);
        setStringPainted(false);
        setPreferredSize(new Dimension(160, 10));
    }

    public void setProgressColor(Color color) {
        this.progressColor = color;
        this.secondaryColor = color.brighter();
        repaint();
    }

    public void setColors(Color primary, Color secondary) {
        this.progressColor = primary;
        this.secondaryColor = secondary;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Track background
        g2.setColor(ThemeConfig.BG_INPUT);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));

        // Progress fill
        int min = getMinimum();
        int max = getMaximum();
        int val = getValue();
        if (max > min && val > min) {
            float ratio = (float)(val - min) / (max - min);
            ratio = Math.min(1.0f, Math.max(0.0f, ratio));
            int fillW = (int)(w * ratio);

            if (fillW > 0) {
                GradientPaint gp = new GradientPaint(0, 0, progressColor, fillW, 0, secondaryColor);
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(0, 0, fillW, h, cornerRadius, cornerRadius));
            }
        }

        g2.dispose();
    }
}
