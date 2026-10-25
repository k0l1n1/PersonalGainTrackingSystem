package com.personaltracker.components;

import com.personaltracker.config.ThemeConfig;
import com.personaltracker.util.IconFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

/**
 * Interactive Workout Timer HUD panel with digital display, live status badge,
 * and quick start, pause, resume, reset, and log actions.
 */
public class WorkoutTimerPanel extends ModernCard {
    private static final long serialVersionUID = 1L;

    private final JLabel lblTime;
    private final JPanel timerBadge;
    private final JLabel lblStatusBadge;
    private final ModernButton btnStartPause;
    private final ModernButton btnReset;
    private final ModernButton btnLogWorkout;

    private final Timer swingTimer;
    private int elapsedSeconds = 0;
    private boolean isRunning = false;
    private Consumer<Integer> onLogCallback;

    public WorkoutTimerPanel() {
        super(12, new Insets(10, 18, 10, 18));

        setLayout(new BorderLayout(16, 0));
        setBackgroundColor(ThemeConfig.BG_CARD);
        setPreferredSize(new Dimension(0, 64));
        setMinimumSize(new Dimension(0, 60));

        // 1. Left side: Circular Icon Badge, Title, and Live Status Badge
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftPanel.setOpaque(false);

        JPanel iconBadge = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeConfig.BG_CARD_HOVER);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(ThemeConfig.BORDER_COLOR);
                g2.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
            }
        };
        iconBadge.setPreferredSize(new Dimension(38, 38));
        iconBadge.setOpaque(false);
        JLabel iconLbl = new JLabel(IconFactory.createIcon(IconFactory.IconType.TIMER, 20, ThemeConfig.PRIMARY_ACCENT));
        iconBadge.add(iconLbl);
        leftPanel.add(iconBadge);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("WORKOUT STOPWATCH");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        textPanel.add(lblTitle);
        textPanel.add(Box.createVerticalStrut(3));

        lblStatusBadge = new JLabel("● IDLE");
        lblStatusBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblStatusBadge.setForeground(ThemeConfig.TEXT_MUTED);
        lblStatusBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(1, 6, 1, 6)
        ));
        textPanel.add(lblStatusBadge);

        leftPanel.add(textPanel);
        add(leftPanel, BorderLayout.WEST);

        // 2. Center: Prominent Digital Timer Display Console
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);

        timerBadge = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeConfig.BG_CARD_HOVER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                Color borderColor = isRunning ? new Color(0x10, 0xB9, 0x81) :
                        (elapsedSeconds > 0 ? new Color(0xF5, 0x9E, 0x0B) : ThemeConfig.BORDER_COLOR);
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        timerBadge.setOpaque(false);
        timerBadge.setBorder(BorderFactory.createEmptyBorder(2, 18, 2, 18));

        lblTime = new JLabel("00 : 00 : 00");
        lblTime.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTime.setForeground(ThemeConfig.TEXT_PRIMARY);
        timerBadge.add(lblTime);

        centerPanel.add(timerBadge);
        add(centerPanel, BorderLayout.CENTER);

        // 3. Right side: Action Buttons with Icons and Tactile Feedback
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actionsPanel.setOpaque(false);

        btnStartPause = new ModernButton("Start Timer", ModernButton.ButtonStyle.PRIMARY);
        btnStartPause.setIcon(IconFactory.createIcon(IconFactory.IconType.TIMER, 13, Color.WHITE));
        btnStartPause.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnStartPause.setPreferredSize(new Dimension(125, 34));

        btnReset = new ModernButton("Reset", ModernButton.ButtonStyle.SECONDARY);
        btnReset.setIcon(IconFactory.createIcon(IconFactory.IconType.REFRESH, 12, ThemeConfig.TEXT_SECONDARY));
        btnReset.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnReset.setPreferredSize(new Dimension(88, 34));

        btnLogWorkout = new ModernButton("Record Workout", ModernButton.ButtonStyle.SUCCESS);
        btnLogWorkout.setIcon(IconFactory.createIcon(IconFactory.IconType.CHECK, 13, Color.WHITE));
        btnLogWorkout.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLogWorkout.setPreferredSize(new Dimension(150, 34));

        btnReset.setEnabled(false);
        btnLogWorkout.setEnabled(false);

        actionsPanel.add(btnStartPause);
        actionsPanel.add(btnReset);
        actionsPanel.add(btnLogWorkout);
        add(actionsPanel, BorderLayout.EAST);

        // Timer ticking every 1 second
        swingTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                elapsedSeconds++;
                updateDisplay();
            }
        });

        btnStartPause.addActionListener(e -> toggleStartPause());
        btnReset.addActionListener(e -> resetTimer());
        btnLogWorkout.addActionListener(e -> {
            if (onLogCallback != null && elapsedSeconds > 0) {
                int mins = Math.max(1, (int) Math.ceil(elapsedSeconds / 60.0));
                onLogCallback.accept(mins);
            }
        });
    }

    public void setOnLogCallback(Consumer<Integer> callback) {
        this.onLogCallback = callback;
    }

    public void startTimer() {
        if (!isRunning) {
            toggleStartPause();
        }
    }

    public void startWithDuration(int minutes) {
        if (isRunning) {
            swingTimer.stop();
            isRunning = false;
        }
        elapsedSeconds = Math.max(0, minutes) * 60;
        updateDisplay();
        startTimer();
    }

    private void toggleStartPause() {
        if (!isRunning) {
            swingTimer.start();
            isRunning = true;
            btnStartPause.setText("Pause");
            btnStartPause.setStyle(ModernButton.ButtonStyle.DANGER);
            btnReset.setEnabled(true);
            btnLogWorkout.setEnabled(true);

            lblStatusBadge.setText("● LIVE SESSION");
            lblStatusBadge.setForeground(ThemeConfig.SUCCESS);
            lblStatusBadge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0x10, 0xB9, 0x81, 140), 1, true),
                    BorderFactory.createEmptyBorder(1, 6, 1, 6)
            ));
            lblTime.setForeground(new Color(0x05, 0x96, 0x69)); // Vivid emerald green
        } else {
            swingTimer.stop();
            isRunning = false;
            btnStartPause.setText("Resume");
            btnStartPause.setStyle(ModernButton.ButtonStyle.PRIMARY);

            lblStatusBadge.setText("⏸ PAUSED");
            lblStatusBadge.setForeground(ThemeConfig.WARNING);
            lblStatusBadge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xF5, 0x9E, 0x0B, 140), 1, true),
                    BorderFactory.createEmptyBorder(1, 6, 1, 6)
            ));
            lblTime.setForeground(new Color(0xD9, 0x77, 0x06)); // Rich warm amber
        }
        timerBadge.repaint();
        repaint();
    }

    public void resetTimer() {
        swingTimer.stop();
        isRunning = false;
        elapsedSeconds = 0;
        updateDisplay();
        btnStartPause.setText("Start Timer");
        btnStartPause.setStyle(ModernButton.ButtonStyle.PRIMARY);
        btnReset.setEnabled(false);
        btnLogWorkout.setEnabled(false);

        lblStatusBadge.setText("● IDLE");
        lblStatusBadge.setForeground(ThemeConfig.TEXT_MUTED);
        lblStatusBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(1, 6, 1, 6)
        ));
        lblTime.setText("00 : 00 : 00");
        lblTime.setForeground(ThemeConfig.TEXT_PRIMARY);
        timerBadge.repaint();
        repaint();
    }

    private void updateDisplay() {
        int hours = elapsedSeconds / 3600;
        int minutes = (elapsedSeconds % 3600) / 60;
        int seconds = elapsedSeconds % 60;
        lblTime.setText(String.format("%02d : %02d : %02d", hours, minutes, seconds));
        timerBadge.repaint();
    }

    public int getElapsedMinutes() {
        return Math.max(1, (int) Math.ceil(elapsedSeconds / 60.0));
    }
}
