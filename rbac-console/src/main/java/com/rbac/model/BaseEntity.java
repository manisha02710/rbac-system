package com.rbac.model;

/** Abstract parent of every database entity: every row has an id. */
public abstract class BaseEntity {
    private int id;

    protected BaseEntity(int id) { this.id = id; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    /** Each entity describes itself in one line (overridden by subclasses). */
    public abstract String describe();

    @Override
    public String toString() { return describe(); }
}
