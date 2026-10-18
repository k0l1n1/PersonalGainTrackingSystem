package com.personaltracker.view;

import com.personaltracker.config.ThemeConfig;
import com.personaltracker.model.UserSession;
import com.personaltracker.service.SupabaseClient;
import com.personaltracker.util.IconFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modern fixed sidebar navigation panel.
 */
public class SidebarPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    public enum NavItem {
        DASHBOARD("Dashboard", IconFactory.IconType.DASHBOARD),
        STUDENTS("My Students", IconFactory.IconType.STUDENTS),
        WORKOUTS("Workouts", IconFactory.IconType.WORKOUT),
        GOALS("Goals", IconFactory.IconType.GOAL),
        MEASUREMENTS("Body & Weight", IconFactory.IconType.MEASUREMENT),
        NUTRITION("Nutrition", IconFactory.IconType.NUTRITION),
        WATER_SLEEP("Water & Sleep", IconFactory.IconType.WATER),
        ANALYTICS("Analytics & Reports", IconFactory.IconType.ANALYTICS),
        SETTINGS("Profile & Settings", IconFactory.IconType.SETTINGS);

        private final String label;
        private final IconFactory.IconType iconType;

        NavItem(String label, IconFactory.IconType iconType) {
            this.label = label;
            this.iconType = iconType;
        }

