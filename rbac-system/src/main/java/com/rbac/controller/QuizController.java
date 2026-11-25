package com.rbac.controller;

import com.rbac.annotation.RequiresPermission;
import com.rbac.exception.AccessDeniedException;
import com.rbac.exception.ResourceNotFoundException;
import com.rbac.model.Course;
import com.rbac.model.Question;
import com.rbac.model.Quiz;
import com.rbac.model.QuizAttempt;
import com.rbac.model.QuizExtensionRequest;
import com.rbac.model.QuizReviewDto;
import com.rbac.model.SessionUser;
import com.rbac.model.Student;
import com.rbac.repository.StudentRepository;
import com.rbac.service.FacultyDepartmentService;
import com.rbac.service.QuizService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller managing online quizzes, question builders, timed student attempts,
 * faculty editing, time extension requests, and auto-grading.
 * Strictly faculty-only management and student-only attempts. Admin gets 403 on all quiz URLs.
 */
@Controller
public class QuizController {

    private final QuizService quizService;
    private final FacultyDepartmentService facultyDepartmentService;
    private final StudentRepository studentRepository;

    public QuizController(QuizService quizService,
                          FacultyDepartmentService facultyDepartmentService,
                          StudentRepository studentRepository) {
        this.quizService = quizService;
        this.facultyDepartmentService = facultyDepartmentService;
        this.studentRepository = studentRepository;
    }

    private SessionUser getSessionUser(HttpSession session) {
        return (SessionUser) session.getAttribute("sessionUser");
    }

