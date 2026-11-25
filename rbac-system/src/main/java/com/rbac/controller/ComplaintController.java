package com.rbac.controller;

import com.rbac.annotation.RequiresPermission;
import com.rbac.exception.ResourceNotFoundException;
import com.rbac.model.Complaint;
import com.rbac.model.SessionUser;
import com.rbac.service.ComplaintService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    private SessionUser getSessionUser(HttpSession session) {
        return (SessionUser) session.getAttribute("sessionUser");
    }

    // -------------------------------------------------------------
    // Student Endpoints
    // -------------------------------------------------------------

    @RequiresPermission("COMPLAINT_VIEW_OWN")
    @GetMapping("/student/complaints")
    public String viewStudentComplaints(Model model, HttpSession session) {
        SessionUser su = getSessionUser(session);
        List<Complaint> complaints = complaintService.getStudentComplaints(su.getUserId());
        model.addAttribute("complaints", complaints);
        return "student/complaints";
    }

    @RequiresPermission("COMPLAINT_CREATE")
    @GetMapping("/student/complaints/new")
    public String newComplaintForm() {
        return "student/newComplaint";
    }

    @RequiresPermission("COMPLAINT_CREATE")
    @PostMapping("/student/complaints/new")
    public String submitComplaint(@RequestParam String subject,
                                  @RequestParam String category,
                                  @RequestParam String description,
                                  RedirectAttributes redirectAttrs,
                                  HttpSession session) {
        try {
            SessionUser su = getSessionUser(session);
            complaintService.submitComplaint(su.getUserId(), subject, description, category);
            redirectAttrs.addFlashAttribute("success", "Your feedback / complaint has been submitted successfully.");
            return "redirect:/student/complaints";
        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/student/complaints/new";
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to submit complaint: " + e.getMessage());
            return "redirect:/student/complaints/new";
        }
    }

    // -------------------------------------------------------------
    // Admin Endpoints
    // -------------------------------------------------------------

    @RequiresPermission("COMPLAINT_MANAGE")
    @GetMapping("/admin/complaints")
    public String viewAllComplaints(@RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
                                    Model model) {
        List<Complaint> complaints = complaintService.getAllComplaints(status);
        model.addAttribute("complaints", complaints);
        model.addAttribute("currentStatus", status);
        return "admin/complaints";
    }

    @RequiresPermission("COMPLAINT_MANAGE")
    @PostMapping("/admin/complaints/{id}/respond")
    public String respondToComplaint(@PathVariable("id") Long id,
                                     @RequestParam String adminResponse,
                                     @RequestParam String status,
                                     RedirectAttributes redirectAttrs,
                                     HttpSession session) {
        try {
            SessionUser su = getSessionUser(session);
            complaintService.respondToComplaint(id, adminResponse, status, su.getUserId());
            redirectAttrs.addFlashAttribute("success", "Response saved and complaint status updated to " + status + ".");
        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to update complaint: " + e.getMessage());
        }
        return "redirect:/admin/complaints";
    }
}
