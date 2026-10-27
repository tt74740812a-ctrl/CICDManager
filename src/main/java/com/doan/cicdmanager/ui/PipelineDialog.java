package com.doan.cicdmanager.ui;

import com.doan.cicdmanager.model.Pipeline;
import com.doan.cicdmanager.model.PipelineStep;
import com.doan.cicdmanager.model.Project;
import com.doan.cicdmanager.model.RunMode;
import com.doan.cicdmanager.model.StepType;
import com.doan.cicdmanager.service.PipelineService;
import com.doan.cicdmanager.service.ServiceException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

public class PipelineDialog extends JDialog {

    private final PipelineService service;
    private final Pipeline editing;

    private final JTextField txtName = new JTextField(30);
    private final JTextArea txtDesc = new JTextArea(3, 30);
    private final JComboBox<Project> cmbProject = new JComboBox<>();
    private final JComboBox<RunMode> cmbMode = new JComboBox<>(RunMode.values());
    private final JTextField txtDeployDir = new JTextField(24);
    private final JLabel lblError = new JLabel(" ");

    private final DefaultTableModel stepModel = new DefaultTableModel(new Object[]{"Thứ tự", "Bước", "Bật"}, 0) {
        @Override
        public Class<?> getColumnClass(int column) {
            return column == 2 ? Boolean.class : Object.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 2;
        }
    };
    private final JTable stepTable = new JTable(stepModel);
    private boolean saved;

    /** editing = null nghĩa là thêm mới. steps là cấu hình các bước ban đầu. */
    public PipelineDialog(Window owner, PipelineService service, Pipeline editing,
                          List<Project> projects, List<PipelineStep> steps) {
        super(owner, editing == null ? "Thêm pipeline" : "Sửa pipeline", ModalityType.APPLICATION_MODAL);
        this.service = service;
        this.editing = editing;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        for (Project p : projects) {
            cmbProject.addItem(p);
        }
        buildUi();
        fillData(projects, steps);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    public boolean isSaved() {
        return saved;
    }

    private void buildUi() {
        txtName.setFont(UiTheme.FONT_NORMAL);
        txtDesc.setFont(UiTheme.FONT_NORMAL);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        txtDeployDir.setFont(UiTheme.FONT_NORMAL);
        cmbProject.setFont(UiTheme.FONT_NORMAL);
        cmbMode.setFont(UiTheme.FONT_NORMAL);

        UiTheme.styleTable(stepTable);
        stepTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stepTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        stepTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        stepTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        JScrollPane stepScroll = new JScrollPane(stepTable);
        stepScroll.setPreferredSize(new Dimension(380, 128));

        JButton btnUp = UiFactory.button("Lên", UiTheme.PRIMARY_LIGHT);
        btnUp.addActionListener(e -> moveStep(-1));
        JButton btnDown = UiFactory.button("Xuống", UiTheme.PRIMARY_LIGHT);
        btnDown.addActionListener(e -> moveStep(1));
        JPanel moveButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        moveButtons.add(btnUp);
        moveButtons.add(btnDown);
        moveButtons.add(new JLabel("(chọn một bước rồi nhấn Lên/Xuống)"));

        JPanel stepsPanel = new JPanel(new BorderLayout(0, 6));
        stepsPanel.add(stepScroll, BorderLayout.CENTER);
        stepsPanel.add(moveButtons, BorderLayout.SOUTH);

        JButton btnBrowse = UiFactory.button("Chọn...", UiTheme.PRIMARY_LIGHT);
        btnBrowse.addActionListener(e -> browse());
        JPanel deployRow = new JPanel(new BorderLayout(6, 0));
        deployRow.add(txtDeployDir, BorderLayout.CENTER);
        deployRow.add(btnBrowse, BorderLayout.EAST);

        JLabel hint = new JLabel("<html>DEMO: mô phỏng, không cần Maven.<br>"
                + "REAL: gọi Maven thật trong thư mục dự án; DEPLOY sao chép bản build vào thư mục triển khai thử nghiệm.</html>");
        hint.setFont(UiTheme.FONT_NORMAL);
        hint.setForeground(UiTheme.MUTED);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        addRow(form, 0, "Tên pipeline *", txtName);
        addRow(form, 1, "Mô tả", new JScrollPane(txtDesc));
        addRow(form, 2, "Dự án *", cmbProject);
        addRow(form, 3, "Chế độ chạy", cmbMode);
        addRow(form, 4, "Các bước", stepsPanel);
        addRow(form, 5, "Thư mục DEPLOY", deployRow);
        addRow(form, 6, "", hint);

        lblError.setForeground(UiTheme.DANGER);
        lblError.setFont(UiTheme.FONT_NORMAL);
        lblError.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));

