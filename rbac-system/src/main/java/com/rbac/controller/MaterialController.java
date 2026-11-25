package com.rbac.controller;

import com.rbac.annotation.RequiresPermission;
import com.rbac.model.Course;
import com.rbac.model.Material;
import com.rbac.model.SessionUser;
import com.rbac.service.FacultyDepartmentService;
import com.rbac.service.MaterialService;
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

import java.util.List;

/**
 * Controller for Course Study Materials (upload, browse, search, and download).
 */
@Controller
public class MaterialController {

    private final MaterialService materialService;
    private final FacultyDepartmentService facultyDepartmentService;

    public MaterialController(MaterialService materialService,
                              FacultyDepartmentService facultyDepartmentService) {
        this.materialService = materialService;
        this.facultyDepartmentService = facultyDepartmentService;
    }

    private SessionUser getSessionUser(HttpSession session) {
        return (SessionUser) session.getAttribute("sessionUser");
    }

    @RequiresPermission("MATERIAL_VIEW")
    @GetMapping("/materials")
    public String listMaterials(Model model,
                                @RequestParam(required = false) Long courseId,
                                @RequestParam(required = false) String keyword,
                                HttpSession session) {
        SessionUser sessionUser = getSessionUser(session);
        List<Material> materials = materialService.listMaterials(courseId, keyword);
        List<Course> courses = facultyDepartmentService.listCourses();

        model.addAttribute("materials", materials);
        model.addAttribute("courses", courses);
        model.addAttribute("selectedCourseId", courseId);
        model.addAttribute("searchKeyword", keyword);
        model.addAttribute("canManage", sessionUser != null && sessionUser.getPermissions().contains("MATERIAL_MANAGE"));
        return "materials/index";
    }

    @RequiresPermission("MATERIAL_MANAGE")
    @PostMapping("/materials/upload")
    public String uploadMaterial(@RequestParam Long courseId,
                                 @RequestParam String title,
                                 @RequestParam(required = false) String description,
                                 @RequestParam("file") MultipartFile file,
                                 HttpSession session,
                                 RedirectAttributes redirectAttrs) {
        SessionUser sessionUser = getSessionUser(session);
        try {
            materialService.uploadMaterial(courseId, sessionUser.getUserId(), title, description, file);
            redirectAttrs.addFlashAttribute("success", "Study material uploaded successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Failed to upload material: " + e.getMessage());
        }
        return "redirect:/materials";
    }

    @RequiresPermission("MATERIAL_VIEW")
    @GetMapping("/materials/{id}/download")
    public ResponseEntity<Resource> downloadMaterial(@PathVariable Long id) {
        Material material = materialService.getMaterialById(id);
        Resource resource = materialService.loadFileAsResource(id);

        String filename = material.getFileName() != null ? material.getFileName() : "material";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename.replace("\"", "") + "\"")
                .body(resource);
    }

    @RequiresPermission("MATERIAL_MANAGE")
    @PostMapping("/materials/{id}/delete")
    public String deleteMaterial(@PathVariable Long id,
                                 HttpSession session,
                                 RedirectAttributes redirectAttrs) {
        SessionUser sessionUser = getSessionUser(session);
        try {
            boolean isAdmin = sessionUser != null && sessionUser.isAdmin();
            materialService.deleteMaterial(id, sessionUser.getUserId(), isAdmin);
            redirectAttrs.addFlashAttribute("success", "Study material deleted successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/materials";
    }
}
