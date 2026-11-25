package com.rbac.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * OOP Concept: Inheritance – Extends BaseEntity.
 * OOP Concept: Encapsulation – Private fields with getters and setters.
 */
public class Announcement extends BaseEntity {

    public enum Category {
        HOLIDAY, EXAM, EVENT, GENERAL
    }

    private String title;
    private String message;
    private Category category;
    private Long postedBy;
    private String postedByName; // Joined from users
    private LocalDate expiresOn;

    public Announcement() {
        super();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Long getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(Long postedBy) {
        this.postedBy = postedBy;
    }

    public String getPostedByName() {
        return postedByName;
    }

    public void setPostedByName(String postedByName) {
        this.postedByName = postedByName;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public void setExpiresOn(LocalDate expiresOn) {
        this.expiresOn = expiresOn;
    }

    public boolean isExpired() {
        return expiresOn != null && expiresOn.isBefore(LocalDate.now());
    }
}
