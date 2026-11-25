package com.rbac.model;

import java.time.LocalDateTime;

/**
 * Student entity extending BaseEntity. Represents a student profile linked to a user.
 * // OOP: Inheritance – extends BaseEntity for common fields.
 */
public class Student extends BaseEntity {
    // OOP: Encapsulation – private fields.
    private Long userId; // Reference to the User (one‑to‑one).
    private String rollNo;
    private String department;
    private String studentName; // Joined from users table for display

    public Student() {
        super();
    }

    public Student(Long id, LocalDateTime createdAt, Long userId, String rollNo, String department) {
        super(id, createdAt);
        this.userId = userId;
        this.rollNo = rollNo;
        this.department = department;
    }

    public Student(Long id, LocalDateTime createdAt, Long userId, String rollNo, String department, String studentName) {
        super(id, createdAt);
        this.userId = userId;
        this.rollNo = rollNo;
        this.department = department;
        this.studentName = studentName;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
}
