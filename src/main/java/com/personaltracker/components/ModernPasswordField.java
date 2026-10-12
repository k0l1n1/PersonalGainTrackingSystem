package com.personaltracker.components;

import com.personaltracker.config.ThemeConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern password input with rounded borders, focus highlight, and placeholder text.
 */
public class ModernPasswordField extends JPasswordField {
    private static final long serialVersionUID = 1L;
    private String placeholder = "";
    private boolean isFocused = false;
    private int cornerRadius = 8;

    public ModernPasswordField() {
        this("");
    }

    public ModernPasswordField(String placeholder) {
        this.placeholder = placeholder;

        setFont(ThemeConfig.FONT_BODY);
        setForeground(ThemeConfig.TEXT_PRIMARY);
        setCaretColor(ThemeConfig.TEXT_PRIMARY);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                isFocused = true;
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                isFocused = false;
                repaint();
            }
        });
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Background
        g2.setColor(ThemeConfig.BG_INPUT);
        g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, cornerRadius, cornerRadius));

        // Border
        if (isFocused) {
            g2.setColor(ThemeConfig.BORDER_FOCUS);
            g2.setStroke(new BasicStroke(1.5f));
        } else {
            g2.setColor(ThemeConfig.BORDER_INPUT);
            g2.setStroke(new BasicStroke(1.0f));
        }
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, cornerRadius, cornerRadius));

        super.paintComponent(g);

        // Placeholder
        if (getPassword().length == 0 && !isFocused && placeholder != null && !placeholder.isEmpty()) {
            g2.setColor(ThemeConfig.TEXT_MUTED);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int y = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(placeholder, getInsets().left, y);
        }

        g2.dispose();
    }
}
