package com.doan.cicdmanager.service;

import com.doan.cicdmanager.dao.PipelineDAO;
import com.doan.cicdmanager.dao.PipelineRunDAO;
import com.doan.cicdmanager.dao.PipelineStepDAO;
import com.doan.cicdmanager.dao.ProjectDAO;
import com.doan.cicdmanager.dao.StepLogDAO;
import com.doan.cicdmanager.model.Pipeline;
import com.doan.cicdmanager.model.PipelineRun;
import com.doan.cicdmanager.model.PipelineStep;
import com.doan.cicdmanager.model.Project;
import com.doan.cicdmanager.model.RunMode;
import com.doan.cicdmanager.model.RunStatus;
import com.doan.cicdmanager.model.StepLog;
import com.doan.cicdmanager.model.StepType;
import com.doan.cicdmanager.model.User;
import com.doan.cicdmanager.util.DateUtil;
import com.doan.cicdmanager.util.MavenLocator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Thực thi pipeline (BUILD, TEST, DEPLOY) ở chế độ DEMO hoặc REAL và lưu lịch sử vào database.
 * Phương thức execute() chạy lâu nên phải gọi từ luồng nền, không gọi trực tiếp trên giao diện.
 */
public class PipelineExecutor {

    private static final AtomicBoolean BUSY = new AtomicBoolean(false);
    private static final long STEP_TIMEOUT_MILLIS = 10L * 60 * 1000;
    private static final int MAX_LOG_CHARS = 200_000;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final StepOutcome OK = new StepOutcome(0, false);

    private final PipelineDAO pipelineDAO = new PipelineDAO();
    private final ProjectDAO projectDAO = new ProjectDAO();
    private final PipelineStepDAO stepDAO = new PipelineStepDAO();
    private final PipelineRunDAO runDAO = new PipelineRunDAO();
    private final StepLogDAO logDAO = new StepLogDAO();

    private volatile boolean cancelled;
    private volatile Process currentProcess;

    /** Kết quả của một bước. exitCode = 0 là thành công. */
    private static final class StepOutcome {
        final int exitCode;
        final boolean cancelled;

        StepOutcome(int exitCode, boolean cancelled) {
            this.exitCode = exitCode;
            this.cancelled = cancelled;
        }
    }

    /** Bộ đệm nhật ký của một bước: vừa gom để lưu database, vừa gửi từng dòng về giao diện. */
    private static final class LogBuffer {
        private final StringBuilder sb = new StringBuilder();
        private final Consumer<ExecutionEvent> listener;
        private boolean truncated;

        LogBuffer(Consumer<ExecutionEvent> listener) {
            this.listener = listener;
        }

        synchronized void line(String text) {
            if (sb.length() < MAX_LOG_CHARS) {
                sb.append(text).append('\n');
            } else if (!truncated) {
                truncated = true;
                sb.append("... (nhật ký quá dài, phần còn lại không được lưu)\n");
            }
            emit(listener, ExecutionEvent.log(text));
        }

        synchronized String text() {
            return sb.toString();
        }
    }

    public static boolean isBusy() {
        return BUSY.get();
    }

    /** Yêu cầu dừng pipeline đang chạy. */
    public void cancel() {
        cancelled = true;
        Process p = currentProcess;
        if (p != null) {
            killTree(p);
        }
    }

    /**
     * Chạy pipeline và trả về trạng thái cuối cùng.
     * demoFailAt: ở chế độ DEMO, bước nào bị mô phỏng lỗi (null = tất cả thành công).
     */
    public RunStatus execute(int pipelineId, StepType demoFailAt, Consumer<ExecutionEvent> listener)
            throws ServiceException {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            throw new ServiceException("Bạn cần đăng nhập để chạy pipeline.");
        }
        if (!BUSY.compareAndSet(false, true)) {
            throw new ServiceException("Đang có một pipeline khác đang chạy. Vui lòng đợi hoàn tất.");
        }
        cancelled = false;
        currentProcess = null;
        int runId = 0;

