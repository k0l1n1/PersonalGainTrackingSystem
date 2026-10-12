package com.personaltracker.components;

import com.personaltracker.config.ThemeConfig;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
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
        setGridColor(new Color(0x35, 0x35, 0x35, 120));
        setSelectionBackground(new Color(0xF0, 0x44, 0x44, 75));
        setSelectionForeground(ThemeConfig.TEXT_PRIMARY);
        setBackground(ThemeConfig.BG_CARD);
        setForeground(ThemeConfig.TEXT_PRIMARY);
        setFont(ThemeConfig.FONT_BODY);

        JTableHeader header = getTableHeader();
        header.setBackground(ThemeConfig.BG_SIDEBAR);
        header.setForeground(ThemeConfig.TEXT_SECONDARY);
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
                    c.setBackground(row % 2 == 0 ? ThemeConfig.BG_CARD : new Color(0x1F, 0x1F, 0x1F));
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
