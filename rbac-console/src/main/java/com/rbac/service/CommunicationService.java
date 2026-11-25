package com.rbac.service;

import com.rbac.exception.ResourceNotFoundException;
import com.rbac.exception.ValidationException;
import com.rbac.model.BaseUser;
import com.rbac.repository.Jdbc;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

/** Complaints (student -> admin) and announcements (admin -> everyone). */
public class CommunicationService {
    public static final Set<String> COMPLAINT_CATEGORIES = Set.of("ACADEMIC", "FACULTY", "FACILITY", "OTHER");
    public static final Set<String> COMPLAINT_STATUSES = Set.of("OPEN", "IN_PROGRESS", "RESOLVED");
    public static final Set<String> ANNOUNCEMENT_CATEGORIES = Set.of("HOLIDAY", "EXAM", "EVENT", "GENERAL");

    private final AuthorizationService authz;
    private final AuditService audit;

    public CommunicationService(AuthorizationService authz, AuditService audit) {
        this.authz = authz;
        this.audit = audit;
    }

    private int ownStudentId(BaseUser actor) {
        return Jdbc.one("SELECT student_id FROM students WHERE user_id = ?", rs -> rs.getInt(1), actor.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Your account has no student record."));
    }

    // ------------------------------------------------------------------ complaints

    public long submitComplaint(BaseUser actor, String subject, String description, String category) {
        authz.require(actor, "COMPLAINT_CREATE");
        category = category.trim().toUpperCase();
        if (subject.trim().length() < 3) throw new ValidationException("Subject must be at least 3 characters.");
        if (description.trim().length() < 10) throw new ValidationException("Description must be at least 10 characters.");
        if (!COMPLAINT_CATEGORIES.contains(category))
            throw new ValidationException("Category must be one of " + COMPLAINT_CATEGORIES);
        long id = Jdbc.insert("INSERT INTO complaints (student_id, subject, description, category) VALUES (?,?,?,?)",
            ownStudentId(actor), subject.trim(), description.trim(), category);
        audit.log(actor.getId(), "COMPLAINT_CREATE", "complaint " + id, true);
        return id;
    }

    /** A student only ever sees their own complaints (student id comes from the login). */
    public List<String[]> myComplaints(BaseUser actor) {
        authz.require(actor, "COMPLAINT_VIEW_OWN");
        return Jdbc.rows("SELECT complaint_id, subject, category, status, COALESCE(admin_response,'-'), created_at "
            + "FROM complaints WHERE student_id = ? ORDER BY complaint_id DESC", ownStudentId(actor));
    }

    public List<String[]> allComplaints(BaseUser actor) {
        authz.require(actor, "COMPLAINT_MANAGE");
        return Jdbc.rows("SELECT c.complaint_id, s.roll_no, u.full_name, c.subject, c.category, c.status, "
            + "COALESCE(c.admin_response,'-'), c.created_at "
            + "FROM complaints c JOIN students s ON s.student_id = c.student_id JOIN users u ON u.user_id = s.user_id "
            + "ORDER BY FIELD(c.status,'OPEN','IN_PROGRESS','RESOLVED'), c.complaint_id DESC");
    }

    public String complaintText(BaseUser actor, int id) {
        authz.require(actor, "COMPLAINT_MANAGE");
        return Jdbc.one("SELECT description FROM complaints WHERE complaint_id = ?", rs -> rs.getString(1), id)
            .orElseThrow(() -> new ResourceNotFoundException("No complaint with id " + id));
    }

    public void respondToComplaint(BaseUser actor, int id, String status, String response) {
        authz.require(actor, "COMPLAINT_MANAGE");
        status = status.trim().toUpperCase();
        if (!COMPLAINT_STATUSES.contains(status)) throw new ValidationException("Status must be one of " + COMPLAINT_STATUSES);
        if (response.trim().isEmpty()) throw new ValidationException("Please write a response.");
        if (Jdbc.update("UPDATE complaints SET status = ?, admin_response = ? WHERE complaint_id = ?",
                status, response.trim(), id) == 0)
            throw new ResourceNotFoundException("No complaint with id " + id);
        audit.log(actor.getId(), "COMPLAINT_RESPOND", "complaint " + id + " -> " + status, true);
    }

    // ------------------------------------------------------------------ announcements

    public long postAnnouncement(BaseUser actor, String title, String message, String category, String expiresOn) {
        authz.require(actor, "ANNOUNCEMENT_MANAGE");
        category = category.trim().toUpperCase();
        if (title.trim().length() < 3) throw new ValidationException("Title must be at least 3 characters.");
        if (message.trim().isEmpty()) throw new ValidationException("Message cannot be empty.");
        if (!ANNOUNCEMENT_CATEGORIES.contains(category))
            throw new ValidationException("Category must be one of " + ANNOUNCEMENT_CATEGORIES);
        Object expiry = null;
        if (expiresOn != null && !expiresOn.trim().isEmpty()) {
            try {
                LocalDate d = LocalDate.parse(expiresOn.trim());
                if (d.isBefore(LocalDate.now())) throw new ValidationException("Expiry date cannot be in the past.");
                expiry = java.sql.Date.valueOf(d);
            } catch (DateTimeParseException e) {
                throw new ValidationException("Use the date format yyyy-mm-dd, for example 2026-12-31.");
            }
        }
        long id = Jdbc.insert("INSERT INTO announcements (title, message, category, posted_by, expires_on) VALUES (?,?,?,?,?)",
            title.trim(), message.trim(), category, actor.getId(), expiry);
        audit.log(actor.getId(), "ANNOUNCEMENT_POST", "announcement " + id + ": " + title.trim(), true);
        return id;
    }

    /** Current announcements: not expired yet. */
    public List<String[]> activeAnnouncements(BaseUser actor) {
        authz.require(actor, "ANNOUNCEMENT_VIEW");
        return Jdbc.rows("SELECT a.announcement_id, a.category, a.title, a.message, u.full_name, "
            + "DATE(a.created_at), COALESCE(a.expires_on,'-') FROM announcements a JOIN users u ON u.user_id = a.posted_by "
            + "WHERE a.expires_on IS NULL OR a.expires_on >= CURDATE() ORDER BY a.announcement_id DESC");
    }

    /** Everything including expired ones, for the people who manage announcements. */
    public List<String[]> allAnnouncements(BaseUser actor) {
        authz.require(actor, "ANNOUNCEMENT_MANAGE");
        return Jdbc.rows("SELECT a.announcement_id, a.category, a.title, u.full_name, DATE(a.created_at), "
            + "COALESCE(a.expires_on,'-'), IF(a.expires_on IS NOT NULL AND a.expires_on < CURDATE(),'Expired','Active') "
            + "FROM announcements a JOIN users u ON u.user_id = a.posted_by ORDER BY a.announcement_id DESC");
    }

    public void deleteAnnouncement(BaseUser actor, int id) {
        authz.require(actor, "ANNOUNCEMENT_MANAGE");
        if (Jdbc.update("DELETE FROM announcements WHERE announcement_id = ?", id) == 0)
            throw new ResourceNotFoundException("No announcement with id " + id);
        audit.log(actor.getId(), "ANNOUNCEMENT_DELETE", "announcement " + id, true);
    }
}
