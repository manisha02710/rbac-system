package com.rbac.config;

import com.rbac.model.AdminUser;
import com.rbac.model.BaseUser;
import com.rbac.model.FacultyUser;
import com.rbac.model.SessionUser;
import com.rbac.model.Student;
import com.rbac.model.StudentUser;
import com.rbac.repository.StudentRepository;
import com.rbac.service.QuizService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global model advice that exposes the SessionUser and polymorphic BaseUser profile
 * stored in the HttpSession to all Thymeleaf views.
 * Also provides the current request path for active link highlighting and
 * the notification badge count for unattempted available quizzes for students.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final QuizService quizService;
    private final StudentRepository studentRepository;

    public GlobalModelAdvice(QuizService quizService, StudentRepository studentRepository) {
        this.quizService = quizService;
        this.studentRepository = studentRepository;
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("sessionUser")
    public SessionUser sessionUser(HttpSession session) {
        return (SessionUser) session.getAttribute("sessionUser");
    }

    /**
     * Notification Badge count for unattempted, currently available quizzes.
     * Computed via QuizService; visible to STUDENT only (QUIZ_ATTEMPT permission).
     * Faculty and admin see no badge.
     */
    @ModelAttribute("unattemptedQuizCount")
    public int unattemptedQuizCount(HttpSession session) {
        SessionUser su = (SessionUser) session.getAttribute("sessionUser");
        if (su != null && su.isStudent() && su.getPermissions().contains("QUIZ_ATTEMPT")) {
            Student student = studentRepository.findByUserId(su.getUserId());
            if (student != null) {
                return quizService.countUnattemptedAvailableQuizzesForStudent(student.getId());
            }
        }
        return 0;
    }

    /**
     * OOP Concept: Polymorphism & Abstraction
     * Returns an instance of the abstract {@link BaseUser} class. At runtime, the
     * concrete subtype is {@link AdminUser}, {@link FacultyUser}, or {@link StudentUser}.
     * Templates can invoke polymorphic methods like activeUser.getRoleLabel().
     */
    @ModelAttribute("activeUser")
    public BaseUser activeUser(HttpSession session) {
        SessionUser su = (SessionUser) session.getAttribute("sessionUser");
        if (su == null) {
            return null;
        }
        if (su.isAdmin()) {
            return new AdminUser(su.getUserId(), su.getUsername(), su.getFullName(), "");
        } else if (su.isFaculty()) {
            return new FacultyUser(su.getUserId(), su.getUsername(), su.getFullName(), "");
        } else if (su.isStudent()) {
            return new StudentUser(su.getUserId(), su.getUsername(), su.getFullName(), "", "", "");
        }
        return new AdminUser(su.getUserId(), su.getUsername(), su.getFullName(), "");
    }
}
