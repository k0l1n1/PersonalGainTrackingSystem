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
        Color fg;
        if (style == ButtonStyle.PRIMARY || style == ButtonStyle.SUCCESS || style == ButtonStyle.DANGER) {
            fg = Color.WHITE;
        } else {
            fg = ThemeConfig.isDarkMode() ? Color.WHITE : ThemeConfig.TEXT_PRIMARY;
        }

        if (isPressed) {
            bg = bg.darker();
        } else if (isHovered) {
            bg = getHoverColor();
        }

        if (!isEnabled()) {
            bg = ThemeConfig.isDarkMode() ? new Color(0x27, 0x27, 0x2A) : new Color(0xE5, 0xE7, 0xEB);
            fg = ThemeConfig.TEXT_MUTED;
        }

        // Draw background
        switch (style) {
            case GHOST -> {
                if (isHovered) {
                    g2.setColor(ThemeConfig.isDarkMode() ? new Color(255, 255, 255, 20) : new Color(0, 0, 0, 15));
                    g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, cornerRadius, cornerRadius));
                }
            }
            case SECONDARY -> {
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, cornerRadius, cornerRadius));
                g2.setColor(ThemeConfig.BORDER_COLOR);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, cornerRadius, cornerRadius));
            }
            case PRIMARY -> {
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, cornerRadius, cornerRadius));
                if (ThemeConfig.isDarkMode()) {
                    g2.setColor(new Color(0x60, 0x6C, 0x80));
                    g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, cornerRadius, cornerRadius));
                }
            }
            default -> {
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, cornerRadius, cornerRadius));
            }
        }

        setForeground(fg);
        g2.dispose();
        super.paintComponent(g);
    }

    private Color getBaseColor() {
        switch (style) {
            case SUCCESS:
                return ThemeConfig.SUCCESS;
            case DANGER:
                return ThemeConfig.ERROR;
            case SECONDARY:
                return ThemeConfig.BG_CARD;
            case GHOST:
                return new Color(0, 0, 0, 0);
            case PRIMARY:
            default:
                return ThemeConfig.isDarkMode() ? new Color(0x3B, 0x42, 0x52) : ThemeConfig.PRIMARY_ACCENT;
        }
    }

    private Color getHoverColor() {
        switch (style) {
            case SUCCESS:
                return new Color(0x38, 0x8E, 0x3C);
            case DANGER:
                return ThemeConfig.isDarkMode() ? new Color(0xDC, 0x26, 0x26) : ThemeConfig.ACCENT_HOVER;
            case SECONDARY:
                return ThemeConfig.BG_CARD_HOVER;
            case GHOST:
                return ThemeConfig.isDarkMode() ? new Color(255, 255, 255, 25) : new Color(0, 0, 0, 20);
            case PRIMARY:
            default:
                return ThemeConfig.isDarkMode() ? new Color(0x4C, 0x56, 0x6A) : ThemeConfig.ACCENT_HOVER;
        }
    }
}
