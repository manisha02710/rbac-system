package com.rbac.model;

/** A user whose only roles were created at runtime by the admin (e.g. DEPARTMENT_COORDINATOR). */
public class CustomRoleUser extends BaseUser {
    private final String roleName;

    public CustomRoleUser(int id, String username, String fullName, String email, String roleName) {
        super(id, username, fullName, email);
        this.roleName = roleName;
    }

    @Override
    public String getRoleLabel() { return roleName; }
}
