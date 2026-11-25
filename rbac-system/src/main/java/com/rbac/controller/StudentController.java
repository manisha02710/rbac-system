package com.rbac.controller;

import com.rbac.annotation.RequiresPermission;
import com.rbac.model.Student;
import com.rbac.model.Mark;
import com.rbac.model.Attendance;
import com.rbac.model.SessionUser;
import com.rbac.service.StudentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for student actions – only data that belongs to the logged‑in student.
 * // OOP: Encapsulation – thin controller forwards to StudentService.
 */
@Controller
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    private SessionUser getSessionUser(HttpSession session) {
        return (SessionUser) session.getAttribute("sessionUser");
    }

    /** View own profile (basic user info) */
    @RequiresPermission("OWN_PROFILE_VIEW")
    @GetMapping("/student/profile")
    public String viewProfile(Model model, HttpSession session, RedirectAttributes redirectAttrs) {
        SessionUser su = getSessionUser(session);
        Student student = studentService.findStudentByUserId(su.getUserId());
        if (student == null) {
            redirectAttrs.addFlashAttribute("error", "This page is for students only.");
            return "redirect:/dashboard";
        }
        model.addAttribute("student", student);
        return "student/profile";
    }

    /** View own marks */
    @RequiresPermission("OWN_MARKS_VIEW")
    @GetMapping("/student/marks")
    public String viewOwnMarks(Model model, HttpSession session, RedirectAttributes redirectAttrs) {
        SessionUser su = getSessionUser(session);
        Student student = studentService.findStudentByUserId(su.getUserId());
        if (student == null) {
            redirectAttrs.addFlashAttribute("error", "This page is for students only.");
            return "redirect:/dashboard";
        }
        model.addAttribute("marks", studentService.getOwnMarks(student.getId()));
        return "student/marks";
    }

    /** View own attendance */
    @RequiresPermission("OWN_ATTENDANCE_VIEW")
    @GetMapping("/student/attendance")
    public String viewOwnAttendance(Model model, HttpSession session, RedirectAttributes redirectAttrs) {
        SessionUser su = getSessionUser(session);
        Student student = studentService.findStudentByUserId(su.getUserId());
        if (student == null) {
            redirectAttrs.addFlashAttribute("error", "This page is for students only.");
            return "redirect:/dashboard";
        }
        java.util.List<Attendance> list = studentService.getOwnAttendance(student.getId());
        boolean hasLowAttendance = list != null && list.stream().anyMatch(Attendance::isLowAttendance);
        model.addAttribute("attendance", list);
        model.addAttribute("hasLowAttendance", hasLowAttendance);
        return "student/attendance";
    }
}
