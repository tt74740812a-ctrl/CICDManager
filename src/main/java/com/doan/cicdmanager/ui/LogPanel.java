package com.doan.cicdmanager.ui;

import com.doan.cicdmanager.model.RunStatus;
import com.doan.cicdmanager.model.StepLog;
import com.doan.cicdmanager.model.StepType;
import com.doan.cicdmanager.service.HistoryService;
import com.doan.cicdmanager.service.ServiceException;
import com.doan.cicdmanager.util.DateUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

public class LogPanel extends JPanel implements Refreshable {

    private static final String[] COLUMNS = {"ID", "Lần chạy", "Pipeline", "Bước", "Trạng thái", "Bắt đầu", "Mã thoát"};
    private static final String[] STEP_FILTER = {"Tất cả các bước", "BUILD", "TEST", "DEPLOY"};
    private static final String[] STATUS_FILTER = {"Mọi trạng thái", "SUCCESS", "FAILED", "SKIPPED", "RUNNING"};

    private final HistoryService service = new HistoryService();

    private final JTextField txtSearch = new JTextField(24);
    private final JComboBox<String> cmbStep = new JComboBox<>(STEP_FILTER);
    private final JComboBox<String> cmbStatus = new JComboBox<>(STATUS_FILTER);
    private final JLabel lblCount = new JLabel(" ");

    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column < 2 ? Integer.class : Object.class;
        }
    };
    private final JTable table = new JTable(model);
    private final LogView logView = new LogView();

    private List<StepLog> current = new ArrayList<>();
    private String lastKeyword = "";

    public LogPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
    }

    private JPanel buildToolbar() {
        txtSearch.setFont(UiTheme.FONT_NORMAL);
        txtSearch.setToolTipText("Nhập từ khóa trong nội dung log, ví dụ: BUILD SUCCESS, Failures, ERROR");
        txtSearch.addActionListener(e -> load());
        cmbStep.setFont(UiTheme.FONT_NORMAL);
        cmbStep.addActionListener(e -> load());
        cmbStatus.setFont(UiTheme.FONT_NORMAL);
        cmbStatus.addActionListener(e -> load());

        JButton btnSearch = UiFactory.button("Tìm kiếm", UiTheme.INFO);
        btnSearch.addActionListener(e -> load());
        JButton btnClear = UiFactory.button("Xóa lọc", UiTheme.MUTED);
        btnClear.addActionListener(e -> {
            txtSearch.setText("");
            cmbStep.setSelectedIndex(0);
            cmbStatus.setSelectedIndex(0);
            load();
        });

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(txtSearch);
        left.add(cmbStep);
        left.add(cmbStatus);
        left.add(btnSearch);
        left.add(btnClear);

        JButton btnSave = UiFactory.button("Lưu log...", UiTheme.SUCCESS);
        btnSave.addActionListener(e -> onSave());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(btnSave);

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.add(left, BorderLayout.WEST);
        toolbar.add(right, BorderLayout.EAST);
        return toolbar;
    }

    private JPanel buildCenter() {
        UiTheme.styleTable(table);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] widths = {50, 80, 200, 90, 110, 160, 80};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(4).setCellRenderer(new StatusCellRenderer());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelected();
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);

        lblCount.setFont(UiTheme.FONT_BOLD);
        lblCount.setForeground(UiTheme.PRIMARY);
        lblCount.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(lblCount, BorderLayout.NORTH);
        top.add(scroll, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, logView);
        split.setResizeWeight(0.4);
        split.setBorder(null);
        split.setOpaque(false);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    @Override
    public void refresh() {
        load();
    }

    private void load() {
        try {
            lastKeyword = txtSearch.getText().trim();
            List<StepLog> all = service.searchLogs(lastKeyword);

            StepType type = cmbStep.getSelectedIndex() <= 0
                    ? null : StepType.valueOf((String) cmbStep.getSelectedItem());
            RunStatus status = cmbStatus.getSelectedIndex() <= 0
                    ? null : RunStatus.valueOf((String) cmbStatus.getSelectedItem());

            current = new ArrayList<>();
            for (StepLog l : all) {
                if ((type == null || l.getStepType() == type) && (status == null || l.getStatus() == status)) {
                    current.add(l);
                }
            }

            model.setRowCount(0);
            for (StepLog l : current) {
                model.addRow(new Object[]{
                    l.getId(), l.getRunId(), l.getPipelineName(), l.getStepType(), l.getStatus(),
                    DateUtil.toDisplay(l.getStartedAt()), l.getExitCode() == null ? "" : l.getExitCode()
                });
            }
            lblCount.setText("Tìm thấy " + current.size() + " nhật ký");
            logView.showText("", null);
            if (!current.isEmpty()) {
                table.setRowSelectionInterval(0, 0);
            }
        } catch (ServiceException e) {
            UiFactory.showError(this, e.getMessage());
        }
    }

    private StepLog selected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return modelRow < current.size() ? current.get(modelRow) : null;
    }

    private void showSelected() {
        StepLog log = selected();
        if (log == null) {
            logView.showText("", null);
        } else {
            logView.showText(log.getLogText(), lastKeyword);
        }
    }

    /** Lưu nội dung log đang chọn ra file văn bản do người dùng chọn. */
    private void onSave() {
        StepLog log = selected();
        if (log == null) {
            UiFactory.showInfo(this, "Vui lòng chọn một nhật ký trong bảng.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Lưu nhật ký");
        chooser.setSelectedFile(new File("log_run" + log.getRunId() + "_" + log.getStepType() + ".txt"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (file.exists() && !UiFactory.confirm(this,
                "File \"" + file.getName() + "\" đã tồn tại. Bạn có muốn ghi đè?", "Xác nhận ghi đè")) {
            return;
        }

        String header = "Lần chạy #" + log.getRunId() + " | Pipeline: " + log.getPipelineName()
                + " | Bước: " + log.getStepType() + " | Trạng thái: " + log.getStatus()
                + " | Bắt đầu: " + DateUtil.toDisplay(log.getStartedAt()) + "\n"
                + "----------------------------------------\n";
        String text = header + (log.getLogText() == null ? "" : log.getLogText());
        try {
            Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
            UiFactory.showInfo(this, "Đã lưu nhật ký vào:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            UiFactory.showError(this, "Không thể lưu file: " + e.getMessage());
        }
    }
}