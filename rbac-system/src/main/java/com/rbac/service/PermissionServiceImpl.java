package com.rbac.service;

import com.rbac.model.Permission;
import com.rbac.repository.PermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * OOP Concept: Abstraction & Polymorphism
 * Implements {@link PermissionService} providing transactional business logic.
 */
@Service
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final AuditService auditService;

    public PermissionServiceImpl(PermissionRepository permissionRepository, AuditService auditService) {
        this.permissionRepository = permissionRepository;
        this.auditService = auditService;
    }

    @Override
    public List<Permission> listAll() {
        return permissionRepository.findAll();
    }

    @Override
    @Transactional
    public void assignPermission(Long roleId, Long permissionId, Long operatorId) {
        permissionRepository.assignPermissionToRole(roleId, permissionId);
        auditService.log(operatorId, "PERMISSION_ASSIGN", "Assigned permissionId=" + permissionId + " to roleId=" + roleId, com.rbac.model.AuditLog.Status.SUCCESS);
    }

    @Override
    @Transactional
    public void removePermission(Long roleId, Long permissionId, Long operatorId) {
        permissionRepository.removePermissionFromRole(roleId, permissionId);
        auditService.log(operatorId, "PERMISSION_REMOVE", "Removed permissionId=" + permissionId + " from roleId=" + roleId, com.rbac.model.AuditLog.Status.SUCCESS);
    }
}
