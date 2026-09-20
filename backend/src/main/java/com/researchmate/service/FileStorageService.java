package com.researchmate.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private final Path uploadDirectory;

    public FileStorageService(
            @Value("${researchmate.storage.upload-dir:uploads/papers}")
            String uploadDirectory) {

        this.uploadDirectory = Paths.get(uploadDirectory)
                .toAbsolutePath()
                .normalize();
    }

    public String storePdf(MultipartFile file) {
        try {
            Files.createDirectories(uploadDirectory);

            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || originalFileName.isBlank()) {
                throw new IllegalArgumentException("Invalid file name");
            }

            String storedFileName = UUID.randomUUID() + ".pdf";
            Path targetPath = uploadDirectory.resolve(storedFileName).normalize();

            // Strict path traversal defense
            if (!targetPath.startsWith(uploadDirectory)) {
                throw new IllegalArgumentException("Invalid file path / path traversal detected");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(
                        inputStream,
                        targetPath,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            log.info("Securely stored PDF file: {} (original: {})", storedFileName, originalFileName);
            return storedFileName;

        } catch (IOException ex) {
            throw new RuntimeException("Failed to store PDF file", ex);
        }
    }

    public Path getFilePath(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) {
            throw new IllegalArgumentException("Stored file name cannot be blank");
        }

        // Strip any path traversal sequences
        String sanitized = Paths.get(storedFileName).getFileName().toString();
        Path targetPath = uploadDirectory.resolve(sanitized).normalize();

        if (!targetPath.startsWith(uploadDirectory)) {
            throw new IllegalArgumentException("Path traversal attempt detected: " + storedFileName);
        }

        return targetPath;
    }

    public void deletePdf(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) {
            return;
        }
        try {
            Path targetPath = getFilePath(storedFileName);
            Files.deleteIfExists(targetPath);
            log.info("Deleted PDF file: {}", storedFileName);
        } catch (Exception ex) {
            log.warn("Could not delete stored file {}: {}", storedFileName, ex.getMessage());
        }
    }
}