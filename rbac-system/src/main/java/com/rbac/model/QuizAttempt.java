package com.rbac.model;

import java.time.LocalDateTime;

/**
 * OOP Concept: Inheritance – Extends BaseEntity.
 * OOP Concept: Encapsulation – Private fields with public accessors.
 * Represents a student's attempt at taking a quiz.
 */
public class QuizAttempt extends BaseEntity {

    private Long quizId;
    private String quizTitle;
    private Long studentId;
    private String studentName;
    private String rollNo;
    private String department;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Integer score;
    private int totalQuizMarks;
    private Integer extraMinutes = 0;

    public QuizAttempt() {
        super();
    }

    public QuizAttempt(Long id, Long quizId, Long studentId, LocalDateTime startedAt,
                       LocalDateTime submittedAt, Integer score) {
        super(id, startedAt);
        this.quizId = quizId;
        this.studentId = studentId;
        this.startedAt = startedAt;
        this.submittedAt = submittedAt;
        this.score = score;
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

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public int getTotalQuizMarks() {
        return totalQuizMarks;
    }

    public void setTotalQuizMarks(int totalQuizMarks) {
        this.totalQuizMarks = totalQuizMarks;
    }

    public Integer getExtraMinutes() {
        return extraMinutes != null ? extraMinutes : 0;
    }

    public void setExtraMinutes(Integer extraMinutes) {
        this.extraMinutes = extraMinutes != null ? extraMinutes : 0;
    }

    public boolean isSubmitted() {
        return submittedAt != null;
    }
}
