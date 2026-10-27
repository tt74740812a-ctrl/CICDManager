package com.doan.cicdmanager.ui;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

public class LogView extends JScrollPane {

    private final JTextArea area = new JTextArea();
    private final Highlighter.HighlightPainter painter =
            new DefaultHighlighter.DefaultHighlightPainter(new Color(176, 124, 0));

    public LogView() {
        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 13));
        area.setBackground(new Color(30, 30, 30));
        area.setForeground(new Color(220, 220, 220));
        area.setCaretColor(Color.WHITE);
        setViewportView(area);
        setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
    }

    /** Hiển thị nội dung log và tô vàng mọi vị trí khớp từ khóa (không phân biệt hoa thường). */
    public void showText(String text, String keyword) {
        area.getHighlighter().removeAllHighlights();
        area.setText(text == null ? "" : text);
        area.setCaretPosition(0);
        if (text != null && keyword != null && !keyword.trim().isEmpty()) {
            highlight(text, keyword.trim());
        }
    }

    private void highlight(String text, String keyword) {
        String lowerText = text.toLowerCase();
        String lowerKey = keyword.toLowerCase();
        int first = -1;
        int index = lowerText.indexOf(lowerKey);
        while (index >= 0) {
            try {
                area.getHighlighter().addHighlight(index, index + lowerKey.length(), painter);
            } catch (BadLocationException e) {
                break;
            }
            if (first < 0) {
                first = index;
            }
            index = lowerText.indexOf(lowerKey, index + lowerKey.length());
        }
        if (first >= 0) {
            area.setCaretPosition(first); // cuộn tới vị trí khớp đầu tiên
        }
    }
}