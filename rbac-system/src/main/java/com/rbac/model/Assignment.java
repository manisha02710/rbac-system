package com.rbac.model;

import java.time.LocalDateTime;

/**
 * OOP Concept: Inheritance – Extends BaseEntity.
 * OOP Concept: Encapsulation – Private fields with public accessors.
 * Represents a course assignment created by faculty/admin.
 */
public class Assignment extends BaseEntity {

    private Long courseId;
    private String courseName;
    private String courseCode;
    private Long createdBy;
    private String creatorName;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private Integer maxMarks;
    private int submissionCount;

    public Assignment() {
        super();
    }

    public Assignment(Long id, Long courseId, Long createdBy, String title, String description,
                      LocalDateTime dueDate, Integer maxMarks, LocalDateTime createdAt) {
        super(id, createdAt);
        this.courseId = courseId;
        this.createdBy = createdBy;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.maxMarks = maxMarks;
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

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public Integer getMaxMarks() {
        return maxMarks != null ? maxMarks : 100;
    }

    public void setMaxMarks(Integer maxMarks) {
        this.maxMarks = maxMarks;
    }

    public int getSubmissionCount() {
        return submissionCount;
    }

    public void setSubmissionCount(int submissionCount) {
        this.submissionCount = submissionCount;
    }

    public boolean isPastDue() {
        return dueDate != null && LocalDateTime.now().isAfter(dueDate);
    }

    public boolean isDueSoon() {
        if (dueDate == null || isPastDue()) return false;
        LocalDateTime now = LocalDateTime.now();
        return dueDate.isBefore(now.plusDays(2));
    }
}
