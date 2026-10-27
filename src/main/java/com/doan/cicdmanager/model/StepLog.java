package com.doan.cicdmanager.model;

import java.time.LocalDateTime;

public class StepLog {

    private int id;
    private int runId;
    private StepType stepType;
    private RunStatus status = RunStatus.WAITING;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Integer exitCode;
    private String logText;
    /** Chỉ dùng để hiển thị khi tìm kiếm log, lấy từ phép JOIN. */
    private String pipelineName;

    public StepLog() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRunId() { return runId; }
    public void setRunId(int runId) { this.runId = runId; }

    public StepType getStepType() { return stepType; }
    public void setStepType(StepType stepType) { this.stepType = stepType; }

    public RunStatus getStatus() { return status; }
    public void setStatus(RunStatus status) { this.status = status; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }

    public Integer getExitCode() { return exitCode; }
    public void setExitCode(Integer exitCode) { this.exitCode = exitCode; }

    public String getLogText() { return logText; }
    public void setLogText(String logText) { this.logText = logText; }

    public String getPipelineName() { return pipelineName; }
    public void setPipelineName(String pipelineName) { this.pipelineName = pipelineName; }
}