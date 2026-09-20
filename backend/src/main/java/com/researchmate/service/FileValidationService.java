package com.researchmate.service;

import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.researchmate.exception.PdfProcessingException;

@Service
public class FileValidationService {

    private static final Logger log = LoggerFactory.getLogger(FileValidationService.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    public void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("PDF file is required and cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("PDF file must not exceed 10 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed (content-type must be application/pdf)");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("File must have a .pdf extension");
        }

        // Deep structural inspection with Apache PDFBox
        try {
            byte[] fileBytes = file.getBytes();
            if (fileBytes.length == 0) {
                throw new IllegalArgumentException("File byte stream is empty");
            }

            try (PDDocument document = Loader.loadPDF(fileBytes)) {
                if (document.isEncrypted()) {
                    throw new IllegalArgumentException("Encrypted or password-protected PDFs are not supported");
                }
                if (document.getNumberOfPages() <= 0) {
                    throw new IllegalArgumentException("PDF does not contain any readable pages");
                }
                log.info("PDF structural validation passed: {} pages found", document.getNumberOfPages());
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (IOException ex) {
            log.warn("PDFBox failed to parse file structure: {}", ex.getMessage());
            throw new PdfProcessingException("Uploaded file is corrupted or not a valid PDF document", ex);
        }
    }
}