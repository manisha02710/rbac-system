package com.rbac.config;

import com.rbac.annotation.RequiresPermission;
import com.rbac.model.Permission;
import com.rbac.repository.PermissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Startup validator that checks all @RequiresPermission annotations on controllers
 * against the database permissions table and logs a warning for any missing permission.
 */
@Component
public class PermissionStartupValidator implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(PermissionStartupValidator.class);

    private final PermissionRepository permissionRepository;
    private final RequestMappingHandlerMapping handlerMapping;

    public PermissionStartupValidator(PermissionRepository permissionRepository, RequestMappingHandlerMapping handlerMapping) {
        this.permissionRepository = permissionRepository;
        this.handlerMapping = handlerMapping;
    }

    @Override
    public void run(String... args) {
        try {
            Set<String> dbPermissions = permissionRepository.findAll().stream()
                    .map(Permission::getPermissionName)
                    .collect(Collectors.toSet());

            Set<String> checkedPermissions = new HashSet<>();
            Map<?, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
            for (HandlerMethod hm : handlerMethods.values()) {
                RequiresPermission ann = hm.getMethodAnnotation(RequiresPermission.class);
                if (ann != null) {
                    String perm = ann.value();
                    checkedPermissions.add(perm);
                    if (!dbPermissions.contains(perm)) {
                        logger.warn("WARNING: Permission '{}' required by handler method '{}' is MISSING from database permissions table!",
                                perm, hm.getMethod().getName());
                    }
                }
            }
            logger.info("Permission startup validation completed. Found {} permissions in DB, verified {} distinct controller permissions.",
                    dbPermissions.size(), checkedPermissions.size());
        } catch (Exception e) {
            logger.warn("Permission startup validation encountered an error: {}", e.getMessage());
        }
    }
}
