package com.rbac.controller;

import com.rbac.annotation.RequiresPermission;
import com.rbac.model.Course;
import com.rbac.model.FacultyFeedback;
import com.rbac.model.FacultyFeedbackViewDto;
import com.rbac.model.FacultyProfile;
import com.rbac.model.SessionUser;
import com.rbac.model.Student;
import com.rbac.repository.FacultyProfileRepository;
import com.rbac.repository.StudentRepository;
import com.rbac.service.FacultyDepartmentService;
import com.rbac.service.FeedbackService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller managing faculty feedback from students, admin aggregate views, and faculty views.
 * Strictly preserves anonymity.
 */
@Controller
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final FacultyDepartmentService facultyDepartmentService;
    private final FacultyProfileRepository facultyProfileRepository;
    private final StudentRepository studentRepository;

    public FeedbackController(FeedbackService feedbackService,
                              FacultyDepartmentService facultyDepartmentService,
                              FacultyProfileRepository facultyProfileRepository,
                              StudentRepository studentRepository) {
        this.feedbackService = feedbackService;
        this.facultyDepartmentService = facultyDepartmentService;
        this.facultyProfileRepository = facultyProfileRepository;
        this.studentRepository = studentRepository;
    }

    private SessionUser getSessionUser(HttpSession session) {
        return (SessionUser) session.getAttribute("sessionUser");
    }

    @RequiresPermission("FEEDBACK_SUBMIT")
    @GetMapping("/feedback/rate")
    public String showRateFacultyPage(Model model, HttpSession session) {
        SessionUser sessionUser = getSessionUser(session);
        Student student = sessionUser != null ? studentRepository.findByUserId(sessionUser.getUserId()) : null;

        List<Course> courses = facultyDepartmentService.listCourses();
        List<FacultyProfile> allFaculty = facultyProfileRepository.findAllFacultyWithDetails();
        // Ensure faculty profiles exist with non-null ID
        List<FacultyProfile> facultyList = allFaculty.stream()
                .peek(fp -> {
                    if (fp.getId() == null) {
                        Long newFid = facultyProfileRepository.saveProfile(fp.getUserId(), fp.getDepartmentId());
                        fp.setId(newFid);
                    }
                })
                .collect(Collectors.toList());

        List<FacultyFeedback> myFeedback = student != null ? feedbackService.getStudentFeedback(student.getId()) : List.of();

        model.addAttribute("courses", courses);
        model.addAttribute("facultyList", facultyList);
        model.addAttribute("myFeedback", myFeedback);
        return "feedback/rate";
    }

    @RequiresPermission("FEEDBACK_SUBMIT")
    @PostMapping("/feedback/submit")
    public String submitFeedback(@RequestParam Long facultyId,
                                 @RequestParam Long courseId,
                                 @RequestParam int rating,
                                 @RequestParam(required = false) String comments,
                                 HttpSession session,
                                 RedirectAttributes redirectAttrs) {
        SessionUser sessionUser = getSessionUser(session);
        Student student = sessionUser != null ? studentRepository.findByUserId(sessionUser.getUserId()) : null;
        if (student == null) {
            redirectAttrs.addFlashAttribute("error", "No linked student profile found for your account.");
            return "redirect:/feedback/rate";
        }

        try {
            feedbackService.submitFeedback(student.getId(), facultyId, courseId, rating, comments, sessionUser.getUserId());
            redirectAttrs.addFlashAttribute("success", "Thank you! Your feedback has been submitted anonymously.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to submit feedback: " + e.getMessage());
        }
        return "redirect:/feedback/rate";
    }

    @RequiresPermission("FEEDBACK_VIEW_ALL")
    @GetMapping("/admin/faculty-feedback")
    public String showAdminFeedback(Model model) {
        List<FacultyFeedbackViewDto> aggregates = feedbackService.getAllFeedbackAggregates();
        model.addAttribute("aggregates", aggregates);
        return "feedback/admin";
    }

    @RequiresPermission("FEEDBACK_VIEW_OWN")
    @GetMapping("/faculty/feedback")
    public String showFacultyOwnFeedback(Model model, HttpSession session) {
        SessionUser sessionUser = getSessionUser(session);
        List<FacultyFeedbackViewDto> aggregates = sessionUser != null
                ? feedbackService.getFacultyFeedbackAggregates(sessionUser.getUserId())
                : List.of();
        model.addAttribute("aggregates", aggregates);
        return "feedback/faculty";
    }
}
