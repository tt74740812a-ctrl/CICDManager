package com.doan.cicdmanager.ui;

import com.doan.cicdmanager.model.PipelineRun;
import com.doan.cicdmanager.service.DashboardService;
import com.doan.cicdmanager.service.DashboardService.DashboardData;
import com.doan.cicdmanager.service.ServiceException;
import com.doan.cicdmanager.util.DateUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

public class DashboardPanel extends JPanel implements Refreshable {

    private static final String[] COLUMNS = {"ID", "Pipeline", "Dự án", "Trạng thái", "Bắt đầu", "Kết thúc", "Thời gian"};
    private static final int RECENT_LIMIT = 10;

    private final DashboardService service = new DashboardService();

    private final JLabel lblProjects = bigNumber(UiTheme.PRIMARY);
    private final JLabel lblPipelines = bigNumber(UiTheme.PRIMARY);
    private final JLabel lblRuns = bigNumber(UiTheme.INFO);
    private final JLabel lblSuccess = bigNumber(UiTheme.SUCCESS);
    private final JLabel lblFailed = bigNumber(UiTheme.DANGER);
    private final JLabel lblStatus = new JLabel(" ");

    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public DashboardPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(buildCards(), BorderLayout.NORTH);
        add(buildHistory(), BorderLayout.CENTER);
    }

    private JPanel buildCards() {
        JPanel cards = new JPanel(new GridLayout(1, 5, 16, 0));
        cards.setOpaque(false);
        cards.setPreferredSize(new Dimension(0, 90));
        cards.add(card("Tổng số dự án", lblProjects, UiTheme.PRIMARY));
        cards.add(card("Tổng số pipeline", lblPipelines, UiTheme.PRIMARY));
        cards.add(card("Tổng lượt chạy", lblRuns, UiTheme.INFO));
        cards.add(card("Chạy thành công", lblSuccess, UiTheme.SUCCESS));
        cards.add(card("Chạy thất bại", lblFailed, UiTheme.DANGER));
        return cards;
    }

    private JPanel buildHistory() {
        JLabel title = new JLabel("Lịch sử chạy gần đây");
        title.setFont(UiTheme.FONT_BOLD);
        title.setForeground(UiTheme.PRIMARY);

        lblStatus.setFont(UiTheme.FONT_NORMAL);
        lblStatus.setForeground(UiTheme.MUTED);

        JButton btnRefresh = new JButton("Làm mới");
        btnRefresh.setFont(UiTheme.FONT_NORMAL);
        btnRefresh.addActionListener(e -> refresh());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(lblStatus);
        right.add(btnRefresh);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        top.add(title, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        UiTheme.styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());
        table.getColumnModel().getColumn(4).setPreferredWidth(150);
        table.getColumnModel().getColumn(5).setPreferredWidth(150);
        table.getColumnModel().getColumn(6).setPreferredWidth(90);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(top, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel card(String title, JLabel value, Color accent) {
        JPanel stripe = new JPanel();
        stripe.setBackground(accent);
        stripe.setPreferredSize(new Dimension(6, 0));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(UiTheme.FONT_NORMAL);
        lblTitle.setForeground(UiTheme.MUTED);

        JPanel body = new JPanel(new GridLayout(2, 1));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        body.add(lblTitle);
        body.add(value);

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        card.add(stripe, BorderLayout.WEST);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private static JLabel bigNumber(Color color) {
        JLabel label = new JLabel("0", SwingConstants.LEFT);
        label.setFont(UiTheme.FONT_BIG_NUMBER);
        label.setForeground(color);
        return label;
    }

    @Override
    public void refresh() {
        lblStatus.setForeground(UiTheme.MUTED);
        lblStatus.setText("Đang tải...");

        SwingWorker<DashboardData, Void> worker = new SwingWorker<DashboardData, Void>() {
            @Override
            protected DashboardData doInBackground() throws Exception {
                return service.load(RECENT_LIMIT);
            }

            @Override
            protected void done() {
                try {
                    apply(get());
                    lblStatus.setText("Cập nhật lúc "
                            + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    lblStatus.setForeground(UiTheme.DANGER);
                    lblStatus.setText(cause instanceof ServiceException
                            ? cause.getMessage()
                            : "Không thể tải dữ liệu.");
                }
            }
        };
        worker.execute();
    }

    private void apply(DashboardData data) {
        lblProjects.setText(String.valueOf(data.totalProjects));
        lblPipelines.setText(String.valueOf(data.totalPipelines));
        lblRuns.setText(String.valueOf(data.totalRuns));
        lblSuccess.setText(String.valueOf(data.successRuns));
        lblFailed.setText(String.valueOf(data.failedRuns));

        model.setRowCount(0);
        for (PipelineRun r : data.recentRuns) {
            Long seconds = r.getDurationSeconds();
            model.addRow(new Object[]{
                r.getId(),
                r.getPipelineName(),
                r.getProjectName(),
                r.getStatus(),
                DateUtil.toDisplay(r.getStartedAt()),
                DateUtil.toDisplay(r.getFinishedAt()),
                seconds == null ? "" : seconds + " giây"
            });
        }
    }
}