package com.doan.cicdmanager.ui;

import com.doan.cicdmanager.model.Pipeline;
import com.doan.cicdmanager.model.PipelineStep;
import com.doan.cicdmanager.model.RunMode;
import com.doan.cicdmanager.model.RunStatus;
import com.doan.cicdmanager.model.StepType;
import com.doan.cicdmanager.service.ExecutionEvent;
import com.doan.cicdmanager.service.PipelineExecutor;
import com.doan.cicdmanager.service.PipelineService;
import com.doan.cicdmanager.service.ServiceException;
import com.doan.cicdmanager.util.DateUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

public class RunPanel extends JPanel implements Refreshable {

    private static final String[] SIM_OPTIONS = {"Thành công", "Lỗi ở BUILD", "Lỗi ở TEST", "Lỗi ở DEPLOY"};
    private static final int MAX_LOG_CHARS = 300_000;

    private final PipelineService pipelineService = new PipelineService();

    private final JComboBox<Pipeline> cmbPipeline = new JComboBox<>();
    private final JComboBox<String> cmbSimulate = new JComboBox<>(SIM_OPTIONS);
    private final JLabel lblMode = new JLabel("-");
    private final JButton btnRun = UiFactory.button("Chạy pipeline", UiTheme.SUCCESS);
    private final JButton btnStop = UiFactory.button("Dừng", UiTheme.DANGER);

    private final JLabel lblStatus = new JLabel("SẴN SÀNG", SwingConstants.CENTER);
    private final JProgressBar progress = new JProgressBar(0, 100);
    private final JPanel stepsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JLabel lblRunInfo = new JLabel(" ");
    private final JLabel lblStart = new JLabel(" ");
    private final JLabel lblEnd = new JLabel(" ");
    private final JLabel lblResult = new JLabel(" ");
    private final JTextArea txtLog = new JTextArea();

    private final Map<StepType, JLabel> chips = new EnumMap<>(StepType.class);
    private final Map<StepType, RunStatus> chipStatus = new EnumMap<>(StepType.class);
    private List<PipelineStep> currentSteps = new ArrayList<>();

    private PipelineExecutor executor;
    private boolean running;
    private boolean updatingCombo;
    private int pendingPipelineId = -1;
    
