package com.rbac.service;

import com.rbac.repository.Jdbc;

import java.util.HashSet;
import java.util.Set;

/** Reads permissions with a 4-table JOIN: users -> user_roles -> role_permissions -> permissions. */
public class JdbcAuthorizationService implements AuthorizationService {
    private static final String SQL =
        "SELECT DISTINCT p.permission_name FROM user_roles ur "
        + "JOIN role_permissions rp ON rp.role_id = ur.role_id "
        + "JOIN permissions p ON p.permission_id = rp.permission_id WHERE ur.user_id = ?";

    @Override
    public Set<String> permissionsOf(int userId) {
        return new HashSet<>(Jdbc.query(SQL, rs -> rs.getString(1), userId));
    }

    @Override
    public boolean has(int userId, String permission) {
        return permissionsOf(userId).contains(permission);
    }
}
