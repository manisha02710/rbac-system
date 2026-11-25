package com.rbac.service;

import com.rbac.exception.AccessDeniedException;
import com.rbac.exception.ResourceNotFoundException;
import com.rbac.exception.ValidationException;
import com.rbac.model.BaseUser;
import com.rbac.repository.Jdbc;
import com.rbac.util.FileStore;

import java.nio.file.Path;
import java.util.List;

/** Study materials: faculty upload files for their courses, everyone with MATERIAL_VIEW can list and download. */
public class MaterialService {
    private final AuthorizationService authz;
    private final AuditService audit;
    private final CourseService courses;

    public MaterialService(AuthorizationService authz, AuditService audit, CourseService courses) {
        this.authz = authz;
        this.audit = audit;
        this.courses = courses;
    }

    public List<String[]> list(BaseUser actor, int courseId) {
        authz.require(actor, "MATERIAL_VIEW");
        String sql = "SELECT m.material_id, c.course_code, m.title, COALESCE(m.description,'-'), m.file_name, "
            + "u.full_name, DATE(m.uploaded_at) FROM materials m JOIN courses c ON c.course_id = m.course_id "
            + "JOIN users u ON u.user_id = m.uploaded_by ";
        if (courseId == 0) return Jdbc.rows(sql + "ORDER BY m.material_id DESC");
        return Jdbc.rows(sql + "WHERE m.course_id = ? ORDER BY m.material_id DESC", courseId);
    }

    public List<String[]> search(BaseUser actor, String keyword) {
        authz.require(actor, "MATERIAL_VIEW");
        String like = "%" + keyword.trim() + "%";
        return Jdbc.rows("SELECT m.material_id, c.course_code, m.title, COALESCE(m.description,'-'), m.file_name, "
            + "u.full_name, DATE(m.uploaded_at) FROM materials m JOIN courses c ON c.course_id = m.course_id "
            + "JOIN users u ON u.user_id = m.uploaded_by WHERE m.title LIKE ? OR m.description LIKE ? "
            + "ORDER BY m.material_id DESC", like, like);
    }

    public long upload(BaseUser actor, int courseId, String title, String description, String filePath) {
        authz.require(actor, "MATERIAL_MANAGE");
        if (title.trim().length() < 3) throw new ValidationException("Title must be at least 3 characters.");
        courses.requireTeaches(actor, courseId);
        String[] stored = FileStore.store(filePath, "materials", "mat_");
        try {
            long id = Jdbc.insert("INSERT INTO materials (course_id, uploaded_by, title, description, file_name, file_path) "
                + "VALUES (?,?,?,?,?,?)", courseId, actor.getId(), title.trim(), description.trim(), stored[0], stored[1]);
            audit.log(actor.getId(), "MATERIAL_UPLOAD", "material " + id + ": " + title.trim(), true);
            return id;
        } catch (RuntimeException e) {
            FileStore.delete(stored[1]);          // do not leave an orphan file if the database insert failed
            throw e;
        }
    }

    /** Copies the file into the downloads folder. */
    public Path download(BaseUser actor, int materialId) {
        authz.require(actor, "MATERIAL_VIEW");
        String[] m = Jdbc.rows("SELECT file_name, file_path FROM materials WHERE material_id = ?", materialId)
            .stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("No material with id " + materialId));
        Path p = FileStore.copyOut(m[1], m[0]);
        audit.log(actor.getId(), "MATERIAL_DOWNLOAD", "material " + materialId, true);
        return p;
    }

    /** Only the uploader (or an admin) may delete a material. */
    public void delete(BaseUser actor, int materialId) {
        authz.require(actor, "MATERIAL_MANAGE");
        String[] m = Jdbc.rows("SELECT uploaded_by, file_path FROM materials WHERE material_id = ?", materialId)
            .stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("No material with id " + materialId));
        if (!CourseService.isAdmin(actor) && Integer.parseInt(m[0]) != actor.getId())
            throw new AccessDeniedException("You can delete only the materials you uploaded.");
        Jdbc.update("DELETE FROM materials WHERE material_id = ?", materialId);
        FileStore.delete(m[1]);
        audit.log(actor.getId(), "MATERIAL_DELETE", "material " + materialId, true);
    }
}
