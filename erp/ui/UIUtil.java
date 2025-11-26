package edu.univ.erp.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class UIUtil {

    /* Track all tables to refresh after theme changes */
    private static final List<JTable> registeredTables = new ArrayList<>();

    /* Initialize look and feel */
    public static void initLookAndFeel(boolean dark, String fontName, int fontSize) {
        try {
            if (dark) FlatDarkLaf.setup();
            else FlatLightLaf.setup();

            UIManager.put("defaultFont", new Font(fontName, Font.PLAIN, fontSize));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /* Table styling + register for auto refresh */
    public static void decorateTable(JTable table) {
        table.setRowHeight(26);
        JTableHeader header = table.getTableHeader();
        header.setFont(header.getFont().deriveFont(Font.BOLD, 14f));

        // alternate row color
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);

                if (!isSelected) {
                    if (row % 2 == 0) c.setBackground(new Color(245, 245, 245));
                    else c.setBackground(new Color(235, 235, 235));
                }
                return c;
            }
        });

        registeredTables.add(table);
    }

    /* Refresh all registered tables after theme toggle */
    public static void refreshAllTables() {
        for (JTable t : registeredTables) SwingUtilities.updateComponentTreeUI(t);
    }

    /* Rounded button */
    public static JButton createRoundedButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    /* Ghost (transparent outline) button */
    public static JButton createGhostButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.putClientProperty("JButton.buttonType", "borderless");
        return b;
    }

    /* Simple list decorator */
    public static void decorateList(JList<?> list) {
        list.setFixedCellHeight(26);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }
}
