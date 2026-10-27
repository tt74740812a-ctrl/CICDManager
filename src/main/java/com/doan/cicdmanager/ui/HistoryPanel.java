package com.doan.cicdmanager.ui;

import com.doan.cicdmanager.model.PipelineRun;
import com.doan.cicdmanager.model.RunStatus;
import com.doan.cicdmanager.model.StepLog;
import com.doan.cicdmanager.service.HistoryService;
import com.doan.cicdmanager.service.HistoryService.OverallStats;
import com.doan.cicdmanager.service.HistoryService.PipelineStat;
import com.doan.cicdmanager.service.HistoryService.Stats;
import com.doan.cicdmanager.service.ServiceException;
import com.doan.cicdmanager.util.DateUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

public class HistoryPanel extends JPanel implements Refreshable {

    private static final String[] RUN_COLUMNS =
        {"ID", "Pipeline", "Dự án", "Trạng thái", "Bắt đầu", "Kết thúc", "Thời gian", "Kết quả"};
    private static final String[] STEP_COLUMNS = {"Bước", "Trạng thái", "Bắt đầu", "Kết thúc", "Mã thoát"};
    private static final String[] STAT_COLUMNS =
        {"Pipeline", "Dự án", "Tổng lượt chạy", "Thành công", "Thất bại", "Tỉ lệ thành công", "Thời gian TB"};
    private static final String[] STATUS_FILTER = {"Tất cả trạng thái", "WAITING", "RUNNING", "SUCCESS", "FAILED"};

    private final HistoryService service = new HistoryService();

    private final JTextField txtSearch = new JTextField(22);
    private final JComboBox<String> cmbStatus = new JComboBox<>(STATUS_FILTER);

    private final JLabel lblTotal = statLabel(UiTheme.PRIMARY);
    private final JLabel lblSuccess = statLabel(UiTheme.SUCCESS);
    private final JLabel lblFailed = statLabel(UiTheme.DANGER);
    private final JLabel lblRate = statLabel(UiTheme.INFO);
    private final JLabel lblAvg = statLabel(UiTheme.MUTED);
    private final JLabel lblDetail = new JLabel(" ");

    private final DefaultTableModel runModel = readOnlyModel(RUN_COLUMNS, 1);
    private final JTable runTable = new JTable(runModel);
    private final DefaultTableModel stepModel = readOnlyModel(STEP_COLUMNS, 0);
    private final JTable stepTable = new JTable(stepModel);
    private final DefaultTableModel statModel = readOnlyModel(STAT_COLUMNS, 0);
    private final JTable statTable = new JTable(statModel);
    private final LogView logView = new LogView();

    private List<PipelineRun> currentRuns = new ArrayList<>();
    private List<StepLog> currentLogs = new ArrayList<>();

