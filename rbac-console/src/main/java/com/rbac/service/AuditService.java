package com.rbac.service;

import com.rbac.repository.Jdbc;

import java.util.List;

/** Writes and reads the security audit trail (table audit_logs). */
public class AuditService {
    public void log(Integer userId, String action, String details, boolean success) {
        try {
            Jdbc.update("INSERT INTO audit_logs (user_id, action, details, status) VALUES (?,?,?,?)",
                    userId, action, details, success ? "SUCCESS" : "DENIED");
        } catch (RuntimeException ignored) {
            // auditing must never break the application
        }
    }

    public List<String[]> recent(int limit) {
        return Jdbc.rows("SELECT a.log_id, a.created_at, COALESCE(u.username,'-'), a.action, a.status, "
                + "COALESCE(a.details,'') FROM audit_logs a LEFT JOIN users u ON u.user_id = a.user_id "
                + "ORDER BY a.log_id DESC LIMIT " + Math.max(1, Math.min(limit, 500)));
    }
}
