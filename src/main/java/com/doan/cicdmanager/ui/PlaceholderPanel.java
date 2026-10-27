package com.doan.cicdmanager.ui;

import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class PlaceholderPanel extends JPanel {

    public PlaceholderPanel(String title, String note) {
        setLayout(new GridLayout(2, 1));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(120, 20, 120, 20));

        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(UiTheme.FONT_TITLE);
        lblTitle.setForeground(UiTheme.PRIMARY);

        JLabel lblNote = new JLabel(note, SwingConstants.CENTER);
        lblNote.setFont(UiTheme.FONT_NORMAL);
        lblNote.setForeground(UiTheme.MUTED);

        add(lblTitle);
        add(lblNote);
    }
}