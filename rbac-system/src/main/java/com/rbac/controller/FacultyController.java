package com.rbac.controller;

import com.rbac.annotation.RequiresPermission;
import com.rbac.exception.DuplicateResourceException;
import com.rbac.model.FacultyProfile;
import com.rbac.model.Student;
import com.rbac.model.Mark;
import com.rbac.model.MarkViewDto;
import com.rbac.model.Attendance;
import com.rbac.model.AttendanceViewDto;
import com.rbac.service.FacultyService;
import com.rbac.service.AuthorizationService;
import com.rbac.service.UserService;
import com.rbac.model.SessionUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for faculty actions.
 * // OOP: Encapsulation – thin controller delegates to FacultyService.
 */
@Controller
public class FacultyController {

    private final FacultyService facultyService;
    private final AuthorizationService authorizationService;
    private final UserService userService;

    public FacultyController(FacultyService facultyService,
                             AuthorizationService authorizationService,
                             UserService userService) {
        this.facultyService = facultyService;
        this.authorizationService = authorizationService;
        this.userService = userService;
    }

    private SessionUser getSessionUser(HttpSession session) {
        return (SessionUser) session.getAttribute("sessionUser");
    }

    @GetMapping("/faculty/profile")
    public String viewFacultyProfile(Model model, HttpSession session) {
        SessionUser su = getSessionUser(session);
        if (su == null || !su.isFaculty()) {
            return "redirect:/dashboard";
        }
        FacultyProfile profile = userService.getFacultyProfileByUserId(su.getUserId());
        model.addAttribute("profile", profile);
        return "faculty/profile";
    }

    @RequiresPermission("STUDENT_READ")
    @GetMapping("/faculty/students")
    public String viewStudents(Model model, HttpSession session) {
        SessionUser su = getSessionUser(session);
        List<Student> students = facultyService.listAllStudents();
        model.addAttribute("students", students);
        return "faculty/students";
    }

    @RequiresPermission("STUDENT_CREATE")
    @GetMapping("/faculty/students/new")
    public String newStudentForm(Model model) {
        return "faculty/newStudent";
    }

    @RequiresPermission("STUDENT_CREATE")
    @PostMapping("/faculty/students/new")
    public String createStudent(@RequestParam String fullName,
                                @RequestParam String username,
                                @RequestParam String email,
                                @RequestParam String password,
                                @RequestParam String rollNo,
                                @RequestParam String department,
                                Model model,
                                RedirectAttributes redirectAttrs,
                                HttpSession session) {
        try {
            SessionUser su = getSessionUser(session);
            Long currentUserId = su != null ? su.getUserId() : null;
            facultyService.createStudent(fullName, username, email, password, rollNo, department, currentUserId);
            redirectAttrs.addFlashAttribute("success", "Student registered successfully.");
            return "redirect:/faculty/students";
        } catch (DuplicateResourceException | IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("fullName", fullName);
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            model.addAttribute("rollNo", rollNo);
            model.addAttribute("department", department);
            return "faculty/newStudent";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to create student: " + e.getMessage());
            model.addAttribute("fullName", fullName);
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            model.addAttribute("rollNo", rollNo);
            model.addAttribute("department", department);
            return "faculty/newStudent";
        }
    }

    /**
     * All students' marks page (accessible without studentId).
     * Filterable by student name, roll number, or subject.
     */
    @RequiresPermission("MARKS_READ")
    @GetMapping("/faculty/marks")
    public String viewAllMarks(@RequestParam(value = "search", required = false) String search,
                               Model model) {
        List<MarkViewDto> marks = facultyService.getAllMarks(search);
        List<Student> students = facultyService.listAllStudents();
        model.addAttribute("marks", marks);
        model.addAttribute("students", students);
        model.addAttribute("search", search);
        return "faculty/marks";
    }

    /**
     * All students' attendance page (accessible without studentId).
     * Filterable by student name, roll number, or subject, plus low attendance filter.
     */
    @RequiresPermission("ATTENDANCE_VIEW")
    @GetMapping("/faculty/attendance")
    public String viewAllAttendance(@RequestParam(value = "search", required = false) String search,
                                    @RequestParam(value = "lowOnly", defaultValue = "false") boolean lowOnly,
                                    Model model) {
        List<AttendanceViewDto> attendanceList = facultyService.getAllAttendance(search, lowOnly);
        List<Student> students = facultyService.listAllStudents();
        model.addAttribute("attendanceList", attendanceList);
        model.addAttribute("students", students);
        model.addAttribute("search", search);
        model.addAttribute("lowOnly", lowOnly);
        return "faculty/attendance";
    }

