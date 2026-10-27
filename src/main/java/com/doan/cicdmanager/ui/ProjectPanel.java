package com.doan.cicdmanager.ui;

import com.doan.cicdmanager.model.Project;
import com.doan.cicdmanager.service.ProjectService;
import com.doan.cicdmanager.service.ServiceException;
import com.doan.cicdmanager.service.SessionManager;
import com.doan.cicdmanager.util.DateUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class ProjectPanel extends JPanel implements Refreshable {

    private static final String[] COLUMNS = {"ID", "Tên dự án", "Mô tả", "Đường dẫn", "Ngày tạo"};

    private final ProjectService service = new ProjectService();
    private final JTextField txtSearch = new JTextField(24);
    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private List<Project> current = new ArrayList<>();

    public ProjectPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
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

        // Người dùng thường chỉ được xem
        boolean admin = SessionManager.isAdmin();
        right.setVisible(admin);

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.add(left, BorderLayout.WEST);
        toolbar.add(right, BorderLayout.EAST);
        return toolbar;
    }

    private JScrollPane buildTable() {
        UiTheme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(260);
        table.getColumnModel().getColumn(3).setPreferredWidth(260);
        table.getColumnModel().getColumn(4).setPreferredWidth(150);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SessionManager.isAdmin()) {
                    onEdit();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    @Override
    public void refresh() {
        load();
    }

    private void load() {
        try {
            current = service.search(txtSearch.getText());
            model.setRowCount(0);
            for (Project p : current) {
                model.addRow(new Object[]{
                    p.getId(), p.getName(), p.getDescription(), p.getPath(), DateUtil.toDisplay(p.getCreatedAt())
                });
            }
        } catch (ServiceException e) {
            UiFactory.showError(this, e.getMessage());
        }
    }

    private Project selected() {
        int row = table.getSelectedRow();
        return row < 0 || row >= current.size() ? null : current.get(row);
    }

    private void onAdd() {
        ProjectDialog dialog = new ProjectDialog(SwingUtilities.getWindowAncestor(this), service, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            load();
            UiFactory.showInfo(this, "Đã thêm dự án thành công.");
        }
    }

    private void onEdit() {
        Project p = selected();
        if (p == null) {
            UiFactory.showInfo(this, "Vui lòng chọn một dự án trong bảng.");
            return;
        }
        ProjectDialog dialog = new ProjectDialog(SwingUtilities.getWindowAncestor(this), service, p);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            load();
            UiFactory.showInfo(this, "Đã cập nhật dự án thành công.");
        }
    }

    private void onDelete() {
        Project p = selected();
        if (p == null) {
            UiFactory.showInfo(this, "Vui lòng chọn một dự án trong bảng.");
            return;
        }
        boolean ok = UiFactory.confirm(this,
                "Xóa dự án \"" + p.getName() + "\"?\nTất cả pipeline, lịch sử chạy và nhật ký của dự án này cũng sẽ bị xóa.",
                "Xác nhận xóa");
        if (!ok) {
            return;
        }
        try {
            service.delete(p.getId());
            load();
            UiFactory.showInfo(this, "Đã xóa dự án.");
        } catch (ServiceException e) {
            UiFactory.showError(this, e.getMessage());
        }
    }
}