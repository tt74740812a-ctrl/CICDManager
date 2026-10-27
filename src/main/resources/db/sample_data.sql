INSERT OR IGNORE INTO projects (id, name, description, path, created_at) VALUES
    (1, 'Sample Maven App', 'Dự án Java Maven mẫu để thử build và test', 'D:\DoAn\SampleMavenApp', '2026-10-20 08:00:00'),
    (2, 'Demo Web Shop', 'Dự án mô phỏng cho chế độ DEMO', 'D:\DoAn\DemoWebShop', '2026-10-21 09:30:00');

INSERT OR IGNORE INTO pipelines (id, project_id, name, description, run_mode, created_at) VALUES
    (1, 1, 'Full CI/CD', 'Build, test rồi deploy thử nghiệm', 'DEMO', '2026-10-20 08:10:00'),
    (2, 2, 'Build và Test', 'Chỉ build và test, không deploy', 'DEMO', '2026-10-21 09:40:00');

INSERT OR IGNORE INTO pipeline_steps (id, pipeline_id, step_type, step_order, enabled, deploy_dir) VALUES
    (1, 1, 'BUILD', 1, 1, NULL),
    (2, 1, 'TEST', 2, 1, NULL),
    (3, 1, 'DEPLOY', 3, 1, 'D:\DoAn\deploy_test'),
    (4, 2, 'BUILD', 1, 1, NULL),
    (5, 2, 'TEST', 2, 1, NULL),
    (6, 2, 'DEPLOY', 3, 0, NULL);

INSERT OR IGNORE INTO pipeline_runs (id, pipeline_id, status, started_at, finished_at, result_message) VALUES
    (1, 1, 'SUCCESS', '2026-10-22 10:00:00', '2026-10-22 10:00:12', 'Pipeline hoàn tất thành công'),
    (2, 2, 'FAILED', '2026-10-22 14:30:00', '2026-10-22 14:30:08', 'Bước TEST thất bại');

INSERT OR IGNORE INTO step_logs (id, run_id, step_type, status, started_at, finished_at, exit_code, log_text) VALUES
    (1, 1, 'BUILD', 'SUCCESS', '2026-10-22 10:00:00', '2026-10-22 10:00:05', 0, 'BUILD SUCCESS (mô phỏng)'),
    (2, 1, 'TEST', 'SUCCESS', '2026-10-22 10:00:05', '2026-10-22 10:00:09', 0, 'Tests run: 5, Failures: 0 (mô phỏng)'),
    (3, 1, 'DEPLOY', 'SUCCESS', '2026-10-22 10:00:09', '2026-10-22 10:00:12', 0, 'Đã triển khai thử nghiệm (mô phỏng)'),
    (4, 2, 'BUILD', 'SUCCESS', '2026-10-22 14:30:00', '2026-10-22 14:30:04', 0, 'BUILD SUCCESS (mô phỏng)'),
    (5, 2, 'TEST', 'FAILED', '2026-10-22 14:30:04', '2026-10-22 14:30:08', 1, 'Tests run: 5, Failures: 2 (mô phỏng)'),
    (6, 2, 'DEPLOY', 'SKIPPED', NULL, NULL, NULL, 'Bỏ qua vì bước trước thất bại');