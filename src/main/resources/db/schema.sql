-- Bảng người dùng. Mật khẩu chỉ lưu dạng hash BCrypt
CREATE TABLE IF NOT EXISTS users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    full_name     TEXT NOT NULL,
    role          TEXT NOT NULL CHECK (role IN ('ADMIN', 'USER')),
    created_at    TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS projects (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL UNIQUE,
    description TEXT,
    path        TEXT NOT NULL,
    created_by  INTEGER,
    created_at  TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS pipelines (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id  INTEGER NOT NULL,
    name        TEXT NOT NULL,
    description TEXT,
    run_mode    TEXT NOT NULL DEFAULT 'DEMO' CHECK (run_mode IN ('DEMO', 'REAL')),
    created_at  TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (project_id, name),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS pipeline_steps (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    pipeline_id INTEGER NOT NULL,
    step_type   TEXT NOT NULL CHECK (step_type IN ('BUILD', 'TEST', 'DEPLOY')),
    step_order  INTEGER NOT NULL CHECK (step_order > 0),
    enabled     INTEGER NOT NULL DEFAULT 1 CHECK (enabled IN (0, 1)),
    deploy_dir  TEXT,
    UNIQUE (pipeline_id, step_type),
    UNIQUE (pipeline_id, step_order),
    FOREIGN KEY (pipeline_id) REFERENCES pipelines(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS pipeline_runs (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    pipeline_id    INTEGER NOT NULL,
    triggered_by   INTEGER,
    status         TEXT NOT NULL CHECK (status IN ('WAITING', 'RUNNING', 'SUCCESS', 'FAILED')),
    started_at     TEXT,
    finished_at    TEXT,
    result_message TEXT,
    FOREIGN KEY (pipeline_id) REFERENCES pipelines(id) ON DELETE CASCADE,
    FOREIGN KEY (triggered_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS step_logs (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id      INTEGER NOT NULL,
    step_type   TEXT NOT NULL CHECK (step_type IN ('BUILD', 'TEST', 'DEPLOY')),
    status      TEXT NOT NULL CHECK (status IN ('WAITING', 'RUNNING', 'SUCCESS', 'FAILED', 'SKIPPED')),
    started_at  TEXT,
    finished_at TEXT,
    exit_code   INTEGER,
    log_text    TEXT,
    FOREIGN KEY (run_id) REFERENCES pipeline_runs(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pipelines_project ON pipelines(project_id);
CREATE INDEX IF NOT EXISTS idx_runs_pipeline ON pipeline_runs(pipeline_id);
CREATE INDEX IF NOT EXISTS idx_logs_run ON step_logs(run_id);