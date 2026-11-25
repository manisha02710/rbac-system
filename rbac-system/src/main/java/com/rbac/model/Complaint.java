package com.rbac.model;

import java.time.LocalDateTime;

/**
 * OOP Concept: Inheritance – Extends BaseEntity.
 * OOP Concept: Encapsulation – Private fields with getters and setters.
 */
public class Complaint extends BaseEntity {

    public enum Category {
        ACADEMIC, FACULTY, FACILITY, OTHER
    }

    public enum Status {
        OPEN, IN_PROGRESS, RESOLVED
    }

    private Long studentId;
    private String studentName; // Joined from users
    private String rollNo;      // Joined from students
    private String subject;
    private String description;
    private Category category;
    private Status status = Status.OPEN;
    private String adminResponse;
    private LocalDateTime updatedAt;

    public Complaint() {
        super();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getRollNo() {
        return rollNo;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getAdminResponse() {
        return adminResponse;
    }

    public void setAdminResponse(String adminResponse) {
        this.adminResponse = adminResponse;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isOpen() {
        return status == Status.OPEN;
    }

    public boolean isInProgress() {
        return status == Status.IN_PROGRESS;
    }

    public boolean isResolved() {
        return status == Status.RESOLVED;
    }
}
