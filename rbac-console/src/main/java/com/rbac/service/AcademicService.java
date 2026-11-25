package com.rbac.service;

import com.rbac.exception.ResourceNotFoundException;
import com.rbac.exception.ValidationException;
import com.rbac.model.BaseUser;
import com.rbac.repository.Jdbc;

import java.util.List;

/**
 * Students, marks and attendance. Every method re-checks the actor's permission in the database,
 * and every change is written to the audit log.
 */
public class AcademicService {
    public static final int LOW_ATTENDANCE_PERCENT = 75;

    private final AuthorizationService authz;
    private final AuditService audit;

    public AcademicService(AuthorizationService authz, AuditService audit) {
        this.authz = authz;
        this.audit = audit;
    }

    // ------------------------------------------------------------------ students

    public List<String[]> listStudents(BaseUser actor) {
        authz.require(actor, "STUDENT_READ");
        return Jdbc.rows("SELECT s.student_id, s.roll_no, u.full_name, s.department, u.email "
            + "FROM students s JOIN users u ON u.user_id = s.user_id ORDER BY s.roll_no");
    }

    private void requireStudent(int studentId) {
        if (Jdbc.count("SELECT COUNT(*) FROM students WHERE student_id = ?", studentId) == 0)
            throw new ResourceNotFoundException("No student with id " + studentId);
    }

