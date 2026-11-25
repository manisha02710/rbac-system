package com.rbac.model;

/** Administrator account - overrides the role label. */
public class AdminUser extends BaseUser {
    public AdminUser(int id, String username, String fullName, String email) { super(id, username, fullName, email); }

    @Override
    public String getRoleLabel() { return "Administrator"; }
}
