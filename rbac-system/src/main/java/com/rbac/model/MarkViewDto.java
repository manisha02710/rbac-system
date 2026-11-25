package com.rbac.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DTO representing an academic mark joined with student and user details.
 */
public class MarkViewDto {
    private Long markId;
    private Long studentId;
    private String studentName;
    private String rollNo;
    private String department;
    private String subject;
    private Integer marks;
    private LocalDateTime createdAt;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public MarkViewDto() {
    }

    public MarkViewDto(Long markId, Long studentId, String studentName, String rollNo, String department, String subject, Integer marks, LocalDateTime createdAt) {
        this.markId = markId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.rollNo = rollNo;
        this.department = department;
        this.subject = subject;
        this.marks = marks;
        this.createdAt = createdAt;
    }

    public Long getMarkId() {
        return markId;
    }

    public void setMarkId(Long markId) {
        this.markId = markId;
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

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Integer getMarks() {
        return marks;
    }

    public void setMarks(Integer marks) {
        this.marks = marks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getFormattedDate() {
        return createdAt != null ? createdAt.format(FORMATTER) : "N/A";
    }
}
