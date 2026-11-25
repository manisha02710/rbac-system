package com.rbac.model;

import java.time.LocalDateTime;

/**
 * OOP Concept: Inheritance – Extends BaseEntity.
 * OOP Concept: Encapsulation – Private fields with public accessors.
 * Represents an online quiz.
 */
public class Quiz extends BaseEntity {

    private Long courseId;
    private String courseName;
    private String courseCode;
    private Long createdBy;
    private String creatorName;
    private String title;
    private Integer durationMinutes;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean isPublished;
    private int questionCount;
    private int totalMarks;

    public Quiz() {
        super();
    }

    public Quiz(Long id, Long courseId, Long createdBy, String title, Integer durationMinutes,
                LocalDateTime startTime, LocalDateTime endTime, Boolean isPublished, LocalDateTime createdAt) {
        super(id, createdAt);
        this.courseId = courseId;
        this.createdBy = createdBy;
        this.title = title;
        this.durationMinutes = durationMinutes;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isPublished = isPublished;
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

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Boolean getIsPublished() {
        return isPublished;
    }

    public void setIsPublished(Boolean published) {
        isPublished = published;
    }

    public boolean isPublished() {
        return Boolean.TRUE.equals(isPublished);
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }

    public int getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(int totalMarks) {
        this.totalMarks = totalMarks;
    }

    public boolean isLive() {
        LocalDateTime now = LocalDateTime.now();
        return isPublished() && (startTime == null || now.isAfter(startTime)) && (endTime == null || now.isBefore(endTime));
    }

    public boolean isAvailable() {
        return isLive();
    }

    public boolean isExpired() {
        return endTime != null && LocalDateTime.now().isAfter(endTime);
    }

    public boolean isUpcoming() {
        return startTime != null && LocalDateTime.now().isBefore(startTime);
    }

    public boolean isDueSoon() {
        if (endTime == null) return false;
        LocalDateTime now = LocalDateTime.now();
        return isLive() && endTime.isAfter(now) && java.time.Duration.between(now, endTime).toHours() < 24;
    }

    public boolean isNew() {
        if (!isPublished()) return false;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime ref = getCreatedAt() != null ? getCreatedAt() : getStartTime();
        if (ref == null) return false;
        return ref.isAfter(now.minusDays(3));
    }
}
