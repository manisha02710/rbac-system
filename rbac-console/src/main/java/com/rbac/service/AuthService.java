package com.rbac.service;

import com.rbac.exception.InvalidCredentialsException;
import com.rbac.model.*;
import com.rbac.repository.Jdbc;
import com.rbac.util.PasswordUtil;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/** Login: checks the password hash, loads roles and permissions, creates the right BaseUser subclass. */
public class AuthService {
    private final AuthorizationService authz;
    private final AuditService audit;

    public AuthService(AuthorizationService authz, AuditService audit) {
        this.authz = authz;
        this.audit = audit;
    }

    private record Row(int id, String username, String hash, String fullName, String email, boolean active) { }

    public BaseUser login(String username, String password) {
        Optional<Row> found = Jdbc.one(
            "SELECT user_id, username, password_hash, full_name, email, is_active FROM users WHERE username = ?",
            rs -> new Row(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getBoolean(6)),
            username);
        if (found.isEmpty() || !PasswordUtil.verify(password, found.get().hash())) {
            audit.log(found.map(Row::id).orElse(null), "LOGIN_FAILED", "username=" + username, false);
            throw new InvalidCredentialsException("Incorrect username or password.");
        }
        Row u = found.get();
        if (!u.active()) {
            audit.log(u.id(), "LOGIN_BLOCKED", "account disabled", false);
            throw new InvalidCredentialsException("This account is disabled. Contact the administrator.");
        }
        Set<String> roles = new TreeSet<>(Jdbc.query(
            "SELECT r.role_name FROM user_roles ur JOIN roles r ON r.role_id = ur.role_id WHERE ur.user_id = ?",
            rs -> rs.getString(1), u.id()));
        BaseUser user = create(u, roles);
        user.load(roles, authz.permissionsOf(u.id()));
        audit.log(u.id(), "LOGIN", "role=" + user.getRoleLabel(), true);
        return user;
    }

    /** Factory: the highest-priority role decides which subclass is created. */
    private BaseUser create(Row u, Set<String> roles) {
        if (roles.contains("ADMIN"))   return new AdminUser(u.id(), u.username(), u.fullName(), u.email());
        if (roles.contains("FACULTY")) return new FacultyUser(u.id(), u.username(), u.fullName(), u.email());
        if (roles.contains("STUDENT")) return new StudentUser(u.id(), u.username(), u.fullName(), u.email());
        String name = roles.isEmpty() ? "NO_ROLE" : String.join(" + ", roles);
        return new CustomRoleUser(u.id(), u.username(), u.fullName(), u.email(), name);
    }

    public void logout(BaseUser user) { audit.log(user.getId(), "LOGOUT", null, true); }

    public List<String[]> profile(BaseUser user) {
        return Jdbc.rows("SELECT u.user_id, u.username, u.full_name, u.email, "
            + "COALESCE(s.roll_no,'-'), COALESCE(s.department, d.department_name, '-') "
            + "FROM users u LEFT JOIN students s ON s.user_id = u.user_id "
            + "LEFT JOIN faculty_profiles f ON f.user_id = u.user_id "
            + "LEFT JOIN departments d ON d.department_id = f.department_id WHERE u.user_id = ?", user.getId());
    }
}
