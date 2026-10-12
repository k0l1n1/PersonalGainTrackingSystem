package com.personaltracker.components;

import com.personaltracker.config.ThemeConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Toast notification banner for displaying non-blocking feedback messages.
 */
public class ToastNotification extends JPanel {
    private static final long serialVersionUID = 1L;

    public enum ToastType {
        SUCCESS,
        INFO,
        ERROR
    }

    private final JLabel lblMessage;
    private final ToastType type;
    private Timer dismissTimer;

    public ToastNotification(String message, ToastType type) {
        this.type = type;
        setOpaque(false);
        setLayout(new BorderLayout(10, 0));
        setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        lblMessage = new JLabel(message);
        lblMessage.setFont(ThemeConfig.FONT_BODY_BOLD);
        lblMessage.setForeground(ThemeConfig.TEXT_PRIMARY);

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
            int y = 24;
            toast.setBounds(x, y, size.width, size.height);
            targetPane.add(toast, JLayeredPane.POPUP_LAYER);
            targetPane.repaint();

            toast.dismissTimer = new Timer(3500, e -> {
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

        Color bg;
        Color border;
        switch (type) {
            case SUCCESS:
                bg = new Color(0x4C, 0xAF, 0x50, 230);
                border = ThemeConfig.SUCCESS;
                break;
            case ERROR:
                bg = new Color(0xF0, 0x44, 0x44, 230);
                border = ThemeConfig.ERROR;
                break;
            case INFO:
            default:
                bg = new Color(0x24, 0x24, 0x24, 240);
                border = ThemeConfig.PRIMARY_ACCENT;
                break;
        }

        g2.setColor(bg);
        g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 10, 10));

        g2.setColor(border);
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 10, 10));

        g2.dispose();
        super.paintComponent(g);
    }
}
