package com.rbac.service;

import com.rbac.model.Permission;
import java.util.List;

/**
 * OOP Concept: Abstraction
 * Defines the public contract for permission-related business operations.
 */
public interface PermissionService {

    /**
     * List all permissions in the system.
     */
    List<Permission> listAll();

    /**
     * Assign a permission to a role.
     */
    void assignPermission(Long roleId, Long permissionId, Long operatorId);

    /**
     * Remove a permission from a role.
     */
    void removePermission(Long roleId, Long permissionId, Long operatorId);
}
