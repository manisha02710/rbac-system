package com.rbac.service;

import com.rbac.exception.AccessDeniedException;
import com.rbac.exception.ResourceNotFoundException;
import com.rbac.model.BaseUser;
import com.rbac.repository.Jdbc;

import java.util.List;

/** Courses and which faculty teach them (table faculty_courses, many-to-many). */
public class CourseService {
    private final AuthorizationService authz;
    private final AuditService audit;

    public CourseService(AuthorizationService authz, AuditService audit) {
        this.authz = authz;
        this.audit = audit;
    }

    public List<String[]> listCourses() {
        return Jdbc.rows("SELECT c.course_id, c.course_code, c.course_name, d.department_name "
            + "FROM courses c JOIN departments d ON d.department_id = c.department_id ORDER BY c.course_code");
    }

    /** Courses taught by the logged-in faculty member (everything for an admin). */
    public List<String[]> myCourses(BaseUser actor) {
        if (isAdmin(actor)) return listCourses();
        return Jdbc.rows("SELECT c.course_id, c.course_code, c.course_name, d.department_name "
            + "FROM faculty_courses fc JOIN faculty_profiles fp ON fp.faculty_id = fc.faculty_id "
            + "JOIN courses c ON c.course_id = fc.course_id JOIN departments d ON d.department_id = c.department_id "
            + "WHERE fp.user_id = ? ORDER BY c.course_code", actor.getId());
    }

    public static boolean isAdmin(BaseUser u) { return u.getRoles().contains("ADMIN"); }

    /** Faculty may only work with their own courses; admin may work with any. */
    public void requireTeaches(BaseUser actor, int courseId) {
        if (Jdbc.count("SELECT COUNT(*) FROM courses WHERE course_id = ?", courseId) == 0)
            throw new ResourceNotFoundException("No course with id " + courseId);
        if (isAdmin(actor)) return;
        long n = Jdbc.count("SELECT COUNT(*) FROM faculty_courses fc JOIN faculty_profiles fp ON fp.faculty_id = fc.faculty_id "
            + "WHERE fp.user_id = ? AND fc.course_id = ?", actor.getId(), courseId);
        if (n == 0) throw new AccessDeniedException("You do not teach this course.");
    }

    public List<String[]> listFaculty(BaseUser actor) {
        authz.require(actor, "USER_UPDATE");
        return Jdbc.rows("SELECT fp.faculty_id, u.full_name, u.username, "
            + "COALESCE(GROUP_CONCAT(c.course_code ORDER BY c.course_code SEPARATOR ', '),'-') "
            + "FROM faculty_profiles fp JOIN users u ON u.user_id = fp.user_id "
            + "LEFT JOIN faculty_courses fc ON fc.faculty_id = fp.faculty_id LEFT JOIN courses c ON c.course_id = fc.course_id "
            + "GROUP BY fp.faculty_id, u.full_name, u.username ORDER BY fp.faculty_id");
    }

    public void assignCourse(BaseUser actor, int facultyId, int courseId) {
        authz.require(actor, "USER_UPDATE");
        if (Jdbc.count("SELECT COUNT(*) FROM faculty_profiles WHERE faculty_id = ?", facultyId) == 0)
            throw new ResourceNotFoundException("No faculty with id " + facultyId);
        if (Jdbc.count("SELECT COUNT(*) FROM courses WHERE course_id = ?", courseId) == 0)
            throw new ResourceNotFoundException("No course with id " + courseId);
        Jdbc.update("INSERT IGNORE INTO faculty_courses (faculty_id, course_id) VALUES (?,?)", facultyId, courseId);
        audit.log(actor.getId(), "COURSE_ASSIGN", "faculty " + facultyId + " -> course " + courseId, true);
    }
}
