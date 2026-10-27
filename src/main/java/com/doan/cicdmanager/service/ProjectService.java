package com.doan.cicdmanager.service;

import com.doan.cicdmanager.dao.ProjectDAO;
import com.doan.cicdmanager.model.Project;
import com.doan.cicdmanager.util.PathUtil;
import com.doan.cicdmanager.util.ValidationUtil;
import java.sql.SQLException;
import java.util.List;

public class ProjectService {

    private final ProjectDAO projectDAO = new ProjectDAO();

    /** Tìm dự án theo từ khóa. Từ khóa trống thì lấy tất cả. */
    public List<Project> search(String keyword) throws ServiceException {
        try {
            return ValidationUtil.isBlank(keyword) ? projectDAO.findAll() : projectDAO.search(keyword);
        } catch (SQLException e) {
            throw new ServiceException("Không thể tải danh sách dự án: " + e.getMessage(), e);
        }
    }

    /** Thêm mới (id = 0) hoặc cập nhật dự án. Chỉ quản trị viên. */
    public void save(Project project) throws ServiceException {
        AccessControl.requireAdmin();
        try {
            String name = ValidationUtil.requireText(project.getName(), "Tên dự án", 100);
            String description = ValidationUtil.optionalText(project.getDescription(), "Mô tả", 500);
            String path = PathUtil.requireAbsolutePath(project.getPath(), "Đường dẫn thư mục dự án", 260);

            int excludeId = project.getId() == 0 ? -1 : project.getId();
            if (projectDAO.existsByName(name, excludeId)) {
                throw new ServiceException("Tên dự án \"" + name + "\" đã tồn tại. Vui lòng chọn tên khác.");
            }

            project.setName(name);
            project.setDescription(description);
            project.setPath(path);

            if (project.getId() == 0) {
                project.setCreatedBy(SessionManager.getCurrentUser().getId());
                project.setId(projectDAO.insert(project));
            } else if (!projectDAO.update(project)) {
                throw new ServiceException("Không tìm thấy dự án cần sửa (có thể đã bị xóa).");
            }
        } catch (IllegalArgumentException e) {
            throw new ServiceException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new ServiceException("Không thể lưu dự án: " + e.getMessage(), e);
        }
    }

    /** Xóa dự án cùng pipeline, lịch sử chạy và log liên quan. Chỉ quản trị viên. */
    public void delete(int projectId) throws ServiceException {
        AccessControl.requireAdmin();
        try {
            if (!projectDAO.delete(projectId)) {
                throw new ServiceException("Không tìm thấy dự án cần xóa (có thể đã bị xóa).");
            }
        } catch (SQLException e) {
            throw new ServiceException("Không thể xóa dự án: " + e.getMessage(), e);
        }
    }
}