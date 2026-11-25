package com.rbac.model;

import java.time.LocalDateTime;

/**
 * OOP Concept: Inheritance & Polymorphism
 * Subclass of {@link BaseUser} representing a Student with additional academic attributes.
 */
public class StudentUser extends BaseUser {

    private String rollNo;
    private String department;

    public StudentUser() {
        super();
    }

    public StudentUser(Long id, LocalDateTime createdAt, String username, String fullName, String email, String rollNo, String department) {
        super(id, createdAt, username, fullName, email);
        this.rollNo = rollNo;
        this.department = department;
    }

    public StudentUser(Long id, String username, String fullName, String email, String rollNo, String department) {
        super(id, LocalDateTime.now(), username, fullName, email);
        this.rollNo = rollNo;
        this.department = department;
    }

    public String getRollNo() {
        return rollNo;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    @Override
    public String getDashboardPath() {
        return "/dashboard";
    }

    @Override
    public String getRoleLabel() {
        return "Student";
    }
}
