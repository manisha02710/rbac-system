package com.rbac.service;

import com.rbac.exception.ResourceNotFoundException;
import com.rbac.exception.ValidationException;
import com.rbac.model.BaseUser;
import com.rbac.repository.Jdbc;

import java.util.List;

/** Dynamic RBAC: the admin creates roles and decides which permissions each role has. */
public class RoleService {
    private final AuthorizationService authz;
    private final AuditService audit;

    public RoleService(AuthorizationService authz, AuditService audit) {
        this.authz = authz;
        this.audit = audit;
    }

    public List<String[]> listRoles() {
        return Jdbc.rows("SELECT r.role_id, r.role_name, COALESCE(r.description,''), COUNT(rp.permission_id) "
            + "FROM roles r LEFT JOIN role_permissions rp ON rp.role_id = r.role_id GROUP BY r.role_id ORDER BY r.role_id");
    }

    public List<String[]> allPermissions() {
        return Jdbc.rows("SELECT permission_id, permission_name, description FROM permissions ORDER BY permission_id");
    }

    public List<String[]> permissionsOfRole(int roleId) {
        return Jdbc.rows("SELECT p.permission_id, p.permission_name, p.description FROM role_permissions rp "
            + "JOIN permissions p ON p.permission_id = rp.permission_id WHERE rp.role_id = ? ORDER BY p.permission_id", roleId);
    }

    public long createRole(BaseUser actor, String name, String description) {
        authz.require(actor, "ROLE_CREATE");
        String clean = name.trim().toUpperCase().replace(' ', '_');
        if (!clean.matches("[A-Z0-9_]{3,50}")) throw new ValidationException("Role name: 3-50 letters, digits or underscores.");
        long id = Jdbc.insert("INSERT INTO roles (role_name, description) VALUES (?,?)", clean, description);
        audit.log(actor.getId(), "ROLE_CREATE", clean, true);
        return id;
    }

    public void grant(BaseUser actor, int roleId, int permissionId) {
        authz.require(actor, "PERMISSION_ASSIGN");
        requireRole(roleId);
        Jdbc.update("INSERT INTO role_permissions (role_id, permission_id) VALUES (?,?)", roleId, permissionId);
        audit.log(actor.getId(), "PERMISSION_GRANT", "role_id=" + roleId + " permission_id=" + permissionId, true);
    }

    public void revoke(BaseUser actor, int roleId, int permissionId) {
        authz.require(actor, "PERMISSION_ASSIGN");
        if (Jdbc.update("DELETE FROM role_permissions WHERE role_id = ? AND permission_id = ?", roleId, permissionId) == 0)
            throw new ResourceNotFoundException("That role does not have that permission.");
        audit.log(actor.getId(), "PERMISSION_REVOKE", "role_id=" + roleId + " permission_id=" + permissionId, true);
    }

    private void requireRole(int roleId) {
        if (Jdbc.count("SELECT COUNT(*) FROM roles WHERE role_id = ?", roleId) == 0)
            throw new ResourceNotFoundException("No role with id " + roleId);
    }
}
