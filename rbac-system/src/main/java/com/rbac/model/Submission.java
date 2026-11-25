package com.rbac.model;

import java.time.LocalDateTime;

/**
 * OOP Concept: Inheritance – Extends BaseEntity.
 * OOP Concept: Encapsulation – Private fields with public accessors.
 * Represents a student's submission for an assignment.
 */
public class Submission extends BaseEntity {

    private Long assignmentId;
    private Long studentId;
    private String studentName;
    private String rollNo;
    private String fileName;
    private String filePath;
    private LocalDateTime submittedAt;
    private Integer marksAwarded;
    private String feedback;

    public Submission() {
        super();
    }

    public Submission(Long id, Long assignmentId, Long studentId, String fileName,
                      String filePath, LocalDateTime submittedAt, Integer marksAwarded, String feedback) {
        super(id, submittedAt);
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.submittedAt = submittedAt;
        this.marksAwarded = marksAwarded;
        this.feedback = feedback;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Integer getMarksAwarded() {
        return marksAwarded;
    }

    public void setMarksAwarded(Integer marksAwarded) {
        this.marksAwarded = marksAwarded;
    }

    public Integer getMarks() {
        return marksAwarded;
    }

    public void setMarks(Integer marks) {
        this.marksAwarded = marks;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public boolean isGraded() {
        return marksAwarded != null;
    }
}
