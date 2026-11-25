package com.rbac.model;

import java.time.LocalDateTime;

/**
 * DTO for displaying assignment status, student submission state, and grades.
 */
public class AssignmentViewDto {

    private final Assignment assignment;
    private final Submission submission;

    public AssignmentViewDto(Assignment assignment, Submission submission) {
        this.assignment = assignment;
        this.submission = submission;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public Submission getSubmission() {
        return submission;
    }

    public Long getId() {
        return assignment.getId();
    }

    public String getTitle() {
        return assignment.getTitle();
    }

    public String getDescription() {
        return assignment.getDescription();
    }

    public String getCourseName() {
        return assignment.getCourseName();
    }

    public String getCourseCode() {
        return assignment.getCourseCode();
    }

    public LocalDateTime getDueDate() {
        return assignment.getDueDate();
    }

    public Integer getMaxMarks() {
        return assignment.getMaxMarks();
    }

    public String getStatus() {
        if (submission != null) {
            return submission.isGraded() ? "GRADED" : "SUBMITTED";
        }
        if (assignment.isPastDue()) {
            return "LATE";
        }
        return "PENDING";
    }

    public String getStatusBadgeClass() {
        String st = getStatus();
        return switch (st) {
            case "GRADED" -> "bg-success text-white";
            case "SUBMITTED" -> "bg-info text-dark";
            case "LATE" -> "bg-danger text-white";
            default -> "bg-warning text-dark";
        };
    }

    public boolean isDueSoon() {
        return submission == null && assignment.isDueSoon();
    }

    public boolean isSubmitted() {
        return submission != null;
    }

    public boolean isGraded() {
        return submission != null && submission.isGraded();
    }

    public boolean isCanSubmit() {
        return !assignment.isPastDue();
    }
}
