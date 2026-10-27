package com.doan.cicdmanager.ui;

import com.doan.cicdmanager.model.RunStatus;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

public class StatusCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        if (value instanceof RunStatus) {
            RunStatus status = (RunStatus) value;
            label.setText(status.name());
            label.setOpaque(true);
            label.setBackground(UiTheme.statusColor(status));
            label.setForeground(Color.WHITE);
            label.setFont(label.getFont().deriveFont(Font.BOLD));
        }
        return label;
    }
}