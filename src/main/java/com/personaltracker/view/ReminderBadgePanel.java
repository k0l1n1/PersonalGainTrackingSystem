package com.personaltracker.view;

import com.personaltracker.config.ThemeConfig;
import com.personaltracker.dao.ReminderDAO;
import com.personaltracker.model.Reminder;
import com.personaltracker.model.UserSession;
import com.personaltracker.service.SupabaseClient;
import com.personaltracker.util.DataSyncManager;
import com.personaltracker.util.IconFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.util.List;

/**
 * Modern interactive badge component that displays pending reminder notifications
 * with real-time sync and click handling for managing reminders.
 */
public class ReminderBadgePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final ReminderDAO reminderDAO = new ReminderDAO();
    private final JLabel lblIcon = new JLabel();
    private final JLabel lblCount = new JLabel("0");
    private final JLabel lblText = new JLabel("Reminders");

    private Runnable onClickAction;
    private int pendingCount = 0;
    private boolean isHovered = false;

    public ReminderBadgePanel() {
        this(null);
    }

    public ReminderBadgePanel(Runnable onClickAction) {
        this.onClickAction = onClickAction;
        initUI();
        refreshReminders();

        DataSyncManager.registerListener(type -> {
            if (type == DataSyncManager.DataType.REMINDERS) {
                refreshReminders();
            }
        });
    }

    private void initUI() {
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.CENTER, 6, 4));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        lblIcon.setIcon(IconFactory.createIcon(IconFactory.IconType.REMINDERS, 14, ThemeConfig.PRIMARY_ACCENT));

        lblCount.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblCount.setForeground(Color.WHITE);
        lblCount.setOpaque(true);
        lblCount.setBackground(ThemeConfig.PRIMARY_ACCENT);
        lblCount.setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));

        lblText.setFont(ThemeConfig.FONT_SMALL);
        lblText.setForeground(ThemeConfig.TEXT_SECONDARY);

        add(lblIcon);
        add(lblText);
        add(lblCount);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                lblText.setForeground(ThemeConfig.TEXT_PRIMARY);
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                lblText.setForeground(ThemeConfig.TEXT_SECONDARY);
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (onClickAction != null) {
                    onClickAction.run();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        Color bg = isHovered ? ThemeConfig.BG_CARD_HOVER : ThemeConfig.BG_INPUT;
        g2.setColor(bg);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 14, 14));

        g2.setColor(isHovered ? ThemeConfig.PRIMARY_ACCENT : ThemeConfig.BORDER_COLOR);
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, 14, 14));

        g2.dispose();
        super.paintComponent(g);
    }

    /**
     * Refreshes the pending reminder count for the current session.
     */
    public void refreshReminders() {
        SwingWorker<Integer, Void> worker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() {
                try {
                    UserSession session = SupabaseClient.getInstance().getCurrentSession();
                    if (session == null || session.getUserId() == null) {
                        return 0;
                    }
                    List<Reminder> list = reminderDAO.getUpcomingReminders(session.getUserId(), LocalDate.now());
                    return (int) list.stream().filter(r -> !r.isCompleted()).count();
                } catch (Exception e) {
                    return 0;
                }
            }

            @Override
            protected void done() {
                try {
                    pendingCount = get();
                    updateBadgeDisplay();
                } catch (Exception ignored) {
                }
            }
        };
        worker.execute();
    }

    private void updateBadgeDisplay() {
        lblCount.setText(String.valueOf(pendingCount));
        if (pendingCount > 0) {
            lblCount.setBackground(ThemeConfig.PRIMARY_ACCENT);
            lblCount.setVisible(true);
            lblText.setText(pendingCount == 1 ? "1 Reminder" : pendingCount + " Reminders");
        } else {
            lblCount.setBackground(ThemeConfig.BORDER_COLOR);
            lblCount.setVisible(false);
            lblText.setText("No Reminders");
        }
        revalidate();
        repaint();
    }

    public void setOnClickAction(Runnable onClickAction) {
        this.onClickAction = onClickAction;
    }

    public int getPendingCount() {
        return pendingCount;
    }
}