    @RequiresPermission("MARKS_READ")
    @GetMapping("/faculty/student/marks")
    public String viewStudentMarks(@RequestParam Long studentId, Model model, HttpSession session) {
        SessionUser su = getSessionUser(session);
        List<Mark> marks = facultyService.getMarksForStudent(studentId);
        model.addAttribute("marks", marks);
        model.addAttribute("studentId", studentId);
        return "faculty/studentMarks";
    }

    @RequiresPermission("MARKS_ADD")
    @PostMapping("/faculty/student/marks/add")
    public String addMark(@RequestParam Long studentId,
                          @RequestParam String subject,
                          @RequestParam Integer marks,
                          @RequestParam(value = "returnUrl", required = false) String returnUrl,
                          RedirectAttributes redirectAttrs,
                          HttpSession session) {
        String defaultRedirect = "redirect:/faculty/student/marks?studentId=" + studentId;
        String targetRedirect = (returnUrl != null && !returnUrl.isBlank()) ? "redirect:" + returnUrl : defaultRedirect;

        if (marks == null || marks < 0 || marks > 100) {
            redirectAttrs.addFlashAttribute("error", "Marks must be between 0 and 100.");
            return targetRedirect;
        }
        SessionUser su = getSessionUser(session);
        facultyService.addMark(studentId, subject, marks, su.getUserId());
        redirectAttrs.addFlashAttribute("success", "Mark added successfully.");
        return targetRedirect;
    }

    @RequiresPermission("MARKS_ADD")
    @GetMapping("/faculty/student/marks/{id}/edit")
    public String editMarkForm(@PathVariable("id") Long id,
                               @RequestParam(value = "returnUrl", required = false) String returnUrl,
                               Model model,
                               RedirectAttributes redirectAttrs) {
        Mark mark = facultyService.getMarkById(id);
        if (mark == null) {
            redirectAttrs.addFlashAttribute("error", "Mark record not found.");
            return "redirect:/faculty/students";
        }
        model.addAttribute("mark", mark);
        model.addAttribute("returnUrl", returnUrl);
        return "faculty/editMark";
    }

    @RequiresPermission("MARKS_ADD")
    @PostMapping("/faculty/student/marks/{id}/edit")
    public String editMark(@PathVariable("id") Long id,
                           @RequestParam String subject,
                           @RequestParam Integer marks,
                           @RequestParam(value = "returnUrl", required = false) String returnUrl,
                           RedirectAttributes redirectAttrs,
                           HttpSession session) {
        Mark existing = facultyService.getMarkById(id);
        if (existing == null) {
            redirectAttrs.addFlashAttribute("error", "Mark record not found.");
            return "redirect:/faculty/students";
        }
        if (marks == null || marks < 0 || marks > 100) {
            redirectAttrs.addFlashAttribute("error", "Marks must be between 0 and 100.");
            return "redirect:/faculty/student/marks/" + id + "/edit" + (returnUrl != null ? "?returnUrl=" + returnUrl : "");
        }
        SessionUser su = getSessionUser(session);
        facultyService.updateMark(id, subject, marks, su.getUserId());
        redirectAttrs.addFlashAttribute("success", "Mark updated successfully.");
        if (returnUrl != null && !returnUrl.isBlank()) {
            return "redirect:" + returnUrl;
        }
        return "redirect:/faculty/student/marks?studentId=" + existing.getStudentId();
    }

    @RequiresPermission("MARKS_ADD")
    @PostMapping("/faculty/student/marks/{id}/delete")
    public String deleteMark(@PathVariable("id") Long id,
                             @RequestParam(value = "returnUrl", required = false) String returnUrl,
                             RedirectAttributes redirectAttrs,
                             HttpSession session) {
        Mark existing = facultyService.getMarkById(id);
        if (existing == null) {
            redirectAttrs.addFlashAttribute("error", "Mark record not found.");
            return "redirect:/faculty/students";
        }
        SessionUser su = getSessionUser(session);
        facultyService.deleteMark(id, su.getUserId());
        redirectAttrs.addFlashAttribute("success", "Mark deleted successfully.");
        if (returnUrl != null && !returnUrl.isBlank()) {
            return "redirect:" + returnUrl;
        }
        return "redirect:/faculty/student/marks?studentId=" + existing.getStudentId();
    }

    @RequiresPermission("ATTENDANCE_VIEW")
    @GetMapping("/faculty/student/attendance")
    public String viewAttendance(@RequestParam Long studentId, Model model, HttpSession session) {
        List<Attendance> attendance = facultyService.getAttendanceForStudent(studentId);
        model.addAttribute("attendance", attendance);
        model.addAttribute("studentId", studentId);
        return "faculty/studentAttendance";
    }

