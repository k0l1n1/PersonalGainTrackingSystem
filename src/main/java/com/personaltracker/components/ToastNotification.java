package com.personaltracker.components;

import com.personaltracker.config.ThemeConfig;
import com.personaltracker.util.IconFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Minimalist modern toast notification banner for sleek non-blocking feedback.
 */
public class ToastNotification extends JPanel {
    private static final long serialVersionUID = 1L;

    public enum ToastType {
        SUCCESS,
        INFO,
        ERROR
    }

    private final JLabel lblIcon;
    private final JLabel lblMessage;
    private final ToastType type;
    private Timer dismissTimer;

    public ToastNotification(String message, ToastType type) {
        this.type = type;
        setOpaque(false);
        setLayout(new BorderLayout(10, 0));
        setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));

        Icon vectorIcon;
        Color accentColor;
        switch (type) {
            case SUCCESS -> {
                accentColor = ThemeConfig.SUCCESS;
                vectorIcon = IconFactory.createIcon(IconFactory.IconType.CHECK, 13, accentColor);
            }
            case ERROR -> {
                accentColor = ThemeConfig.ERROR;
                vectorIcon = IconFactory.createIcon(IconFactory.IconType.CLOSE, 11, accentColor);
            }
            case INFO -> {
                accentColor = ThemeConfig.PRIMARY_ACCENT;
                vectorIcon = IconFactory.createIcon(IconFactory.IconType.INFO, 13, accentColor);
            }
            default -> {
                accentColor = ThemeConfig.PRIMARY_ACCENT;
                vectorIcon = IconFactory.createIcon(IconFactory.IconType.INFO, 13, accentColor);
            }
        }

        lblIcon = new JLabel(vectorIcon);
        add(lblIcon, BorderLayout.WEST);

        lblMessage = new JLabel(message);
        lblMessage.setFont(ThemeConfig.FONT_BODY_BOLD);
        lblMessage.setForeground(Color.WHITE);
        add(lblMessage, BorderLayout.CENTER);
    }

    public static void showToast(Component parent, String message, ToastType type) {
        if (parent == null) return;
        ToastNotification toast = new ToastNotification(message, type);

        JLayeredPane layeredPane = null;
        if (parent instanceof JFrame) {
            layeredPane = ((JFrame) parent).getLayeredPane();
        } else if (parent instanceof JRootPane) {
            layeredPane = ((JRootPane) parent).getLayeredPane();
        } else {
            JRootPane rootPane = SwingUtilities.getRootPane(parent);
            if (rootPane != null) {
                layeredPane = rootPane.getLayeredPane();
            }
        }

        if (layeredPane != null) {
            final JLayeredPane targetPane = layeredPane;
            Dimension size = toast.getPreferredSize();
            int x = (targetPane.getWidth() - size.width) / 2;
            int y = 20;
            toast.setBounds(x, y, size.width, size.height);
            targetPane.add(toast, JLayeredPane.POPUP_LAYER);
            targetPane.repaint();

            toast.dismissTimer = new Timer(3200, e -> {
                targetPane.remove(toast);
                targetPane.repaint();
            });
            toast.dismissTimer.setRepeats(false);
            toast.dismissTimer.start();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Sleek minimal dark pill surface (#161616 with high opacity)
        g2.setColor(new Color(0x16, 0x16, 0x16, 245));
        g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 22, 22));

        // Subtle accent border outline
        Color border;
        switch (type) {
            case SUCCESS -> border = new Color(0x4C, 0xAF, 0x50, 160);
            case ERROR -> border = new Color(0xF0, 0x44, 0x44, 160);
            case INFO -> border = new Color(0xF0, 0x44, 0x44, 140);
            default -> border = new Color(0x35, 0x35, 0x35, 180);
        }

        g2.setColor(border);
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new RoundRectangle2D.Float(0.6f, 0.6f, w - 1.2f, h - 1.2f, 22, 22));

        g2.dispose();
        super.paintComponent(g);
    }
}
