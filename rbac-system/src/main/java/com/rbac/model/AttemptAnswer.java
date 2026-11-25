package com.rbac.model;

/**
 * OOP Concept: Encapsulation – Private fields with public accessors.
 * Represents an answer chosen by a student for a question during a quiz attempt.
 */
public class AttemptAnswer {

    private Long attemptId;
    private Long questionId;
    private String selectedOption;

    public AttemptAnswer() {
    }

    public AttemptAnswer(Long attemptId, Long questionId, String selectedOption) {
        this.attemptId = attemptId;
        this.questionId = questionId;
        this.selectedOption = selectedOption;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(String selectedOption) {
        this.selectedOption = selectedOption;
    }
}