    @RequiresPermission("ATTENDANCE_MANAGE")
    @PostMapping("/faculty/student/attendance/save")
    public String saveAttendance(@RequestParam Long studentId,
                                 @RequestParam String subject,
                                 @RequestParam Integer totalClasses,
                                 @RequestParam Integer attendedClasses,
                                 @RequestParam(value = "returnUrl", required = false) String returnUrl,
                                 RedirectAttributes redirectAttrs,
                                 HttpSession session) {
        String defaultRedirect = "redirect:/faculty/student/attendance?studentId=" + studentId;
        String targetRedirect = (returnUrl != null && !returnUrl.isBlank()) ? "redirect:" + returnUrl : defaultRedirect;

        if (totalClasses == null || totalClasses <= 0) {
            redirectAttrs.addFlashAttribute("error", "Total classes must be greater than 0.");
            return targetRedirect;
        }
        if (attendedClasses == null || attendedClasses < 0 || attendedClasses > totalClasses) {
            redirectAttrs.addFlashAttribute("error", "Attended classes must be between 0 and total classes (" + totalClasses + ").");
            return targetRedirect;
        }
        SessionUser su = getSessionUser(session);
        facultyService.saveAttendance(studentId, subject, totalClasses, attendedClasses, su.getUserId());
        redirectAttrs.addFlashAttribute("success", "Attendance saved successfully.");
        return targetRedirect;
    }

    @RequiresPermission("ATTENDANCE_MANAGE")
    @GetMapping("/faculty/student/attendance/{id}/edit")
    public String editAttendanceForm(@PathVariable("id") Long id,
                                     @RequestParam(value = "returnUrl", required = false) String returnUrl,
                                     Model model,
                                     RedirectAttributes redirectAttrs) {
        Attendance attendance = facultyService.getAttendanceById(id);
        if (attendance == null) {
            redirectAttrs.addFlashAttribute("error", "Attendance record not found.");
            return "redirect:/faculty/students";
        }
        model.addAttribute("attendance", attendance);
        model.addAttribute("returnUrl", returnUrl);
        return "faculty/editAttendance";
    }

    @RequiresPermission("ATTENDANCE_MANAGE")
    @PostMapping("/faculty/student/attendance/{id}/edit")
    public String editAttendance(@PathVariable("id") Long id,
                                 @RequestParam String subject,
                                 @RequestParam Integer totalClasses,
                                 @RequestParam Integer attendedClasses,
                                 @RequestParam(value = "returnUrl", required = false) String returnUrl,
                                 RedirectAttributes redirectAttrs,
                                 HttpSession session) {
        Attendance existing = facultyService.getAttendanceById(id);
        if (existing == null) {
            redirectAttrs.addFlashAttribute("error", "Attendance record not found.");
            return "redirect:/faculty/students";
        }
        if (totalClasses == null || totalClasses <= 0) {
            redirectAttrs.addFlashAttribute("error", "Total classes must be greater than 0.");
            return "redirect:/faculty/student/attendance/" + id + "/edit" + (returnUrl != null ? "?returnUrl=" + returnUrl : "");
        }
        if (attendedClasses == null || attendedClasses < 0 || attendedClasses > totalClasses) {
            redirectAttrs.addFlashAttribute("error", "Attended classes must be between 0 and total classes (" + totalClasses + ").");
            return "redirect:/faculty/student/attendance/" + id + "/edit" + (returnUrl != null ? "?returnUrl=" + returnUrl : "");
        }
        SessionUser su = getSessionUser(session);
        facultyService.updateAttendance(id, subject, totalClasses, attendedClasses, su.getUserId());
        redirectAttrs.addFlashAttribute("success", "Attendance updated successfully.");
        if (returnUrl != null && !returnUrl.isBlank()) {
            return "redirect:" + returnUrl;
        }
        return "redirect:/faculty/student/attendance?studentId=" + existing.getStudentId();
    }

    @RequiresPermission("ATTENDANCE_MANAGE")
    @PostMapping("/faculty/student/attendance/{id}/delete")
    public String deleteAttendance(@PathVariable("id") Long id,
                                   @RequestParam(value = "returnUrl", required = false) String returnUrl,
                                   RedirectAttributes redirectAttrs,
                                   HttpSession session) {
        Attendance existing = facultyService.getAttendanceById(id);
        if (existing == null) {
            redirectAttrs.addFlashAttribute("error", "Attendance record not found.");
            return "redirect:/faculty/students";
        }
        SessionUser su = getSessionUser(session);
        facultyService.deleteAttendance(id, su.getUserId());
        redirectAttrs.addFlashAttribute("success", "Attendance record deleted successfully.");
        if (returnUrl != null && !returnUrl.isBlank()) {
            return "redirect:" + returnUrl;
        }
        return "redirect:/faculty/student/attendance?studentId=" + existing.getStudentId();
    }
}