    private LocalDateTime parseDateTime(String dtStr, LocalDateTime defaultVal) {
        if (dtStr == null || dtStr.isBlank()) {
            return defaultVal;
        }
        try {
            return LocalDateTime.parse(dtStr);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dtStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            } catch (Exception ignored) {
                return defaultVal;
            }
        }
    }

    private Quiz checkFacultyQuizOwnership(Long quizId, SessionUser sessionUser) {
        if (sessionUser == null) {
            throw new AccessDeniedException("Authentication required.");
        }
        if (sessionUser.isAdmin()) {
            throw new AccessDeniedException("Administrators are not permitted to manage quizzes.");
        }
        if (!sessionUser.isFaculty()) {
            throw new AccessDeniedException("Only faculty members can manage quizzes.");
        }
        Quiz quiz = quizService.getQuizById(quizId);
        if (quiz == null) {
            throw new ResourceNotFoundException("Quiz with ID " + quizId + " not found.");
        }
        if (!sessionUser.getUserId().equals(quiz.getCreatedBy())) {
            throw new AccessDeniedException("Access denied: You can only edit or view results for quizzes you created.");
        }
        return quiz;
    }

    @RequiresPermission("QUIZ_MANAGE")
    @GetMapping("/quizzes/manage")
    public String manageQuizzesRedirect(HttpSession session) {
        SessionUser sessionUser = getSessionUser(session);
        if (sessionUser != null && sessionUser.isAdmin()) {
            throw new AccessDeniedException("Administrators are not permitted to access quiz portals.");
        }
        return "redirect:/quizzes";
    }

    @GetMapping("/quizzes")
    public String listQuizzes(Model model, HttpSession session) {
        SessionUser sessionUser = getSessionUser(session);
        if (sessionUser == null) {
            return "redirect:/login";
        }
        if (sessionUser.isAdmin()) {
            throw new AccessDeniedException("Administrators are not permitted to access quizzes.");
        }

        boolean isFaculty = sessionUser.isFaculty();
        boolean isStudent = sessionUser.isStudent();

        if (!isFaculty && !isStudent) {
            throw new AccessDeniedException("You do not have permission to access quizzes.");
        }

        model.addAttribute("canManage", isFaculty);
        model.addAttribute("isStudent", isStudent);

        if (isFaculty) {
            List<Quiz> quizzes = quizService.listQuizzesByCreator(sessionUser.getUserId());
            List<Course> courses = facultyDepartmentService.listCourses();
            model.addAttribute("quizzes", quizzes);
            model.addAttribute("courses", courses);
        } else {
            // Student view
            Student student = studentRepository.findByUserId(sessionUser.getUserId());
            List<Quiz> quizzes = quizService.listPublishedQuizzesForStudent(student != null ? student.getId() : null);
            Map<Long, QuizAttempt> studentAttempts = new HashMap<>();
            Map<Long, QuizExtensionRequest> latestRequests = new HashMap<>();

            if (student != null) {
                // Mark extension update notifications as seen when student opens quizzes page
                quizService.markExtensionUpdatesAsSeenForStudent(student.getId());

                for (Quiz q : quizzes) {
                    QuizAttempt attempt = quizService.getStudentAttempt(q.getId(), student.getId());
                    if (attempt != null) {
                        studentAttempts.put(q.getId(), attempt);
                    }
                    QuizExtensionRequest req = quizService.getStudentLatestExtensionRequest(q.getId(), student.getId());
                    if (req != null) {
                        latestRequests.put(q.getId(), req);
                    }
                }
            }

            model.addAttribute("quizzes", quizzes);
            model.addAttribute("attempts", studentAttempts);
            model.addAttribute("extensionRequests", latestRequests);
            model.addAttribute("student", student);
        }

        return "quizzes/index";
    }

    @RequiresPermission("QUIZ_MANAGE")
    @PostMapping("/quizzes/create")
    public String createQuiz(@RequestParam Long courseId,
                             @RequestParam String title,
                             @RequestParam(defaultValue = "15") int durationMinutes,
                             @RequestParam String startTime,
                             @RequestParam String endTime,
                             HttpSession session,
                             RedirectAttributes redirectAttrs) {
        SessionUser sessionUser = getSessionUser(session);
        if (sessionUser == null) {
            return "redirect:/login";
        }
        if (sessionUser.isAdmin()) {
            throw new AccessDeniedException("Administrators are not permitted to create quizzes.");
        }
        if (!sessionUser.isFaculty()) {
            throw new AccessDeniedException("Only faculty members can create quizzes.");
        }
        try {
            LocalDateTime start = parseDateTime(startTime, LocalDateTime.now());
            LocalDateTime end = parseDateTime(endTime, LocalDateTime.now().plusDays(7));
            quizService.createQuiz(courseId, sessionUser.getUserId(), title, durationMinutes, start, end);
            redirectAttrs.addFlashAttribute("success", "Quiz created successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to create quiz: " + e.getMessage());
        }
        return "redirect:/quizzes";
    }

    @RequiresPermission("QUIZ_MANAGE")
    @GetMapping("/quizzes/{id}/edit")
    public String showEditQuiz(@PathVariable Long id, Model model, HttpSession session) {
        SessionUser sessionUser = getSessionUser(session);
        Quiz quiz = checkFacultyQuizOwnership(id, sessionUser);
        boolean hasAttempts = quizService.hasAttempts(id);
        List<Question> questions = quizService.listQuestions(id);
        List<Course> courses = facultyDepartmentService.listCourses();

        model.addAttribute("quiz", quiz);
        model.addAttribute("hasAttempts", hasAttempts);
        model.addAttribute("questions", questions);
        model.addAttribute("courses", courses);
        return "quizzes/edit";
    }

    @RequiresPermission("QUIZ_MANAGE")
    @PostMapping("/quizzes/{id}/edit")
    public String editQuiz(@PathVariable Long id,
                           @RequestParam(required = false) Long courseId,
                           @RequestParam String title,
                           @RequestParam(defaultValue = "15") int durationMinutes,
                           @RequestParam String startTime,
                           @RequestParam String endTime,
                           @RequestParam(required = false, defaultValue = "false") boolean isPublished,
                           HttpSession session,
                           RedirectAttributes redirectAttrs) {
        SessionUser sessionUser = getSessionUser(session);
        Quiz quiz = checkFacultyQuizOwnership(id, sessionUser);
        try {
            LocalDateTime start = parseDateTime(startTime, quiz.getStartTime());
            LocalDateTime end = parseDateTime(endTime, quiz.getEndTime());
            quizService.updateQuiz(id, courseId != null ? courseId : quiz.getCourseId(),
                    title, durationMinutes, start, end, isPublished, sessionUser.getUserId());
            redirectAttrs.addFlashAttribute("success", "Quiz updated successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to update quiz: " + e.getMessage());
        }
        return "redirect:/quizzes/" + id + "/edit";
    }

    @RequiresPermission("QUIZ_MANAGE")
    @GetMapping("/quizzes/{id}/questions")
    public String manageQuestions(@PathVariable Long id, Model model, HttpSession session) {
        SessionUser sessionUser = getSessionUser(session);
        Quiz quiz = checkFacultyQuizOwnership(id, sessionUser);
        boolean hasAttempts = quizService.hasAttempts(id);
        List<Question> questions = quizService.listQuestions(id);
        model.addAttribute("quiz", quiz);
        model.addAttribute("hasAttempts", hasAttempts);
        model.addAttribute("questions", questions);
        return "quizzes/questions";
    }

    @RequiresPermission("QUIZ_MANAGE")
    @PostMapping("/quizzes/{id}/questions/add")
    public String addQuestion(@PathVariable Long id,
                              @RequestParam String questionText,
                              @RequestParam String optionA,
                              @RequestParam String optionB,
                              @RequestParam String optionC,
                              @RequestParam String optionD,
                              @RequestParam String correctOption,
                              @RequestParam(defaultValue = "1") Integer marks,
                              @RequestParam(required = false, defaultValue = "edit") String source,
                              HttpSession session,
                              RedirectAttributes redirectAttrs) {
        SessionUser sessionUser = getSessionUser(session);
        checkFacultyQuizOwnership(id, sessionUser);
        try {
            quizService.addQuestion(id, questionText, optionA, optionB, optionC, optionD, correctOption, marks, sessionUser.getUserId());
            redirectAttrs.addFlashAttribute("success", "Question added successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to add question: " + e.getMessage());
        }
        return "questions".equalsIgnoreCase(source) ? "redirect:/quizzes/" + id + "/questions" : "redirect:/quizzes/" + id + "/edit";
    }

    @RequiresPermission("QUIZ_MANAGE")
    @PostMapping("/quizzes/{id}/questions/{qId}/edit")
    public String updateQuestion(@PathVariable Long id,
                                 @PathVariable Long qId,
                                 @RequestParam String questionText,
                                 @RequestParam String optionA,
                                 @RequestParam String optionB,
                                 @RequestParam String optionC,
                                 @RequestParam String optionD,
                                 @RequestParam String correctOption,
                                 @RequestParam(defaultValue = "1") Integer marks,
                                 @RequestParam(required = false, defaultValue = "edit") String source,
                                 HttpSession session,
                                 RedirectAttributes redirectAttrs) {
        SessionUser sessionUser = getSessionUser(session);
        checkFacultyQuizOwnership(id, sessionUser);
        try {
            quizService.updateQuestion(qId, questionText, optionA, optionB, optionC, optionD, correctOption, marks, sessionUser.getUserId());
            redirectAttrs.addFlashAttribute("success", "Question updated successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to update question: " + e.getMessage());
        }
        return "questions".equalsIgnoreCase(source) ? "redirect:/quizzes/" + id + "/questions" : "redirect:/quizzes/" + id + "/edit";
    }

    @RequiresPermission("QUIZ_MANAGE")
    @PostMapping("/quizzes/{id}/questions/{qId}/delete")
    public String deleteQuestion(@PathVariable Long id,
                                 @PathVariable Long qId,
                                 @RequestParam(required = false, defaultValue = "edit") String source,
                                 HttpSession session,
                                 RedirectAttributes redirectAttrs) {
        SessionUser sessionUser = getSessionUser(session);
        checkFacultyQuizOwnership(id, sessionUser);
        try {
            quizService.deleteQuestion(qId, sessionUser.getUserId());
            redirectAttrs.addFlashAttribute("success", "Question removed successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to remove question: " + e.getMessage());
        }
        return "questions".equalsIgnoreCase(source) ? "redirect:/quizzes/" + id + "/questions" : "redirect:/quizzes/" + id + "/edit";
    }

    @RequiresPermission("QUIZ_MANAGE")
    @PostMapping("/quizzes/{id}/publish")
    public String togglePublish(@PathVariable Long id,
                                @RequestParam boolean published,
                                HttpSession session,
                                RedirectAttributes redirectAttrs) {
        SessionUser sessionUser = getSessionUser(session);
        checkFacultyQuizOwnership(id, sessionUser);
        try {
            quizService.setPublished(id, published, sessionUser.getUserId());
            redirectAttrs.addFlashAttribute("success", published ? "Quiz published successfully." : "Quiz unpublished.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to update quiz status: " + e.getMessage());
        }
        return "redirect:/quizzes";
    }

    @RequiresPermission("QUIZ_MANAGE")
    @PostMapping("/quizzes/{id}/delete")
    public String deleteQuiz(@PathVariable Long id,
                             HttpSession session,
                             RedirectAttributes redirectAttrs) {
        SessionUser sessionUser = getSessionUser(session);
        checkFacultyQuizOwnership(id, sessionUser);
        try {
            quizService.deleteQuiz(id, sessionUser.getUserId());
            redirectAttrs.addFlashAttribute("success", "Quiz deleted successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to delete quiz: " + e.getMessage());
        }
        return "redirect:/quizzes";
    }

    @RequiresPermission("QUIZ_MANAGE")
    @GetMapping("/quizzes/{id}/results")
    public String viewResults(@PathVariable Long id, Model model, HttpSession session) {
        SessionUser sessionUser = getSessionUser(session);
        Quiz quiz = checkFacultyQuizOwnership(id, sessionUser);
        List<QuizAttempt> attempts = quizService.listQuizResults(id);
        List<QuizExtensionRequest> extensionRequests = quizService.listExtensionRequestsForQuiz(id);

        model.addAttribute("quiz", quiz);
        model.addAttribute("attempts", attempts);
        model.addAttribute("extensionRequests", extensionRequests);
        return "quizzes/results";
    }

    @RequiresPermission("QUIZ_MANAGE")
    @PostMapping("/quizzes/extensions/{requestId}/approve")
    public String approveExtension(@PathVariable Long requestId, HttpSession session, RedirectAttributes redirectAttrs) {
        SessionUser sessionUser = getSessionUser(session);
        if (sessionUser == null || sessionUser.isAdmin() || !sessionUser.isFaculty()) {
            throw new AccessDeniedException("Only faculty members can review extension requests.");
        }
        QuizExtensionRequest req = quizService.getExtensionRequestById(requestId);
        if (req == null) {
            throw new ResourceNotFoundException("Extension request not found.");
        }
        checkFacultyQuizOwnership(req.getQuizId(), sessionUser);
        try {
            quizService.approveExtensionRequest(requestId, sessionUser.getUserId());
            redirectAttrs.addFlashAttribute("success", "Extension request approved (+" + req.getRequestedMinutes() + " mins).");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to approve request: " + e.getMessage());
        }
        return "redirect:/quizzes/" + req.getQuizId() + "/results";
    }

    @RequiresPermission("QUIZ_MANAGE")
    @PostMapping("/quizzes/extensions/{requestId}/reject")
    public String rejectExtension(@PathVariable Long requestId, HttpSession session, RedirectAttributes redirectAttrs) {
        SessionUser sessionUser = getSessionUser(session);
        if (sessionUser == null || sessionUser.isAdmin() || !sessionUser.isFaculty()) {
            throw new AccessDeniedException("Only faculty members can review extension requests.");
        }
        QuizExtensionRequest req = quizService.getExtensionRequestById(requestId);
        if (req == null) {
            throw new ResourceNotFoundException("Extension request not found.");
        }
        checkFacultyQuizOwnership(req.getQuizId(), sessionUser);
        try {
            quizService.rejectExtensionRequest(requestId, sessionUser.getUserId());
            redirectAttrs.addFlashAttribute("success", "Extension request rejected.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to reject request: " + e.getMessage());
        }
        return "redirect:/quizzes/" + req.getQuizId() + "/results";
    }

    // ==========================================
    // Student Actions
    // ==========================================

    @RequiresPermission("QUIZ_EXTENSION_REQUEST")
    @PostMapping("/quizzes/{id}/request-extension")
    public String requestExtension(@PathVariable Long id,
                                   @RequestParam String reason,
                                   @RequestParam int requestedMinutes,
                                   @RequestParam(required = false, defaultValue = "index") String source,
                                   HttpSession session,
                                   RedirectAttributes redirectAttrs) {
        SessionUser sessionUser = getSessionUser(session);
        if (sessionUser == null) return "redirect:/login";
        if (sessionUser.isAdmin()) {
            throw new AccessDeniedException("Administrators are not permitted to request extensions.");
        }
        if (!sessionUser.isStudent()) {
            throw new AccessDeniedException("Only students can request time extensions.");
        }
        Student student = studentRepository.findByUserId(sessionUser.getUserId());
        if (student == null) {
            redirectAttrs.addFlashAttribute("error", "No student profile found.");
            return "redirect:/quizzes";
        }
        try {
            quizService.createExtensionRequest(id, student.getId(), reason, requestedMinutes, sessionUser.getUserId());
            redirectAttrs.addFlashAttribute("success", "Time extension request of " + requestedMinutes + " minutes submitted.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        if ("take".equalsIgnoreCase(source)) {
            return "redirect:/quizzes/" + id + "/take";
        }
        return "redirect:/quizzes";
    }

    @RequiresPermission("QUIZ_ATTEMPT")
    @PostMapping("/quizzes/extensions/{requestId}/dismiss")
    public String dismissExtensionBanner(@PathVariable Long requestId, HttpSession session, HttpServletRequest request) {
        SessionUser sessionUser = getSessionUser(session);
        if (sessionUser != null && sessionUser.isStudent()) {
            Student student = studentRepository.findByUserId(sessionUser.getUserId());
            if (student != null) {
                quizService.markExtensionRequestSeen(requestId, student.getId());
            }
        }
        String referer = request.getHeader("Referer");
        return referer != null && !referer.isBlank() ? "redirect:" + referer : "redirect:/dashboard";
    }

    @RequiresPermission("QUIZ_ATTEMPT")
    @PostMapping("/quizzes/{id}/start")
    public String startAttempt(@PathVariable Long id,
                               HttpSession session,
                               RedirectAttributes redirectAttrs) {
        SessionUser sessionUser = getSessionUser(session);
        if (sessionUser == null) return "redirect:/login";
        if (sessionUser.isAdmin()) {
            throw new AccessDeniedException("Administrators are not permitted to attempt quizzes.");
        }
        if (!sessionUser.isStudent()) {
            throw new AccessDeniedException("Only students can attempt quizzes.");
        }
        Student student = studentRepository.findByUserId(sessionUser.getUserId());
        if (student == null) {
            redirectAttrs.addFlashAttribute("error", "No linked student profile found.");
            return "redirect:/quizzes";
        }

        try {
            quizService.startAttempt(id, student.getId(), sessionUser.getUserId());
            return "redirect:/quizzes/" + id + "/take";
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/quizzes";
        }
    }

    @RequiresPermission("QUIZ_ATTEMPT")
    @GetMapping("/quizzes/{id}/take")
    public String takeQuiz(@PathVariable Long id, Model model, HttpSession session, RedirectAttributes redirectAttrs) {
        SessionUser sessionUser = getSessionUser(session);
        if (sessionUser == null) return "redirect:/login";
        if (sessionUser.isAdmin()) {
            throw new AccessDeniedException("Administrators are not permitted to attempt quizzes.");
        }
        if (!sessionUser.isStudent()) {
            throw new AccessDeniedException("Only students can attempt quizzes.");
        }
        Student student = studentRepository.findByUserId(sessionUser.getUserId());
        if (student == null) {
            redirectAttrs.addFlashAttribute("error", "No linked student profile found.");
            return "redirect:/quizzes";
        }

        Quiz quiz = quizService.getQuizById(id);
        if (quiz == null) {
            redirectAttrs.addFlashAttribute("error", "Quiz not found.");
            return "redirect:/quizzes";
        }

        List<Question> questions = quizService.getQuestionsForStudentAttempt(id);
        if (questions == null || questions.isEmpty()) {
            redirectAttrs.addFlashAttribute("error", "This quiz has no questions available to attempt.");
            return "redirect:/quizzes";
        }

        QuizAttempt attempt = quizService.getStudentAttempt(id, student.getId());
        if (attempt == null) {
            try {
                attempt = quizService.startAttempt(id, student.getId(), sessionUser.getUserId());
            } catch (Exception e) {
                redirectAttrs.addFlashAttribute("error", e.getMessage());
                return "redirect:/quizzes";
            }
        }

        if (attempt.isSubmitted()) {
            return "redirect:/quizzes/" + id + "/review";
        }

        // Calculate remaining seconds incorporating extra_minutes
        long baseMinutes = quiz.getDurationMinutes() != null && quiz.getDurationMinutes() > 0 ? quiz.getDurationMinutes() : 15;
        long extraMinutes = attempt.getExtraMinutes() != null ? attempt.getExtraMinutes() : 0;
        long durationSeconds = (baseMinutes + extraMinutes) * 60L;
        long elapsedSeconds = Duration.between(attempt.getStartedAt(), LocalDateTime.now()).getSeconds();
        long remainingSeconds = Math.max(0, durationSeconds - elapsedSeconds);

        QuizExtensionRequest latestRequest = quizService.getStudentLatestExtensionRequest(id, student.getId());

        model.addAttribute("quiz", quiz);
        model.addAttribute("attempt", attempt);
        model.addAttribute("questions", questions);
        model.addAttribute("remainingSeconds", remainingSeconds);
        model.addAttribute("latestRequest", latestRequest);
        return "quizzes/take";
    }

    @RequiresPermission("QUIZ_ATTEMPT")
    @PostMapping("/quizzes/{id}/submit")
    public String submitQuiz(@PathVariable Long id,
                             HttpServletRequest request,
                             HttpSession session,
                             RedirectAttributes redirectAttrs) {
        SessionUser sessionUser = getSessionUser(session);
        if (sessionUser == null) return "redirect:/login";
        if (sessionUser.isAdmin()) {
            throw new AccessDeniedException("Administrators are not permitted to submit quizzes.");
        }
        if (!sessionUser.isStudent()) {
            throw new AccessDeniedException("Only students can submit quizzes.");
        }
        Student student = studentRepository.findByUserId(sessionUser.getUserId());
        if (student == null) {
            redirectAttrs.addFlashAttribute("error", "No linked student profile found.");
            return "redirect:/quizzes";
        }

        try {
            Map<Long, String> selectedAnswers = new HashMap<>();
            request.getParameterMap().forEach((key, values) -> {
                if (key.startsWith("answer_") && values != null && values.length > 0) {
                    try {
                        Long qId = Long.parseLong(key.substring("answer_".length()));
                        selectedAnswers.put(qId, values[0]);
                    } catch (NumberFormatException ignored) {
                    }
                }
            });

            quizService.submitAttempt(id, student.getId(), selectedAnswers, sessionUser.getUserId());
            redirectAttrs.addFlashAttribute("success", "Quiz submitted and auto-graded successfully!");
            return "redirect:/quizzes/" + id + "/review";
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to submit quiz: " + e.getMessage());
            return "redirect:/quizzes";
        }
    }

    @RequiresPermission("QUIZ_ATTEMPT")
    @GetMapping("/quizzes/{id}/review")
    public String reviewQuiz(@PathVariable Long id, Model model, HttpSession session, RedirectAttributes redirectAttrs) {
        SessionUser sessionUser = getSessionUser(session);
        if (sessionUser == null) return "redirect:/login";
        if (sessionUser.isAdmin()) {
            throw new AccessDeniedException("Administrators are not permitted to review quiz attempts.");
        }
        if (!sessionUser.isStudent()) {
            throw new AccessDeniedException("Only students can review their quiz attempts.");
        }
        Student student = studentRepository.findByUserId(sessionUser.getUserId());
        if (student == null) {
            redirectAttrs.addFlashAttribute("error", "No linked student profile found.");
            return "redirect:/quizzes";
        }

        Quiz quiz = quizService.getQuizById(id);
        QuizAttempt attempt = quizService.getStudentAttempt(id, student.getId());
        if (attempt == null || !attempt.isSubmitted()) {
            redirectAttrs.addFlashAttribute("error", "You have not submitted this quiz yet.");
            return "redirect:/quizzes";
        }

        List<QuizReviewDto> reviews = quizService.getAttemptReview(id, student.getId());
        model.addAttribute("quiz", quiz);
        model.addAttribute("attempt", attempt);
        model.addAttribute("reviews", reviews);
        return "quizzes/review";
    }
}
