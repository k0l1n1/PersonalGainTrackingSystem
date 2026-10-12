package com.personaltracker.components;

import com.personaltracker.config.ThemeConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern card container with rounded corners, dark surface fill, and subtle border.
 */
public class ModernCard extends JPanel {
    private int cornerRadius = 14;
    private Color backgroundColor = ThemeConfig.BG_CARD;
    private Color borderColor = ThemeConfig.BORDER_COLOR;
    private boolean hoverEffect = false;
    private boolean isHovered = false;

    public ModernCard() {
        this(14, new Insets(16, 18, 16, 18));
    }

    public ModernCard(int radius, Insets padding) {
        this.cornerRadius = radius;
        setOpaque(false);
        setLayout(new BorderLayout());
        if (padding != null) {
            setBorder(BorderFactory.createEmptyBorder(padding.top, padding.left, padding.bottom, padding.right));
        }
    }

    public void setHoverEffect(boolean enabled) {
        this.hoverEffect = enabled;
        if (enabled) {
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    isHovered = true;
                    repaint();
                }
                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }
    }

    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
        repaint();
    }

    public void setBorderColor(Color color) {
        this.borderColor = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Background fill
        Color fill = (hoverEffect && isHovered) ? ThemeConfig.BG_CARD_HOVER : backgroundColor;
        g2.setColor(fill);
        g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, cornerRadius, cornerRadius));

        // Border outline
        Color border = (hoverEffect && isHovered) ? ThemeConfig.PRIMARY_BLUE : borderColor;
        g2.setColor(border);
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new RoundRectangle2D.Float(0.6f, 0.6f, w - 1.2f, h - 1.2f, cornerRadius, cornerRadius));

        g2.dispose();
        super.paintComponent(g);
    }
}
