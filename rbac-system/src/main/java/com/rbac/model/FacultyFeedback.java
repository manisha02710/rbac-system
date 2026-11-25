package com.rbac.model;

import java.time.LocalDateTime;

/**
 * OOP Concept: Inheritance – Extends BaseEntity.
 * OOP Concept: Encapsulation – Private fields with public accessors.
 * Represents student feedback and ratings (1-5 stars) for faculty.
 */
public class FacultyFeedback extends BaseEntity {

    private Long studentId;
    private Long facultyId;
    private String facultyName;
    private Long courseId;
    private String courseName;
    private String courseCode;
    private Integer rating;
    private String comments;
    private LocalDateTime createdAt;

    public FacultyFeedback() {
        super();
    }

    public FacultyFeedback(Long id, Long studentId, Long facultyId, Long courseId,
                           Integer rating, String comments, LocalDateTime createdAt) {
        super(id, createdAt);
        this.studentId = studentId;
        this.facultyId = facultyId;
        this.courseId = courseId;
        this.rating = rating;
        this.comments = comments;
        this.createdAt = createdAt;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(Long facultyId) {
        this.facultyId = facultyId;
    }

    public String getFacultyName() {
        return facultyName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
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

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
