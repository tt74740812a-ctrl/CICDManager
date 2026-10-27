package com.doan.cicdmanager.model;

public class PipelineStep {

    private int id;
    private int pipelineId;
    private StepType stepType;
    private int stepOrder;
    private boolean enabled = true;
    private String deployDir;

    public PipelineStep() {
    }

    public PipelineStep(StepType stepType, int stepOrder, boolean enabled) {
        this.stepType = stepType;
        this.stepOrder = stepOrder;
        this.enabled = enabled;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPipelineId() { return pipelineId; }
    public void setPipelineId(int pipelineId) { this.pipelineId = pipelineId; }

    public StepType getStepType() { return stepType; }
    public void setStepType(StepType stepType) { this.stepType = stepType; }

    public int getStepOrder() { return stepOrder; }
    public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getDeployDir() { return deployDir; }
    public void setDeployDir(String deployDir) { this.deployDir = deployDir; }
}