package com.rbac.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated faculty feedback view object for Admin and Faculty dashboards.
 * Guaranteed anonymous: comments do NOT reveal student identity.
 */
public class FacultyFeedbackViewDto {

    private Long facultyId;
    private String facultyName;
    private String departmentName;
    private Long courseId;
    private String courseName;
    private String courseCode;
    private double averageRating;
    private int totalReviews;
    private List<AnonymousComment> comments = new ArrayList<>();

    public static class AnonymousComment {
        private final int rating;
        private final String text;
        private final String dateFormatted;

        public AnonymousComment(int rating, String text, String dateFormatted) {
            this.rating = rating;
            this.text = text;
            this.dateFormatted = dateFormatted;
        }

        public int getRating() {
            return rating;
        }

        public String getText() {
            return text;
        }

        public String getDateFormatted() {
            return dateFormatted;
        }
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

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
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

    public double getAverageRating() {
        return averageRating;
    }

    public double getAvgRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public int getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(int totalReviews) {
        this.totalReviews = totalReviews;
    }

    public List<AnonymousComment> getComments() {
        return comments;
    }

    public List<String> getAnonymousComments() {
        List<String> list = new ArrayList<>();
        if (comments != null) {
            for (AnonymousComment c : comments) {
                if (c.getText() != null && !c.getText().isBlank()) {
                    list.add(c.getText());
                }
            }
        }
        return list;
    }

    public void setComments(List<AnonymousComment> comments) {
        this.comments = comments;
    }

    public int getRoundedRating() {
        return (int) Math.round(averageRating);
    }
}