    /** Finds the students row that belongs to the logged-in student account. */
    private int ownStudentId(BaseUser actor) {
        return Jdbc.one("SELECT student_id FROM students WHERE user_id = ?", rs -> rs.getInt(1), actor.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Your account has no student record."));
    }

    // ------------------------------------------------------------------ marks

    private static final String MARK_SELECT =
        "SELECT m.mark_id, s.student_id, s.roll_no, u.full_name, m.subject, m.marks, "
        + "CASE WHEN m.marks >= 90 THEN 'A+' WHEN m.marks >= 80 THEN 'A' WHEN m.marks >= 70 THEN 'B' "
        + "WHEN m.marks >= 60 THEN 'C' WHEN m.marks >= 50 THEN 'D' ELSE 'F' END "
        + "FROM marks m JOIN students s ON s.student_id = m.student_id JOIN users u ON u.user_id = s.user_id ";

    /** All marks (studentId == 0) or the marks of one student. */
    public List<String[]> listMarks(BaseUser actor, int studentId) {
        authz.require(actor, "MARKS_READ");
        if (studentId == 0) return Jdbc.rows(MARK_SELECT + "ORDER BY s.roll_no, m.subject");
        requireStudent(studentId);
        return Jdbc.rows(MARK_SELECT + "WHERE s.student_id = ? ORDER BY m.subject", studentId);
    }

    public long addMark(BaseUser actor, int studentId, String subject, int marks) {
        authz.require(actor, "MARKS_ADD");
        checkMarks(subject, marks);
        requireStudent(studentId);
        long id = Jdbc.insert("INSERT INTO marks (student_id, subject, marks) VALUES (?,?,?)", studentId, subject, marks);
        audit.log(actor.getId(), "MARK_ADD", "student " + studentId + " " + subject + "=" + marks, true);
        return id;
    }

    public void updateMark(BaseUser actor, int markId, int marks) {
        authz.require(actor, "MARKS_ADD");
        checkMarks("x", marks);
        if (Jdbc.update("UPDATE marks SET marks = ? WHERE mark_id = ?", marks, markId) == 0)
            throw new ResourceNotFoundException("No mark record with id " + markId);
        audit.log(actor.getId(), "MARK_UPDATE", "mark " + markId + " -> " + marks, true);
    }

    public void deleteMark(BaseUser actor, int markId) {
        authz.require(actor, "MARKS_ADD");
        if (Jdbc.update("DELETE FROM marks WHERE mark_id = ?", markId) == 0)
            throw new ResourceNotFoundException("No mark record with id " + markId);
        audit.log(actor.getId(), "MARK_DELETE", "mark " + markId, true);
    }

    private void checkMarks(String subject, int marks) {
        if (subject == null || subject.trim().isEmpty()) throw new ValidationException("Subject cannot be empty.");
        if (marks < 0 || marks > 100) throw new ValidationException("Marks must be between 0 and 100.");
    }

    /** A student sees ONLY their own marks: the student id comes from the login, never from input. */
    public List<String[]> myMarks(BaseUser actor) {
        authz.require(actor, "OWN_MARKS_VIEW");
        return Jdbc.rows("SELECT m.subject, m.marks, "
            + "CASE WHEN m.marks >= 90 THEN 'A+' WHEN m.marks >= 80 THEN 'A' WHEN m.marks >= 70 THEN 'B' "
            + "WHEN m.marks >= 60 THEN 'C' WHEN m.marks >= 50 THEN 'D' ELSE 'F' END "
            + "FROM marks m WHERE m.student_id = ? ORDER BY m.subject", ownStudentId(actor));
    }

    /** Average and count of marks for the logged-in student. */
    public String myMarksSummary(BaseUser actor) {
        authz.require(actor, "OWN_MARKS_VIEW");
        List<String[]> r = Jdbc.rows("SELECT COUNT(*), COALESCE(ROUND(AVG(marks),1),0) FROM marks WHERE student_id = ?",
            ownStudentId(actor));
        return r.get(0)[0] + " subject(s), average " + r.get(0)[1];
    }

    // ------------------------------------------------------------------ attendance

    private static final String ATT_PCT = "ROUND(a.attended_classes * 100 / a.total_classes, 1)";
    private static final String ATT_STATUS =
        "IF(a.attended_classes * 100 / a.total_classes >= " + LOW_ATTENDANCE_PERCENT + ", 'GOOD', 'LOW')";

    /** Attendance of everyone (studentId == 0) or one student. */
    public List<String[]> listAttendance(BaseUser actor, int studentId) {
        authz.require(actor, "ATTENDANCE_VIEW");
        String sql = "SELECT a.attendance_id, s.student_id, s.roll_no, u.full_name, a.subject, a.attended_classes, "
            + "a.total_classes, " + ATT_PCT + ", " + ATT_STATUS
            + " FROM attendance a JOIN students s ON s.student_id = a.student_id JOIN users u ON u.user_id = s.user_id ";
        if (studentId == 0) return Jdbc.rows(sql + "ORDER BY s.roll_no, a.subject");
        requireStudent(studentId);
        return Jdbc.rows(sql + "WHERE s.student_id = ? ORDER BY a.subject", studentId);
    }

    /** Students whose attendance in any subject is below the limit (GROUP BY / HAVING demo). */
    public List<String[]> lowAttendanceReport(BaseUser actor) {
        authz.require(actor, "ATTENDANCE_VIEW");
        return Jdbc.rows("SELECT s.roll_no, u.full_name, SUM(a.attended_classes), SUM(a.total_classes), "
            + "ROUND(SUM(a.attended_classes) * 100 / SUM(a.total_classes), 1) "
            + "FROM attendance a JOIN students s ON s.student_id = a.student_id JOIN users u ON u.user_id = s.user_id "
            + "GROUP BY s.student_id, s.roll_no, u.full_name "
            + "HAVING SUM(a.attended_classes) * 100 / SUM(a.total_classes) < " + LOW_ATTENDANCE_PERCENT
            + " ORDER BY 5");
    }

    public long addAttendance(BaseUser actor, int studentId, String subject, int total, int attended) {
        authz.require(actor, "ATTENDANCE_MANAGE");
        checkAttendance(subject, total, attended);
        requireStudent(studentId);
        long id = Jdbc.insert("INSERT INTO attendance (student_id, subject, total_classes, attended_classes) VALUES (?,?,?,?)",
            studentId, subject, total, attended);
        audit.log(actor.getId(), "ATTENDANCE_ADD", "student " + studentId + " " + subject + " " + attended + "/" + total, true);
        return id;
    }

    public void updateAttendance(BaseUser actor, int attendanceId, int total, int attended) {
        authz.require(actor, "ATTENDANCE_MANAGE");
        checkAttendance("x", total, attended);
        if (Jdbc.update("UPDATE attendance SET total_classes = ?, attended_classes = ? WHERE attendance_id = ?",
                total, attended, attendanceId) == 0)
            throw new ResourceNotFoundException("No attendance record with id " + attendanceId);
        audit.log(actor.getId(), "ATTENDANCE_UPDATE", "record " + attendanceId + " -> " + attended + "/" + total, true);
    }

    public void deleteAttendance(BaseUser actor, int attendanceId) {
        authz.require(actor, "ATTENDANCE_MANAGE");
        if (Jdbc.update("DELETE FROM attendance WHERE attendance_id = ?", attendanceId) == 0)
            throw new ResourceNotFoundException("No attendance record with id " + attendanceId);
        audit.log(actor.getId(), "ATTENDANCE_DELETE", "record " + attendanceId, true);
    }

    private void checkAttendance(String subject, int total, int attended) {
        if (subject == null || subject.trim().isEmpty()) throw new ValidationException("Subject cannot be empty.");
        if (total <= 0) throw new ValidationException("Total classes must be greater than 0.");
        if (attended < 0 || attended > total) throw new ValidationException("Attended classes must be between 0 and total classes.");
    }

    public List<String[]> myAttendance(BaseUser actor) {
        authz.require(actor, "OWN_ATTENDANCE_VIEW");
        return Jdbc.rows("SELECT a.subject, a.attended_classes, a.total_classes, " + ATT_PCT + ", " + ATT_STATUS
            + " FROM attendance a WHERE a.student_id = ? ORDER BY a.subject", ownStudentId(actor));
    }

    /** Overall attendance text such as "83.3% (GOOD)" for the logged-in student. */
    public String myOverallAttendance(BaseUser actor) {
        authz.require(actor, "OWN_ATTENDANCE_VIEW");
        List<String[]> r = Jdbc.rows("SELECT COALESCE(SUM(attended_classes),0), COALESCE(SUM(total_classes),0) "
            + "FROM attendance WHERE student_id = ?", ownStudentId(actor));
        long att = Long.parseLong(r.get(0)[0]), tot = Long.parseLong(r.get(0)[1]);
        if (tot == 0) return "no attendance recorded yet";
        double pct = att * 100.0 / tot;
        return String.format("%.1f%% (%s)", pct, pct >= LOW_ATTENDANCE_PERCENT ? "GOOD" : "LOW - below " + LOW_ATTENDANCE_PERCENT + "%");
    }
}
