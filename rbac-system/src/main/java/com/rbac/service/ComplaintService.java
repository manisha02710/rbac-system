package com.rbac.service;

import com.rbac.exception.ResourceNotFoundException;
import com.rbac.model.AuditLog;
import com.rbac.model.Complaint;
import com.rbac.model.Student;
import com.rbac.repository.ComplaintRepository;
import com.rbac.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final StudentRepository studentRepository;
    private final AuditService auditService;

    public ComplaintService(ComplaintRepository complaintRepository,
                            StudentRepository studentRepository,
                            AuditService auditService) {
        this.complaintRepository = complaintRepository;
        this.studentRepository = studentRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Complaint submitComplaint(Long studentUserId, String subject, String description, String categoryStr) {
        Student student = studentRepository.findByUserId(studentUserId);
        if (student == null) {
            throw new ResourceNotFoundException("Student profile not found for user id: " + studentUserId);
        }
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("Subject is required.");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required.");
        }
        Complaint.Category category;
        try {
            category = Complaint.Category.valueOf(categoryStr.toUpperCase().trim());
        } catch (Exception e) {
            category = Complaint.Category.OTHER;
        }

        Complaint complaint = new Complaint();
        complaint.setStudentId(student.getId());
        complaint.setSubject(subject.trim());
        complaint.setDescription(description.trim());
        complaint.setCategory(category);
        complaint.setStatus(Complaint.Status.OPEN);

        Long id = complaintRepository.save(complaint);
        complaint.setId(id);

        auditService.log(studentUserId, "COMPLAINT_CREATE", "Submitted complaint #" + id + ": " + complaint.getSubject(), AuditLog.Status.SUCCESS);
        return complaint;
    }

    public List<Complaint> getStudentComplaints(Long studentUserId) {
        Student student = studentRepository.findByUserId(studentUserId);
        if (student == null) {
            return List.of();
        }
        return complaintRepository.findByStudentId(student.getId());
    }

    public List<Complaint> getAllComplaints(String statusFilter) {
        return complaintRepository.findAll(statusFilter);
    }

    public Complaint getComplaintById(Long complaintId) {
        Complaint c = complaintRepository.findById(complaintId);
        if (c == null) {
            throw new ResourceNotFoundException("Complaint", complaintId);
        }
        return c;
    }

    @Transactional
    public void respondToComplaint(Long complaintId, String adminResponse, String statusStr, Long adminUserId) {
        Complaint complaint = complaintRepository.findById(complaintId);
        if (complaint == null) {
            throw new ResourceNotFoundException("Complaint", complaintId);
        }
        if (adminResponse == null || adminResponse.trim().isEmpty()) {
            throw new IllegalArgumentException("Response text is required.");
        }
        Complaint.Status status;
        try {
            status = Complaint.Status.valueOf(statusStr.toUpperCase().trim());
        } catch (Exception e) {
            status = Complaint.Status.RESOLVED;
        }

        complaintRepository.updateResponse(complaintId, adminResponse.trim(), status.name());
        auditService.log(adminUserId, "COMPLAINT_MANAGE", "Responded to complaint #" + complaintId + " [Status: " + status + "]", AuditLog.Status.SUCCESS);
    }
}
