package com.doan.cicdmanager.ui;

import com.doan.cicdmanager.model.User;
import com.doan.cicdmanager.service.AuthService;
import com.doan.cicdmanager.service.PipelineExecutor;
import com.doan.cicdmanager.service.SessionManager;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class MainFrame extends JFrame {

    private static final String VIEW_DASHBOARD = "dashboard";
    private static final String VIEW_PROJECTS = "projects";
    private static final String VIEW_PIPELINES = "pipelines";
    private static final String VIEW_RUN = "run";
    private static final String VIEW_HISTORY = "history";
    private static final String VIEW_LOGS = "logs";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);
    private final JPanel navPanel = new JPanel();
    private final JLabel lblTitle = new JLabel();
    private final RunPanel runPanel = new RunPanel();

    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final Map<String, JComponent> views = new HashMap<>();
    private final Map<String, String> titles = new HashMap<>();

    public MainFrame() {
        super("CI/CD Manager");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });
        setSize(1200, 720);
        setMinimumSize(new Dimension(1000, 600));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setOpaque(false);

        addView(VIEW_DASHBOARD, "Dashboard", new DashboardPanel());
        addView(VIEW_PROJECTS, "Quản lý dự án", new ProjectPanel());
        PipelinePanel pipelinePanel = new PipelinePanel();
        pipelinePanel.setOnRunRequested(p -> {
            if (runPanel.isRunning()) {
                UiFactory.showInfo(this, "Đang có một pipeline chạy. Vui lòng đợi hoàn tất hoặc nhấn Dừng.");
                return;
            }
            runPanel.selectPipeline(p.getId());
            showView(VIEW_RUN);
        });
        addView(VIEW_PIPELINES, "Quản lý pipeline", pipelinePanel);
        addView(VIEW_RUN, "Chạy pipeline", runPanel);
        addView(VIEW_HISTORY, "Lịch sử chạy", new HistoryPanel());
        addView(VIEW_LOGS, "Nhật ký (Log)", new LogPanel());

        add(buildSidebar(), BorderLayout.WEST);
        add(buildMainArea(), BorderLayout.CENTER);

        showView(VIEW_DASHBOARD);
    }

    private void addView(String key, String title, JComponent view) {
        content.add(view, key);
        views.put(key, view);
        titles.put(key, title);

        JButton button = new JButton(title);
        button.setFont(UiTheme.FONT_NORMAL);
        button.setForeground(Color.WHITE);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBackground(UiTheme.PRIMARY_DARK);
        button.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 12));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        button.setPreferredSize(new Dimension(220, 46));
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.addActionListener(e -> showView(key));

        navButtons.put(key, button);
        navPanel.add(button);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(UiTheme.PRIMARY_DARK);
        sidebar.setPreferredSize(new Dimension(220, 0));

        JLabel brand = new JLabel("CI/CD MANAGER", SwingConstants.CENTER);
        brand.setFont(new Font("Segoe UI", Font.BOLD, 18));
        brand.setForeground(Color.WHITE);
        brand.setBorder(BorderFactory.createEmptyBorder(24, 8, 24, 8));

        User user = SessionManager.getCurrentUser();
        String role = SessionManager.isAdmin() ? "Quản trị viên" : "Người dùng";

        JLabel lblName = new JLabel(user.getFullName());
        lblName.setFont(UiTheme.FONT_BOLD);
        lblName.setForeground(Color.WHITE);

        JLabel lblRole = new JLabel(role);
        lblRole.setFont(UiTheme.FONT_NORMAL);
        lblRole.setForeground(new Color(170, 190, 215));

        JPanel info = new JPanel(new GridLayout(2, 1));
        info.setOpaque(false);
        info.add(lblName);
        info.add(lblRole);

        JButton btnChangePassword = new JButton("Đổi mật khẩu");
        btnChangePassword.setFont(UiTheme.FONT_NORMAL);
        btnChangePassword.setFocusPainted(false);
        btnChangePassword.addActionListener(e -> changePassword());

        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.setFont(UiTheme.FONT_BOLD);
        btnLogout.setFocusPainted(false);
        btnLogout.addActionListener(e -> logout());

        JPanel buttons = new JPanel(new GridLayout(2, 1, 0, 8));
        buttons.setOpaque(false);
        buttons.add(btnChangePassword);
        buttons.add(btnLogout);

        JPanel bottom = new JPanel(new BorderLayout(0, 10));
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(12, 16, 16, 16));
        bottom.add(info, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.SOUTH);

        sidebar.add(brand, BorderLayout.NORTH);
        sidebar.add(navPanel, BorderLayout.CENTER);
        sidebar.add(bottom, BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel buildMainArea() {
        lblTitle.setFont(UiTheme.FONT_TITLE);
        lblTitle.setForeground(UiTheme.PRIMARY);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDER),
                BorderFactory.createEmptyBorder(14, 24, 14, 24)));
        header.add(lblTitle, BorderLayout.WEST);

        content.setBackground(UiTheme.BACKGROUND);

        JPanel main = new JPanel(new BorderLayout());
        main.add(header, BorderLayout.NORTH);
        main.add(content, BorderLayout.CENTER);
        return main;
    }

    private void showView(String key) {
        cardLayout.show(content, key);
        lblTitle.setText(titles.get(key));

        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            entry.getValue().setBackground(
                    entry.getKey().equals(key) ? UiTheme.PRIMARY_LIGHT : UiTheme.PRIMARY_DARK);
        }

        JComponent view = views.get(key);
        if (view instanceof Refreshable) {
            ((Refreshable) view).refresh();
        }
    }

    private void changePassword() {
        new ChangePasswordDialog(this).setVisible(true);
    }
        
    private void logout() {
        if (runPanel.isRunning()) {
            UiFactory.showInfo(this, "Pipeline đang chạy. Vui lòng đợi hoàn tất hoặc nhấn Dừng trước khi đăng xuất.");
            return;
        }
        if (UiFactory.confirm(this, "Bạn có chắc muốn đăng xuất?", "Xác nhận đăng xuất")) {
            new AuthService().logout();
            new LoginFrame().setVisible(true);
            dispose();
        }
    }

    private void onClose() {
        if (runPanel.isRunning()) {
            boolean ok = UiFactory.confirm(this,
                    "Pipeline đang chạy. Thoát ứng dụng sẽ dừng pipeline. Bạn có chắc muốn thoát?",
                    "Xác nhận thoát");
            if (!ok) {
                return;
            }
            runPanel.cancelRun();
            // Chờ tối đa 5 giây để pipeline kịp ghi trạng thái cuối cùng vào database
            long waited = 0;
            while (PipelineExecutor.isBusy() && waited < 5000) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                waited += 100;
            }
        }
        dispose();
        System.exit(0);
    }
}