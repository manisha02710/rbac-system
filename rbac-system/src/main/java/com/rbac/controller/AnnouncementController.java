package com.rbac.controller;

import com.rbac.annotation.RequiresPermission;
import com.rbac.exception.ResourceNotFoundException;
import com.rbac.model.Announcement;
import com.rbac.model.SessionUser;
import com.rbac.service.AnnouncementService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    private SessionUser getSessionUser(HttpSession session) {
        return (SessionUser) session.getAttribute("sessionUser");
    }

    // -------------------------------------------------------------
    // General / Everyone (Admin, Faculty, Student)
    // -------------------------------------------------------------

    @RequiresPermission("ANNOUNCEMENT_VIEW")
    @GetMapping("/announcements")
    public String listAnnouncements(Model model) {
        List<Announcement> announcements = announcementService.getAllAnnouncements();
        model.addAttribute("announcements", announcements);
        return "announcements/index";
    }

    // -------------------------------------------------------------
    // Admin Management
    // -------------------------------------------------------------

    @RequiresPermission("ANNOUNCEMENT_MANAGE")
    @GetMapping("/admin/announcements")
    public String adminAnnouncements(Model model) {
        List<Announcement> announcements = announcementService.getAllAnnouncements();
        model.addAttribute("announcements", announcements);
        return "admin/announcements";
    }

    @RequiresPermission("ANNOUNCEMENT_MANAGE")
    @PostMapping("/admin/announcements/create")
    public String createAnnouncement(@RequestParam String title,
                                     @RequestParam String message,
                                     @RequestParam String category,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiresOn,
                                     RedirectAttributes redirectAttrs,
                                     HttpSession session) {
        try {
            SessionUser su = getSessionUser(session);
            announcementService.createAnnouncement(title, message, category, expiresOn, su.getUserId());
            redirectAttrs.addFlashAttribute("success", "Announcement published successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to create announcement: " + e.getMessage());
        }
        return "redirect:/admin/announcements";
    }

    @RequiresPermission("ANNOUNCEMENT_MANAGE")
    @PostMapping("/admin/announcements/{id}/edit")
    public String editAnnouncement(@PathVariable("id") Long id,
                                   @RequestParam String title,
                                   @RequestParam String message,
                                   @RequestParam String category,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiresOn,
                                   RedirectAttributes redirectAttrs,
                                   HttpSession session) {
        try {
            SessionUser su = getSessionUser(session);
            announcementService.updateAnnouncement(id, title, message, category, expiresOn, su.getUserId());
            redirectAttrs.addFlashAttribute("success", "Announcement updated successfully.");
        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to update announcement: " + e.getMessage());
        }
        return "redirect:/admin/announcements";
    }

    @RequiresPermission("ANNOUNCEMENT_MANAGE")
    @PostMapping("/admin/announcements/{id}/delete")
    public String deleteAnnouncement(@PathVariable("id") Long id,
                                     RedirectAttributes redirectAttrs,
                                     HttpSession session) {
        try {
            SessionUser su = getSessionUser(session);
            announcementService.deleteAnnouncement(id, su.getUserId());
            redirectAttrs.addFlashAttribute("success", "Announcement deleted successfully.");
        } catch (ResourceNotFoundException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to delete announcement: " + e.getMessage());
        }
        return "redirect:/admin/announcements";
    }
}
