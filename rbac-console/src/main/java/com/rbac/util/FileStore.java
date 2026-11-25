package com.rbac.util;

import com.rbac.exception.ValidationException;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

/** Copies files into the application's uploads folder and back out again (the console version of upload/download). */
public final class FileStore {
    public static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED = Set.of("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "zip", "jpg", "jpeg", "png");
    private static final Path ROOT = Paths.get("uploads");
    private FileStore() { }

    /** Removes the quotes Windows adds when you drag a file into the terminal. */
    public static Path clean(String typedPath) {
        String p = typedPath.trim();
        if (p.length() >= 2 && p.startsWith("\"") && p.endsWith("\"")) p = p.substring(1, p.length() - 1);
        return Paths.get(p);
    }

    public static String extension(String fileName) {
        int i = fileName.lastIndexOf('.');
        return i < 0 ? "" : fileName.substring(i + 1).toLowerCase();
    }

    /** Validates (exists, allowed type, max 10 MB) and copies the file. Returns {originalName, storedRelativePath}. */
    public static String[] store(String typedPath, String subFolder, String prefix) {
        Path src = clean(typedPath);
        if (!Files.isRegularFile(src)) throw new ValidationException("File not found: " + src);
        String name = src.getFileName().toString();
        if (!ALLOWED.contains(extension(name)))
            throw new ValidationException("File type not allowed. Allowed: " + new java.util.TreeSet<>(ALLOWED));
        try {
            if (Files.size(src) > MAX_BYTES) throw new ValidationException("File is larger than 10 MB.");
            Path dir = ROOT.resolve(subFolder);
            Files.createDirectories(dir);
            String stored = prefix + UUID.randomUUID() + "." + extension(name);      // generated name, original is kept in the DB
            Files.copy(src, dir.resolve(stored));
            return new String[]{name, ROOT.resolve(subFolder).resolve(stored).toString().replace('\\', '/')};
        } catch (IOException e) {
            throw new ValidationException("Could not copy the file: " + e.getMessage());
        }
    }

    public static void delete(String storedPath) {
        try { Files.deleteIfExists(Paths.get(storedPath)); } catch (IOException ignored) { }
    }

    /** Copies a stored file into the downloads folder under its original name. Returns where it went. */
    public static Path copyOut(String storedPath, String originalName) {
        try {
            Path src = Paths.get(storedPath);
            if (!Files.isRegularFile(src)) throw new ValidationException("The stored file is missing on disk.");
            Path dir = Paths.get("downloads");
            Files.createDirectories(dir);
            Path dest = dir.resolve(originalName);
            int n = 1;
            while (Files.exists(dest)) dest = dir.resolve("(" + (n++) + ") " + originalName);
            Files.copy(src, dest);
            return dest.toAbsolutePath();
        } catch (IOException e) {
            throw new ValidationException("Could not save the file: " + e.getMessage());
        }
    }
}