    public HistoryPanel() {
        setLayout(new BorderLayout());
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UiTheme.FONT_BOLD);
        tabs.addTab("Lịch sử chạy", buildHistoryTab());
        tabs.addTab("Thống kê theo pipeline", buildStatsTab());
        add(tabs, BorderLayout.CENTER);
    }

    // ------------------------------------------------------------------ dựng giao diện

    private JPanel buildHistoryTab() {
        JPanel statsBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 32, 10));
        statsBar.setBackground(Color.WHITE);
        statsBar.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        statsBar.add(lblTotal);
        statsBar.add(lblSuccess);
        statsBar.add(lblFailed);
        statsBar.add(lblRate);
        statsBar.add(lblAvg);

        txtSearch.setFont(UiTheme.FONT_NORMAL);
        txtSearch.addActionListener(e -> load());
        cmbStatus.setFont(UiTheme.FONT_NORMAL);
        cmbStatus.addActionListener(e -> load());

        JButton btnSearch = UiFactory.button("Tìm kiếm", UiTheme.INFO);
        btnSearch.addActionListener(e -> load());
        JButton btnClear = UiFactory.button("Xóa lọc", UiTheme.MUTED);
        btnClear.addActionListener(e -> {
            txtSearch.setText("");
            cmbStatus.setSelectedIndex(0);
            load();
        });
        JButton btnRefresh = UiFactory.button("Làm mới", UiTheme.PRIMARY_LIGHT);
        btnRefresh.addActionListener(e -> load());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.add(txtSearch);
        toolbar.add(cmbStatus);
        toolbar.add(btnSearch);
        toolbar.add(btnClear);
        toolbar.add(btnRefresh);

        JPanel top = new JPanel(new GridLayout(2, 1, 0, 10));
        top.setOpaque(false);
        top.add(statsBar);
        top.add(toolbar);

        // Bảng các lần chạy
        UiTheme.styleTable(runTable);
        runTable.setAutoCreateRowSorter(true);
        runTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] widths = {50, 160, 150, 110, 165, 165, 95, 280};
        for (int i = 0; i < widths.length; i++) {
            runTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        runTable.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());
        runTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSteps();
            }
        });
        JScrollPane runScroll = new JScrollPane(runTable);
        runScroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        runScroll.getViewport().setBackground(Color.WHITE);

        // Bảng các bước và khung log
        UiTheme.styleTable(stepTable);
        stepTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stepTable.getColumnModel().getColumn(1).setCellRenderer(new StatusCellRenderer());
        stepTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedLog();
            }
        });
        JScrollPane stepScroll = new JScrollPane(stepTable);
        stepScroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        stepScroll.getViewport().setBackground(Color.WHITE);

        JSplitPane detailSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, stepScroll, logView);
        detailSplit.setResizeWeight(0.4);
        detailSplit.setBorder(null);

        lblDetail.setFont(UiTheme.FONT_BOLD);
        lblDetail.setForeground(UiTheme.PRIMARY);
        lblDetail.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        JPanel detail = new JPanel(new BorderLayout());
        detail.setOpaque(false);
        detail.add(lblDetail, BorderLayout.NORTH);
        detail.add(detailSplit, BorderLayout.CENTER);

        JSplitPane main = new JSplitPane(JSplitPane.VERTICAL_SPLIT, runScroll, detail);
        main.setResizeWeight(0.5);
        main.setBorder(null);
        main.setOpaque(false);

        JPanel tab = new JPanel(new BorderLayout(0, 10));
        tab.setBackground(UiTheme.BACKGROUND);
        tab.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        tab.add(top, BorderLayout.NORTH);
        tab.add(main, BorderLayout.CENTER);
        return tab;
    }

    private JPanel buildStatsTab() {
        UiTheme.styleTable(statTable);
        statTable.setAutoCreateRowSorter(true);
        JScrollPane scroll = new JScrollPane(statTable);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);

        JPanel tab = new JPanel(new BorderLayout());
        tab.setBackground(UiTheme.BACKGROUND);
        tab.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        tab.add(scroll, BorderLayout.CENTER);
        return tab;
    }

    private static JLabel statLabel(Color color) {
        JLabel label = new JLabel(" ");
        label.setFont(UiTheme.FONT_BOLD);
        label.setForeground(color);
        return label;
    }

    /** Bảng chỉ đọc. Các cột đầu (số lượng numericColumns) có kiểu số để sắp xếp đúng. */
    private static DefaultTableModel readOnlyModel(String[] columns, int numericColumns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column < numericColumns ? Integer.class : Object.class;
            }
        };
    }

    // ------------------------------------------------------------------ dữ liệu

    @Override
    public void refresh() {
        load();
    }

    private void load() {
        try {
            RunStatus status = cmbStatus.getSelectedIndex() <= 0
                    ? null
                    : RunStatus.valueOf((String) cmbStatus.getSelectedItem());
            currentRuns = service.searchRuns(txtSearch.getText(), status);

            runModel.setRowCount(0);
            for (PipelineRun r : currentRuns) {
                Long seconds = r.getDurationSeconds();
                runModel.addRow(new Object[]{
                    r.getId(), r.getPipelineName(), r.getProjectName(), r.getStatus(),
                    DateUtil.toDisplay(r.getStartedAt()), DateUtil.toDisplay(r.getFinishedAt()),
                    seconds == null ? "" : seconds + " giây",
                    r.getResultMessage() == null ? "" : r.getResultMessage()
                });
            }
            loadSteps();
            loadStats();
        } catch (ServiceException e) {
            UiFactory.showError(this, e.getMessage());
        }
    }

    private void loadStats() throws ServiceException {
        Stats stats = service.loadStats();
        OverallStats o = stats.overall;
        lblTotal.setText("Tổng lượt chạy: " + o.total);
        lblSuccess.setText("Thành công: " + o.success);
        lblFailed.setText("Thất bại: " + o.failed);
        lblRate.setText(String.format("Tỉ lệ thành công: %.1f%%", o.successRate));
        lblAvg.setText("Thời gian trung bình: " + o.avgSeconds + " giây");

        statModel.setRowCount(0);
        for (PipelineStat ps : stats.byPipeline) {
            statModel.addRow(new Object[]{
                ps.pipelineName, ps.projectName, ps.total, ps.success, ps.failed,
                String.format("%.1f%%", ps.successRate), ps.avgSeconds + " giây"
            });
        }
    }

    private PipelineRun selectedRun() {
        int viewRow = runTable.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = runTable.convertRowIndexToModel(viewRow);
        return modelRow < currentRuns.size() ? currentRuns.get(modelRow) : null;
    }

    private void loadSteps() {
        stepModel.setRowCount(0);
        currentLogs = new ArrayList<>();
        logView.showText("", null);

        PipelineRun run = selectedRun();
        if (run == null) {
            lblDetail.setText("Chọn một lần chạy ở bảng trên để xem các bước và nhật ký chi tiết");
            return;
        }
        try {
            currentLogs = service.getStepLogs(run.getId());
        } catch (ServiceException e) {
            UiFactory.showError(this, e.getMessage());
            return;
        }
        lblDetail.setText("Lần chạy #" + run.getId() + "  |  " + run.getPipelineName()
                + "  |  " + (run.getResultMessage() == null ? "" : run.getResultMessage()));

        int focus = 0;
        for (int i = 0; i < currentLogs.size(); i++) {
            StepLog l = currentLogs.get(i);
            stepModel.addRow(new Object[]{
                l.getStepType(), l.getStatus(),
                DateUtil.toDisplay(l.getStartedAt()), DateUtil.toDisplay(l.getFinishedAt()),
                l.getExitCode() == null ? "" : l.getExitCode()
            });
            if (l.getStatus() == RunStatus.FAILED && focus == 0) {
                focus = i; // ưu tiên hiển thị bước thất bại đầu tiên
            }
        }
        if (!currentLogs.isEmpty()) {
            stepTable.setRowSelectionInterval(focus, focus);
        }
    }

    private void showSelectedLog() {
        int row = stepTable.getSelectedRow();
        if (row < 0 || row >= currentLogs.size()) {
            logView.showText("", null);
            return;
        }
        logView.showText(currentLogs.get(row).getLogText(), null);
    }
}