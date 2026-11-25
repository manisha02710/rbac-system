package com.rbac.model;

/** Student account - overrides the role label. */
public class StudentUser extends BaseUser {
    public StudentUser(int id, String username, String fullName, String email) { super(id, username, fullName, email); }

    @Override
    public String getRoleLabel() { return "Student"; }
}
