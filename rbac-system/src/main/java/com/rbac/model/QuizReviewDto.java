package com.rbac.model;

/**
 * DTO for post-attempt student quiz review.
 */
public class QuizReviewDto {

    private final Question question;
    private final String selectedOption;
    private final boolean correct;
    private final int marksEarned;

    public QuizReviewDto(Question question, String selectedOption, boolean correct, int marksEarned) {
        this.question = question;
        this.selectedOption = selectedOption;
        this.correct = correct;
        this.marksEarned = marksEarned;
    }

    public Question getQuestion() {
        return question;
    }

    public String getSelectedOption() {
        return selectedOption;
    }

    public boolean isCorrect() {
        return correct;
    }

    public int getMarksEarned() {
        return marksEarned;
    }
}
