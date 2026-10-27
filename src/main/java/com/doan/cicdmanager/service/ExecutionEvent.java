package com.doan.cicdmanager.service;

import com.doan.cicdmanager.model.RunStatus;
import com.doan.cicdmanager.model.StepType;

/** Sự kiện phát ra trong lúc pipeline chạy, để giao diện cập nhật trạng thái. */
public class ExecutionEvent {

    public enum Kind { RUN_STARTED, STEP_STARTED, LOG, PROGRESS, STEP_FINISHED, RUN_FINISHED }

    public final Kind kind;
    public final int runId;
    public final StepType step;
    public final RunStatus status;
    public final String text;
    public final int index;
    public final int total;
    public final int percent;

    private ExecutionEvent(Kind kind, int runId, StepType step, RunStatus status,
                           String text, int index, int total, int percent) {
        this.kind = kind;
        this.runId = runId;
        this.step = step;
        this.status = status;
        this.text = text;
        this.index = index;
        this.total = total;
        this.percent = percent;
    }

    public static ExecutionEvent runStarted(int runId, int total) {
        return new ExecutionEvent(Kind.RUN_STARTED, runId, null, RunStatus.RUNNING, null, 0, total, 0);
    }

    public static ExecutionEvent stepStarted(StepType step, int index, int total) {
        return new ExecutionEvent(Kind.STEP_STARTED, 0, step, RunStatus.RUNNING, null, index, total, 0);
    }

    public static ExecutionEvent log(String text) {
        return new ExecutionEvent(Kind.LOG, 0, null, null, text, 0, 0, 0);
    }

    public static ExecutionEvent progress(int percent) {
        return new ExecutionEvent(Kind.PROGRESS, 0, null, null, null, 0, 0, percent);
    }

    public static ExecutionEvent stepFinished(StepType step, RunStatus status) {
        return new ExecutionEvent(Kind.STEP_FINISHED, 0, step, status, null, 0, 0, 0);
    }

    public static ExecutionEvent runFinished(RunStatus status, String message) {
        return new ExecutionEvent(Kind.RUN_FINISHED, 0, null, status, message, 0, 0, 0);
    }
}