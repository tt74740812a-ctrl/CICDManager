package com.doan.cicdmanager.service;

import com.doan.cicdmanager.dao.PipelineDAO;
import com.doan.cicdmanager.dao.PipelineRunDAO;
import com.doan.cicdmanager.dao.ProjectDAO;
import com.doan.cicdmanager.model.PipelineRun;
import com.doan.cicdmanager.model.RunStatus;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DashboardService {

    /** Dữ liệu tổng hợp cho Dashboard. */
    public static class DashboardData {
        public int totalProjects;
        public int totalPipelines;
        public int totalRuns;
        public int successRuns;
        public int failedRuns;
        public List<PipelineRun> recentRuns = new ArrayList<>();
    }

    private final ProjectDAO projectDAO = new ProjectDAO();
    private final PipelineDAO pipelineDAO = new PipelineDAO();
    private final PipelineRunDAO runDAO = new PipelineRunDAO();

    public DashboardData load(int recentLimit) throws ServiceException {
        try {
            DashboardData data = new DashboardData();
            data.totalProjects = projectDAO.count();
            data.totalPipelines = pipelineDAO.count();
            data.totalRuns = runDAO.count();
            data.successRuns = runDAO.countByStatus(RunStatus.SUCCESS);
            data.failedRuns = runDAO.countByStatus(RunStatus.FAILED);
            data.recentRuns = runDAO.findRecent(recentLimit);
            return data;
        } catch (SQLException e) {
            throw new ServiceException("Không thể tải dữ liệu Dashboard: " + e.getMessage(), e);
        }
    }
}