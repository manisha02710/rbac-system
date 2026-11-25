package com.rbac.service;

import com.rbac.exception.ResourceNotFoundException;
import com.rbac.model.Announcement;
import com.rbac.model.AuditLog;
import com.rbac.repository.AnnouncementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AuditService auditService;

    public AnnouncementService(AnnouncementRepository announcementRepository, AuditService auditService) {
        this.announcementRepository = announcementRepository;
        this.auditService = auditService;
    }

    public List<Announcement> getAllAnnouncements() {
        return announcementRepository.findAll();
    }

    public List<Announcement> getActiveAnnouncements() {
        return announcementRepository.findActiveNonExpired();
    }

    public Announcement getById(Long id) {
        Announcement a = announcementRepository.findById(id);
        if (a == null) {
            throw new ResourceNotFoundException("Announcement", id);
        }
        return a;
    }

    @Transactional
    public Announcement createAnnouncement(String title, String message, String categoryStr, LocalDate expiresOn, Long adminUserId) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required.");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message is required.");
        }
        Announcement.Category category;
        try {
            category = Announcement.Category.valueOf(categoryStr.toUpperCase().trim());
        } catch (Exception e) {
            category = Announcement.Category.GENERAL;
        }

        Announcement a = new Announcement();
        a.setTitle(title.trim());
        a.setMessage(message.trim());
        a.setCategory(category);
        a.setPostedBy(adminUserId);
        a.setExpiresOn(expiresOn);

        Long id = announcementRepository.save(a);
        a.setId(id);

        auditService.log(adminUserId, "ANNOUNCEMENT_CREATE", "Created announcement #" + id + ": " + a.getTitle(), AuditLog.Status.SUCCESS);
        return a;
    }

    @Transactional
    public Announcement updateAnnouncement(Long id, String title, String message, String categoryStr, LocalDate expiresOn, Long adminUserId) {
        Announcement a = announcementRepository.findById(id);
        if (a == null) {
            throw new ResourceNotFoundException("Announcement", id);
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required.");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message is required.");
        }
        Announcement.Category category;
        try {
            category = Announcement.Category.valueOf(categoryStr.toUpperCase().trim());
        } catch (Exception e) {
            category = Announcement.Category.GENERAL;
        }

        a.setTitle(title.trim());
        a.setMessage(message.trim());
        a.setCategory(category);
        a.setExpiresOn(expiresOn);

        announcementRepository.update(a);
        auditService.log(adminUserId, "ANNOUNCEMENT_UPDATE", "Updated announcement #" + id + ": " + a.getTitle(), AuditLog.Status.SUCCESS);
        return a;
    }

    @Transactional
    public void deleteAnnouncement(Long id, Long adminUserId) {
        Announcement a = announcementRepository.findById(id);
        if (a == null) {
            throw new ResourceNotFoundException("Announcement", id);
        }
        announcementRepository.deleteById(id);
        auditService.log(adminUserId, "ANNOUNCEMENT_DELETE", "Deleted announcement #" + id + ": " + a.getTitle(), AuditLog.Status.SUCCESS);
    }
}
