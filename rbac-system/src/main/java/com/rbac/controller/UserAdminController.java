package com.rbac.controller;

import com.rbac.annotation.RequiresPermission;
import com.rbac.exception.DuplicateResourceException;
import com.rbac.exception.InvalidCredentialsException;
import com.rbac.exception.ResourceNotFoundException;
import com.rbac.model.Course;
import com.rbac.model.Department;
import com.rbac.model.FacultyProfile;
import com.rbac.model.SessionUser;
import com.rbac.model.User;
import com.rbac.service.FacultyDepartmentService;
import com.rbac.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

/**
 * Administrative UI for managing users, faculty, departments, and courses.
 * // OOP: Encapsulation – controller delegates to UserService and FacultyDepartmentService.
 */
@Controller
public class UserAdminController {

    private final UserService userService;
    private final FacultyDepartmentService facultyDepartmentService;

    public UserAdminController(UserService userService, FacultyDepartmentService facultyDepartmentService) {
        this.userService = userService;
        this.facultyDepartmentService = facultyDepartmentService;
    }

    @RequiresPermission("USER_READ")
    @GetMapping("/admin/users")
    public String listUsers(Model model) {
        List<User> users = userService.listUsers();
        model.addAttribute("users", users);
        model.addAttribute("roles", userService.listAllRoles());
        return "admin/users"; // Thymeleaf template
    }

    @RequiresPermission("USER_CREATE")
    @GetMapping("/admin/users/create")
    public String createUserForm() {
        return "admin/createUser";
    }

