package com.personaltracker.view;

import com.personaltracker.components.ModernButton;
import com.personaltracker.components.ModernCard;
import com.personaltracker.components.ModernTextField;
import com.personaltracker.components.ToastNotification;
import com.personaltracker.config.ThemeConfig;
import com.personaltracker.dao.AchievementDAO;
import com.personaltracker.dao.ReminderDAO;
import com.personaltracker.model.Achievement;
import com.personaltracker.model.Reminder;
import com.personaltracker.util.DateUtil;
import com.personaltracker.util.IconFactory;
import com.personaltracker.util.ValidationUtil;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reminders management and Gamification Badges showcase.
 */
public class ReminderBadgePanel extends JPanel {

    private final ReminderDAO reminderDAO = new ReminderDAO();
    private final AchievementDAO achievementDAO = new AchievementDAO();

    private final JPanel pnlRemindersList = new JPanel();
    private final JPanel pnlBadgesGrid = new JPanel();

    private List<Reminder> currentReminders = new ArrayList<>();
    private List<Achievement> currentBadges = new ArrayList<>();

    public ReminderBadgePanel() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        titlePanel.setOpaque(false);
        JLabel title = new JLabel("Reminders & Achievement Badges");
        title.setFont(ThemeConfig.FONT_TITLE_LARGE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        titlePanel.add(title);

        JLabel subtitle = new JLabel("Stay accountable with active reminders and celebrate your fitness milestones");
        subtitle.setFont(ThemeConfig.FONT_BODY);
        subtitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        titlePanel.add(subtitle);
        header.add(titlePanel, BorderLayout.WEST);

        ModernButton btnNewReminder = new ModernButton("+ Set Reminder", ModernButton.ButtonStyle.PRIMARY);
        btnNewReminder.addActionListener(e -> openReminderDialog());
        header.add(btnNewReminder, BorderLayout.EAST);

        content.add(header);
        content.add(Box.createVerticalStrut(20));

        // Section 1: Reminders Card
        ModernCard cardReminders = new ModernCard(14, new Insets(18, 20, 18, 20));
        JLabel lblRemHead = new JLabel("Active & Upcoming Reminders");
        lblRemHead.setFont(ThemeConfig.FONT_SUBTITLE);
        lblRemHead.setForeground(ThemeConfig.TEXT_PRIMARY);
        cardReminders.add(lblRemHead, BorderLayout.NORTH);

        pnlRemindersList.setLayout(new BoxLayout(pnlRemindersList, BoxLayout.Y_AXIS));
        pnlRemindersList.setOpaque(false);
        pnlRemindersList.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JScrollPane scrollRem = new JScrollPane(pnlRemindersList);
        scrollRem.setOpaque(false);
        scrollRem.getViewport().setOpaque(false);
        scrollRem.setBorder(null);
        scrollRem.setPreferredSize(new Dimension(0, 160));
        cardReminders.add(scrollRem, BorderLayout.CENTER);
        content.add(cardReminders);
        content.add(Box.createVerticalStrut(24));

        // Section 2: Achievement Badges Showcase
        JLabel lblBadgeSection = new JLabel("Milestones & Trophies");
        lblBadgeSection.setFont(ThemeConfig.FONT_TITLE);
        lblBadgeSection.setForeground(ThemeConfig.TEXT_PRIMARY);
        content.add(lblBadgeSection);
        content.add(Box.createVerticalStrut(12));

        pnlBadgesGrid.setLayout(new GridLayout(0, 3, 16, 16));
        pnlBadgesGrid.setOpaque(false);
        content.add(pnlBadgesGrid);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void loadData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                currentReminders = reminderDAO.getAllReminders();
                currentBadges = achievementDAO.getAllAchievementsWithUserStatus();
                return null;
            }

