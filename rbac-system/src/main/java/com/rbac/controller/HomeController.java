package com.rbac.controller;

import com.rbac.model.Announcement;
import com.rbac.model.Complaint;
import com.rbac.model.FacultyProfile;
import com.rbac.model.Quiz;
import com.rbac.model.QuizExtensionRequest;
import com.rbac.model.SessionUser;
import com.rbac.model.Student;
import com.rbac.repository.FacultyProfileRepository;
import com.rbac.repository.StudentRepository;
import com.rbac.service.AnnouncementService;
import com.rbac.service.ComplaintService;
import com.rbac.service.QuizService;
import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Controller for home root redirect and user dashboard.
 * // OOP: Abstraction & Encapsulation.
 */
@Controller
public class HomeController {

    private final JdbcTemplate jdbcTemplate;
    private final AnnouncementService announcementService;
    private final FacultyProfileRepository facultyProfileRepository;
    private final ComplaintService complaintService;
    private final QuizService quizService;
    private final StudentRepository studentRepository;

    public HomeController(JdbcTemplate jdbcTemplate,
                          AnnouncementService announcementService,
                          FacultyProfileRepository facultyProfileRepository,
                          ComplaintService complaintService,
                          QuizService quizService,
                          StudentRepository studentRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.announcementService = announcementService;
        this.facultyProfileRepository = facultyProfileRepository;
        this.complaintService = complaintService;
        this.quizService = quizService;
        this.studentRepository = studentRepository;
    }

    @GetMapping("/")
    public String rootRedirect() {
        return "redirect:/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        if (result != null && result == 1) {
            model.addAttribute("message", "Database connected");
        } else {
            model.addAttribute("message", "Database connection failed");
        }

        // Announcements on dashboard
        List<Announcement> announcements = announcementService.getActiveAnnouncements();
        model.addAttribute("announcements", announcements);

        // Role-specific dashboard enhancements
        SessionUser su = (SessionUser) session.getAttribute("sessionUser");
        if (su != null) {
            if (su.isFaculty()) {
                // Faculty sees own department and courses on dashboard
                FacultyProfile facultyProfile = facultyProfileRepository.findByUserId(su.getUserId());
                model.addAttribute("facultyProfile", facultyProfile);
            } else if (su.isStudent()) {
                // Student complaints summary
                List<Complaint> myComplaints = complaintService.getStudentComplaints(su.getUserId());
                model.addAttribute("myComplaints", myComplaints);

                // Student Quiz Notifications (Students only)
                Student student = studentRepository.findByUserId(su.getUserId());
                if (student != null) {
                    List<Quiz> unattemptedQuizzes = quizService.getUnattemptedAvailableQuizzesForStudent(student.getId());
                    model.addAttribute("quizBanners", unattemptedQuizzes);

                    List<QuizExtensionRequest> extensionUpdates = quizService.getUnseenDecidedRequestsForStudent(student.getId());
                    model.addAttribute("extensionUpdates", extensionUpdates);
                }
            }
        }

        return "dashboard";
    }
}
