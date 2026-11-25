package com.rbac.model;

import java.time.LocalDateTime;

/**
 * OOP Concept: Inheritance – Extends BaseEntity.
 * OOP Concept: Encapsulation – Private fields with getters and setters.
 */
public class Department extends BaseEntity {

    private String departmentName;

    public Department() {
        super();
    }

    public Department(Long id, String departmentName) {
        super(id, LocalDateTime.now());
        this.departmentName = departmentName;
    }

    public Department(Long id, LocalDateTime createdAt, String departmentName) {
        super(id, createdAt);
        this.departmentName = departmentName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }
}
