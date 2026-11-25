package com.rbac.model;

import java.time.LocalDateTime;

/**
 * OOP Concept: Inheritance – Extends BaseEntity.
 * OOP Concept: Encapsulation – Private fields with getters and setters.
 */
public class Course extends BaseEntity {

    private String courseName;
    private String courseCode;
    private Long departmentId;
    private String departmentName; // Joined for display

    public Course() {
        super();
    }

    public Course(Long id, String courseName, String courseCode, Long departmentId) {
        super(id, LocalDateTime.now());
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.departmentId = departmentId;
    }

    public Course(Long id, String courseName, String courseCode, Long departmentId, String departmentName) {
        super(id, LocalDateTime.now());
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }
}
