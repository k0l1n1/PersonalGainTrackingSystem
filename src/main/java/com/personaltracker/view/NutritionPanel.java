package com.personaltracker.view;

import com.personaltracker.components.ModernButton;
import com.personaltracker.components.ModernCard;
import com.personaltracker.components.ModernProgressBar;
import com.personaltracker.components.ModernTextField;
import com.personaltracker.components.ToastNotification;
import com.personaltracker.config.ThemeConfig;
import com.personaltracker.dao.AchievementDAO;
import com.personaltracker.dao.NutritionDAO;
import com.personaltracker.model.NutritionEntry;
import com.personaltracker.model.UserSession;
import com.personaltracker.service.SupabaseClient;
import com.personaltracker.util.DateUtil;
import java.awt.geom.Arc2D;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Nutrition and Calorie Tracking panel with macronutrient analysis.
 */
public class NutritionPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final NutritionDAO nutritionDAO = new NutritionDAO();
    private final AchievementDAO achievementDAO = new AchievementDAO();

    private LocalDate selectedDate = LocalDate.now();
    private final JLabel lblSelectedDate = new JLabel(DateUtil.formatDate(LocalDate.now()));
    private final JLabel lblMealsHead = new JLabel("Meals for " + DateUtil.formatDate(LocalDate.now()));

    // Calorie stats
    private final JLabel lblTotalCalories = new JLabel("0 kcal");
    private final JLabel lblCalorieTarget = new JLabel("Target: 2,000 kcal");
    private final ModernProgressBar barCalories = new ModernProgressBar(0, 2000);

    // Macro Color Palette
    public static final Color COLOR_PROTEIN = Color.decode("#F04444"); // Crimson Accent
    public static final Color COLOR_CARBS = Color.decode("#F59E0B");   // Warm Amber Gold
    public static final Color COLOR_FAT = Color.decode("#38BDF8");     // Sky Blue

    // Macros stats
    private final JLabel lblProtein = new JLabel("0.0 g  •  0%");
    private final JLabel lblCarbs = new JLabel("0.0 g  •  0%");
    private final JLabel lblFat = new JLabel("0.0 g  •  0%");

    // Donut chart
    private final MacroDonutChart macroDonutChart = new MacroDonutChart();

    // Meal section panels
    private final JPanel pnlBreakfast = new JPanel();
    private final JPanel pnlLunch = new JPanel();
    private final JPanel pnlDinner = new JPanel();
    private final JPanel pnlSnacks = new JPanel();

    private List<NutritionEntry> entriesForDate = new ArrayList<>();

    public NutritionPanel() {
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
        JLabel title = new JLabel("Nutrition & Calorie Tracker");
        title.setFont(ThemeConfig.FONT_TITLE_LARGE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        titlePanel.add(title);

        JLabel subtitle = new JLabel("Log meals, track daily calories & balance your macros");
        subtitle.setFont(ThemeConfig.FONT_BODY);
        subtitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        titlePanel.add(subtitle);
        header.add(titlePanel, BorderLayout.WEST);

        // Date navigator
        JPanel dateNav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        dateNav.setOpaque(false);

        ModernButton btnPrev = new ModernButton("< Prev", ModernButton.ButtonStyle.GHOST);
        btnPrev.addActionListener(e -> {
            selectedDate = selectedDate.minusDays(1);
            updateDateLabel();
            loadNutrition();
        });

        lblSelectedDate.setFont(ThemeConfig.FONT_BODY_BOLD);
        lblSelectedDate.setForeground(ThemeConfig.TEXT_PRIMARY);

        ModernButton btnNext = new ModernButton("Next >", ModernButton.ButtonStyle.GHOST);
        btnNext.addActionListener(e -> {
            selectedDate = selectedDate.plusDays(1);
            updateDateLabel();
            loadNutrition();
        });

        ModernButton btnToday = new ModernButton("Today", ModernButton.ButtonStyle.SECONDARY);
        btnToday.addActionListener(e -> {
            selectedDate = LocalDate.now();
            updateDateLabel();
            loadNutrition();
        });

        ModernButton btnAddMeal = new ModernButton("+ Log Food", ModernButton.ButtonStyle.PRIMARY);
        btnAddMeal.addActionListener(e -> openFoodDialog(null, "LUNCH"));

        dateNav.add(btnPrev);
        dateNav.add(lblSelectedDate);
        dateNav.add(btnNext);
        dateNav.add(btnToday);
        dateNav.add(Box.createHorizontalStrut(10));
        dateNav.add(btnAddMeal);
        header.add(dateNav, BorderLayout.EAST);

        content.add(header);
        content.add(Box.createVerticalStrut(18));

        // Middle Grid: Calorie Progress + Macro Pie Chart + Macro Stat cards
        JPanel summaryGrid = new JPanel(new GridLayout(1, 2, 16, 0));
        summaryGrid.setOpaque(false);
        summaryGrid.setPreferredSize(new Dimension(0, 210));
        summaryGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 210));

        // Calorie Card
        ModernCard cardCalories = new ModernCard(14, new Insets(16, 18, 16, 18));
        JLabel calTitle = new JLabel("DAILY CALORIE BUDGET");
        calTitle.setFont(ThemeConfig.FONT_SMALL);
        calTitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        cardCalories.add(calTitle, BorderLayout.NORTH);

        JPanel calCenter = new JPanel();
        calCenter.setLayout(new BoxLayout(calCenter, BoxLayout.Y_AXIS));
        calCenter.setOpaque(false);
        calCenter.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        lblTotalCalories.setFont(ThemeConfig.FONT_STAT_NUM);
        lblTotalCalories.setForeground(ThemeConfig.PRIMARY_ACCENT);
        calCenter.add(lblTotalCalories);
        calCenter.add(Box.createVerticalStrut(8));

        barCalories.setColors(ThemeConfig.PRIMARY_ACCENT, ThemeConfig.ACCENT_HOVER);
        calCenter.add(barCalories);
        calCenter.add(Box.createVerticalStrut(6));

        lblCalorieTarget.setFont(ThemeConfig.FONT_SMALL);
        lblCalorieTarget.setForeground(ThemeConfig.TEXT_MUTED);
        calCenter.add(lblCalorieTarget);
        cardCalories.add(calCenter, BorderLayout.CENTER);
        summaryGrid.add(cardCalories);

        // Macro Card with Chart
        ModernCard cardMacros = new ModernCard(14, new Insets(16, 18, 16, 18));
        JLabel macTitle = new JLabel("MACRONUTRIENT DISTRIBUTION");
        macTitle.setFont(ThemeConfig.FONT_SMALL);
        macTitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        cardMacros.add(macTitle, BorderLayout.NORTH);

        JPanel macCenter = new JPanel(new BorderLayout(16, 0));
        macCenter.setOpaque(false);

        macroDonutChart.setPreferredSize(new Dimension(140, 140));
        macCenter.add(macroDonutChart, BorderLayout.WEST);

        JPanel macStats = new JPanel(new GridLayout(3, 1, 0, 8));
        macStats.setOpaque(false);
        macStats.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        macStats.add(createMacroIndicator("Protein", lblProtein, COLOR_PROTEIN));
        macStats.add(createMacroIndicator("Carbohydrates", lblCarbs, COLOR_CARBS));
        macStats.add(createMacroIndicator("Healthy Fats", lblFat, COLOR_FAT));
        macCenter.add(macStats, BorderLayout.CENTER);

        cardMacros.add(macCenter, BorderLayout.CENTER);
        summaryGrid.add(cardMacros);

        content.add(summaryGrid);
        content.add(Box.createVerticalStrut(20));

        // Meal Categories Grid
        lblMealsHead.setFont(ThemeConfig.FONT_SUBTITLE);
        lblMealsHead.setForeground(ThemeConfig.TEXT_PRIMARY);
        content.add(lblMealsHead);
        content.add(Box.createVerticalStrut(10));

        JPanel mealsGrid = new JPanel(new GridLayout(2, 2, 16, 16));
        mealsGrid.setOpaque(false);

        mealsGrid.add(createMealCategoryCard("Breakfast", "BREAKFAST", pnlBreakfast));
        mealsGrid.add(createMealCategoryCard("Lunch", "LUNCH", pnlLunch));
        mealsGrid.add(createMealCategoryCard("Dinner", "DINNER", pnlDinner));
        mealsGrid.add(createMealCategoryCard("Snacks", "SNACK", pnlSnacks));

        content.add(mealsGrid);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void updateDateLabel() {
        String formatted = DateUtil.formatDate(selectedDate);
        lblSelectedDate.setText(formatted);
        lblMealsHead.setText("Meals for " + formatted);
    }

    private JPanel createMacroIndicator(String label, JLabel valLbl, Color color) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(0, 5, 8, 8);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(10, 18));
        left.add(dot);

        JLabel l = new JLabel(label);
        l.setFont(ThemeConfig.FONT_BODY);
        l.setForeground(ThemeConfig.TEXT_SECONDARY);
        left.add(l);
        p.add(left, BorderLayout.WEST);

        valLbl.setFont(ThemeConfig.FONT_BODY_BOLD);
        valLbl.setForeground(ThemeConfig.TEXT_PRIMARY);
        p.add(valLbl, BorderLayout.EAST);
        return p;
    }

    private ModernCard createMealCategoryCard(String title, String mealType, JPanel itemContainer) {
        ModernCard card = new ModernCard(14, new Insets(16, 18, 16, 18));
        card.setHoverEffect(true);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(ThemeConfig.FONT_SUBTITLE);
        lblTitle.setForeground(ThemeConfig.TEXT_PRIMARY);
        top.add(lblTitle, BorderLayout.WEST);

        ModernButton btnAdd = new ModernButton("+ Add", ModernButton.ButtonStyle.SECONDARY);
        btnAdd.setFont(ThemeConfig.FONT_SMALL);
        btnAdd.addActionListener(e -> openFoodDialog(null, mealType));
        top.add(btnAdd, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        itemContainer.setLayout(new BoxLayout(itemContainer, BoxLayout.Y_AXIS));
        itemContainer.setOpaque(false);
        itemContainer.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JScrollPane scroll = new JScrollPane(itemContainer);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(0, 140));
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    public void loadNutrition() {
        SwingWorker<List<NutritionEntry>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<NutritionEntry> doInBackground() throws Exception {
                return nutritionDAO.getEntriesForDate(selectedDate);
            }

            @Override
            protected void done() {
                try {
                    entriesForDate = get();
                    updateView();
                } catch (Exception e) {
                    System.err.println("Failed to load nutrition: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void updateView() {
        lblMealsHead.setText("Meals for " + DateUtil.formatDate(selectedDate));
        pnlBreakfast.removeAll();
        pnlLunch.removeAll();
        pnlDinner.removeAll();
        pnlSnacks.removeAll();

        int totalCal = 0;
        double totalProtein = 0;
        double totalCarbs = 0;
        double totalFat = 0;

        for (NutritionEntry entry : entriesForDate) {
            totalCal += entry.getCalories();
            totalProtein += entry.getProteinG();
            totalCarbs += entry.getCarbsG();
            totalFat += entry.getFatG();

            JPanel row = createFoodItemRow(entry);
            String mealType = entry.getMealType() != null ? entry.getMealType().toUpperCase() : "SNACK";
            switch (mealType) {
                case "BREAKFAST":
                    pnlBreakfast.add(row);
                    pnlBreakfast.add(Box.createVerticalStrut(6));
                    break;
                case "LUNCH":
                    pnlLunch.add(row);
                    pnlLunch.add(Box.createVerticalStrut(6));
                    break;
                case "DINNER":
                    pnlDinner.add(row);
                    pnlDinner.add(Box.createVerticalStrut(6));
                    break;
                case "SNACK":
                default:
                    pnlSnacks.add(row);
                    pnlSnacks.add(Box.createVerticalStrut(6));
                    break;
            }
        }

        checkEmptyState(pnlBreakfast, "No breakfast logged yet");
        checkEmptyState(pnlLunch, "No lunch logged yet");
        checkEmptyState(pnlDinner, "No dinner logged yet");
        checkEmptyState(pnlSnacks, "No snacks logged yet");

        pnlBreakfast.revalidate();
        pnlBreakfast.repaint();
        pnlLunch.revalidate();
        pnlLunch.repaint();
        pnlDinner.revalidate();
        pnlDinner.repaint();
        pnlSnacks.revalidate();
        pnlSnacks.repaint();

        // Update Calorie budget
        int calTarget = 2000;
        UserSession sess = SupabaseClient.getInstance().getCurrentSession();
        if (sess != null && sess.getProfile() != null && sess.getProfile().getDailyCalorieTarget() > 0) {
            calTarget = sess.getProfile().getDailyCalorieTarget();
        }
        barCalories.setMaximum(calTarget);
        barCalories.setValue(totalCal);
        lblTotalCalories.setText(totalCal + " kcal");
        int pct = (int) Math.round(((double) totalCal / calTarget) * 100);
        lblCalorieTarget.setText("Target: " + calTarget + " kcal (" + pct + "%)");

        // Update Macro labels & percentages
        double totalMacroKcal = (totalProtein * 4.0) + (totalCarbs * 4.0) + (totalFat * 9.0);
        if (totalMacroKcal > 0) {
            int pctP = (int) Math.round(((totalProtein * 4.0) / totalMacroKcal) * 100);
            int pctC = (int) Math.round(((totalCarbs * 4.0) / totalMacroKcal) * 100);
            int pctF = Math.max(0, 100 - pctP - pctC);
            lblProtein.setText(String.format("%.1f g  •  %d%%", totalProtein, pctP));
            lblCarbs.setText(String.format("%.1f g  •  %d%%", totalCarbs, pctC));
            lblFat.setText(String.format("%.1f g  •  %d%%", totalFat, pctF));
        } else {
            lblProtein.setText("0.0 g  •  0%");
            lblCarbs.setText("0.0 g  •  0%");
            lblFat.setText("0.0 g  •  0%");
        }

        // Update Macro Chart
        macroDonutChart.setData(totalProtein, totalCarbs, totalFat, totalCal);

        if (entriesForDate.size() >= 3) {
            new Thread(() -> {
                try {
                    achievementDAO.unlockAchievement("NUTRITION_TRACKER");
                } catch (Exception ignored) {}
            }).start();
        }
    }

    private void checkEmptyState(JPanel panel, String msg) {
        if (panel.getComponentCount() == 0) {
            JLabel lbl = new JLabel(msg);
            lbl.setFont(ThemeConfig.FONT_SMALL);
            lbl.setForeground(ThemeConfig.TEXT_MUTED);
            panel.add(lbl);
        }
    }

    private JPanel createFoodItemRow(NutritionEntry entry) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeConfig.BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 0, 4, 0)
        ));

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 2));
        left.setOpaque(false);

        JLabel name = new JLabel(entry.getFoodName() != null ? entry.getFoodName() : "Unnamed Food");
        name.setFont(ThemeConfig.FONT_BODY_BOLD);
        name.setForeground(ThemeConfig.TEXT_PRIMARY);
        left.add(name);

        String serving = (entry.getServingSize() != null && !entry.getServingSize().isBlank()) ? entry.getServingSize() + " • " : "";
        JLabel sub = new JLabel(String.format(java.util.Locale.US, "%sP: %.1fg  C: %.1fg  F: %.1fg", serving, entry.getProteinG(), entry.getCarbsG(), entry.getFatG()));
        sub.setFont(ThemeConfig.FONT_SMALL);
        sub.setForeground(ThemeConfig.TEXT_MUTED);
        left.add(sub);
        row.add(left, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);

        JLabel cal = new JLabel(entry.getCalories() + " kcal");
        cal.setFont(ThemeConfig.FONT_BODY_BOLD);
        cal.setForeground(ThemeConfig.WARNING);
        right.add(cal);

        JButton btnEdit = new JButton("✎");
        btnEdit.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnEdit.setForeground(ThemeConfig.TEXT_SECONDARY);
        btnEdit.setContentAreaFilled(false);
        btnEdit.setBorderPainted(false);
        btnEdit.setFocusPainted(false);
        btnEdit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEdit.setToolTipText("Edit meal");
        btnEdit.addActionListener(e -> openFoodDialog(entry, entry.getMealType()));
        right.add(btnEdit);

        JButton btnDel = new JButton("×");
        btnDel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnDel.setForeground(ThemeConfig.TEXT_MUTED);
        btnDel.setContentAreaFilled(false);
        btnDel.setBorderPainted(false);
        btnDel.setFocusPainted(false);
        btnDel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnDel.addActionListener(e -> deleteEntry(entry));
        right.add(btnDel);

        row.add(right, BorderLayout.EAST);
        return row;
    }

    /**
     * Custom minimal anti-aliased macronutrient donut chart.
     * Preserves a 1:1 circular ratio without distortion and displays center calorie summary.
     */
    private static class MacroDonutChart extends JComponent {
        private static final long serialVersionUID = 1L;

        private double proteinKcal = 0;
        private double carbsKcal = 0;
        private double fatKcal = 0;
        private int totalCalories = 0;

        public MacroDonutChart() {
            setOpaque(false);
            setPreferredSize(new Dimension(140, 140));
            setMinimumSize(new Dimension(120, 120));
        }

        public void setData(double pGrams, double cGrams, double fGrams, int totalCal) {
            this.proteinKcal = Math.max(0, pGrams * 4.0);
            this.carbsKcal = Math.max(0, cGrams * 4.0);
            this.fatKcal = Math.max(0, fGrams * 9.0);
            this.totalCalories = totalCal;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            int w = getWidth();
            int h = getHeight();
            int diameter = Math.min(w, h) - 18;
            if (diameter < 20) {
                g2.dispose();
                return;
            }

            int x = (w - diameter) / 2;
            int y = (h - diameter) / 2;
            int strokeWidth = 14;

            // Background subtle track
            g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(ThemeConfig.BORDER_COLOR);
            g2.drawOval(x + strokeWidth / 2, y + strokeWidth / 2, diameter - strokeWidth, diameter - strokeWidth);

            double totalKcal = proteinKcal + carbsKcal + fatKcal;
            if (totalKcal > 0) {
                double pExtent = (proteinKcal / totalKcal) * 360.0;
                double cExtent = (carbsKcal / totalKcal) * 360.0;
                double fExtent = 360.0 - pExtent - cExtent;

                double startAngle = 90.0; // Start at 12 o'clock
                double gap = 3.0; // Minimal elegant gap between arcs

                // 1. Protein Arc
                if (pExtent > gap) {
                    g2.setColor(COLOR_PROTEIN);
                    g2.draw(new Arc2D.Double(x + strokeWidth / 2.0, y + strokeWidth / 2.0,
                            diameter - strokeWidth, diameter - strokeWidth,
                            startAngle - pExtent + (gap / 2.0), pExtent - gap, Arc2D.OPEN));
                }
                startAngle -= pExtent;

                // 2. Carbs Arc
                if (cExtent > gap) {
                    g2.setColor(COLOR_CARBS);
                    g2.draw(new Arc2D.Double(x + strokeWidth / 2.0, y + strokeWidth / 2.0,
                            diameter - strokeWidth, diameter - strokeWidth,
                            startAngle - cExtent + (gap / 2.0), cExtent - gap, Arc2D.OPEN));
                }
                startAngle -= cExtent;

                // 3. Fats Arc
                if (fExtent > gap) {
                    g2.setColor(COLOR_FAT);
                    g2.draw(new Arc2D.Double(x + strokeWidth / 2.0, y + strokeWidth / 2.0,
                            diameter - strokeWidth, diameter - strokeWidth,
                            startAngle - fExtent + (gap / 2.0), fExtent - gap, Arc2D.OPEN));
                }
            }

            // Center Text: Total Calories
            Font numFont = new Font("Segoe UI", Font.BOLD, 17);
            Font lblFont = new Font("Segoe UI", Font.BOLD, 10);

            String calStr = String.valueOf(totalCalories);
            String unitStr = "kcal";

            FontMetrics fmNum = g2.getFontMetrics(numFont);
            FontMetrics fmLbl = g2.getFontMetrics(lblFont);

            int totalTextHeight = fmNum.getAscent() + fmLbl.getAscent() + 2;
            int textYStart = (h - totalTextHeight) / 2 + fmNum.getAscent() - 2;

            g2.setFont(numFont);
            g2.setColor(ThemeConfig.TEXT_PRIMARY);
            int calX = (w - fmNum.stringWidth(calStr)) / 2;
            g2.drawString(calStr, calX, textYStart);

            g2.setFont(lblFont);
            g2.setColor(ThemeConfig.TEXT_MUTED);
            int unitX = (w - fmLbl.stringWidth(unitStr)) / 2;
            g2.drawString(unitStr, unitX, textYStart + fmLbl.getAscent() + 2);

            g2.dispose();
        }
    }

    private void openFoodDialog(NutritionEntry editing, String defaultMeal) {
        FoodDialog dialog = new FoodDialog(SwingUtilities.getWindowAncestor(this), editing, defaultMeal);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            ToastNotification.showToast(this, editing == null ? "Meal logged successfully!" : "Meal updated.", ToastNotification.ToastType.SUCCESS);
            loadNutrition();
        }
    }

    private void deleteEntry(NutritionEntry entry) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                nutritionDAO.deleteEntry(entry.getId());
                return null;
            }
            @Override
            protected void done() {
                ToastNotification.showToast(NutritionPanel.this, "Meal removed.", ToastNotification.ToastType.INFO);
                loadNutrition();
            }
        };
        worker.execute();
    }

    // Modal Add Food Dialog
    private class FoodDialog extends JDialog {
        private static final long serialVersionUID = 1L;
        private boolean saved = false;
        private final NutritionEntry editing;

        private final ModernTextField txtFood = new ModernTextField("", "e.g. Grilled Salmon Fillet");
        private final JComboBox<String> cmbMeal = new JComboBox<>(new String[]{"BREAKFAST", "LUNCH", "DINNER", "SNACK"});
        private final ModernTextField txtServing = new ModernTextField("", "e.g. 1 fillet (150g)");
        private final ModernTextField txtCalories = new ModernTextField("", "e.g. 320");
        private final ModernTextField txtProtein = new ModernTextField("", "e.g. 34.0");
        private final ModernTextField txtCarbs = new ModernTextField("", "e.g. 0.0");
        private final ModernTextField txtFat = new ModernTextField("", "e.g. 18.0");

        public FoodDialog(Window owner, NutritionEntry entry, String defaultMeal) {
            super(owner, entry == null ? "Log Food / Meal Item (" + DateUtil.formatDate(selectedDate) + ")" : "Edit Meal", ModalityType.APPLICATION_MODAL);
            this.editing = entry;
            setSize(490, 620);
            setLocationRelativeTo(owner);
            setResizable(false);
            getContentPane().setBackground(ThemeConfig.BG_DARK);

            JPanel main = new JPanel();
            main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
            main.setOpaque(false);
            main.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

            // Section 1: Quick Common Food Preset Chips
            if (entry == null) {
                JLabel lblQuick = new JLabel("Quick Meal Presets");
                lblQuick.setFont(ThemeConfig.FONT_SMALL);
                lblQuick.setForeground(ThemeConfig.TEXT_SECONDARY);
                main.add(lblQuick);
                main.add(Box.createVerticalStrut(4));

                JPanel presetRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
                presetRow.setOpaque(false);
                presetRow.add(createFoodPresetChip("3 Eggs & Toast", "BREAKFAST", "3 eggs, 2 bread", 350, 24, 28, 16));
                presetRow.add(createFoodPresetChip("Chicken & Rice", "LUNCH", "200g chicken, rice", 520, 45, 60, 10));
                presetRow.add(createFoodPresetChip("Whey Shake", "SNACK", "1 scoop in water", 160, 30, 4, 2));
                presetRow.add(createFoodPresetChip("Steak & Greens", "DINNER", "250g sirloin, salad", 480, 42, 6, 32));
                main.add(presetRow);
                main.add(Box.createVerticalStrut(12));
            }

            // Food Name
            main.add(createFieldRow("Food Name *", txtFood));
            main.add(Box.createVerticalStrut(10));

            // Meal Category & Serving
            JPanel r2 = new JPanel(new GridLayout(1, 2, 10, 0));
            r2.setOpaque(false);
            r2.add(createFieldRow("Meal Category", cmbMeal));
            r2.add(createFieldRow("Serving Quantity", txtServing));
            main.add(r2);
            main.add(Box.createVerticalStrut(10));

            // Calories & Protein
            JPanel r3 = new JPanel(new GridLayout(1, 2, 10, 0));
            r3.setOpaque(false);
            r3.add(createFieldRow("Calories (kcal) *", txtCalories));
            r3.add(createFieldRow("Protein (g)", txtProtein));
            main.add(r3);
            main.add(Box.createVerticalStrut(10));

            // Carbs & Fat
            JPanel r4 = new JPanel(new GridLayout(1, 2, 10, 0));
            r4.setOpaque(false);
            r4.add(createFieldRow("Carbohydrates (g)", txtCarbs));
            r4.add(createFieldRow("Fat (g)", txtFat));
            main.add(r4);
            main.add(Box.createVerticalStrut(12));

            // Live Macro Summary Card
            JPanel macroBox = new JPanel(new BorderLayout(8, 4));
            macroBox.setBackground(ThemeConfig.BG_CARD);
            macroBox.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
            JLabel macroTag = new JLabel("LIVE MACRO CALCULATION");
            macroTag.setFont(new Font("Segoe UI", Font.BOLD, 10));
            macroTag.setForeground(ThemeConfig.TEXT_SECONDARY);
            lblMacroLive.setFont(ThemeConfig.FONT_SMALL);
            lblMacroLive.setForeground(ThemeConfig.PRIMARY_ACCENT);
            macroBox.add(macroTag, BorderLayout.NORTH);
            macroBox.add(lblMacroLive, BorderLayout.CENTER);
            main.add(macroBox);

            main.add(Box.createVerticalGlue());
            main.add(Box.createVerticalStrut(16));

            // Action Buttons
            JPanel actions = new JPanel(new BorderLayout());
            actions.setOpaque(false);
            JLabel lblHint = new JLabel("Tip: Press Enter to save");
            lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            lblHint.setForeground(ThemeConfig.TEXT_MUTED);
            actions.add(lblHint, BorderLayout.WEST);

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            btnRow.setOpaque(false);
            ModernButton btnCancel = new ModernButton("Cancel", ModernButton.ButtonStyle.GHOST);
            btnCancel.addActionListener(e -> dispose());
            ModernButton btnSave = new ModernButton(entry == null ? "Save Food Entry" : "Update Food Entry", ModernButton.ButtonStyle.PRIMARY);
            btnSave.addActionListener(e -> saveFoodData());
            btnRow.add(btnCancel);
            btnRow.add(btnSave);
            actions.add(btnRow, BorderLayout.EAST);
            main.add(actions);

            setContentPane(main);

            // Document listeners for live macro analysis
            DocumentListener macroListener = new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { updateLiveMacroSummary(); }
                @Override public void removeUpdate(DocumentEvent e) { updateLiveMacroSummary(); }
                @Override public void changedUpdate(DocumentEvent e) { updateLiveMacroSummary(); }
            };
            txtProtein.getDocument().addDocumentListener(macroListener);
            txtCarbs.getDocument().addDocumentListener(macroListener);
            txtFat.getDocument().addDocumentListener(macroListener);
            txtCalories.getDocument().addDocumentListener(macroListener);
            updateLiveMacroSummary();

            // Shortcuts
            txtFood.addActionListener(e -> saveFoodData());
            txtServing.addActionListener(e -> saveFoodData());
            txtCalories.addActionListener(e -> saveFoodData());
            txtProtein.addActionListener(e -> saveFoodData());
            txtCarbs.addActionListener(e -> saveFoodData());
            txtFat.addActionListener(e -> saveFoodData());

            getRootPane().registerKeyboardAction(e -> dispose(),
                    KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);

            if (entry != null) {
                txtFood.setText(entry.getFoodName());
                cmbMeal.setSelectedItem(entry.getMealType());
                txtServing.setText(entry.getServingSize() != null ? entry.getServingSize() : "");
                txtCalories.setText(String.valueOf(entry.getCalories()));
                txtProtein.setText(String.valueOf(entry.getProteinG()));
                txtCarbs.setText(String.valueOf(entry.getCarbsG()));
                txtFat.setText(String.valueOf(entry.getFatG()));
                updateLiveMacroSummary();
            } else if (defaultMeal != null) {
                cmbMeal.setSelectedItem(defaultMeal);
            }

            SwingUtilities.invokeLater(() -> {
                txtFood.requestFocusInWindow();
                txtFood.selectAll();
            });
        }

        private final JLabel lblMacroLive = new JLabel("P: 0g | C: 0g | F: 0g");

        private JButton createFoodPresetChip(String label, String meal, String serving, int cal, double p, double c, double f) {
            JButton btn = new JButton(label);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btn.setForeground(ThemeConfig.TEXT_PRIMARY);
            btn.setBackground(ThemeConfig.BG_CARD);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));
            btn.addActionListener(e -> {
                txtFood.setText(label.replaceAll("^[^a-zA-Z0-9]+", "").trim());
                cmbMeal.setSelectedItem(meal);
                txtServing.setText(serving);
                txtCalories.setText(String.valueOf(cal));
                txtProtein.setText(String.valueOf(p));
                txtCarbs.setText(String.valueOf(c));
                txtFat.setText(String.valueOf(f));
                updateLiveMacroSummary();
            });
            return btn;
        }

        private void updateLiveMacroSummary() {
            try {
                double p = txtProtein.getText().trim().isEmpty() ? 0 : Double.parseDouble(txtProtein.getText().trim());
                double c = txtCarbs.getText().trim().isEmpty() ? 0 : Double.parseDouble(txtCarbs.getText().trim());
                double f = txtFat.getText().trim().isEmpty() ? 0 : Double.parseDouble(txtFat.getText().trim());
                double calcCal = (p * 4) + (c * 4) + (f * 9);
                lblMacroLive.setText(String.format("Calculated from macros: %.0f kcal  (P: %.0fg | C: %.0fg | F: %.0fg)", calcCal, p, c, f));
            } catch (Exception ignored) {
                lblMacroLive.setText("Enter macros to view summary");
            }
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

        private void saveFoodData() {
            String name = txtFood.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a food name.");
                return;
            }

            int parsedCal;
            double parsedP = 0;
            double parsedC = 0;
            double parsedF = 0;
            try {
                parsedCal = Integer.parseInt(txtCalories.getText().trim());
                if (!txtProtein.getText().trim().isEmpty()) parsedP = Double.parseDouble(txtProtein.getText().trim());
                if (!txtCarbs.getText().trim().isEmpty()) parsedC = Double.parseDouble(txtCarbs.getText().trim());
                if (!txtFat.getText().trim().isEmpty()) parsedF = Double.parseDouble(txtFat.getText().trim());
                if (parsedCal < 0 || parsedP < 0 || parsedC < 0 || parsedF < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter valid non-negative numbers for calories and macros.");
                return;
            }

            final int cal = parsedCal;
            final double p = parsedP;
            final double c = parsedC;
            final double f = parsedF;

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    if (editing != null) {
                        editing.setFoodName(name);
                        editing.setMealType((String) cmbMeal.getSelectedItem());
                        editing.setServingSize(txtServing.getText().trim());
                        editing.setCalories(cal);
                        editing.setProteinG(p);
                        editing.setCarbsG(c);
                        editing.setFatG(f);
                        nutritionDAO.updateEntry(editing);
                    } else {
                        NutritionEntry newEntry = new NutritionEntry(name, (String) cmbMeal.getSelectedItem(), cal, p, c, f, selectedDate);
                        newEntry.setServingSize(txtServing.getText().trim());
                        nutritionDAO.saveEntry(newEntry);
                    }
                    return null;
                }
                @Override
                protected void done() {
                    try {
                        get();
                        saved = true;
                        ToastNotification.showToast(NutritionPanel.this, editing != null ? "Food updated!" : "Food logged!", ToastNotification.ToastType.SUCCESS);
                        dispose();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(FoodDialog.this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }

        public boolean isSaved() {
            return saved;
        }
    }
}
