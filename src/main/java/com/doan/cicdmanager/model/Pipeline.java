package com.doan.cicdmanager.model;

import java.time.LocalDateTime;

public class Pipeline {

    private int id;
    private int projectId;
    private String name;
    private String description;
    private RunMode runMode = RunMode.DEMO;
    private LocalDateTime createdAt;
    /** Chỉ dùng để hiển thị, lấy từ phép JOIN với bảng projects. */
    private String projectName;

    public Pipeline() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public RunMode getRunMode() { return runMode; }
    public void setRunMode(RunMode runMode) { this.runMode = runMode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    @Override
    public String toString() {
        return name;
    }
}