package com.doan.cicdmanager.service;

import com.doan.cicdmanager.dao.PipelineRunDAO;
import com.doan.cicdmanager.dao.StepLogDAO;
import com.doan.cicdmanager.model.PipelineRun;
import com.doan.cicdmanager.model.RunStatus;
import com.doan.cicdmanager.model.StepLog;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Truy vấn lịch sử chạy, nhật ký và thống kê. Chỉ đọc dữ liệu, mọi người dùng đều xem được. */
public class HistoryService {

    /** Thống kê tổng hợp toàn hệ thống. */
    public static class OverallStats {
        public int total;
        public int success;
        public int failed;
        /** Số lần chạy chưa kết thúc (WAITING hoặc RUNNING). */
        public int active;
        public double successRate;
        public long avgSeconds;
    }

    /** Thống kê cho một pipeline. */
    public static class PipelineStat {
        public String pipelineName;
        public String projectName;
        public int total;
        public int success;
        public int failed;
        public double successRate;
        public long avgSeconds;
    }

    public static class Stats {
        public final OverallStats overall = new OverallStats();
        public final List<PipelineStat> byPipeline = new ArrayList<>();
    }

    private final PipelineRunDAO runDAO = new PipelineRunDAO();
    private final StepLogDAO logDAO = new StepLogDAO();

    /** Tìm các lần chạy theo từ khóa và trạng thái (status = null là tất cả). */
    public List<PipelineRun> searchRuns(String keyword, RunStatus status) throws ServiceException {
        try {
            return runDAO.search(keyword == null ? "" : keyword, status);
        } catch (SQLException e) {
            throw new ServiceException("Không thể tải lịch sử chạy: " + e.getMessage(), e);
        }
    }

    public List<StepLog> getStepLogs(int runId) throws ServiceException {
        try {
            return logDAO.findByRunId(runId);
        } catch (SQLException e) {
            throw new ServiceException("Không thể tải nhật ký của lần chạy: " + e.getMessage(), e);
        }
    }

    /** Tìm trong nội dung log, loại bước hoặc tên pipeline. */
    public List<StepLog> searchLogs(String keyword) throws ServiceException {
        try {
            return logDAO.search(keyword == null ? "" : keyword);
        } catch (SQLException e) {
            throw new ServiceException("Không thể tìm kiếm nhật ký: " + e.getMessage(), e);
        }
    }

    public Stats loadStats() throws ServiceException {
        List<PipelineRun> runs;
        try {
            runs = runDAO.search("", null);
        } catch (SQLException e) {
            throw new ServiceException("Không thể tải thống kê: " + e.getMessage(), e);
        }

        Stats stats = new Stats();
        OverallStats overall = stats.overall;
        Map<Integer, PipelineStat> byId = new LinkedHashMap<>();
        Map<Integer, long[]> sums = new LinkedHashMap<>(); // [0] = tổng giây, [1] = số lần đã kết thúc
        long totalSeconds = 0;
        int finished = 0;

        for (PipelineRun r : runs) {
            PipelineStat ps = byId.get(r.getPipelineId());
            if (ps == null) {
                ps = new PipelineStat();
                ps.pipelineName = r.getPipelineName();
                ps.projectName = r.getProjectName();
                byId.put(r.getPipelineId(), ps);
                sums.put(r.getPipelineId(), new long[2]);
            }

            overall.total++;
            ps.total++;

            boolean done = false;
            if (r.getStatus() == RunStatus.SUCCESS) {
                overall.success++;
                ps.success++;
                done = true;
            } else if (r.getStatus() == RunStatus.FAILED) {
                overall.failed++;
                ps.failed++;
                done = true;
            } else {
                overall.active++;
            }

            Long seconds = r.getDurationSeconds();
            if (done && seconds != null) {
                totalSeconds += seconds;
                finished++;
                long[] sum = sums.get(r.getPipelineId());
                sum[0] += seconds;
                sum[1]++;
            }
        }

        overall.successRate = rate(overall.success, overall.failed);
        overall.avgSeconds = finished == 0 ? 0 : totalSeconds / finished;

        for (Map.Entry<Integer, PipelineStat> entry : byId.entrySet()) {
            PipelineStat ps = entry.getValue();
            long[] sum = sums.get(entry.getKey());
            ps.successRate = rate(ps.success, ps.failed);
            ps.avgSeconds = sum[1] == 0 ? 0 : sum[0] / sum[1];
            stats.byPipeline.add(ps);
        }
        return stats;
    }

    private static double rate(int success, int failed) {
        int done = success + failed;
        return done == 0 ? 0 : success * 100.0 / done;
    }
}