    public RunPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(buildControls(), BorderLayout.NORTH);
        add(buildMiddle(), BorderLayout.CENTER);
        btnStop.setEnabled(false);
        resetDisplay();
    }

    /** Chọn sẵn pipeline này trong ô chọn ở lần mở màn hình kế tiếp. */
    public void selectPipeline(int pipelineId) {
        pendingPipelineId = pipelineId;
    }
    
    public boolean isRunning() {
        return running;
    }

    /** Gửi yêu cầu dừng pipeline đang chạy (dùng khi đóng ứng dụng). */
    public void cancelRun() {
        if (executor != null) {
            executor.cancel();
        }
    }

    // ------------------------------------------------------------------ dựng giao diện

    private JPanel buildControls() {
        cmbPipeline.setFont(UiTheme.FONT_NORMAL);
        cmbPipeline.setPreferredSize(new Dimension(320, 32));
        cmbPipeline.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Pipeline) {
                    Pipeline p = (Pipeline) value;
                    setText(p.getName() + "  (" + p.getProjectName() + ")");
                }
                return c;
            }
        });
        cmbPipeline.addActionListener(e -> {
            if (!updatingCombo) {
                onPipelineChanged();
            }
        });

        cmbSimulate.setFont(UiTheme.FONT_NORMAL);
        lblMode.setFont(UiTheme.FONT_BOLD);

        btnRun.addActionListener(e -> onRun());
        btnStop.addActionListener(e -> onStop());

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        bar.add(label("Pipeline:"));
        bar.add(cmbPipeline);
        bar.add(label("Chế độ:"));
        bar.add(lblMode);
        bar.add(label("Mô phỏng (DEMO):"));
        bar.add(cmbSimulate);
        bar.add(btnRun);
        bar.add(btnStop);
        return bar;
    }

    private JPanel buildMiddle() {
        lblStatus.setOpaque(true);
        lblStatus.setForeground(Color.WHITE);
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblStatus.setPreferredSize(new Dimension(150, 0));

        progress.setStringPainted(true);
        progress.setFont(UiTheme.FONT_NORMAL);

        for (JLabel l : new JLabel[]{lblRunInfo, lblStart, lblEnd, lblResult}) {
            l.setFont(UiTheme.FONT_NORMAL);
        }
        lblRunInfo.setFont(UiTheme.FONT_BOLD);

        stepsRow.setOpaque(false);
        JPanel infoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 0));
        infoRow.setOpaque(false);
        infoRow.add(lblRunInfo);
        infoRow.add(lblStart);
        infoRow.add(lblEnd);
        infoRow.add(lblResult);

        JPanel center = new JPanel(new GridLayout(3, 1, 0, 8));
        center.setOpaque(false);
        center.add(progress);
        center.add(stepsRow);
        center.add(infoRow);

        JPanel statusCard = new JPanel(new BorderLayout(16, 0));
        statusCard.setBackground(Color.WHITE);
        statusCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        statusCard.setPreferredSize(new Dimension(0, 140));
        statusCard.add(lblStatus, BorderLayout.WEST);
        statusCard.add(center, BorderLayout.CENTER);

        txtLog.setEditable(false);
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtLog.setBackground(new Color(30, 30, 30));
        txtLog.setForeground(new Color(220, 220, 220));
        txtLog.setCaretColor(Color.WHITE);
        JScrollPane scroll = new JScrollPane(txtLog);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));

        JLabel logTitle = new JLabel("Nhật ký thực thi");
        logTitle.setFont(UiTheme.FONT_BOLD);
        logTitle.setForeground(UiTheme.PRIMARY);
        logTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setOpaque(false);
        logPanel.add(logTitle, BorderLayout.NORTH);
        logPanel.add(scroll, BorderLayout.CENTER);

        JPanel middle = new JPanel(new BorderLayout(0, 12));
        middle.setOpaque(false);
        middle.add(statusCard, BorderLayout.NORTH);
        middle.add(logPanel, BorderLayout.CENTER);
        return middle;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UiTheme.FONT_NORMAL);
        return l;
    }

    // ------------------------------------------------------------------ dữ liệu và trạng thái hiển thị

    @Override
    public void refresh() {
        if (running) {
            return;
        }
        try {
            Pipeline previous = (Pipeline) cmbPipeline.getSelectedItem();
            List<Pipeline> list = pipelineService.search("");
            updatingCombo = true;
            try {
                cmbPipeline.removeAllItems();
                for (Pipeline p : list) {
                    cmbPipeline.addItem(p);
                }
                int wantedId = pendingPipelineId > 0
                        ? pendingPipelineId
                        : (previous != null ? previous.getId() : -1);
                pendingPipelineId = -1;
                for (Pipeline p : list) {
                    if (p.getId() == wantedId) {
                        cmbPipeline.setSelectedItem(p);
                        break;
                    }
                }
            } finally {
                updatingCombo = false;
            }
            btnRun.setEnabled(!list.isEmpty());
            onPipelineChanged();
        } catch (ServiceException e) {
            UiFactory.showError(this, e.getMessage());
        }
    }

    private void onPipelineChanged() {
        Pipeline p = (Pipeline) cmbPipeline.getSelectedItem();
        if (p == null) {
            lblMode.setText("-");
            currentSteps = new ArrayList<>();
            rebuildChips();
            resetDisplay();
            return;
        }
        lblMode.setText(p.getRunMode().name());
        lblMode.setForeground(p.getRunMode() == RunMode.DEMO ? UiTheme.INFO : UiTheme.WARNING);
        cmbSimulate.setEnabled(!running && p.getRunMode() == RunMode.DEMO);
        try {
            currentSteps = pipelineService.getSteps(p.getId());
        } catch (ServiceException e) {
            currentSteps = new ArrayList<>();
            UiFactory.showError(this, e.getMessage());
        }
        rebuildChips();
        resetDisplay();
    }

    private void resetDisplay() {
        showBadge("SẴN SÀNG", UiTheme.MUTED);
        progress.setValue(0);
        progress.setString("Chưa chạy");
        lblRunInfo.setText(" ");
        lblStart.setText(" ");
        lblEnd.setText(" ");
        lblResult.setText(" ");
        lblResult.setForeground(UiTheme.MUTED);
    }

    private void showBadge(String text, Color color) {
        lblStatus.setText(text);
        lblStatus.setBackground(color);
    }

    private void rebuildChips() {
        stepsRow.removeAll();
        chips.clear();
        chipStatus.clear();
        for (PipelineStep s : currentSteps) {
            JLabel chip = new JLabel(s.getStepOrder() + ". " + s.getStepType() + (s.isEnabled() ? "" : " (tắt)"),
                    SwingConstants.CENTER);
            chip.setOpaque(true);
            chip.setForeground(Color.WHITE);
            chip.setFont(UiTheme.FONT_BOLD);
            chip.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
            chips.put(s.getStepType(), chip);
            stepsRow.add(chip);
            setChip(s.getStepType(), s.isEnabled() ? RunStatus.WAITING : RunStatus.SKIPPED);
        }
        stepsRow.revalidate();
        stepsRow.repaint();
    }

    private void setChip(StepType type, RunStatus status) {
        JLabel chip = chips.get(type);
        if (chip == null) {
            return;
        }
        chipStatus.put(type, status);
        chip.setBackground(UiTheme.statusColor(status));
    }

    private void setRunning(boolean value) {
        running = value;
        Pipeline p = (Pipeline) cmbPipeline.getSelectedItem();
        cmbPipeline.setEnabled(!value);
        cmbSimulate.setEnabled(!value && p != null && p.getRunMode() == RunMode.DEMO);
        btnRun.setEnabled(!value && p != null);
        btnStop.setEnabled(value);
    }

    private void appendLog(String text) {
        txtLog.append(text + "\n");
        int length = txtLog.getDocument().getLength();
        if (length > MAX_LOG_CHARS) {
            txtLog.replaceRange("", 0, length - MAX_LOG_CHARS / 2);
        }
        txtLog.setCaretPosition(txtLog.getDocument().getLength());
    }

    // ------------------------------------------------------------------ chạy và dừng

    private void onRun() {
        Pipeline p = (Pipeline) cmbPipeline.getSelectedItem();
        if (p == null) {
            UiFactory.showInfo(this, "Chưa có pipeline nào. Hãy tạo pipeline ở mục Quản lý pipeline.");
            return;
        }
        if (p.getRunMode() == RunMode.REAL) {
            boolean ok = UiFactory.confirm(this,
                    "Chế độ REAL sẽ chạy Maven thật trong thư mục của dự án \"" + p.getProjectName()
                    + "\" và có thể sao chép file vào thư mục triển khai thử nghiệm.\nBạn có muốn tiếp tục?",
                    "Xác nhận chạy REAL");
            if (!ok) {
                return;
            }
        }

        StepType failAt = null;
        switch (cmbSimulate.getSelectedIndex()) {
            case 1:
                failAt = StepType.BUILD;
                break;
            case 2:
                failAt = StepType.TEST;
                break;
            case 3:
                failAt = StepType.DEPLOY;
                break;
            default:
                break;
        }
        final StepType simulateFailure = failAt;
        final int pipelineId = p.getId();

        onPipelineChanged(); // làm mới danh sách bước và vùng hiển thị trước khi chạy
        txtLog.setText("");
        setRunning(true);

        executor = new PipelineExecutor();
        final PipelineExecutor exec = executor;

        SwingWorker<RunStatus, ExecutionEvent> worker = new SwingWorker<RunStatus, ExecutionEvent>() {
            @Override
            protected RunStatus doInBackground() throws Exception {
                return exec.execute(pipelineId, simulateFailure, event -> publish(event));
            }

            @Override
            protected void process(List<ExecutionEvent> chunks) {
                for (ExecutionEvent event : chunks) {
                    handle(event);
                }
            }

            @Override
            protected void done() {
                setRunning(false);
                try {
                    get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    UiFactory.showError(RunPanel.this, cause instanceof ServiceException
                            ? cause.getMessage()
                            : "Đã xảy ra lỗi không mong muốn: " + cause);
                    resetDisplay();
                }
            }
        };
        worker.execute();
    }

    private void onStop() {
        if (executor != null) {
            executor.cancel();
            btnStop.setEnabled(false);
            appendLog("[INFO] Đã gửi yêu cầu dừng pipeline...");
        }
    }

    // ------------------------------------------------------------------ xử lý sự kiện từ luồng chạy

    private void handle(ExecutionEvent e) {
        switch (e.kind) {
            case RUN_STARTED:
                lblRunInfo.setText("Lần chạy #" + e.runId);
                lblStart.setText("Bắt đầu: " + DateUtil.toDisplay(DateUtil.now()));
                showBadge("RUNNING", UiTheme.statusColor(RunStatus.RUNNING));
                progress.setValue(0);
                progress.setString("Bắt đầu...");
                break;
            case STEP_STARTED:
                setChip(e.step, RunStatus.RUNNING);
                progress.setString("Đang chạy " + e.step + " (" + e.index + "/" + e.total + ")");
                break;
            case LOG:
                appendLog(e.text);
                break;
            case PROGRESS:
                progress.setValue(e.percent);
                break;
            case STEP_FINISHED:
                setChip(e.step, e.status);
                break;
            case RUN_FINISHED:
                finishRun(e);
                break;
            default:
                break;
        }
    }

    private void finishRun(ExecutionEvent e) {
        Color color = UiTheme.statusColor(e.status);
        showBadge(e.status.name(), color);
        lblEnd.setText("Kết thúc: " + DateUtil.toDisplay(DateUtil.now()));
        lblResult.setText("Kết quả: " + e.text);
        lblResult.setForeground(color);
        if (e.status == RunStatus.SUCCESS) {
            progress.setValue(100);
            progress.setString("Hoàn tất");
        } else {
            progress.setString("Thất bại hoặc đã dừng");
        }
        for (StepType type : chips.keySet()) {
            if (chipStatus.get(type) == RunStatus.WAITING) {
                setChip(type, RunStatus.SKIPPED);
            }
        }
    }
}