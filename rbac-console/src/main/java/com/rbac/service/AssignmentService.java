package com.rbac.service;

import com.rbac.exception.AccessDeniedException;
import com.rbac.exception.ResourceNotFoundException;
import com.rbac.exception.ValidationException;
import com.rbac.model.BaseUser;
import com.rbac.repository.Jdbc;
import com.rbac.util.FileStore;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Assignments: faculty create and grade, students submit a file.
 * All times are LocalDateTime values passed as parameters, so the database and the program always agree on "now".
 */
public class AssignmentService {
    public static final DateTimeFormatter DUE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AuthorizationService authz;
    private final AuditService audit;
    private final CourseService courses;

    public AssignmentService(AuthorizationService authz, AuditService audit, CourseService courses) {
        this.authz = authz;
        this.audit = audit;
        this.courses = courses;
    }

    private int ownStudentId(BaseUser actor) {
        return Jdbc.one("SELECT student_id FROM students WHERE user_id = ?", rs -> rs.getInt(1), actor.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Your account has no student record."));
    }

    // ------------------------------------------------------------------ faculty / admin

    public long create(BaseUser actor, int courseId, String title, String description, String due, int maxMarks) {
        authz.require(actor, "ASSIGNMENT_MANAGE");
        if (title.trim().length() < 3) throw new ValidationException("Title must be at least 3 characters.");
        if (maxMarks < 1 || maxMarks > 1000) throw new ValidationException("Maximum marks must be between 1 and 1000.");
        LocalDateTime dueAt;
        try { dueAt = LocalDateTime.parse(due.trim(), DUE_FORMAT); }
        catch (DateTimeParseException e) { throw new ValidationException("Use the format yyyy-mm-dd hh:mm, for example 2026-12-31 23:59."); }
        if (dueAt.isBefore(LocalDateTime.now())) throw new ValidationException("The due date cannot be in the past.");
        courses.requireTeaches(actor, courseId);
        long id = Jdbc.insert("INSERT INTO assignments (course_id, created_by, title, description, due_date, max_marks) "
            + "VALUES (?,?,?,?,?,?)", courseId, actor.getId(), title.trim(), description.trim(), dueAt, maxMarks);
        audit.log(actor.getId(), "ASSIGNMENT_CREATE", "assignment " + id + ": " + title.trim(), true);
        return id;
    }

    /** Faculty see their own assignments; an admin sees all. */
    public List<String[]> listManaged(BaseUser actor) {
        authz.require(actor, "ASSIGNMENT_MANAGE");
        String sql = "SELECT a.assignment_id, c.course_code, a.title, DATE_FORMAT(a.due_date,'%Y-%m-%d %H:%i'), a.max_marks, "
            + "(SELECT COUNT(*) FROM submissions s WHERE s.assignment_id = a.assignment_id), "
            + "(SELECT COUNT(*) FROM submissions s WHERE s.assignment_id = a.assignment_id AND s.marks_awarded IS NULL) "
            + "FROM assignments a JOIN courses c ON c.course_id = a.course_id ";
        if (CourseService.isAdmin(actor)) return Jdbc.rows(sql + "ORDER BY a.due_date DESC");
        return Jdbc.rows(sql + "WHERE a.created_by = ? ORDER BY a.due_date DESC", actor.getId());
    }

    private String[] ownedAssignment(BaseUser actor, int assignmentId) {
        String[] a = Jdbc.rows("SELECT created_by, max_marks FROM assignments WHERE assignment_id = ?", assignmentId)
            .stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("No assignment with id " + assignmentId));
        if (!CourseService.isAdmin(actor) && Integer.parseInt(a[0]) != actor.getId())
            throw new AccessDeniedException("This is not your assignment.");
        return a;
    }

    public void delete(BaseUser actor, int assignmentId) {
        authz.require(actor, "ASSIGNMENT_MANAGE");
        ownedAssignment(actor, assignmentId);
        List<String[]> files = Jdbc.rows("SELECT file_path FROM submissions WHERE assignment_id = ?", assignmentId);
        Jdbc.update("DELETE FROM assignments WHERE assignment_id = ?", assignmentId);      // submissions go with it (CASCADE)
        for (String[] f : files) FileStore.delete(f[0]);
        audit.log(actor.getId(), "ASSIGNMENT_DELETE", "assignment " + assignmentId, true);
    }

    public List<String[]> submissions(BaseUser actor, int assignmentId) {
        authz.require(actor, "ASSIGNMENT_MANAGE");
        ownedAssignment(actor, assignmentId);
        return Jdbc.rows("SELECT s.submission_id, st.roll_no, u.full_name, s.file_name, "
            + "DATE_FORMAT(s.submitted_at,'%Y-%m-%d %H:%i'), IF(s.submitted_at > a.due_date,'LATE','ON TIME'), "
            + "COALESCE(s.marks_awarded,'-'), COALESCE(s.feedback,'-') "
            + "FROM submissions s JOIN assignments a ON a.assignment_id = s.assignment_id "
            + "JOIN students st ON st.student_id = s.student_id JOIN users u ON u.user_id = st.user_id "
            + "WHERE s.assignment_id = ? ORDER BY st.roll_no", assignmentId);
    }

