package com.rbac.model;

import java.time.LocalDateTime;

/**
 * Entity representing a student's request for additional time on a quiz.
 * // OOP: Inheritance – extends BaseEntity.
 * // OOP: Encapsulation – private fields with getters and setters.
 */
public class QuizExtensionRequest extends BaseEntity {

    private Long quizId;
    private String quizTitle;
    private Long studentId;
    private String studentName;
    private String rollNo;
    private String department;
    private String reason;
    private int requestedMinutes;
    private String status; // PENDING, APPROVED, REJECTED
    private LocalDateTime decidedAt;
    private Long decidedBy;
    private String decidedByName;
    private Boolean seen;

    public QuizExtensionRequest() {
        super();
    }

    public QuizExtensionRequest(Long id, Long quizId, Long studentId, String reason, int requestedMinutes,
                                String status, LocalDateTime createdAt, LocalDateTime decidedAt, Long decidedBy) {
        super(id, createdAt);
        this.quizId = quizId;
        this.studentId = studentId;
        this.reason = reason;
        this.requestedMinutes = requestedMinutes;
        this.status = status;
        this.decidedAt = decidedAt;
        this.decidedBy = decidedBy;
    }

    public Long getQuizId() {
        return quizId;
    }

    public void setQuizId(Long quizId) {
        this.quizId = quizId;
    }

    public String getQuizTitle() {
        return quizTitle;
    }

    public void setQuizTitle(String quizTitle) {
        this.quizTitle = quizTitle;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getRequestedMinutes() {
        return requestedMinutes;
    }

    public void setRequestedMinutes(int requestedMinutes) {
        this.requestedMinutes = requestedMinutes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }

    public Long getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(Long decidedBy) {
        this.decidedBy = decidedBy;
    }

    public String getDecidedByName() {
        return decidedByName;
    }

    public void setDecidedByName(String decidedByName) {
        this.decidedByName = decidedByName;
    }

    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(status);
    }

    public boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(status);
    }

    public boolean isRejected() {
        return "REJECTED".equalsIgnoreCase(status);
    }

    public Boolean getSeen() {
        return seen;
    }

    public void setSeen(Boolean seen) {
        this.seen = seen;
    }

    public boolean isSeen() {
        return Boolean.TRUE.equals(seen);
    }
}
