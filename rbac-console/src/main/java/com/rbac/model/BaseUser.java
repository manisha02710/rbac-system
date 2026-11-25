package com.rbac.model;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/** Abstract logged-in user. Subclasses decide their own label and permission rules (polymorphism). */
public abstract class BaseUser extends BaseEntity {
    private final String username;
    private final String fullName;
    private final String email;
    private final Set<String> permissions = new TreeSet<>();
    private final Set<String> roles = new TreeSet<>();

    protected BaseUser(int id, String username, String fullName, String email) {
        super(id);
        this.username = username;
        this.fullName = fullName;
        this.email = email;
    }

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public Set<String> getPermissions() { return Collections.unmodifiableSet(permissions); }
    public Set<String> getRoles() { return Collections.unmodifiableSet(roles); }

    public void load(Set<String> roleNames, Set<String> permissionNames) {
        roles.clear(); roles.addAll(roleNames);
        permissions.clear(); permissions.addAll(permissionNames);
    }

    public boolean can(String permission) { return permission == null || permissions.contains(permission); }

    /** Label shown in the header, e.g. "Administrator". */
    public abstract String getRoleLabel();

    @Override
    public String describe() { return fullName + " (" + username + ") - " + getRoleLabel(); }
}
