package com.doan.cicdmanager.ui;

import com.doan.cicdmanager.model.Project;
import com.doan.cicdmanager.service.ProjectService;
import com.doan.cicdmanager.service.ServiceException;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ProjectDialog extends JDialog {

    private final ProjectService service;
    private final Project editing;

    private final JTextField txtName = new JTextField(30);
    private final JTextArea txtDesc = new JTextArea(4, 30);
    private final JTextField txtPath = new JTextField(24);
    private final JLabel lblError = new JLabel(" ");
    private boolean saved;

    /** editing = null nghĩa là thêm mới. */
    public ProjectDialog(Window owner, ProjectService service, Project editing) {
        super(owner, editing == null ? "Thêm dự án" : "Sửa dự án", ModalityType.APPLICATION_MODAL);
        this.service = service;
        this.editing = editing;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        buildUi();
        if (editing != null) {
            txtName.setText(editing.getName());
            txtDesc.setText(editing.getDescription());
            txtPath.setText(editing.getPath());
        }
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    public boolean isSaved() {
        return saved;
    }

    private void buildUi() {
        txtName.setFont(UiTheme.FONT_NORMAL);
        txtPath.setFont(UiTheme.FONT_NORMAL);
        txtDesc.setFont(UiTheme.FONT_NORMAL);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);

        JButton btnBrowse = UiFactory.button("Chọn...", UiTheme.PRIMARY_LIGHT);
        btnBrowse.addActionListener(e -> browse());

        JPanel pathRow = new JPanel(new BorderLayout(6, 0));
        pathRow.add(txtPath, BorderLayout.CENTER);
        pathRow.add(btnBrowse, BorderLayout.EAST);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        addRow(form, 0, "Tên dự án *", txtName);
        addRow(form, 1, "Mô tả", new JScrollPane(txtDesc));
        addRow(form, 2, "Thư mục dự án *", pathRow);

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
        getRootPane().setDefaultButton(btnSave);
    }

    private void addRow(JPanel panel, int row, String label, java.awt.Component field) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 4, 6, 4);
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

    private void browse() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn thư mục dự án");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        File current = new File(txtPath.getText().trim());
        if (current.isDirectory()) {
            chooser.setCurrentDirectory(current);
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            txtPath.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void onSave() {
        Project p = new Project();
        if (editing != null) {
            p.setId(editing.getId());
            p.setCreatedBy(editing.getCreatedBy());
            p.setCreatedAt(editing.getCreatedAt());
        }
        p.setName(txtName.getText());
        p.setDescription(txtDesc.getText());
        p.setPath(txtPath.getText());

        try {
            service.save(p);
            saved = true;
            dispose();
        } catch (ServiceException e) {
            lblError.setText(e.getMessage());
            pack();
        }
    }
}