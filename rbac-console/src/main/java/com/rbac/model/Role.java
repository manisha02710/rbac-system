package com.rbac.model;

/** A role (ADMIN, FACULTY, STUDENT or any role the admin creates). */
public class Role extends BaseEntity {
    private final String name;
    private final String description;

    public Role(int id, String name, String description) {
        super(id);
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }

    @Override
    public String describe() { return name + (description == null ? "" : " - " + description); }
}
