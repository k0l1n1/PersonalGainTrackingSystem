package com.personaltracker.components;

import com.personaltracker.config.ThemeConfig;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import java.awt.*;

/**
 * Modern JTable with dark styling, alternating rows, and comfortable padding.
 */
public class ModernTable extends JTable {
    private static final long serialVersionUID = 1L;

    public ModernTable(TableModel model) {
        super(model);
        setRowHeight(38);
        setShowVerticalLines(false);
        setShowHorizontalLines(true);
        setGridColor(ThemeConfig.BORDER_COLOR);
        Color selBg = ThemeConfig.isDarkMode()
                ? new Color(255, 255, 255, 30)
                : new Color(0x18, 0x18, 0x1B, 22);
        setSelectionBackground(selBg);
        setSelectionForeground(ThemeConfig.TEXT_PRIMARY);
        setBackground(ThemeConfig.BG_CARD);
        setForeground(ThemeConfig.TEXT_PRIMARY);
        setFont(ThemeConfig.FONT_BODY);

        JTableHeader header = getTableHeader();
        header.setBackground(ThemeConfig.isDarkMode() ? Color.decode("#252530") : Color.decode("#F1F3F5"));
        header.setForeground(ThemeConfig.isDarkMode() ? ThemeConfig.TEXT_ON_ACCENT : ThemeConfig.TEXT_SECONDARY);
        header.setFont(ThemeConfig.FONT_BODY_BOLD);
        header.setPreferredSize(new Dimension(header.getWidth(), 38));
        header.setReorderingAllowed(false);

        // Default cell renderer with padding
        setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? ThemeConfig.BG_CARD : ThemeConfig.BG_CARD_HOVER);
                    c.setForeground(ThemeConfig.TEXT_PRIMARY);
                }
                return c;
            }
        });
    }

    public static JScrollPane wrapInScrollPane(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER_COLOR, 1));
        scrollPane.getViewport().setBackground(ThemeConfig.BG_CARD);
        scrollPane.setBackground(ThemeConfig.BG_CARD);
        return scrollPane;
    }
}
