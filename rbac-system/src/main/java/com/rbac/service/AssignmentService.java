package com.rbac.service;

import com.rbac.exception.AccessDeniedException;
import com.rbac.exception.InvalidFileException;
import com.rbac.exception.ResourceNotFoundException;
import com.rbac.model.Assignment;
import com.rbac.model.AssignmentViewDto;
import com.rbac.model.AuditLog;
import com.rbac.model.Student;
import com.rbac.model.Submission;
import com.rbac.repository.AssignmentRepository;
import com.rbac.repository.StudentRepository;
import com.rbac.repository.SubmissionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service managing course assignments, student submissions, and grading.
 * // OOP: Encapsulation – encapsulates assignment workflow logic.
 */
@Service
public class AssignmentService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "pdf", "doc", "docx", "ppt", "pptx", "zip", "jpg", "jpeg", "png", "txt"
    );

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final StudentRepository studentRepository;
    private final AuditService auditService;
    private final Path submissionUploadPath;

    public AssignmentService(AssignmentRepository assignmentRepository,
                             SubmissionRepository submissionRepository,
                             StudentRepository studentRepository,
                             AuditService auditService,
                             @Value("${app.upload.dir:uploads}") String uploadDir) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.studentRepository = studentRepository;
        this.auditService = auditService;
        this.submissionUploadPath = Paths.get(uploadDir, "submissions").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.submissionUploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize submissions upload directory: " + e.getMessage(), e);
        }
    }

    public List<Assignment> listAssignments() {
        return assignmentRepository.findAll();
    }

    public Assignment getAssignmentById(Long id) {
        Assignment a = assignmentRepository.findById(id);
        if (a == null) {
            throw new ResourceNotFoundException("Assignment with ID " + id + " not found.");
        }
        return a;
    }

    @Transactional
    public Assignment createAssignment(Long courseId, Long createdBy, String title, String description,
                                      LocalDateTime dueDate, Integer maxMarks) {
        Assignment a = new Assignment();
        a.setCourseId(courseId);
        a.setCreatedBy(createdBy);
        a.setTitle(title.trim());
        a.setDescription(description != null ? description.trim() : null);
        a.setDueDate(dueDate);
        a.setMaxMarks(maxMarks != null && maxMarks > 0 ? maxMarks : 100);

        Long id = assignmentRepository.save(a);
        a.setId(id);
        auditService.log(createdBy, "ASSIGNMENT_CREATE", "Created assignment '" + title + "' (ID: " + id + ")", AuditLog.Status.SUCCESS);
        return a;
    }

    @Transactional
    public void updateAssignment(Long id, Long courseId, String title, String description,
                                 LocalDateTime dueDate, Integer maxMarks, Long currentUserId) {
        Assignment existing = getAssignmentById(id);
        existing.setCourseId(courseId);
        existing.setTitle(title.trim());
        existing.setDescription(description != null ? description.trim() : null);
        existing.setDueDate(dueDate);
        existing.setMaxMarks(maxMarks != null && maxMarks > 0 ? maxMarks : 100);

        assignmentRepository.update(existing);
        auditService.log(currentUserId, "ASSIGNMENT_UPDATE", "Updated assignment ID " + id, AuditLog.Status.SUCCESS);
    }

    @Transactional
    public void deleteAssignment(Long id, Long currentUserId) {
        Assignment a = getAssignmentById(id);
        assignmentRepository.deleteById(id);
        auditService.log(currentUserId, "ASSIGNMENT_DELETE", "Deleted assignment '" + a.getTitle() + "' (ID: " + id + ")", AuditLog.Status.SUCCESS);
    }

    public List<AssignmentViewDto> listStudentAssignments(Long studentId) {
        List<Assignment> all = assignmentRepository.findAll();
        List<Submission> studentSubmissions = submissionRepository.findByStudentId(studentId);
        Map<Long, Submission> subMap = studentSubmissions.stream()
                .collect(Collectors.toMap(Submission::getAssignmentId, Function.identity(), (s1, s2) -> s1));

        List<AssignmentViewDto> views = new ArrayList<>();
        for (Assignment a : all) {
            Submission s = subMap.get(a.getId());
            views.add(new AssignmentViewDto(a, s));
        }
        return views;
    }

    public List<Submission> listSubmissionsForAssignment(Long assignmentId) {
        getAssignmentById(assignmentId);
        return submissionRepository.findByAssignmentId(assignmentId);
    }

    public Submission getSubmissionById(Long submissionId) {
        Submission s = submissionRepository.findById(submissionId);
        if (s == null) {
            throw new ResourceNotFoundException("Submission with ID " + submissionId + " not found.");
        }
        return s;
    }

    @Transactional
    public Submission submitAssignment(Long assignmentId, Long studentId, MultipartFile file, Long userId) {
        Assignment a = getAssignmentById(assignmentId);
        validateFile(file);

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "submission");
        String extension = getFileExtension(originalFilename);
        String storedFilename = "sub_" + assignmentId + "_" + studentId + "_" + UUID.randomUUID().toString() + "." + extension;
        Path targetPath = submissionUploadPath.resolve(storedFilename);

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store submitted assignment file: " + e.getMessage(), e);
        }

        Submission existing = submissionRepository.findByAssignmentAndStudent(assignmentId, studentId);
        if (existing != null) {
            // Delete old file
            try {
                Files.deleteIfExists(Paths.get(existing.getFilePath()));
            } catch (IOException ignored) {
            }
            submissionRepository.updateFile(existing.getId(), originalFilename, targetPath.toString());
            existing.setFileName(originalFilename);
            existing.setFilePath(targetPath.toString());
            auditService.log(userId, "ASSIGNMENT_RESUBMIT", "Resubmitted assignment ID " + assignmentId, AuditLog.Status.SUCCESS);
            return existing;
        } else {
            Submission s = new Submission();
            s.setAssignmentId(assignmentId);
            s.setStudentId(studentId);
            s.setFileName(originalFilename);
            s.setFilePath(targetPath.toString());
            Long id = submissionRepository.save(s);
            s.setId(id);
            auditService.log(userId, "ASSIGNMENT_SUBMIT", "Submitted assignment ID " + assignmentId + " (Sub ID: " + id + ")", AuditLog.Status.SUCCESS);
            return s;
        }
    }

    @Transactional
    public void gradeSubmission(Long submissionId, Integer marks, String feedback, Long facultyUserId) {
        Submission s = getSubmissionById(submissionId);
        Assignment a = getAssignmentById(s.getAssignmentId());

        if (marks != null && (marks < 0 || marks > a.getMaxMarks())) {
            throw new IllegalArgumentException("Marks must be between 0 and " + a.getMaxMarks() + ".");
        }

        submissionRepository.grade(submissionId, marks, feedback != null ? feedback.trim() : null);
        auditService.log(facultyUserId, "ASSIGNMENT_GRADE", "Graded submission ID " + submissionId + " with score: " + marks, AuditLog.Status.SUCCESS);
    }

    public Resource loadSubmissionFile(Long submissionId, Long currentUserId, boolean isStaff) {
        Submission s = getSubmissionById(submissionId);
        if (!isStaff) {
            // Verify student ownership
            Student student = studentRepository.findByUserId(currentUserId);
            if (student == null || !student.getId().equals(s.getStudentId())) {
                throw new AccessDeniedException("You are not authorized to download this submission.");
            }
        }

        try {
            Path filePath = Paths.get(s.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Submission file not found on disk.");
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File path error: " + e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Please select a file to submit.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException("File size exceeds 10 MB limit.");
        }
        String originalFilename = file.getOriginalFilename();
        String ext = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new InvalidFileException("Unsupported file type ." + ext + ". Allowed types: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