        JButton btnSave = UiFactory.button("Lưu", UiTheme.SUCCESS);
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
    }

    private void addRow(JPanel panel, int row, String label, Component field) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 4, 5, 4);
        c.gridy = row;
        c.anchor = GridBagConstraints.NORTHWEST;

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

    private void fillData(List<Project> projects, List<PipelineStep> steps) {
        if (editing != null) {
            txtName.setText(editing.getName());
            txtDesc.setText(editing.getDescription());
            cmbMode.setSelectedItem(editing.getRunMode());
            for (Project p : projects) {
                if (p.getId() == editing.getProjectId()) {
                    cmbProject.setSelectedItem(p);
                    break;
                }
            }
        }

        List<PipelineStep> sorted = new ArrayList<>(steps);
        sorted.sort(Comparator.comparingInt(PipelineStep::getStepOrder));
        for (int i = 0; i < sorted.size(); i++) {
            PipelineStep s = sorted.get(i);
            stepModel.addRow(new Object[]{i + 1, s.getStepType(), s.isEnabled()});
            if (s.getStepType() == StepType.DEPLOY && s.getDeployDir() != null) {
                txtDeployDir.setText(s.getDeployDir());
            }
        }
    }

    private void moveStep(int direction) {
        if (stepTable.isEditing()) {
            stepTable.getCellEditor().stopCellEditing();
        }
        int row = stepTable.getSelectedRow();
        int target = row + direction;
        if (row < 0 || target < 0 || target >= stepModel.getRowCount()) {
            return;
        }
        stepModel.moveRow(row, row, target);
        for (int i = 0; i < stepModel.getRowCount(); i++) {
            stepModel.setValueAt(i + 1, i, 0);
        }
        stepTable.setRowSelectionInterval(target, target);
    }

    private void browse() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn thư mục triển khai thử nghiệm");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        File current = new File(txtDeployDir.getText().trim());
        if (current.isDirectory()) {
            chooser.setCurrentDirectory(current);
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            txtDeployDir.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void onSave() {
        if (stepTable.isEditing()) {
            stepTable.getCellEditor().stopCellEditing();
        }

        Pipeline p = new Pipeline();
        if (editing != null) {
            p.setId(editing.getId());
            p.setCreatedAt(editing.getCreatedAt());
        }
        Project project = (Project) cmbProject.getSelectedItem();
        p.setProjectId(project == null ? 0 : project.getId());
        p.setName(txtName.getText());
        p.setDescription(txtDesc.getText());
        p.setRunMode((RunMode) cmbMode.getSelectedItem());

        List<PipelineStep> steps = new ArrayList<>();
        for (int i = 0; i < stepModel.getRowCount(); i++) {
            StepType type = (StepType) stepModel.getValueAt(i, 1);
            boolean enabled = Boolean.TRUE.equals(stepModel.getValueAt(i, 2));
            PipelineStep step = new PipelineStep(type, i + 1, enabled);
            if (type == StepType.DEPLOY) {
                step.setDeployDir(txtDeployDir.getText());
            }
            steps.add(step);
        }

        try {
            service.save(p, steps);
            saved = true;
            dispose();
        } catch (ServiceException e) {
            lblError.setText(e.getMessage());
            pack();
        }
    }
}