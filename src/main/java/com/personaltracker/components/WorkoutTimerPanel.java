package com.personaltracker.components;

import com.personaltracker.config.ThemeConfig;
import com.personaltracker.util.IconFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

/**
 * Interactive Workout Timer panel with start, pause, resume, reset, and log actions.
 */
public class WorkoutTimerPanel extends ModernCard {
    private static final long serialVersionUID = 1L;
    private final JLabel lblTime;
    private final ModernButton btnStartPause;
    private final ModernButton btnReset;
    private final ModernButton btnLogWorkout;

    private final Timer swingTimer;
    private int elapsedSeconds = 0;
    private boolean isRunning = false;
    private Consumer<Integer> onLogCallback;

    public WorkoutTimerPanel() {
        super(14, new Insets(16, 20, 16, 20));

        setLayout(new BorderLayout(16, 0));
        setBackgroundColor(new Color(22, 33, 49));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftPanel.setOpaque(false);

        JLabel iconLbl = new JLabel(IconFactory.createIcon(IconFactory.IconType.TIMER, 24, ThemeConfig.PRIMARY_BLUE));
        leftPanel.add(iconLbl);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Workout Stopwatch");
        lblTitle.setFont(ThemeConfig.FONT_SUBTITLE);
        lblTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
        textPanel.add(lblTitle);

        lblTime = new JLabel("00:00");
        lblTime.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTime.setForeground(ThemeConfig.PRIMARY_BLUE);
        textPanel.add(lblTime);

        leftPanel.add(textPanel);
        add(leftPanel, BorderLayout.WEST);

        // Right side buttons
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        actionsPanel.setOpaque(false);

        btnStartPause = new ModernButton("Start Timer", ModernButton.ButtonStyle.PRIMARY);
        btnReset = new ModernButton("Reset", ModernButton.ButtonStyle.SECONDARY);
        btnLogWorkout = new ModernButton("Record As Workout", ModernButton.ButtonStyle.SUCCESS);

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

    private void toggleStartPause() {
        if (!isRunning) {
            swingTimer.start();
            isRunning = true;
            btnStartPause.setText("Pause");
            btnStartPause.setStyle(ModernButton.ButtonStyle.DANGER);
            btnReset.setEnabled(true);
            btnLogWorkout.setEnabled(true);
        } else {
            swingTimer.stop();
            isRunning = false;
            btnStartPause.setText("Resume");
            btnStartPause.setStyle(ModernButton.ButtonStyle.PRIMARY);
        }
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
    }

    private void updateDisplay() {
        int minutes = elapsedSeconds / 60;
        int seconds = elapsedSeconds % 60;
        lblTime.setText(String.format("%02d:%02d", minutes, seconds));
    }

    public int getElapsedMinutes() {
        return Math.max(1, (int) Math.ceil(elapsedSeconds / 60.0));
    }
}
