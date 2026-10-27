package com.doan.cicdmanager.model;

import java.time.Duration;
import java.time.LocalDateTime;

public class PipelineRun {

    private int id;
    private int pipelineId;
    private Integer triggeredBy;
    private RunStatus status = RunStatus.WAITING;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String resultMessage;
    /** Chỉ dùng để hiển thị, lấy từ phép JOIN. */
    private String pipelineName;
    private String projectName;

    public PipelineRun() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPipelineId() { return pipelineId; }
    public void setPipelineId(int pipelineId) { this.pipelineId = pipelineId; }

    public Integer getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(Integer triggeredBy) { this.triggeredBy = triggeredBy; }

    public RunStatus getStatus() { return status; }
    public void setStatus(RunStatus status) { this.status = status; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }

    public String getResultMessage() { return resultMessage; }
    public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }

    public String getPipelineName() { return pipelineName; }
    public void setPipelineName(String pipelineName) { this.pipelineName = pipelineName; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    /** Thời gian chạy (giây), hoặc null nếu chưa chạy xong. */
    public Long getDurationSeconds() {
        if (startedAt == null || finishedAt == null) {
            return null;
        }
        return Duration.between(startedAt, finishedAt).getSeconds();
    }
}