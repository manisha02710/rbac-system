package com.rbac.service;

import com.rbac.exception.AccessDeniedException;
import com.rbac.exception.InvalidFileException;
import com.rbac.exception.ResourceNotFoundException;
import com.rbac.model.AuditLog;
import com.rbac.model.Material;
import com.rbac.repository.MaterialRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service managing course study materials, validation, and storage.
 * // OOP: Encapsulation – business rules isolated in service layer.
 */
@Service
public class MaterialService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "pdf", "doc", "docx", "ppt", "pptx", "zip", "jpg", "jpeg", "png", "gif"
    );

    private final MaterialRepository materialRepository;
    private final AuditService auditService;
    private final Path materialUploadPath;

    public MaterialService(MaterialRepository materialRepository,
                           AuditService auditService,
                           @Value("${app.upload.dir:uploads}") String uploadDir) {
        this.materialRepository = materialRepository;
        this.auditService = auditService;
        this.materialUploadPath = Paths.get(uploadDir, "materials").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.materialUploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize material upload directory: " + e.getMessage(), e);
        }
    }

    public List<Material> listMaterials(Long courseId, String keyword) {
        if ((courseId != null && courseId > 0) || (keyword != null && !keyword.trim().isEmpty())) {
            return materialRepository.search(keyword, courseId);
        }
        return materialRepository.findAll();
    }

    public Material getMaterialById(Long id) {
        Material m = materialRepository.findById(id);
        if (m == null) {
            throw new ResourceNotFoundException("Study material with ID " + id + " not found.");
        }
        return m;
    }

    @Transactional
    public Material uploadMaterial(Long courseId, Long uploadedBy, String title, String description, MultipartFile file) {
        validateFile(file);

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "material");
        String extension = getFileExtension(originalFilename);
        String storedFilename = UUID.randomUUID().toString() + "." + extension;
        Path targetPath = materialUploadPath.resolve(storedFilename);

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store uploaded material file: " + e.getMessage(), e);
        }

        Material m = new Material();
        m.setCourseId(courseId);
        m.setUploadedBy(uploadedBy);
        m.setTitle(title.trim());
        m.setDescription(description != null ? description.trim() : null);
        m.setFileName(originalFilename);
        m.setFilePath(targetPath.toString());

        Long id = materialRepository.save(m);
        m.setId(id);

        auditService.log(uploadedBy, "MATERIAL_UPLOAD", "Uploaded material '" + title + "' (ID: " + id + ")", AuditLog.Status.SUCCESS);
        return m;
    }

    public Resource loadFileAsResource(Long materialId) {
        Material m = getMaterialById(materialId);
        try {
            Path filePath = Paths.get(m.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found on disk: " + m.getFileName());
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File path invalid: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteMaterial(Long materialId, Long currentUserId, boolean isAdmin) {
        Material m = getMaterialById(materialId);
        if (!isAdmin && !m.getUploadedBy().equals(currentUserId)) {
            auditService.log(currentUserId, "MATERIAL_DELETE", "Unauthorized delete attempt on material " + materialId, AuditLog.Status.DENIED);
            throw new AccessDeniedException("You can only delete materials that you uploaded.");
        }

        // Delete physical file
        try {
            Path filePath = Paths.get(m.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
        }

        materialRepository.deleteById(materialId);
        auditService.log(currentUserId, "MATERIAL_DELETE", "Deleted study material '" + m.getTitle() + "' (ID: " + materialId + ")", AuditLog.Status.SUCCESS);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Please select a file to upload.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException("File size exceeds 10 MB limit.");
        }
        String originalFilename = file.getOriginalFilename();
        String ext = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new InvalidFileException("Unsupported file type ." + ext + ". Allowed types: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
