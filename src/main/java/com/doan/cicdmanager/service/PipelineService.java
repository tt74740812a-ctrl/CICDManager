package com.doan.cicdmanager.service;

import com.doan.cicdmanager.dao.PipelineDAO;
import com.doan.cicdmanager.dao.PipelineStepDAO;
import com.doan.cicdmanager.dao.ProjectDAO;
import com.doan.cicdmanager.model.Pipeline;
import com.doan.cicdmanager.model.PipelineStep;
import com.doan.cicdmanager.model.Project;
import com.doan.cicdmanager.model.RunMode;
import com.doan.cicdmanager.model.StepType;
import com.doan.cicdmanager.util.PathUtil;
import com.doan.cicdmanager.util.ValidationUtil;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class PipelineService {

    private final PipelineDAO pipelineDAO = new PipelineDAO();
    private final PipelineStepDAO stepDAO = new PipelineStepDAO();
    private final ProjectDAO projectDAO = new ProjectDAO();

    public List<Pipeline> search(String keyword) throws ServiceException {
        try {
            return ValidationUtil.isBlank(keyword) ? pipelineDAO.findAll() : pipelineDAO.search(keyword);
        } catch (SQLException e) {
            throw new ServiceException("Không thể tải danh sách pipeline: " + e.getMessage(), e);
        }
    }

    public List<Project> listProjects() throws ServiceException {
        try {
            return projectDAO.findAll();
        } catch (SQLException e) {
            throw new ServiceException("Không thể tải danh sách dự án: " + e.getMessage(), e);
        }
    }

    /** Lấy đủ 3 bước của pipeline theo thứ tự thực hiện (bước nào thiếu sẽ được bổ sung ở trạng thái tắt). */
    public List<PipelineStep> getSteps(int pipelineId) throws ServiceException {
        try {
            List<PipelineStep> steps = new ArrayList<>(stepDAO.findByPipelineId(pipelineId));
            for (StepType type : StepType.values()) {
                boolean exists = steps.stream().anyMatch(s -> s.getStepType() == type);
                if (!exists) {
                    steps.add(new PipelineStep(type, steps.size() + 1, false));
                }
            }
            return steps;
        } catch (SQLException e) {
            throw new ServiceException("Không thể tải các bước của pipeline: " + e.getMessage(), e);
        }
    }

    /** Cấu hình mặc định cho pipeline mới: BUILD, TEST, DEPLOY đều bật. */
    public List<PipelineStep> defaultSteps() {
        List<PipelineStep> steps = new ArrayList<>();
        steps.add(new PipelineStep(StepType.BUILD, 1, true));
        steps.add(new PipelineStep(StepType.TEST, 2, true));
        steps.add(new PipelineStep(StepType.DEPLOY, 3, true));
        return steps;
    }

    /** Thêm mới (id = 0) hoặc cập nhật pipeline cùng các bước. Chỉ quản trị viên. */
    public void save(Pipeline pipeline, List<PipelineStep> steps) throws ServiceException {
        AccessControl.requireAdmin();
        try {
            String name = ValidationUtil.requireText(pipeline.getName(), "Tên pipeline", 100);
            String description = ValidationUtil.optionalText(pipeline.getDescription(), "Mô tả", 500);
            if (pipeline.getRunMode() == null) {
                throw new IllegalArgumentException("Vui lòng chọn chế độ chạy.");
            }
            if (pipeline.getProjectId() <= 0 || projectDAO.findById(pipeline.getProjectId()) == null) {
                throw new IllegalArgumentException("Vui lòng chọn một dự án hợp lệ.");
            }
            List<PipelineStep> normalized = validateSteps(steps, pipeline.getRunMode());

            int excludeId = pipeline.getId() == 0 ? -1 : pipeline.getId();
            if (pipelineDAO.existsByName(pipeline.getProjectId(), name, excludeId)) {
                throw new IllegalArgumentException("Dự án này đã có pipeline tên \"" + name + "\".");
            }

            pipeline.setName(name);
            pipeline.setDescription(description);

            if (pipeline.getId() == 0) {
                int id = pipelineDAO.insert(pipeline);
                pipeline.setId(id);
                try {
                    stepDAO.replaceSteps(id, normalized);
                } catch (SQLException e) {
                    pipelineDAO.delete(id); // hoàn tác để không để lại pipeline thiếu bước
                    throw e;
                }
            } else {
                if (!pipelineDAO.update(pipeline)) {
                    throw new IllegalArgumentException("Không tìm thấy pipeline cần sửa (có thể đã bị xóa).");
                }
                stepDAO.replaceSteps(pipeline.getId(), normalized);
            }
        } catch (IllegalArgumentException e) {
            throw new ServiceException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new ServiceException("Không thể lưu pipeline: " + e.getMessage(), e);
        }
    }

    /** Xóa pipeline cùng các bước, lịch sử chạy và log liên quan. Chỉ quản trị viên. */
    public void delete(int pipelineId) throws ServiceException {
        AccessControl.requireAdmin();
        try {
            if (!pipelineDAO.delete(pipelineId)) {
                throw new ServiceException("Không tìm thấy pipeline cần xóa (có thể đã bị xóa).");
            }
        } catch (SQLException e) {
            throw new ServiceException("Không thể xóa pipeline: " + e.getMessage(), e);
        }
    }

    /** Kiểm tra và chuẩn hóa các bước: sắp xếp theo thứ tự rồi đánh số lại 1, 2, 3. */
    private List<PipelineStep> validateSteps(List<PipelineStep> steps, RunMode mode) {
        if (steps == null || steps.size() != 3) {
            throw new IllegalArgumentException("Pipeline phải có đủ 3 bước BUILD, TEST và DEPLOY.");
        }
        List<PipelineStep> sorted = new ArrayList<>(steps);
        sorted.sort(Comparator.comparingInt(PipelineStep::getStepOrder));

        Set<StepType> seen = EnumSet.noneOf(StepType.class);
        boolean anyEnabled = false;
        int buildPos = -1;
        int testPos = -1;
        int deployPos = -1;

        for (int i = 0; i < sorted.size(); i++) {
            PipelineStep step = sorted.get(i);
            if (step.getStepType() == null || !seen.add(step.getStepType())) {
                throw new IllegalArgumentException("Mỗi bước BUILD, TEST, DEPLOY chỉ được xuất hiện một lần.");
            }
            step.setStepOrder(i + 1);
            if (step.isEnabled()) {
                anyEnabled = true;
                if (step.getStepType() == StepType.BUILD) {
                    buildPos = i;
                } else if (step.getStepType() == StepType.TEST) {
                    testPos = i;
                } else {
                    deployPos = i;
                }
            }

            if (step.getStepType() == StepType.DEPLOY) {
                if (ValidationUtil.isBlank(step.getDeployDir())) {
                    step.setDeployDir(null);
                    if (step.isEnabled() && mode == RunMode.REAL) {
                        throw new IllegalArgumentException(
                                "Chế độ REAL cần chọn thư mục triển khai thử nghiệm cho bước DEPLOY.");
                    }
                } else {
                    step.setDeployDir(PathUtil.requireAbsolutePath(step.getDeployDir(), "Thư mục triển khai", 260));
                }
            } else {
                step.setDeployDir(null);
            }
        }

        if (!anyEnabled) {
            throw new IllegalArgumentException("Cần bật ít nhất một bước cho pipeline.");
        }
        if (deployPos != -1 && ((buildPos != -1 && buildPos > deployPos) || (testPos != -1 && testPos > deployPos))) {
            throw new IllegalArgumentException("Bước DEPLOY phải chạy sau BUILD và TEST.");
        }
        return sorted;
    }
}