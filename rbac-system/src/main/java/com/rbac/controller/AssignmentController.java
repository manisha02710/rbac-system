package com.rbac.controller;

import com.rbac.annotation.RequiresPermission;
import com.rbac.exception.AccessDeniedException;
import com.rbac.model.Assignment;
import com.rbac.model.AssignmentViewDto;
import com.rbac.model.Course;
import com.rbac.model.SessionUser;
import com.rbac.model.Student;
import com.rbac.model.Submission;
import com.rbac.repository.StudentRepository;
import com.rbac.service.AssignmentService;
import com.rbac.service.FacultyDepartmentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller handling assignments, submissions, and grading.
 */
@Controller
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final FacultyDepartmentService facultyDepartmentService;
    private final StudentRepository studentRepository;

    public AssignmentController(AssignmentService assignmentService,
                                FacultyDepartmentService facultyDepartmentService,
                                StudentRepository studentRepository) {
        this.assignmentService = assignmentService;
        this.facultyDepartmentService = facultyDepartmentService;
        this.studentRepository = studentRepository;
    }

    private SessionUser getSessionUser(HttpSession session) {
        return (SessionUser) session.getAttribute("sessionUser");
    }

    private LocalDateTime parseDateTime(String dtStr) {
        if (dtStr == null || dtStr.isBlank()) {
            return LocalDateTime.now().plusDays(7);
        }
        try {
            return LocalDateTime.parse(dtStr);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dtStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            } catch (Exception ignored) {
                return LocalDateTime.now().plusDays(7);
            }
        }
    }

    @RequiresPermission("ASSIGNMENT_VIEW")
    @GetMapping("/assignments")
    public String listAssignments(Model model, HttpSession session) {
        SessionUser sessionUser = getSessionUser(session);
        boolean canManage = sessionUser != null && sessionUser.getPermissions().contains("ASSIGNMENT_MANAGE");
        List<Course> courses = facultyDepartmentService.listCourses();
        model.addAttribute("courses", courses);
        model.addAttribute("canManage", canManage);

        if (canManage) {
            List<Assignment> assignments = assignmentService.listAssignments();
            model.addAttribute("assignments", assignments);
            model.addAttribute("isStudent", false);
        } else {
            // Student view
            Student student = sessionUser != null ? studentRepository.findByUserId(sessionUser.getUserId()) : null;
            if (student != null) {
                List<AssignmentViewDto> studentAssignments = assignmentService.listStudentAssignments(student.getId());
                model.addAttribute("assignments", studentAssignments);
            } else {
                model.addAttribute("assignments", List.of());
            }
            model.addAttribute("isStudent", true);
        }

        return "assignments/index";
    }

    @RequiresPermission("ASSIGNMENT_MANAGE")
    @PostMapping("/assignments/create")
    public String createAssignment(@RequestParam Long courseId,
                                   @RequestParam String title,
                                   @RequestParam(required = false) String description,
                                   @RequestParam String dueDate,
                                   @RequestParam(defaultValue = "100") Integer maxMarks,
                                   HttpSession session,
                                   RedirectAttributes redirectAttrs) {
        SessionUser sessionUser = getSessionUser(session);
        try {
            LocalDateTime parsedDueDate = parseDateTime(dueDate);
            assignmentService.createAssignment(courseId, sessionUser.getUserId(), title, description, parsedDueDate, maxMarks);
            redirectAttrs.addFlashAttribute("success", "Assignment created successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to create assignment: " + e.getMessage());
        }
        return "redirect:/assignments";
    }

    @RequiresPermission("ASSIGNMENT_MANAGE")
    @PostMapping("/assignments/{id}/edit")
    public String editAssignment(@PathVariable Long id,
                                 @RequestParam Long courseId,
                                 @RequestParam String title,
                                 @RequestParam(required = false) String description,
                                 @RequestParam String dueDate,
                                 @RequestParam(defaultValue = "100") Integer maxMarks,
                                 HttpSession session,
                                 RedirectAttributes redirectAttrs) {
        SessionUser sessionUser = getSessionUser(session);
        try {
            LocalDateTime parsedDueDate = parseDateTime(dueDate);
            assignmentService.updateAssignment(id, courseId, title, description, parsedDueDate, maxMarks, sessionUser.getUserId());
            redirectAttrs.addFlashAttribute("success", "Assignment updated successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to update assignment: " + e.getMessage());
        }
        return "redirect:/assignments";
    }

    @RequiresPermission("ASSIGNMENT_MANAGE")
    @PostMapping("/assignments/{id}/delete")
    public String deleteAssignment(@PathVariable Long id,
                                   HttpSession session,
                                   RedirectAttributes redirectAttrs) {
        SessionUser sessionUser = getSessionUser(session);
        try {
            assignmentService.deleteAssignment(id, sessionUser.getUserId());
            redirectAttrs.addFlashAttribute("success", "Assignment deleted successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to delete assignment: " + e.getMessage());
        }
        return "redirect:/assignments";
    }

    @RequiresPermission("ASSIGNMENT_MANAGE")
    @GetMapping("/assignments/{id}/submissions")
    public String listSubmissions(@PathVariable Long id, Model model) {
        Assignment assignment = assignmentService.getAssignmentById(id);
        List<Submission> submissions = assignmentService.listSubmissionsForAssignment(id);
        model.addAttribute("assignment", assignment);
        model.addAttribute("submissions", submissions);
        return "assignments/submissions";
    }

    @RequiresPermission("ASSIGNMENT_MANAGE")
    @PostMapping("/assignments/{id}/grade")
    public String gradeSubmission(@PathVariable Long id,
                                  @RequestParam Long submissionId,
                                  @RequestParam Integer marks,
                                  @RequestParam(required = false) String feedback,
                                  HttpSession session,
                                  RedirectAttributes redirectAttrs) {
        SessionUser sessionUser = getSessionUser(session);
        try {
            assignmentService.gradeSubmission(submissionId, marks, feedback, sessionUser.getUserId());
            redirectAttrs.addFlashAttribute("success", "Submission graded successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Grading failed: " + e.getMessage());
        }
        return "redirect:/assignments/" + id + "/submissions";
    }

    @RequiresPermission("ASSIGNMENT_SUBMIT")
    @PostMapping("/assignments/{id}/submit")
    public String submitAssignment(@PathVariable Long id,
                                   @RequestParam("file") MultipartFile file,
                                   HttpSession session,
                                   RedirectAttributes redirectAttrs) {
        SessionUser sessionUser = getSessionUser(session);
        Student student = sessionUser != null ? studentRepository.findByUserId(sessionUser.getUserId()) : null;
        if (student == null) {
            redirectAttrs.addFlashAttribute("error", "No linked student profile found for your account.");
            return "redirect:/assignments";
        }

        try {
            assignmentService.submitAssignment(id, student.getId(), file, sessionUser.getUserId());
            redirectAttrs.addFlashAttribute("success", "Assignment submitted successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Submission failed: " + e.getMessage());
        }
        return "redirect:/assignments";
    }

    @RequiresPermission("ASSIGNMENT_VIEW")
    @GetMapping("/assignments/submissions/{id}/download")
    public ResponseEntity<Resource> downloadSubmission(@PathVariable Long id, HttpSession session) {
        SessionUser sessionUser = getSessionUser(session);
        if (sessionUser == null) {
            throw new AccessDeniedException("User is not authenticated.");
        }

        boolean isStaff = sessionUser.isAdmin() || sessionUser.getPermissions().contains("ASSIGNMENT_MANAGE");
        Resource resource = assignmentService.loadSubmissionFile(id, sessionUser.getUserId(), isStaff);
        Submission submission = assignmentService.getSubmissionById(id);

        String filename = submission.getFileName() != null ? submission.getFileName() : "submission";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename.replace("\"", "") + "\"")
                .body(resource);
    }
}
