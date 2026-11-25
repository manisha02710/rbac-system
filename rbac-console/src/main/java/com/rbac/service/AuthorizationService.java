package com.rbac.service;

import com.rbac.exception.AccessDeniedException;
import com.rbac.model.BaseUser;

import java.util.Set;

/** Abstraction for permission checks. The implementation decides where permissions come from. */
public interface AuthorizationService {
    Set<String> permissionsOf(int userId);

    boolean has(int userId, String permission);

    /** Throws AccessDeniedException unless the user currently holds the permission in the database. */
    default void require(BaseUser actor, String permission) {
        if (!has(actor.getId(), permission))
            throw new AccessDeniedException("You do not have permission: " + permission);
    }
}
