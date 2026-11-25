package com.rbac.model;

/** Faculty account - overrides the role label. */
public class FacultyUser extends BaseUser {
    public FacultyUser(int id, String username, String fullName, String email) { super(id, username, fullName, email); }

    @Override
    public String getRoleLabel() { return "Faculty"; }
}
