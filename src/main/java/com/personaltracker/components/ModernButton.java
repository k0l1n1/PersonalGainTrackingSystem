package com.personaltracker.components;

import com.personaltracker.config.ThemeConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern styled JButton with customizable styles, rounded corners, and hover states.
 */
public class ModernButton extends JButton {
    private static final long serialVersionUID = 1L;

    public enum ButtonStyle {
        PRIMARY,
        SUCCESS,
        DANGER,
        SECONDARY,
        GHOST
    }

    private ButtonStyle style;
    private int cornerRadius = 8;
    private boolean isHovered = false;
    private boolean isPressed = false;

    public void setStyle(ButtonStyle style) {
        this.style = style;
        repaint();
    }

    public ModernButton(String text) {
        this(text, ButtonStyle.PRIMARY, null);
    }

    public ModernButton(String text, ButtonStyle style) {
        this(text, style, null);
    }

    public ModernButton(String text, Icon icon) {
        this(text, ButtonStyle.PRIMARY, icon);
    }

    public ModernButton(String text, ButtonStyle style, Icon icon) {
        super(text, icon);
        this.style = style;

        setFont(ThemeConfig.FONT_BODY_BOLD);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(8, 16, 8, 16));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    isHovered = true;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    isPressed = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        Color bg = getBaseColor();
        Color fg = ThemeConfig.TEXT_PRIMARY;

        if (isPressed) {
            bg = bg.darker();
        } else if (isHovered) {
            bg = getHoverColor();
        }

        if (!isEnabled()) {
            bg = new Color(51, 65, 85, 120);
            fg = ThemeConfig.TEXT_MUTED;
        }

        // Draw background
        if (style == ButtonStyle.GHOST) {
            if (isHovered) {
                g2.setColor(new Color(255, 255, 255, 20));
                g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, cornerRadius, cornerRadius));
            }
        } else if (style == ButtonStyle.SECONDARY) {
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, cornerRadius, cornerRadius));
            g2.setColor(ThemeConfig.BORDER_COLOR);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, cornerRadius, cornerRadius));
        } else {
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, cornerRadius, cornerRadius));
        }

        setForeground(fg);
        g2.dispose();
        super.paintComponent(g);
    }

    private Color getBaseColor() {
        switch (style) {
            case SUCCESS:
                return ThemeConfig.SUCCESS_EMERALD;
            case DANGER:
                return ThemeConfig.DANGER_ROSE;
            case SECONDARY:
                return ThemeConfig.BG_CARD;
            case GHOST:
                return new Color(0, 0, 0, 0);
            case PRIMARY:
            default:
                return ThemeConfig.PRIMARY_BLUE;
        }
    }

    private Color getHoverColor() {
        switch (style) {
            case SUCCESS:
                return new Color(20, 205, 145);
            case DANGER:
                return new Color(248, 113, 113);
            case SECONDARY:
                return ThemeConfig.BG_CARD_HOVER;
            case GHOST:
                return new Color(255, 255, 255, 30);
            case PRIMARY:
            default:
                return ThemeConfig.PRIMARY_HOVER;
        }
    }
}