    public Path downloadSubmission(BaseUser actor, int submissionId) {
        authz.require(actor, "ASSIGNMENT_MANAGE");
        String[] s = Jdbc.rows("SELECT assignment_id, file_name, file_path FROM submissions WHERE submission_id = ?", submissionId)
            .stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("No submission with id " + submissionId));
        ownedAssignment(actor, Integer.parseInt(s[0]));
        return FileStore.copyOut(s[2], s[1]);
    }

    public void grade(BaseUser actor, int submissionId, int marks, String feedback) {
        authz.require(actor, "ASSIGNMENT_MANAGE");
        String[] s = Jdbc.rows("SELECT assignment_id FROM submissions WHERE submission_id = ?", submissionId)
            .stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("No submission with id " + submissionId));
        String[] a = ownedAssignment(actor, Integer.parseInt(s[0]));
        int max = Integer.parseInt(a[1]);
        if (marks < 0 || marks > max) throw new ValidationException("Marks must be between 0 and " + max + ".");
        Jdbc.update("UPDATE submissions SET marks_awarded = ?, feedback = ? WHERE submission_id = ?", marks, feedback.trim(), submissionId);
        audit.log(actor.getId(), "ASSIGNMENT_GRADE", "submission " + submissionId + " = " + marks, true);
    }

    // ------------------------------------------------------------------ students

    /** Assignments with the student's own status: PENDING, DUE SOON, MISSED, SUBMITTED, LATE or GRADED. */
    public List<String[]> listForStudent(BaseUser actor) {
        authz.require(actor, "ASSIGNMENT_VIEW");
        LocalDateTime now = LocalDateTime.now();
        return Jdbc.rows("SELECT a.assignment_id, c.course_code, a.title, DATE_FORMAT(a.due_date,'%Y-%m-%d %H:%i'), a.max_marks, "
            + "CASE WHEN s.submission_id IS NULL THEN IF(a.due_date < ?, 'MISSED', IF(a.due_date < ?, 'DUE SOON', 'PENDING')) "
            + "WHEN s.marks_awarded IS NOT NULL THEN 'GRADED' WHEN s.submitted_at > a.due_date THEN 'LATE' ELSE 'SUBMITTED' END, "
            + "COALESCE(CONCAT(s.marks_awarded,'/',a.max_marks),'-'), COALESCE(s.feedback,'-') "
            + "FROM assignments a JOIN courses c ON c.course_id = a.course_id "
            + "LEFT JOIN submissions s ON s.assignment_id = a.assignment_id AND s.student_id = ? "
            + "ORDER BY a.due_date", now, now.plusHours(48), ownStudentId(actor));
    }

    /**
     * Submit a file. A resubmission before the deadline replaces the old file;
     * after the deadline a first submission is accepted but marked LATE, and it cannot be replaced.
     */
    public void submit(BaseUser actor, int assignmentId, String filePath) {
        authz.require(actor, "ASSIGNMENT_SUBMIT");
        int studentId = ownStudentId(actor);
        LocalDateTime due = Jdbc.one("SELECT due_date FROM assignments WHERE assignment_id = ?",
                rs -> rs.getObject(1, LocalDateTime.class), assignmentId)
            .orElseThrow(() -> new ResourceNotFoundException("No assignment with id " + assignmentId));
        LocalDateTime now = LocalDateTime.now();
        List<String[]> old = Jdbc.rows("SELECT submission_id, file_path, marks_awarded FROM submissions "
            + "WHERE assignment_id = ? AND student_id = ?", assignmentId, studentId);
        if (!old.isEmpty()) {
            if (old.get(0)[2] != null) throw new ValidationException("This submission is already graded and cannot be replaced.");
            if (now.isAfter(due)) throw new ValidationException("The deadline has passed, so your submission cannot be replaced.");
        }
        String[] stored = FileStore.store(filePath, "submissions", "sub_" + assignmentId + "_" + studentId + "_");
        try {
            if (old.isEmpty()) {
                Jdbc.update("INSERT INTO submissions (assignment_id, student_id, file_name, file_path, submitted_at) VALUES (?,?,?,?,?)",
                    assignmentId, studentId, stored[0], stored[1], now);
            } else {
                Jdbc.update("UPDATE submissions SET file_name = ?, file_path = ?, submitted_at = ? WHERE submission_id = ?",
                    stored[0], stored[1], now, Integer.parseInt(old.get(0)[0]));
                FileStore.delete(old.get(0)[1]);
            }
        } catch (RuntimeException e) {
            FileStore.delete(stored[1]);
            throw e;
        }
        audit.log(actor.getId(), "ASSIGNMENT_SUBMIT", "assignment " + assignmentId + (now.isAfter(due) ? " (late)" : ""), true);
    }
}
