package com.researchmate.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileValidationService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    public void validatePdf(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "PDF file is required");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "PDF file must not exceed 10 MB");
        }

        String contentType = file.getContentType();

        if (!"application/pdf".equalsIgnoreCase(contentType)) {
            throw new IllegalArgumentException(
                    "Only PDF files are allowed");
        }

        String filename = file.getOriginalFilename();

        if (filename == null ||
                !filename.toLowerCase().endsWith(".pdf")) {

            throw new IllegalArgumentException(
                    "File must have a .pdf extension");
        }
    }
}