            @Override
            protected void done() {
                updateRemindersView();
                updateBadgesView();
            }
        };
        worker.execute();
    }

    private void updateRemindersView() {
        pnlRemindersList.removeAll();
        for (Reminder r : currentReminders) {
            JPanel row = new JPanel(new BorderLayout(12, 0));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(51, 65, 85, 60)),
                    BorderFactory.createEmptyBorder(8, 0, 8, 0)
            ));

            JCheckBox chk = new JCheckBox();
            chk.setSelected(r.isCompleted());
            chk.setOpaque(false);
            chk.addActionListener(e -> toggleReminderStatus(r, chk.isSelected()));
            row.add(chk, BorderLayout.WEST);

            JPanel center = new JPanel(new GridLayout(2, 1, 0, 2));
            center.setOpaque(false);
            JLabel title = new JLabel(r.getTitle());
            title.setFont(ThemeConfig.FONT_BODY_BOLD);
            title.setForeground(r.isCompleted() ? ThemeConfig.TEXT_MUTED : ThemeConfig.TEXT_PRIMARY);
            center.add(title);

            JLabel sub = new JLabel((r.getDescription() != null ? r.getDescription() : "") + " | Due: " +
                    DateUtil.formatDate(r.getReminderDate()) + " at " + DateUtil.formatTime(r.getReminderTime()));
            sub.setFont(ThemeConfig.FONT_SMALL);
            sub.setForeground(r.isDueOrOverdue() ? ThemeConfig.DANGER_ROSE : ThemeConfig.TEXT_MUTED);
            center.add(sub);
            row.add(center, BorderLayout.CENTER);

            JButton btnDel = new JButton("Delete");
            btnDel.setFont(ThemeConfig.FONT_SMALL);
            btnDel.setForeground(ThemeConfig.DANGER_ROSE);
            btnDel.setContentAreaFilled(false);
            btnDel.setBorderPainted(false);
            btnDel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnDel.addActionListener(e -> deleteReminder(r));
            row.add(btnDel, BorderLayout.EAST);

            pnlRemindersList.add(row);
        }

        if (currentReminders.isEmpty()) {
            JLabel lblEmpty = new JLabel("No reminders set. Click '+ Set Reminder' to stay on track!");
            lblEmpty.setFont(ThemeConfig.FONT_BODY);
            lblEmpty.setForeground(ThemeConfig.TEXT_MUTED);
            pnlRemindersList.add(lblEmpty);
        }

        pnlRemindersList.revalidate();
        pnlRemindersList.repaint();
    }

    private void updateBadgesView() {
        pnlBadgesGrid.removeAll();
        for (Achievement a : currentBadges) {
            ModernCard badgeCard = new ModernCard(14, new Insets(16, 16, 16, 16));
            badgeCard.setHoverEffect(true);

            if (a.isUnlocked()) {
                badgeCard.setBorderColor(new Color(245, 158, 11, 150));
            }

            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);

            Color iconColor = a.isUnlocked() ? ThemeConfig.WARNING_AMBER : ThemeConfig.TEXT_MUTED;
            JLabel icon = new JLabel(IconFactory.createIcon(IconFactory.IconType.BADGES, 26, iconColor));
            top.add(icon, BorderLayout.WEST);

            JLabel status = new JLabel(a.isUnlocked() ? "UNLOCKED 🎉" : "LOCKED 🔒");
            status.setFont(new Font("Segoe UI", Font.BOLD, 10));
            status.setForeground(a.isUnlocked() ? ThemeConfig.WARNING_AMBER : ThemeConfig.TEXT_MUTED);
            top.add(status, BorderLayout.EAST);
            badgeCard.add(top, BorderLayout.NORTH);

            JPanel center = new JPanel();
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.setOpaque(false);
            center.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

            JLabel name = new JLabel(a.getName());
            name.setFont(ThemeConfig.FONT_SUBTITLE);
            name.setForeground(a.isUnlocked() ? ThemeConfig.TEXT_PRIMARY : ThemeConfig.TEXT_SECONDARY);
            center.add(name);
            center.add(Box.createVerticalStrut(4));

            JLabel desc = new JLabel("<html>" + a.getDescription() + "</html>");
            desc.setFont(ThemeConfig.FONT_SMALL);
            desc.setForeground(ThemeConfig.TEXT_MUTED);
            center.add(desc);

            badgeCard.add(center, BorderLayout.CENTER);
            pnlBadgesGrid.add(badgeCard);
        }

        pnlBadgesGrid.revalidate();
        pnlBadgesGrid.repaint();
    }

    private void toggleReminderStatus(Reminder r, boolean completed) {
        r.setCompleted(completed);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                reminderDAO.updateReminder(r);
                return null;
            }
            @Override
            protected void done() {
                updateRemindersView();
            }
        };
        worker.execute();
    }

    private void deleteReminder(Reminder r) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                reminderDAO.deleteReminder(r.getId());
                return null;
            }
            @Override
            protected void done() {
                loadData();
            }
        };
        worker.execute();
    }

    private void openReminderDialog() {
        ReminderDialog dialog = new ReminderDialog(SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadData();
        }
    }

    // Modal Reminder Dialog
    private class ReminderDialog extends JDialog {
        private boolean saved = false;

        private final ModernTextField txtTitle = new ModernTextField("Evening Workout Session");
        private final ModernTextField txtDesc = new ModernTextField("Leg day workout & protein shake");
        private final ModernTextField txtDate = new ModernTextField(LocalDate.now().toString());
        private final ModernTextField txtTime = new ModernTextField("18:00");

        public ReminderDialog(Window owner) {
            super(owner, "Create New Reminder", ModalityType.APPLICATION_MODAL);
            setSize(420, 380);
            setLocationRelativeTo(owner);
            setResizable(false);
            getContentPane().setBackground(ThemeConfig.BG_DARK);

            JPanel main = new JPanel();
            main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
            main.setOpaque(false);
            main.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

            main.add(createFieldRow("Reminder Title*", txtTitle));
            main.add(Box.createVerticalStrut(10));
            main.add(createFieldRow("Description", txtDesc));
            main.add(Box.createVerticalStrut(10));

            JPanel r2 = new JPanel(new GridLayout(1, 2, 10, 0));
            r2.setOpaque(false);
            r2.add(createFieldRow("Date (YYYY-MM-DD)", txtDate));
            r2.add(createFieldRow("Time (HH:MM 24h)", txtTime));
            main.add(r2);
            main.add(Box.createVerticalStrut(20));

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            actions.setOpaque(false);
            ModernButton btnCancel = new ModernButton("Cancel", ModernButton.ButtonStyle.GHOST);
            btnCancel.addActionListener(e -> dispose());
            ModernButton btnSave = new ModernButton("Set Reminder", ModernButton.ButtonStyle.PRIMARY);
            btnSave.addActionListener(e -> saveReminder());
            actions.add(btnCancel);
            actions.add(btnSave);
            main.add(actions);

            setContentPane(main);
        }

        private JPanel createFieldRow(String label, JComponent comp) {
            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.setOpaque(false);
            JLabel l = new JLabel(label);
            l.setFont(ThemeConfig.FONT_SMALL);
            l.setForeground(ThemeConfig.TEXT_SECONDARY);
            p.add(l);
            p.add(Box.createVerticalStrut(4));
            p.add(comp);
            return p;
        }

        private void saveReminder() {
            String title = txtTitle.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a reminder title.");
                return;
            }

            LocalDate date = DateUtil.parseDateOrNull(txtDate.getText().trim());
            if (date == null) {
                JOptionPane.showMessageDialog(this, "Please enter a valid date.");
                return;
            }

            try {
                String[] parts = txtTime.getText().trim().split(":");
                LocalTime time = LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                Reminder r = new Reminder(title, txtDesc.getText().trim(), date, time);

                SwingWorker<Void, Void> worker = new SwingWorker<>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        reminderDAO.saveReminder(r);
                        return null;
                    }
                    @Override
                    protected void done() {
                        saved = true;
                        ToastNotification.showToast(ReminderBadgePanel.this, "Reminder created!", ToastNotification.ToastType.SUCCESS);
                        dispose();
                    }
                };
                worker.execute();

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Please enter time in HH:MM format (e.g. 18:30).");
            }
        }

        public boolean isSaved() {
            return saved;
        }
    }
}
