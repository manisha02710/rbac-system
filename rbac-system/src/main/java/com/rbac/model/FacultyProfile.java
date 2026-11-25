package com.rbac.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * OOP Concept: Inheritance – Extends BaseEntity.
 * OOP Concept: Encapsulation – Private fields with getters and setters.
 * Represents faculty academic details and many-to-many course assignments.
 */
public class FacultyProfile extends BaseEntity {

    private Long userId;
    private Long departmentId;
    private String departmentName;
    private String facultyName;
    private String username;
    private String email;
    private Boolean isActive;
    private List<Course> courses = new ArrayList<>();

    public FacultyProfile() {
        super();
    }

    public FacultyProfile(Long id, Long userId, Long departmentId) {
        super(id, LocalDateTime.now());
        this.userId = userId;
        this.departmentId = departmentId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return (departmentName != null && !departmentName.isBlank()) ? departmentName : "Not assigned";
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getFacultyName() {
        return facultyName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public void setCourses(List<Course> courses) {
        this.courses = courses != null ? courses : new ArrayList<>();
    }

    public boolean hasCourses() {
        return courses != null && !courses.isEmpty();
    }
}
