package com.rbac.service;

import com.rbac.exception.ResourceNotFoundException;
import com.rbac.exception.ValidationException;
import com.rbac.model.BaseUser;
import com.rbac.repository.Jdbc;

import java.util.List;

/**
 * Students rate faculty (1-5) for a course. Admin sees averages and ANONYMOUS comments;
 * faculty see only their own averages and anonymous comments.
 */
public class FeedbackService {
    private final AuthorizationService authz;
    private final AuditService audit;

    public FeedbackService(AuthorizationService authz, AuditService audit) {
        this.authz = authz;
        this.audit = audit;
    }

    private int ownStudentId(BaseUser actor) {
        return Jdbc.one("SELECT student_id FROM students WHERE user_id = ?", rs -> rs.getInt(1), actor.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Your account has no student record."));
    }

    /** Faculty-course pairs a student can rate. */
    public List<String[]> ratableOptions(BaseUser actor) {
        authz.require(actor, "FEEDBACK_SUBMIT");
        return Jdbc.rows("SELECT fp.faculty_id, u.full_name, c.course_id, c.course_code, c.course_name "
            + "FROM faculty_courses fc JOIN faculty_profiles fp ON fp.faculty_id = fc.faculty_id "
            + "JOIN users u ON u.user_id = fp.user_id JOIN courses c ON c.course_id = fc.course_id "
            + "ORDER BY u.full_name, c.course_code");
    }

    /** One rating per student, faculty and course; rating again updates the old one (UNIQUE key + upsert). */
    public void submit(BaseUser actor, int facultyId, int courseId, int rating, String comments) {
        authz.require(actor, "FEEDBACK_SUBMIT");
        if (rating < 1 || rating > 5) throw new ValidationException("Rating must be between 1 and 5.");
        if (Jdbc.count("SELECT COUNT(*) FROM faculty_courses WHERE faculty_id = ? AND course_id = ?", facultyId, courseId) == 0)
            throw new ResourceNotFoundException("That faculty member does not teach that course.");
        Jdbc.update("INSERT INTO faculty_feedback (student_id, faculty_id, course_id, rating, comments) VALUES (?,?,?,?,?) "
            + "ON DUPLICATE KEY UPDATE rating = VALUES(rating), comments = VALUES(comments)",
            ownStudentId(actor), facultyId, courseId, rating, comments.trim());
        audit.log(actor.getId(), "FEEDBACK_SUBMIT", "faculty " + facultyId + " course " + courseId, true);   // rating itself is not logged
    }

    public List<String[]> myFeedback(BaseUser actor) {
        authz.require(actor, "FEEDBACK_SUBMIT");
        return Jdbc.rows("SELECT u.full_name, c.course_code, f.rating, COALESCE(f.comments,'-') "
            + "FROM faculty_feedback f JOIN faculty_profiles fp ON fp.faculty_id = f.faculty_id "
            + "JOIN users u ON u.user_id = fp.user_id JOIN courses c ON c.course_id = f.course_id "
            + "WHERE f.student_id = ? ORDER BY u.full_name", ownStudentId(actor));
    }

    private static final String AVG_SQL = "SELECT u.full_name, c.course_code, COUNT(*), ROUND(AVG(f.rating),2), "
        + "REPEAT('*', ROUND(AVG(f.rating))) FROM faculty_feedback f "
        + "JOIN faculty_profiles fp ON fp.faculty_id = f.faculty_id JOIN users u ON u.user_id = fp.user_id "
        + "JOIN courses c ON c.course_id = f.course_id ";
    private static final String AVG_GROUP = "GROUP BY fp.faculty_id, u.full_name, c.course_id, c.course_code ORDER BY u.full_name, c.course_code";

    public List<String[]> averagesAll(BaseUser actor) {
        authz.require(actor, "FEEDBACK_VIEW_ALL");
        return Jdbc.rows(AVG_SQL + AVG_GROUP);
    }

    public List<String[]> commentsAll(BaseUser actor) {
        authz.require(actor, "FEEDBACK_VIEW_ALL");
        return Jdbc.rows("SELECT u.full_name, c.course_code, f.rating, f.comments "
            + "FROM faculty_feedback f JOIN faculty_profiles fp ON fp.faculty_id = f.faculty_id "
            + "JOIN users u ON u.user_id = fp.user_id JOIN courses c ON c.course_id = f.course_id "
            + "WHERE f.comments IS NOT NULL AND f.comments <> '' ORDER BY u.full_name, c.course_code");          // no student names
    }

    private int ownFacultyId(BaseUser actor) {
        return Jdbc.one("SELECT faculty_id FROM faculty_profiles WHERE user_id = ?", rs -> rs.getInt(1), actor.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Your account has no faculty profile."));
    }

    public List<String[]> averagesOwn(BaseUser actor) {
        authz.require(actor, "FEEDBACK_VIEW_OWN");
        return Jdbc.rows(AVG_SQL + "WHERE fp.faculty_id = ? " + AVG_GROUP, ownFacultyId(actor));
    }

    public List<String[]> commentsOwn(BaseUser actor) {
        authz.require(actor, "FEEDBACK_VIEW_OWN");
        return Jdbc.rows("SELECT c.course_code, f.rating, f.comments FROM faculty_feedback f "
            + "JOIN courses c ON c.course_id = f.course_id WHERE f.faculty_id = ? "
            + "AND f.comments IS NOT NULL AND f.comments <> '' ORDER BY c.course_code", ownFacultyId(actor));
    }
}
