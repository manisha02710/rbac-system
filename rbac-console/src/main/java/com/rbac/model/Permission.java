package com.rbac.model;

/** A single permission such as MARKS_ADD. */
public class Permission extends BaseEntity {
    private final String name;
    private final String description;

    public Permission(int id, String name, String description) {
        super(id);
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }

    @Override
    public String describe() { return name + " - " + description; }
}
