package com.doan.cicdmanager.ui;

import com.doan.cicdmanager.model.RunStatus;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

public final class UiTheme {

    public static final Color PRIMARY = new Color(30, 58, 95);
    public static final Color PRIMARY_DARK = new Color(21, 41, 68);
    public static final Color PRIMARY_LIGHT = new Color(52, 86, 133);
    public static final Color BACKGROUND = new Color(244, 246, 249);
    public static final Color BORDER = new Color(225, 229, 234);

    public static final Color SUCCESS = new Color(39, 174, 96);
    public static final Color DANGER = new Color(192, 57, 43);
    public static final Color WARNING = new Color(230, 145, 20);
    public static final Color INFO = new Color(41, 128, 185);
    public static final Color MUTED = new Color(110, 122, 132);

    public static final Font FONT_NORMAL = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_BIG_NUMBER = new Font("Segoe UI", Font.BOLD, 30);

    private UiTheme() {
    }

    /** Màu đại diện cho từng trạng thái. */
    public static Color statusColor(RunStatus status) {
        switch (status) {
            case SUCCESS:
                return SUCCESS;
            case FAILED:
                return DANGER;
            case RUNNING:
                return INFO;
            case WAITING:
                return WARNING;
            default:
                return MUTED;
        }
    }

    /** Áp dụng kiểu giao diện chung cho JTable. */
    public static void styleTable(JTable table) {
        table.setFont(FONT_NORMAL);
        table.setRowHeight(30);
        table.setGridColor(BORDER);
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(214, 228, 245));
        table.setSelectionForeground(Color.BLACK);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 34));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(tbl, value, false, false, row, column);
                label.setOpaque(true);
                label.setBackground(PRIMARY);
                label.setForeground(Color.WHITE);
                label.setFont(FONT_BOLD);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return label;
            }
        });
    }
}