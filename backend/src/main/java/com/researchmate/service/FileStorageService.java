package com.researchmate.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private final Path uploadDirectory;

    public FileStorageService(
            @Value("${researchmate.storage.upload-dir}")
            String uploadDirectory) {

        this.uploadDirectory = Paths.get(uploadDirectory)
                .toAbsolutePath()
                .normalize();
    }

    public String storePdf(MultipartFile file) {

        try {
            Files.createDirectories(uploadDirectory);

            String originalFileName = file.getOriginalFilename();

            if (originalFileName == null ||
                    originalFileName.isBlank()) {
                throw new IllegalArgumentException(
                        "Invalid file name");
            }

            String storedFileName =
                    UUID.randomUUID() + ".pdf";

            Path targetPath =
                    uploadDirectory.resolve(storedFileName)
                            .normalize();

            if (!targetPath.startsWith(uploadDirectory)) {
                throw new IllegalArgumentException(
                        "Invalid file path");
            }

            try (InputStream inputStream = file.getInputStream()) {

                Files.copy(
                        inputStream,
                        targetPath,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            return storedFileName;

        } catch (IOException ex) {

            throw new RuntimeException(
                    "Failed to store PDF", ex);
        }
    }
}