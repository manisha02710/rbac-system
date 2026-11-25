package com.rbac.service;

import com.rbac.exception.AccessDeniedException;
import com.rbac.exception.ResourceNotFoundException;
import com.rbac.exception.ValidationException;
import com.rbac.model.BaseUser;
import com.rbac.repository.Jdbc;
import com.rbac.util.PasswordUtil;

import java.util.List;

/** User management. Every method re-checks the actor's permission in the database (defence in depth). */
public class UserService {
    private final AuthorizationService authz;
    private final AuditService audit;

    public UserService(AuthorizationService authz, AuditService audit) {
        this.authz = authz;
        this.audit = audit;
    }

    public List<String[]> list(BaseUser actor) {
        authz.require(actor, "USER_READ");
        return Jdbc.rows("SELECT u.user_id, u.username, u.full_name, u.email, IF(u.is_active,'Active','Disabled'), "
            + "COALESCE(GROUP_CONCAT(r.role_name ORDER BY r.role_name SEPARATOR ', '),'-') "
            + "FROM users u LEFT JOIN user_roles ur ON ur.user_id = u.user_id "
            + "LEFT JOIN roles r ON r.role_id = ur.role_id GROUP BY u.user_id ORDER BY u.user_id");
    }

    /** Creates the user, the role link and (for STUDENT) the students row in ONE transaction. */
    public long create(BaseUser actor, String username, String password, String fullName, String email,
                       int roleId, String rollNo, String department) {
        authz.require(actor, "USER_CREATE");
        if (username.length() < 3) throw new ValidationException("Username must be at least 3 characters.");
        if (password.length() < 6) throw new ValidationException("Password must be at least 6 characters.");
        if (!email.contains("@")) throw new ValidationException("Enter a valid email address.");
        String roleName = Jdbc.one("SELECT role_name FROM roles WHERE role_id = ?", rs -> rs.getString(1), roleId)
            .orElseThrow(() -> new ResourceNotFoundException("No role with id " + roleId));
        if (roleName.equals("STUDENT") && (rollNo.isEmpty() || department.isEmpty()))
            throw new ValidationException("Students need a roll number and a department.");

        long id = Jdbc.transaction(() -> {
            long uid = Jdbc.insert("INSERT INTO users (username, password_hash, full_name, email, is_active) "
                + "VALUES (?,?,?,?,TRUE)", username, PasswordUtil.hash(password), fullName, email);
            Jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?,?)", uid, roleId);
            if (roleName.equals("STUDENT"))
                Jdbc.update("INSERT INTO students (user_id, roll_no, department) VALUES (?,?,?)", uid, rollNo, department);
            if (roleName.equals("FACULTY"))
                Jdbc.update("INSERT INTO faculty_profiles (user_id) VALUES (?)", uid);
            return uid;
        });
        audit.log(actor.getId(), "USER_CREATE", username + " as " + roleName, true);
        return id;
    }

    public void setActive(BaseUser actor, int userId, boolean active) {
        authz.require(actor, "USER_DISABLE");
        if (!active && userId == actor.getId()) throw new ValidationException("You cannot disable your own account.");
        if (Jdbc.update("UPDATE users SET is_active = ? WHERE user_id = ?", active, userId) == 0)
            throw new ResourceNotFoundException("No user with id " + userId);
        audit.log(actor.getId(), active ? "USER_ENABLE" : "USER_DISABLE", "user_id=" + userId, true);
    }

    public void assignRole(BaseUser actor, int userId, int roleId) {
        authz.require(actor, "ROLE_ASSIGN");
        requireUser(userId);
        Jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?,?)", userId, roleId);   // duplicate -> friendly error
        audit.log(actor.getId(), "ROLE_ASSIGN", "user_id=" + userId + " role_id=" + roleId, true);
    }

    public void removeRole(BaseUser actor, int userId, int roleId) {
        authz.require(actor, "ROLE_ASSIGN");
        boolean isAdminRole = Jdbc.count("SELECT COUNT(*) FROM roles WHERE role_id = ? AND role_name = 'ADMIN'", roleId) > 0;
        if (isAdminRole && Jdbc.count("SELECT COUNT(*) FROM user_roles WHERE role_id = ?", roleId) <= 1)
            throw new ValidationException("Cannot remove the ADMIN role from the last administrator.");
        if (Jdbc.update("DELETE FROM user_roles WHERE user_id = ? AND role_id = ?", userId, roleId) == 0)
            throw new ResourceNotFoundException("That user does not have that role.");
        audit.log(actor.getId(), "ROLE_REMOVE", "user_id=" + userId + " role_id=" + roleId, true);
    }

    public void denied(BaseUser actor, AccessDeniedException e) {
        audit.log(actor.getId(), "ACCESS_DENIED", e.getMessage(), false);
    }

    private void requireUser(int userId) {
        if (Jdbc.count("SELECT COUNT(*) FROM users WHERE user_id = ?", userId) == 0)
            throw new ResourceNotFoundException("No user with id " + userId);
    }
}