    @RequiresPermission("USER_CREATE")
    @PostMapping("/admin/users/create")
    public String createUser(
            @RequestParam @NotBlank @Size(min = 3, max = 20) String username,
            @RequestParam @NotBlank @Size(min = 6) String password,
            @RequestParam @NotBlank String fullName,
            @RequestParam @Email String email,
            @RequestParam(defaultValue = "true") boolean active,
            RedirectAttributes redirectAttrs) {
        try {
            userService.createUser(username, password, fullName, email, active);
            redirectAttrs.addFlashAttribute("success", "User created successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to create user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @RequiresPermission("USER_DISABLE")
    @PostMapping("/admin/users/setActive")
    public String setActive(@RequestParam Long userId,
                            @RequestParam boolean active,
                            RedirectAttributes redirectAttrs) {
        try {
            userService.setActive(userId, active);
            redirectAttrs.addFlashAttribute("success", "User " + (active ? "enabled" : "disabled") + ".");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to update status: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @RequiresPermission("ROLE_ASSIGN")
    @PostMapping("/admin/users/assignRole")
    public String assignRole(@RequestParam Long userId,
                              @RequestParam Long roleId,
                              RedirectAttributes redirectAttrs) {
        try {
            userService.assignRole(userId, roleId);
            redirectAttrs.addFlashAttribute("success", "Role assigned successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage() != null ? e.getMessage() : "Failed to assign role.");
        }
        return "redirect:/admin/users";
    }

    @RequiresPermission("ROLE_ASSIGN")
    @PostMapping("/admin/users/removeRole")
    public String removeRole(@RequestParam Long userId,
                              @RequestParam Long roleId,
                              RedirectAttributes redirectAttrs) {
        try {
            userService.removeRole(userId, roleId);
            redirectAttrs.addFlashAttribute("success", "Role removed successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage() != null ? e.getMessage() : "Failed to remove role.");
        }
        return "redirect:/admin/users";
    }

    @RequiresPermission("USER_UPDATE")
    @PostMapping(value = {"/admin/users/resetPassword", "/admin/users/{userId}/reset-password"})
    public String resetPassword(@RequestParam(name = "userId", required = false) Long paramUserId,
                                @PathVariable(name = "userId", required = false) Long pathUserId,
                                @RequestParam String newPassword,
                                HttpSession session,
                                RedirectAttributes redirectAttrs) {
        Long targetId = paramUserId != null ? paramUserId : pathUserId;
        try {
            SessionUser sessionUser = getSessionUser(session);
            Long currentUserId = sessionUser != null ? sessionUser.getUserId() : null;
            userService.resetPassword(targetId, newPassword, currentUserId);
            redirectAttrs.addFlashAttribute("success", "Password reset successfully.");
        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to reset password: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    private SessionUser getSessionUser(HttpSession session) {
        return (SessionUser) session.getAttribute("sessionUser");
    }

    @RequiresPermission("USER_READ")
    @GetMapping("/admin/faculty")
    public String listFaculty(Model model) {
        List<FacultyProfile> facultyProfiles = userService.listFacultyProfiles();
        List<Department> departments = facultyDepartmentService.listDepartments();
        List<Course> allCourses = facultyDepartmentService.listCourses();
        model.addAttribute("facultyProfiles", facultyProfiles);
        model.addAttribute("departments", departments);
        model.addAttribute("allCourses", allCourses);
        return "admin/faculty";
    }

    @RequiresPermission("USER_CREATE")
    @PostMapping("/admin/faculty/add")
    public String addFaculty(
            @RequestParam @NotBlank String fullName,
            @RequestParam @NotBlank @Size(min = 3, max = 50) String username,
            @RequestParam @NotBlank @Email String email,
            @RequestParam @NotBlank @Size(min = 6) String password,
            @RequestParam Long departmentId,
            @RequestParam(name = "courseIds", required = false) List<Long> courseIds,
            RedirectAttributes redirectAttrs,
            HttpSession session) {
        try {
            SessionUser su = getSessionUser(session);
            Long currentUserId = su != null ? su.getUserId() : null;
            userService.createFaculty(username, password, fullName, email, departmentId, courseIds, currentUserId);
            redirectAttrs.addFlashAttribute("success", "Faculty member added successfully with assigned department and courses.");
        } catch (DuplicateResourceException | IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to add faculty: " + e.getMessage());
        }
        return "redirect:/admin/faculty";
    }

    @RequiresPermission("USER_CREATE")
    @PostMapping("/admin/faculty/{id}/edit")
    public String editFaculty(
            @PathVariable("id") Long id,
            @RequestParam @NotBlank String fullName,
            @RequestParam @NotBlank @Email String email,
            @RequestParam Long departmentId,
            @RequestParam(name = "courseIds", required = false) List<Long> courseIds,
            RedirectAttributes redirectAttrs,
            HttpSession session) {
        try {
            SessionUser su = getSessionUser(session);
            Long currentUserId = su != null ? su.getUserId() : null;
            userService.updateFaculty(id, fullName, email, departmentId, courseIds, currentUserId);
            redirectAttrs.addFlashAttribute("success", "Faculty member updated successfully.");
        } catch (DuplicateResourceException | IllegalArgumentException | ResourceNotFoundException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to update faculty: " + e.getMessage());
        }
        return "redirect:/admin/faculty";
    }

    @RequiresPermission("USER_DISABLE")
    @PostMapping("/admin/faculty/{id}/delete")
    public String deleteFaculty(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttrs,
            HttpSession session) {
        try {
            SessionUser su = getSessionUser(session);
            Long currentUserId = su != null ? su.getUserId() : null;
            boolean deleted = userService.deleteFaculty(id, currentUserId);
            if (deleted) {
                redirectAttrs.addFlashAttribute("success", "Faculty member deleted successfully.");
            } else {
                redirectAttrs.addFlashAttribute("success", "Faculty account could not be deleted due to associated records. The account has been disabled instead.");
            }
        } catch (ResourceNotFoundException | IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to delete faculty member: " + e.getMessage());
        }
        return "redirect:/admin/faculty";
    }

    @RequiresPermission("USER_CREATE")
    @PostMapping("/admin/faculty/departments/add")
    public String addDepartment(
            @RequestParam String departmentName,
            RedirectAttributes redirectAttrs,
            HttpSession session) {
        try {
            SessionUser su = getSessionUser(session);
            Long currentUserId = su != null ? su.getUserId() : null;
            facultyDepartmentService.addDepartment(departmentName, currentUserId);
            redirectAttrs.addFlashAttribute("success", "Department '" + departmentName + "' added successfully.");
        } catch (DuplicateResourceException | IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to add department: " + e.getMessage());
        }
        return "redirect:/admin/faculty";
    }

    @RequiresPermission("USER_DISABLE")
    @PostMapping("/admin/faculty/departments/{id}/delete")
    public String deleteDepartment(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttrs,
            HttpSession session) {
        try {
            SessionUser su = getSessionUser(session);
            Long currentUserId = su != null ? su.getUserId() : null;
            facultyDepartmentService.deleteDepartment(id, currentUserId);
            redirectAttrs.addFlashAttribute("success", "Department deleted successfully.");
        } catch (IllegalStateException | ResourceNotFoundException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to delete department: " + e.getMessage());
        }
        return "redirect:/admin/faculty";
    }

    @RequiresPermission("USER_CREATE")
    @PostMapping("/admin/faculty/courses/add")
    public String addCourse(
            @RequestParam String courseName,
            @RequestParam String courseCode,
            @RequestParam Long departmentId,
            RedirectAttributes redirectAttrs,
            HttpSession session) {
        try {
            SessionUser su = getSessionUser(session);
            Long currentUserId = su != null ? su.getUserId() : null;
            facultyDepartmentService.addCourse(courseName, courseCode, departmentId, currentUserId);
            redirectAttrs.addFlashAttribute("success", "Course '" + courseCode + "' added successfully.");
        } catch (DuplicateResourceException | IllegalArgumentException | ResourceNotFoundException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to add course: " + e.getMessage());
        }
        return "redirect:/admin/faculty";
    }

    @RequiresPermission("USER_DISABLE")
    @PostMapping("/admin/faculty/courses/{id}/delete")
    public String deleteCourse(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttrs,
            HttpSession session) {
        try {
            SessionUser su = getSessionUser(session);
            Long currentUserId = su != null ? su.getUserId() : null;
            facultyDepartmentService.deleteCourse(id, currentUserId);
            redirectAttrs.addFlashAttribute("success", "Course deleted successfully.");
        } catch (IllegalStateException | ResourceNotFoundException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to delete course: " + e.getMessage());
        }
        return "redirect:/admin/faculty";
    }
}