        public String getLabel() { return label; }
        public IconFactory.IconType getIconType() { return iconType; }
    }

    private final List<NavButton> navButtons = new ArrayList<>();
    private final JPanel menuPanel = new JPanel();
    private NavItem activeItem = NavItem.DASHBOARD;
    private Consumer<NavItem> onNavigationListener;
    private Runnable onLogoutListener;

    private final JLabel lblUserName;
    private final JLabel lblUserEmail;

    public SidebarPanel() {
        setPreferredSize(new Dimension(240, 0));
        setBackground(ThemeConfig.BG_SIDEBAR);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, ThemeConfig.BORDER_COLOR));

        // Brand Header
        JPanel brandPanel = new JPanel(new BorderLayout(10, 0));
        brandPanel.setOpaque(false);
        brandPanel.setBorder(BorderFactory.createEmptyBorder(22, 20, 20, 20));

        JLabel brandIcon = new JLabel(IconFactory.createIcon(IconFactory.IconType.WORKOUT, 28, ThemeConfig.PRIMARY_ACCENT));
        brandPanel.add(brandIcon, BorderLayout.WEST);

        JPanel brandTextPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        brandTextPanel.setOpaque(false);
        JLabel title = new JLabel("PERSONAL GAIN");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        brandTextPanel.add(title);

        JLabel subtitle = new JLabel("Fitness & Health Tracker");
        subtitle.setFont(ThemeConfig.FONT_SMALL);
        subtitle.setForeground(ThemeConfig.TEXT_MUTED);
        brandTextPanel.add(subtitle);

        brandPanel.add(brandTextPanel, BorderLayout.CENTER);
        add(brandPanel, BorderLayout.NORTH);

        // Center: Navigation menu
        menuPanel.setOpaque(false);
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        refreshNavItems();

        JScrollPane scrollMenu = new JScrollPane(menuPanel);
        scrollMenu.setOpaque(false);
        scrollMenu.getViewport().setOpaque(false);
        scrollMenu.setBorder(null);
        scrollMenu.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollMenu, BorderLayout.CENTER);

        // Bottom: User profile & Logout
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 18, 16));

        JSeparator sep = new JSeparator();
        sep.setForeground(ThemeConfig.BORDER_COLOR);
        sep.setBackground(ThemeConfig.BORDER_COLOR);
        bottomPanel.add(sep);
        bottomPanel.add(Box.createVerticalStrut(12));

        JPanel userRow = new JPanel(new BorderLayout(10, 0));
        userRow.setOpaque(false);

        JLabel avatarLbl = new JLabel(IconFactory.createIcon(IconFactory.IconType.USER, 22, ThemeConfig.PRIMARY_ACCENT));
        userRow.add(avatarLbl, BorderLayout.WEST);

        JPanel userInfoPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        userInfoPanel.setOpaque(false);
        lblUserName = new JLabel("Athlete");
        lblUserName.setFont(ThemeConfig.FONT_BODY_BOLD);
        lblUserName.setForeground(ThemeConfig.TEXT_PRIMARY);
        userInfoPanel.add(lblUserName);

        lblUserEmail = new JLabel("athlete@demo.com");
        lblUserEmail.setFont(ThemeConfig.FONT_SMALL);
        lblUserEmail.setForeground(ThemeConfig.TEXT_MUTED);
        userInfoPanel.add(lblUserEmail);

        userRow.add(userInfoPanel, BorderLayout.CENTER);
        bottomPanel.add(userRow);

        bottomPanel.add(Box.createVerticalStrut(10));

        JButton btnLogout = new JButton("Sign Out", IconFactory.createIcon(IconFactory.IconType.LOGOUT, 16, ThemeConfig.ERROR));
        btnLogout.setFont(ThemeConfig.FONT_SMALL);
        btnLogout.setForeground(ThemeConfig.ERROR);
        btnLogout.setContentAreaFilled(false);
        btnLogout.setBorderPainted(false);
        btnLogout.setFocusPainted(false);
        btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogout.setHorizontalAlignment(SwingConstants.LEFT);
        btnLogout.addActionListener(e -> {
            if (onLogoutListener != null) onLogoutListener.run();
        });
        bottomPanel.add(btnLogout);

        add(bottomPanel, BorderLayout.SOUTH);

        updateActiveButton();
        updateUserInfo();
    }

    public void setOnNavigationListener(Consumer<NavItem> listener) {
        this.onNavigationListener = listener;
    }

    public void setOnLogoutListener(Runnable listener) {
        this.onLogoutListener = listener;
    }

    public void setActiveItem(NavItem item) {
        this.activeItem = item;
        updateActiveButton();
    }

    public void refreshNavItems() {
        menuPanel.removeAll();
        navButtons.clear();

        UserSession session = SupabaseClient.getInstance().getCurrentSession();
        boolean isTrainer = session != null && session.getProfile() != null && session.getProfile().isTrainer();

        for (NavItem item : NavItem.values()) {
            if (item == NavItem.STUDENTS && !isTrainer) {
                continue; // Do not show trainer students tab for normal members
            }
            NavButton btn = new NavButton(item);
            navButtons.add(btn);
            menuPanel.add(btn);
            menuPanel.add(Box.createVerticalStrut(4));
        }

        updateActiveButton();
        menuPanel.revalidate();
        menuPanel.repaint();
    }

    public void updateUserInfo() {
        UserSession session = SupabaseClient.getInstance().getCurrentSession();
        if (session != null && session.getProfile() != null) {
            String roleTag = session.getProfile().isTrainer() ? " (Coach)" : "";
            lblUserName.setText(session.getProfile().getDisplayName() + roleTag);
            lblUserEmail.setText(session.getEmail() != null ? session.getEmail() : "");
        } else {
            lblUserName.setText("Guest");
            lblUserEmail.setText("Not signed in");
        }
        refreshNavItems();
    }

    private void updateActiveButton() {
        for (NavButton btn : navButtons) {
            btn.setActive(btn.item == activeItem);
        }
    }

    private class NavButton extends JPanel {
        private static final long serialVersionUID = 1L;
        private final NavItem item;
        private boolean isActive = false;
        private boolean isHovered = false;

        public NavButton(NavItem item) {
            this.item = item;
            setOpaque(false);
            setLayout(new BorderLayout(12, 0));
            setPreferredSize(new Dimension(216, 40));
            setMaximumSize(new Dimension(216, 40));
            setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel iconLbl = new JLabel(IconFactory.createIcon(item.getIconType(), 18, ThemeConfig.TEXT_SECONDARY)) {
                @Override
                public Icon getIcon() {
                    Color c = isActive ? ThemeConfig.PRIMARY_ACCENT : (isHovered ? ThemeConfig.TEXT_PRIMARY : ThemeConfig.TEXT_SECONDARY);
                    return IconFactory.createIcon(item.getIconType(), 18, c);
                }
            };
            add(iconLbl, BorderLayout.WEST);

            JLabel labelLbl = new JLabel(item.getLabel()) {
                @Override
                public Color getForeground() {
                    return isActive ? ThemeConfig.TEXT_PRIMARY : (isHovered ? ThemeConfig.TEXT_PRIMARY : ThemeConfig.TEXT_SECONDARY);
                }
            };
            labelLbl.setFont(ThemeConfig.FONT_BODY);
            add(labelLbl, BorderLayout.CENTER);

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

                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    setActiveItem(item);
                    if (onNavigationListener != null) {
                        onNavigationListener.accept(item);
                    }
                }
            });
        }

        public void setActive(boolean active) {
            this.isActive = active;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (isActive) {
                g2.setColor(new Color(0xF0, 0x44, 0x44, 38));
                g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 8, 8));

                // Left accent bar
                g2.setColor(ThemeConfig.PRIMARY_ACCENT);
                g2.fill(new RoundRectangle2D.Float(0, 8, 4, h - 16, 2, 2));
            } else if (isHovered) {
                g2.setColor(new Color(255, 255, 255, 12));
                g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 8, 8));
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
