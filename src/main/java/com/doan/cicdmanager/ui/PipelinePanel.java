package com.doan.cicdmanager.ui;

import java.util.function.Consumer;
import com.doan.cicdmanager.model.Pipeline;
import com.doan.cicdmanager.model.PipelineStep;
import com.doan.cicdmanager.model.Project;
import com.doan.cicdmanager.service.PipelineService;
import com.doan.cicdmanager.service.ServiceException;
import com.doan.cicdmanager.service.SessionManager;
import com.doan.cicdmanager.util.DateUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class PipelinePanel extends JPanel implements Refreshable {

    private static final String[] COLUMNS = {"ID", "Tên pipeline", "Dự án", "Chế độ", "Các bước", "Ngày tạo"};
    private static final String[] STEP_COLUMNS = {"Thứ tự", "Bước", "Trạng thái", "Thư mục triển khai"};

    private final PipelineService service = new PipelineService();
    private final JTextField txtSearch = new JTextField(24);
    private final JLabel lblDetail = new JLabel("Chọn một pipeline để xem cấu hình");

    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    private final DefaultTableModel stepModel = new DefaultTableModel(STEP_COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable stepTable = new JTable(stepModel);

    private List<Pipeline> current = new ArrayList<>();
    
    private Consumer<Pipeline> onRunRequested;
    
    public PipelinePanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
    }
    
    /** MainFrame đăng ký hàm này để chuyển sang màn hình Chạy pipeline. */
    public void setOnRunRequested(Consumer<Pipeline> handler) {
        this.onRunRequested = handler;
    }
    
    private JPanel buildToolbar() {
        txtSearch.setFont(UiTheme.FONT_NORMAL);
        txtSearch.addActionListener(e -> load());

        JButton btnSearch = UiFactory.button("Tìm kiếm", UiTheme.INFO);
        btnSearch.addActionListener(e -> load());
        JButton btnClear = UiFactory.button("Xóa lọc", UiTheme.MUTED);
        btnClear.addActionListener(e -> {
            txtSearch.setText("");
            load();
        });

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(txtSearch);
        left.add(btnSearch);
        left.add(btnClear);

        JButton btnRun = UiFactory.button("Chạy pipeline", UiTheme.PRIMARY_LIGHT);
        btnRun.addActionListener(e -> onRun());
        left.add(btnRun);

        JButton btnAdd = UiFactory.button("Thêm", UiTheme.SUCCESS);
        btnAdd.addActionListener(e -> onAdd());
        JButton btnEdit = UiFactory.button("Sửa", UiTheme.WARNING);
        btnEdit.addActionListener(e -> onEdit());
        JButton btnDelete = UiFactory.button("Xóa", UiTheme.DANGER);
        btnDelete.addActionListener(e -> onDelete());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(btnAdd);
        right.add(btnEdit);
        right.add(btnDelete);
        right.setVisible(SessionManager.isAdmin()); // người dùng thường chỉ được xem

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.add(left, BorderLayout.WEST);
        toolbar.add(right, BorderLayout.EAST);
        return toolbar;
    }

    private JSplitPane buildCenter() {
        UiTheme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(170);
        table.getColumnModel().getColumn(2).setPreferredWidth(160);
        table.getColumnModel().getColumn(3).setPreferredWidth(70);
        table.getColumnModel().getColumn(4).setPreferredWidth(280);
        table.getColumnModel().getColumn(5).setPreferredWidth(150);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSteps();
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SessionManager.isAdmin()) {
                    onEdit();
                }
            }
        });
        JScrollPane listScroll = new JScrollPane(table);
        listScroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        listScroll.getViewport().setBackground(Color.WHITE);

        lblDetail.setFont(UiTheme.FONT_BOLD);
        lblDetail.setForeground(UiTheme.PRIMARY);
        lblDetail.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        UiTheme.styleTable(stepTable);
        stepTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        stepTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        stepTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        stepTable.getColumnModel().getColumn(3).setPreferredWidth(400);
        stepTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setFont(label.getFont().deriveFont(Font.BOLD));
                label.setForeground("Bật".equals(value) ? UiTheme.SUCCESS : UiTheme.MUTED);
                return label;
            }
        });
        JScrollPane stepScroll = new JScrollPane(stepTable);
        stepScroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        stepScroll.getViewport().setBackground(Color.WHITE);

        JPanel detail = new JPanel(new BorderLayout());
        detail.setOpaque(false);
        detail.add(lblDetail, BorderLayout.NORTH);
        detail.add(stepScroll, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, listScroll, detail);
        split.setResizeWeight(0.6);
        split.setBorder(null);
        split.setOpaque(false);
        return split;
    }

    @Override
    public void refresh() {
        load();
    }

    private void load() {
        try {
            current = service.search(txtSearch.getText());
            model.setRowCount(0);
            for (Pipeline p : current) {
                model.addRow(new Object[]{
                    p.getId(), p.getName(), p.getProjectName(), p.getRunMode(),
                    summarize(service.getSteps(p.getId())), DateUtil.toDisplay(p.getCreatedAt())
                });
            }
            loadSteps();
        } catch (ServiceException e) {
            UiFactory.showError(this, e.getMessage());
        }
    }

    private String summarize(List<PipelineStep> steps) {
        return steps.stream()
                .map(s -> s.getStepOrder() + "." + s.getStepType() + (s.isEnabled() ? "" : " (tắt)"))
                .collect(Collectors.joining(" → "));
    }

    private void loadSteps() {
        stepModel.setRowCount(0);
        Pipeline p = selected();
        if (p == null) {
            lblDetail.setText("Chọn một pipeline để xem cấu hình");
            return;
        }
        try {
            lblDetail.setText("Cấu hình: " + p.getName() + "  |  Dự án: " + p.getProjectName()
                    + "  |  Chế độ: " + p.getRunMode());
            for (PipelineStep s : service.getSteps(p.getId())) {
                stepModel.addRow(new Object[]{
                    s.getStepOrder(), s.getStepType(), s.isEnabled() ? "Bật" : "Tắt",
                    s.getDeployDir() == null ? "" : s.getDeployDir()
                });
            }
        } catch (ServiceException e) {
            UiFactory.showError(this, e.getMessage());
        }
    }

    private Pipeline selected() {
        int row = table.getSelectedRow();
        return row < 0 || row >= current.size() ? null : current.get(row);
    }

    private void onRun() {
        Pipeline p = selected();
        if (p == null) {
            UiFactory.showInfo(this, "Vui lòng chọn một pipeline trong bảng.");
            return;
        }
        if (onRunRequested != null) {
            onRunRequested.accept(p);
        }
    }
    
    private void onAdd() {
        try {
            List<Project> projects = service.listProjects();
            if (projects.isEmpty()) {
                UiFactory.showInfo(this, "Cần tạo ít nhất một dự án trước khi tạo pipeline.");
                return;
            }
            PipelineDialog dialog = new PipelineDialog(SwingUtilities.getWindowAncestor(this),
                    service, null, projects, service.defaultSteps());
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                load();
                UiFactory.showInfo(this, "Đã thêm pipeline thành công.");
            }
        } catch (ServiceException e) {
            UiFactory.showError(this, e.getMessage());
        }
    }

    private void onEdit() {
        Pipeline p = selected();
        if (p == null) {
            UiFactory.showInfo(this, "Vui lòng chọn một pipeline trong bảng.");
            return;
        }
        try {
            PipelineDialog dialog = new PipelineDialog(SwingUtilities.getWindowAncestor(this),
                    service, p, service.listProjects(), service.getSteps(p.getId()));
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                load();
                UiFactory.showInfo(this, "Đã cập nhật pipeline thành công.");
            }
        } catch (ServiceException e) {
            UiFactory.showError(this, e.getMessage());
        }
    }

    private void onDelete() {
        Pipeline p = selected();
        if (p == null) {
            UiFactory.showInfo(this, "Vui lòng chọn một pipeline trong bảng.");
            return;
        }
        boolean ok = UiFactory.confirm(this,
                "Xóa pipeline \"" + p.getName() + "\"?\nLịch sử chạy và nhật ký của pipeline này cũng sẽ bị xóa.",
                "Xác nhận xóa");
        if (!ok) {
            return;
        }
        try {
            service.delete(p.getId());
            load();
            UiFactory.showInfo(this, "Đã xóa pipeline.");
        } catch (ServiceException e) {
            UiFactory.showError(this, e.getMessage());
        }
    }
}