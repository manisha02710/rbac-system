package com.rbac.service;

import com.rbac.model.AuditLog;
import com.rbac.model.FacultyFeedback;
import com.rbac.model.FacultyFeedbackViewDto;
import com.rbac.model.FacultyProfile;
import com.rbac.repository.FacultyFeedbackRepository;
import com.rbac.repository.FacultyProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing student faculty ratings and feedback.
 * Strictly guarantees anonymity: neither Admin nor Faculty are ever shown student names.
 */
@Service
public class FeedbackService {

    private final FacultyFeedbackRepository feedbackRepository;
    private final FacultyProfileRepository facultyProfileRepository;
    private final AuditService auditService;

    public FeedbackService(FacultyFeedbackRepository feedbackRepository,
                           FacultyProfileRepository facultyProfileRepository,
                           AuditService auditService) {
        this.feedbackRepository = feedbackRepository;
        this.facultyProfileRepository = facultyProfileRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void submitFeedback(Long studentId, Long facultyId, Long courseId, int rating, String comments, Long userId) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5 stars.");
        }

        feedbackRepository.saveOrUpdate(studentId, facultyId, courseId, rating, comments != null ? comments.trim() : null);
        auditService.log(userId, "FEEDBACK_SUBMIT", "Submitted feedback for faculty ID " + facultyId + " in course ID " + courseId + " (Rating: " + rating + ")", AuditLog.Status.SUCCESS);
    }

    public List<FacultyFeedback> getStudentFeedback(Long studentId) {
        return feedbackRepository.findByStudent(studentId);
    }

    public List<FacultyFeedbackViewDto> getAllFeedbackAggregates() {
        return feedbackRepository.findAggregates(null);
    }

    public List<FacultyFeedbackViewDto> getFacultyFeedbackAggregates(Long facultyUserId) {
        FacultyProfile fp = facultyProfileRepository.findByUserId(facultyUserId);
        if (fp == null || fp.getId() == null) {
            return List.of();
        }
        return feedbackRepository.findAggregates(fp.getId());
    }
}