        try {
            Pipeline pipeline = pipelineDAO.findById(pipelineId);
            if (pipeline == null) {
                throw new ServiceException("Không tìm thấy pipeline (có thể đã bị xóa).");
            }
            Project project = projectDAO.findById(pipeline.getProjectId());
            if (project == null) {
                throw new ServiceException("Không tìm thấy dự án của pipeline.");
            }

            List<PipelineStep> steps = new ArrayList<>(stepDAO.findByPipelineId(pipelineId));
            steps.sort(Comparator.comparingInt(PipelineStep::getStepOrder));
            int total = 0;
            for (PipelineStep s : steps) {
                if (s.isEnabled()) {
                    total++;
                }
            }
            if (total == 0) {
                throw new ServiceException("Pipeline chưa bật bước nào để chạy.");
            }

            String mavenCmd = pipeline.getRunMode() == RunMode.REAL ? preflightReal(project, steps) : null;

            PipelineRun run = new PipelineRun();
            run.setPipelineId(pipelineId);
            run.setTriggeredBy(user.getId());
            run.setStatus(RunStatus.WAITING);
            runId = runDAO.insert(run);
            runDAO.updateStatus(runId, RunStatus.RUNNING, DateUtil.now(), null, null);
            emit(listener, ExecutionEvent.runStarted(runId, total));

            RunStatus finalStatus = RunStatus.SUCCESS;
            String message = "Pipeline hoàn tất thành công.";
            boolean stop = false;
            int done = 0;

            for (PipelineStep step : steps) {
                StepType type = step.getStepType();
                if (!stop && cancelled) {
                    stop = true;
                    finalStatus = RunStatus.FAILED;
                    message = "Pipeline đã bị người dùng dừng.";
                }

                if (stop || !step.isEnabled()) {
                    StepLog skipped = new StepLog();
                    skipped.setRunId(runId);
                    skipped.setStepType(type);
                    skipped.setStatus(RunStatus.SKIPPED);
                    skipped.setLogText(step.isEnabled()
                            ? "Bỏ qua vì bước trước đó thất bại hoặc pipeline đã bị dừng."
                            : "Bước đã bị tắt trong cấu hình pipeline.");
                    logDAO.insert(skipped);
                    continue;
                }

                StepLog log = new StepLog();
                log.setRunId(runId);
                log.setStepType(type);
                log.setStatus(RunStatus.RUNNING);
                log.setStartedAt(DateUtil.now());
                int logId = logDAO.insert(log);
                emit(listener, ExecutionEvent.stepStarted(type, done + 1, total));

                LogBuffer buffer = new LogBuffer(listener);
                buffer.line("[" + clock() + "] Bắt đầu bước " + type + " (chế độ " + pipeline.getRunMode() + ")");

                StepOutcome outcome;
                try {
                    outcome = runStep(pipeline, project, step, mavenCmd, demoFailAt, buffer, done, total, listener);
                } catch (Exception e) {
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    buffer.line("[LỖI] " + (e.getMessage() == null ? e.toString() : e.getMessage()));
                    outcome = new StepOutcome(-1, false);
                }

                boolean ok = outcome.exitCode == 0;
                RunStatus stepStatus = ok ? RunStatus.SUCCESS : RunStatus.FAILED;
                buffer.line("[" + clock() + "] Kết thúc bước " + type + " với mã thoát " + outcome.exitCode);
                logDAO.update(logId, stepStatus, null, DateUtil.now(), outcome.exitCode, buffer.text());
                emit(listener, ExecutionEvent.stepFinished(type, stepStatus));

                done++;
                emit(listener, ExecutionEvent.progress(done * 100 / total));

                if (!ok) {
                    stop = true;
                    finalStatus = RunStatus.FAILED;
                    message = outcome.cancelled
                            ? "Pipeline đã bị người dùng dừng ở bước " + type + "."
                            : "Bước " + type + " thất bại (mã thoát " + outcome.exitCode + ").";
                }
            }

            runDAO.updateStatus(runId, finalStatus, null, DateUtil.now(), message);
            emit(listener, ExecutionEvent.runFinished(finalStatus, message));
            return finalStatus;

        } catch (SQLException e) {
            markFailedQuietly(runId, "Lỗi cơ sở dữ liệu: " + e.getMessage());
            throw new ServiceException("Lỗi cơ sở dữ liệu khi chạy pipeline: " + e.getMessage(), e);
        } finally {
            currentProcess = null;
            BUSY.set(false);
        }
    }

    // ------------------------------------------------------------------ kiểm tra trước khi chạy REAL

    private String preflightReal(Project project, List<PipelineStep> steps) throws ServiceException {
        boolean needsMaven = false;
        for (PipelineStep s : steps) {
            if (s.isEnabled() && (s.getStepType() == StepType.BUILD || s.getStepType() == StepType.TEST)) {
                needsMaven = true;
            }
        }
        if (!needsMaven) {
            return null;
        }

        try {
            Path dir = Paths.get(project.getPath());
            if (!Files.isDirectory(dir)) {
                throw new ServiceException("Thư mục dự án không tồn tại: " + project.getPath()
                        + ". Hãy sửa đường dẫn dự án hoặc dùng chế độ DEMO.");
            }
            if (!Files.isRegularFile(dir.resolve("pom.xml"))) {
                throw new ServiceException("Thư mục dự án không có file pom.xml nên không phải dự án Maven: "
                        + project.getPath());
            }
        } catch (InvalidPathException e) {
            throw new ServiceException("Đường dẫn dự án không hợp lệ: " + project.getPath(), e);
        }

        String maven = MavenLocator.find();
        if (maven == null) {
            throw new ServiceException("Không tìm thấy Maven trên máy. Hãy dùng chế độ DEMO, "
                    + "hoặc khai báo đường dẫn mvn trong file data/config.properties (maven.path=...).");
        }
        return maven;
    }

    // ------------------------------------------------------------------ chạy từng bước

    private StepOutcome runStep(Pipeline pipeline, Project project, PipelineStep step, String mavenCmd,
                                StepType demoFailAt, LogBuffer buf, int done, int total,
                                Consumer<ExecutionEvent> listener) throws Exception {
        if (pipeline.getRunMode() == RunMode.DEMO) {
            return runDemo(project, step.getStepType(), demoFailAt, buf, done, total, listener);
        }
        if (step.getStepType() == StepType.DEPLOY) {
            return deployReal(project, step, buf);
        }
        return runMaven(project, step.getStepType(), mavenCmd, buf);
    }

    /** Gọi Maven thật với lệnh cố định, chạy trong thư mục dự án. */
    private StepOutcome runMaven(Project project, StepType type, String mavenCmd, LogBuffer buf)
            throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add(mavenCmd);
        cmd.add("-B");
        if (type == StepType.BUILD) {
            cmd.add("clean");
            cmd.add("package");
            cmd.add("-DskipTests");
        } else {
            cmd.add("test");
        }

        buf.line("$ " + String.join(" ", cmd));
        buf.line("(thư mục làm việc: " + project.getPath() + ")");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(Paths.get(project.getPath()).toFile());
        pb.redirectErrorStream(true);
        pb.environment().put("JAVA_HOME", System.getProperty("java.home"));

        final Process process = pb.start();
        currentProcess = process;

        Thread reader = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    buf.line(line);
                }
            } catch (IOException e) {
                // luồng đọc bị đóng khi tiến trình kết thúc
            }
        }, "pipeline-output");
        reader.setDaemon(true);
        reader.start();

        long deadline = System.currentTimeMillis() + STEP_TIMEOUT_MILLIS;
        try {
            while (!process.waitFor(300, TimeUnit.MILLISECONDS)) {
                if (cancelled) {
                    killTree(process);
                    reader.join(2000);
                    buf.line("[INFO] Đã dừng tiến trình theo yêu cầu.");
                    return new StepOutcome(-1, true);
                }
                if (System.currentTimeMillis() > deadline) {
                    killTree(process);
                    reader.join(2000);
                    buf.line("[LỖI] Quá thời gian chờ tối đa (10 phút), đã dừng tiến trình.");
                    return new StepOutcome(-2, false);
                }
            }
            reader.join(3000);
            return new StepOutcome(process.exitValue(), false);
        } finally {
            currentProcess = null;
        }
    }

    /**
     * DEPLOY thử nghiệm: sao chép file .jar trong target vào thư mục release_<ngày_giờ> mới tạo
     * bên trong thư mục triển khai. Không ghi đè, không xóa file nào.
     */
    private StepOutcome deployReal(Project project, PipelineStep step, LogBuffer buf) throws IOException {
        if (step.getDeployDir() == null || step.getDeployDir().trim().isEmpty()) {
            buf.line("[LỖI] Pipeline chưa cấu hình thư mục triển khai.");
            return new StepOutcome(1, false);
        }
        Path projectDir = Paths.get(project.getPath()).toAbsolutePath().normalize();
        Path deployRoot = Paths.get(step.getDeployDir()).toAbsolutePath().normalize();

        if (deployRoot.getParent() == null) {
            buf.line("[LỖI] Không được triển khai vào thư mục gốc của ổ đĩa.");
            return new StepOutcome(1, false);
        }
        if (deployRoot.startsWith(projectDir)) {
            buf.line("[LỖI] Thư mục triển khai không được trùng hoặc nằm trong thư mục dự án.");
            return new StepOutcome(1, false);
        }
        if (Files.exists(deployRoot) && !Files.isDirectory(deployRoot)) {
            buf.line("[LỖI] Đường dẫn triển khai là một file, không phải thư mục: " + deployRoot);
            return new StepOutcome(1, false);
        }

        List<Path> artifacts = new ArrayList<>();
        Path targetDir = projectDir.resolve("target");
        if (Files.isDirectory(targetDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetDir, "*.jar")) {
                for (Path p : stream) {
                    if (Files.isRegularFile(p)) {
                        artifacts.add(p);
                    }
                }
            }
        }
        if (artifacts.isEmpty()) {
            buf.line("[LỖI] Không tìm thấy file .jar trong thư mục target. Hãy bật bước BUILD trước DEPLOY.");
            return new StepOutcome(1, false);
        }

        Files.createDirectories(deployRoot);
        Path releaseDir = deployRoot.resolve("release_" + LocalDateTime.now().format(STAMP));
        Files.createDirectory(releaseDir);
        buf.line("[INFO] Đã tạo thư mục phát hành: " + releaseDir);
        for (Path artifact : artifacts) {
            Files.copy(artifact, releaseDir.resolve(artifact.getFileName())); // không ghi đè
            buf.line("[INFO] Đã sao chép: " + artifact.getFileName());
        }
        buf.line("[INFO] Triển khai thử nghiệm hoàn tất.");
        return OK;
    }

    // ------------------------------------------------------------------ chế độ DEMO

    private StepOutcome runDemo(Project project, StepType type, StepType failAt, LogBuffer buf,
                                int done, int total, Consumer<ExecutionEvent> listener) {
        boolean fail = failAt == type;
        List<String> script = demoScript(type, fail, project.getName());
        int base = done * 100 / total;
        int span = 100 / total;

        for (int i = 0; i < script.size(); i++) {
            if (!pause(450)) {
                buf.line("[INFO] Đã nhận yêu cầu dừng.");
                return new StepOutcome(-1, true);
            }
            buf.line(script.get(i));
            emit(listener, ExecutionEvent.progress(Math.min(99, base + span * (i + 1) / script.size())));
        }
        return fail ? new StepOutcome(1, false) : OK;
    }

    private List<String> demoScript(StepType type, boolean fail, String projectName) {
        List<String> s = new ArrayList<>();
        switch (type) {
            case BUILD:
                s.add("[INFO] Scanning for projects...");
                s.add("[INFO] Building " + projectName + " 1.0-SNAPSHOT");
                s.add("[INFO] --- maven-resources-plugin:resources ---");
                s.add("[INFO] --- maven-compiler-plugin:compile ---");
                s.add("[INFO] Compiling 12 source files");
                if (fail) {
                    s.add("[ERROR] COMPILATION ERROR: cannot find symbol (mô phỏng lỗi)");
                    s.add("[INFO] BUILD FAILURE");
                } else {
                    s.add("[INFO] --- maven-jar-plugin:jar ---");
                    s.add("[INFO] Building jar: target/app-1.0-SNAPSHOT.jar");
                    s.add("[INFO] BUILD SUCCESS");
                }
                break;
            case TEST:
                s.add("[INFO] --- maven-surefire-plugin:test ---");
                s.add("[INFO] Running CalculatorTest");
                if (fail) {
                    s.add("[ERROR] Tests run: 5, Failures: 2, Errors: 0, Skipped: 0 (mô phỏng lỗi)");
                    s.add("[ERROR] testDivide: expected:<2> but was:<3>");
                    s.add("[INFO] BUILD FAILURE");
                } else {
                    s.add("[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0");
                    s.add("[INFO] BUILD SUCCESS");
                }
                break;
            default:
                s.add("[DEMO] Chế độ mô phỏng: không ghi file nào ra ngoài.");
                s.add("[DEMO] Chuẩn bị gói triển khai...");
                s.add("[DEMO] Sao chép bản build vào thư mục triển khai thử nghiệm (mô phỏng)...");
                if (fail) {
                    s.add("[ERROR] Không ghi được vào thư mục triển khai (mô phỏng lỗi).");
                } else {
                    s.add("[DEMO] Kiểm tra sau triển khai...");
                    s.add("[DEMO] Triển khai mô phỏng thành công.");
                }
                break;
        }
        return s;
    }

    /** Tạm dừng nhưng kiểm tra yêu cầu dừng mỗi 50 ms. Trả về false nếu bị yêu cầu dừng. */
    private boolean pause(long millis) {
        long end = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < end) {
            if (cancelled) {
                return false;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return !cancelled;
    }

    // ------------------------------------------------------------------ tiện ích

    /** Dừng tiến trình và cả các tiến trình con (Maven chạy qua cmd.exe nên có tiến trình con). */
    private static void killTree(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    private static String clock() {
        return LocalTime.now().format(CLOCK);
    }

    private static void emit(Consumer<ExecutionEvent> listener, ExecutionEvent event) {
        if (listener != null) {
            listener.accept(event);
        }
    }

    private void markFailedQuietly(int runId, String message) {
        if (runId <= 0) {
            return;
        }
        try {
            runDAO.updateStatus(runId, RunStatus.FAILED, null, DateUtil.now(), message);
        } catch (SQLException ignored) {
            // không thể ghi thêm vào database, bỏ qua
        }
    }
}