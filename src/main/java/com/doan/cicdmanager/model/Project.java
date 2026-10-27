package com.doan.cicdmanager.model;

import java.time.LocalDateTime;

public class Project {

    private int id;
    private String name;
    private String description;
    private String path;
    private Integer createdBy;
    private LocalDateTime createdAt;

    public Project() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** Dùng để hiển thị tên dự án trong ô chọn (JComboBox). */
    @Override
    public String toString() {
        return name;
    }
}