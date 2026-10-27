package com.doan.cicdmanager.ui;

import com.doan.cicdmanager.model.User;
import com.doan.cicdmanager.service.AuthService;
import com.doan.cicdmanager.service.ServiceException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

public class LoginFrame extends JFrame {

    private static final Color PRIMARY = new Color(30, 58, 95);
    private static final Font FONT = new Font("Segoe UI", Font.PLAIN, 14);

    private final JTextField txtUsername = new JTextField(20);
    private final JPasswordField txtPassword = new JPasswordField(20);
    private final JCheckBox chkShow = new JCheckBox("Hiện mật khẩu");
    private final JButton btnLogin = new JButton("Đăng nhập");
    private final JLabel lblMessage = new JLabel(" ", SwingConstants.CENTER);
    private final AuthService authService = new AuthService();
    private final char defaultEchoChar;

    public LoginFrame() {
        super("CI/CD Manager - Đăng nhập");
        defaultEchoChar = txtPassword.getEchoChar();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        buildUi();
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void buildUi() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel title = new JLabel("CI/CD MANAGER", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        JLabel subtitle = new JLabel("Hệ thống tự động hóa kiểm thử và triển khai phần mềm", SwingConstants.CENTER);
        subtitle.setForeground(new Color(200, 215, 235));
        subtitle.setFont(FONT);
        header.add(title, BorderLayout.CENTER);
        header.add(subtitle, BorderLayout.SOUTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 4, 6, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0; c.weightx = 0;
        form.add(styled(new JLabel("Tên đăng nhập")), c);
        c.gridx = 1; c.weightx = 1;
        txtUsername.setFont(FONT);
        form.add(txtUsername, c);

        c.gridx = 0; c.gridy = 1; c.weightx = 0;
        form.add(styled(new JLabel("Mật khẩu")), c);
        c.gridx = 1; c.weightx = 1;
        txtPassword.setFont(FONT);
        form.add(txtPassword, c);

        c.gridx = 1; c.gridy = 2;
        chkShow.setFont(FONT);
        chkShow.addActionListener(e -> txtPassword.setEchoChar(chkShow.isSelected() ? (char) 0 : defaultEchoChar));
        form.add(chkShow, c);

        c.gridx = 0; c.gridy = 3; c.gridwidth = 2;
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setBackground(PRIMARY);
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setPreferredSize(new Dimension(100, 36));
        btnLogin.addActionListener(e -> doLogin());
        form.add(btnLogin, c);

        c.gridy = 4;
        lblMessage.setForeground(new Color(192, 57, 43));
        lblMessage.setFont(FONT);
        form.add(lblMessage, c);

        getRootPane().setDefaultButton(btnLogin); // nhấn Enter để đăng nhập

        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
    }

    private JLabel styled(JLabel label) {
        label.setFont(FONT);
        return label;
    }

    private void doLogin() {
        final String username = txtUsername.getText();
        final String password = new String(txtPassword.getPassword());
        lblMessage.setText(" ");
        setBusy(true);

        SwingWorker<User, Void> worker = new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws Exception {
                return authService.login(username, password);
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    get();
                    openMainFrame();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    lblMessage.setText(cause instanceof ServiceException
                            ? cause.getMessage()
                            : "Đã xảy ra lỗi không mong muốn.");
                    txtPassword.setText("");
                    txtPassword.requestFocusInWindow();
                }
            }
        };
        worker.execute();
    }

    private void setBusy(boolean busy) {
        btnLogin.setEnabled(!busy);
        btnLogin.setText(busy ? "Đang đăng nhập..." : "Đăng nhập");
    }

    private void openMainFrame() {
        new MainFrame().setVisible(true);
        dispose();
    }
}