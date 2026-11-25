package com.rbac.model;

import java.time.LocalDateTime;

/**
 * OOP Concept: Inheritance – Extends BaseEntity to inherit ID and createdAt.
 * OOP Concept: Encapsulation – Private fields with public accessors.
 * Represents course study material uploaded by faculty/admin.
 */
public class Material extends BaseEntity {

    private Long courseId;
    private String courseName;
    private String courseCode;
    private Long uploadedBy;
    private String uploaderName;
    private String title;
    private String description;
    private String fileName;
    private String filePath;
    private LocalDateTime uploadedAt;

    public Material() {
        super();
    }

    public Material(Long id, Long courseId, Long uploadedBy, String title, String description,
                    String fileName, String filePath, LocalDateTime uploadedAt) {
        super(id, uploadedAt);
        this.courseId = courseId;
        this.uploadedBy = uploadedBy;
        this.title = title;
        this.description = description;
        this.fileName = fileName;
        this.filePath = filePath;
        this.uploadedAt = uploadedAt;
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

    public Long getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Long uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getUploaderName() {
        return uploaderName;
    }

    public void setUploaderName(String uploaderName) {
        this.uploaderName = uploaderName;
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

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getFileExtension() {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        }
        return "file";
    }
}
