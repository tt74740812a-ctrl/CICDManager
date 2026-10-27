package com.doan.cicdmanager.ui;

import com.doan.cicdmanager.service.AuthService;
import com.doan.cicdmanager.service.ServiceException;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingWorker;

public class ChangePasswordDialog extends JDialog {

    private final AuthService authService = new AuthService();

    private final JPasswordField txtCurrent = new JPasswordField(20);
    private final JPasswordField txtNew = new JPasswordField(20);
    private final JPasswordField txtConfirm = new JPasswordField(20);
    private final JCheckBox chkShow = new JCheckBox("Hiện mật khẩu");
    private final JLabel lblError = new JLabel(" ");
    private final JButton btnSave = UiFactory.button("Đổi mật khẩu", UiTheme.SUCCESS);
    private final char defaultEchoChar;

    public ChangePasswordDialog(Window owner) {
        super(owner, "Đổi mật khẩu", ModalityType.APPLICATION_MODAL);
        defaultEchoChar = txtCurrent.getEchoChar();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUi();
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void buildUi() {
        for (JPasswordField f : new JPasswordField[]{txtCurrent, txtNew, txtConfirm}) {
            f.setFont(UiTheme.FONT_NORMAL);
        }
        chkShow.setFont(UiTheme.FONT_NORMAL);
        chkShow.addActionListener(e -> {
            char echo = chkShow.isSelected() ? (char) 0 : defaultEchoChar;
            txtCurrent.setEchoChar(echo);
            txtNew.setEchoChar(echo);
            txtConfirm.setEchoChar(echo);
        });

        JLabel hint = new JLabel("<html>Mật khẩu mới: 8 đến 64 ký tự, có cả chữ và số,<br>"
                + "không chứa tên đăng nhập.</html>");
        hint.setFont(UiTheme.FONT_NORMAL);
        hint.setForeground(UiTheme.MUTED);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        addRow(form, 0, "Mật khẩu hiện tại", txtCurrent);
        addRow(form, 1, "Mật khẩu mới", txtNew);
        addRow(form, 2, "Nhập lại mật khẩu mới", txtConfirm);
        addRow(form, 3, "", chkShow);
        addRow(form, 4, "", hint);

        lblError.setForeground(UiTheme.DANGER);
        lblError.setFont(UiTheme.FONT_NORMAL);
        lblError.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));

        btnSave.addActionListener(e -> onSave());
        JButton btnCancel = UiFactory.button("Hủy", UiTheme.MUTED);
        btnCancel.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.add(btnSave);
        buttons.add(btnCancel);

        JPanel south = new JPanel(new BorderLayout());
        south.add(lblError, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnSave);
    }

    private void addRow(JPanel panel, int row, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 4, 6, 4);
        c.gridy = row;
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.weightx = 0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(UiTheme.FONT_NORMAL);
        panel.add(lbl, c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, c);
    }

    private void onSave() {
        final String current = new String(txtCurrent.getPassword());
        final String newPass = new String(txtNew.getPassword());
        final String confirm = new String(txtConfirm.getPassword());
        lblError.setText(" ");
        setBusy(true);

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                authService.changePassword(current, newPass, confirm);
                return null;
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    get();
                    UiFactory.showInfo(ChangePasswordDialog.this,
                            "Đổi mật khẩu thành công. Lần đăng nhập sau hãy dùng mật khẩu mới.");
                    dispose();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    lblError.setText("<html><body style='width: 300px'>"
                            + (cause instanceof ServiceException
                                    ? cause.getMessage()
                                    : "Đã xảy ra lỗi không mong muốn.")
                            + "</body></html>");
                    txtCurrent.setText("");
                    txtCurrent.requestFocusInWindow();
                    pack();
                }
            }
        };
        worker.execute();
    }

    private void setBusy(boolean busy) {
        btnSave.setEnabled(!busy);
        btnSave.setText(busy ? "Đang xử lý..." : "Đổi mật khẩu");
    }
}