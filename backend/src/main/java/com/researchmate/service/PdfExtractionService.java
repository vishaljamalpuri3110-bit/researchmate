package com.researchmate.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.Normalizer;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.researchmate.entity.Notification;
import com.researchmate.entity.Paper;
import com.researchmate.entity.PaperStatus;
import com.researchmate.entity.ResearchActivity;
import com.researchmate.repository.NotificationRepository;
import com.researchmate.repository.PaperRepository;
import com.researchmate.repository.ResearchActivityRepository;

@Service
public class PdfExtractionService {

    private static final Logger log = LoggerFactory.getLogger(PdfExtractionService.class);

    private final PaperRepository paperRepository;
    private final FileStorageService fileStorageService;
    private final NotificationRepository notificationRepository;
    private final ResearchActivityRepository activityRepository;

    public PdfExtractionService(
            PaperRepository paperRepository,
            FileStorageService fileStorageService,
            NotificationRepository notificationRepository,
            ResearchActivityRepository activityRepository) {
        this.paperRepository = paperRepository;
        this.fileStorageService = fileStorageService;
        this.notificationRepository = notificationRepository;
        this.activityRepository = activityRepository;
    }

    
    @Transactional
    public void extractTextAsync(Long paperId) {
        log.info("Starting asynchronous PDF text extraction for paper ID: {}", paperId);

        Paper paper = paperRepository.findById(paperId).orElse(null);
        if (paper == null) {
            log.error("Cannot extract text: Paper not found with ID: {}", paperId);
            return;
        }

        try {
            paper.setStatus(PaperStatus.EXTRACTING);
            paperRepository.saveAndFlush(paper);

            Path filePath = fileStorageService.getFilePath(paper.getStoredFileName());
            File pdfFile = filePath.toFile();

            if (!pdfFile.exists() || !pdfFile.canRead()) {
                throw new IOException("Stored PDF file does not exist or is unreadable: " + paper.getStoredFileName());
            }

            String rawText;
            try (PDDocument document = Loader.loadPDF(pdfFile)) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                rawText = stripper.getText(document);
            }

            String cleanedText = cleanText(rawText);

            if (cleanedText == null || cleanedText.trim().length() < 50) {
                log.warn("Extracted text for paper {} has insufficient content (length: {}). Possibly scanned or image-only PDF.",
                        paperId, cleanedText != null ? cleanedText.length() : 0);
                paper.setStatus(PaperStatus.FAILED);
                paperRepository.save(paper);

                notificationRepository.save(new Notification(
                        paper.getOwner(),
                        "PDF text extraction failed for '" + paper.getTitle() + "'. No extractable digital text found (scanned PDFs require OCR)."
                ));
                return;
            }

            paper.setExtractedText(cleanedText);
            paper.setStatus(PaperStatus.EXTRACTED);
            
            // If abstract was empty, try to populate abstract from first 1000 characters
            if (paper.getAbstractText() == null || paper.getAbstractText().isBlank()) {
                paper.setAbstractText(inferAbstract(cleanedText));
            }

            paperRepository.save(paper);

            activityRepository.save(new ResearchActivity(
                    paper.getOwner(),
                    "TEXT_EXTRACTION",
                    "Extracted " + cleanedText.length() + " characters of text from paper: " + paper.getTitle()
            ));

            notificationRepository.save(new Notification(
                    paper.getOwner(),
                    "PDF extraction completed successfully for: " + paper.getTitle()
            ));

            log.info("PDF text extraction successfully finished for paper ID: {} ({} characters)",
                    paperId, cleanedText.length());

        } catch (Exception ex) {
            log.error("Text extraction failed for paper ID {}: {}", paperId, ex.getMessage(), ex);
            paper.setStatus(PaperStatus.FAILED);
            paperRepository.save(paper);

            notificationRepository.save(new Notification(
                    paper.getOwner(),
                    "PDF extraction failed for '" + paper.getTitle() + "': " + ex.getMessage()
            ));
        }
    }

    public String cleanText(String raw) {
        if (raw == null) {
            return "";
        }

        // Unicode normalization (canonical decomposition followed by canonical composition)
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFC);

        // Replace unicode non-breaking spaces and exotic whitespace with standard space
        normalized = normalized.replace('\u00A0', ' ')
                               .replace('\u2007', ' ')
                               .replace('\u202F', ' ')
                               .replace('\uFEFF', ' ');

        // Fix hyphenated line-break word splits: e.g. "connec-\ntion" -> "connection"
        normalized = normalized.replaceAll("(?i)(\\b[a-z]+)-\\r?\\n([a-z]+\\b)", "$1$2");

        // Normalize line breaks
        normalized = normalized.replace("\r\n", "\n").replace("\r", "\n");

        // Remove repetitive multiple line breaks (e.g. 3+ newlines -> 2 newlines)
        normalized = normalized.replaceAll("\\n{3,}", "\n\n");

        // Normalize multiple spaces and tabs to single space
        normalized = normalized.replaceAll("[ \\t]+", " ");

        // Remove isolated non-printable control characters, but keep valid newlines
        normalized = normalized.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");

        return normalized.trim();
    }

    private String inferAbstract(String text) {
        int absIndex = text.toLowerCase().indexOf("abstract");
        if (absIndex >= 0 && absIndex + 8 < text.length()) {
            int endIndex = Math.min(absIndex + 1200, text.length());
            String snippet = text.substring(absIndex + 8, endIndex).trim();
            // Stop at introduction if present
            int introIndex = snippet.toLowerCase().indexOf("1. introduction");
            if (introIndex < 0) {
                introIndex = snippet.toLowerCase().indexOf("introduction");
            }
            if (introIndex > 100) {
                snippet = snippet.substring(0, introIndex).trim();
            }
            return snippet;
        }
        return text.substring(0, Math.min(text.length(), 600)).trim();
    }
}
