package com.personaltracker.components;

import com.personaltracker.config.ThemeConfig;
import com.personaltracker.util.IconFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Metric summary card displaying title, big bold number, subtext, and an accent icon badge.
 */
public class StatCard extends ModernCard {
    private static final long serialVersionUID = 1L;
    private final JLabel lblTitle;
    private final JLabel lblValue;
    private final JLabel lblSubtitle;
    private final JPanel iconBadge;

    public StatCard(String title, String initialValue, String subtitle, IconFactory.IconType iconType, Color accentColor) {
        this(title, initialValue, subtitle, iconType, accentColor, false);
    }

    public StatCard(String title, String initialValue, String subtitle, IconFactory.IconType iconType, Color accentColor, boolean compact) {
        super(compact ? 10 : 14, compact ? new Insets(10, 14, 10, 14) : new Insets(16, 18, 16, 18));
        setHoverEffect(true);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        lblTitle = new JLabel(title);
        lblTitle.setFont(compact ? new Font("Segoe UI", Font.BOLD, 10) : ThemeConfig.FONT_SMALL);
        lblTitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        topRow.add(lblTitle, BorderLayout.WEST);

        iconBadge = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 35));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), compact ? 6 : 8, compact ? 6 : 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconBadge.setOpaque(false);
        int badgeSize = compact ? 26 : 32;
        iconBadge.setPreferredSize(new Dimension(badgeSize, badgeSize));
        JLabel iconLbl = new JLabel(IconFactory.createIcon(iconType, compact ? 13 : 16, accentColor));
        iconBadge.add(iconLbl);
        topRow.add(iconBadge, BorderLayout.EAST);

        add(topRow, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(compact ? 6 : 12, 0, 0, 0));

        lblValue = new JLabel(initialValue);
        lblValue.setFont(compact ? new Font("Segoe UI", Font.BOLD, 20) : ThemeConfig.FONT_STAT_NUM);
        lblValue.setForeground(ThemeConfig.TEXT_PRIMARY);
        centerPanel.add(lblValue);

        centerPanel.add(Box.createVerticalStrut(compact ? 2 : 4));

        lblSubtitle = new JLabel(subtitle);
        lblSubtitle.setFont(compact ? new Font("Segoe UI", Font.PLAIN, 10) : ThemeConfig.FONT_SMALL);
        lblSubtitle.setForeground(ThemeConfig.TEXT_MUTED);
        centerPanel.add(lblSubtitle);

        add(centerPanel, BorderLayout.CENTER);
    }

    public void setValue(String value) {
        lblValue.setText(value);
    }

    public void setSubtitle(String subtitle) {
        lblSubtitle.setText(subtitle);
    }

    public void setSubtitleColor(Color color) {
        lblSubtitle.setForeground(color);
    }
}
