package com.researchmate.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.researchmate.dto.request.CreatePaperRequest;
import com.researchmate.dto.response.PageResponse;
import com.researchmate.dto.response.PaperResponse;
import com.researchmate.entity.Paper;
import com.researchmate.entity.PaperStatus;
import com.researchmate.entity.ResearchActivity;
import com.researchmate.entity.User;
import com.researchmate.exception.PaperNotFoundException;
import com.researchmate.repository.PaperAnalysisRepository;
import com.researchmate.repository.PaperRepository;
import com.researchmate.repository.PaperSimilarityRepository;
import com.researchmate.repository.ResearchActivityRepository;
import com.researchmate.repository.ResearchGapEvidenceRepository;
import com.researchmate.repository.UserRepository;
import com.researchmate.security.SecurityUtils;

@Service
@Transactional(readOnly = true)
public class PaperService {

    private static final Logger log = LoggerFactory.getLogger(PaperService.class);

    private final PaperRepository paperRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final FileValidationService fileValidationService;
    private final PdfExtractionService pdfExtractionService;
    private final PaperAnalysisRepository analysisRepository;
    private final PaperSimilarityRepository similarityRepository;
    private final ResearchGapEvidenceRepository gapEvidenceRepository;
    private final ResearchActivityRepository activityRepository;

    public PaperService(
            PaperRepository paperRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService,
            FileValidationService fileValidationService,
            PdfExtractionService pdfExtractionService,
            PaperAnalysisRepository analysisRepository,
            PaperSimilarityRepository similarityRepository,
            ResearchGapEvidenceRepository gapEvidenceRepository,
            ResearchActivityRepository activityRepository) {
        this.paperRepository = paperRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.fileValidationService = fileValidationService;
        this.pdfExtractionService = pdfExtractionService;
        this.analysisRepository = analysisRepository;
        this.similarityRepository = similarityRepository;
        this.gapEvidenceRepository = gapEvidenceRepository;
        this.activityRepository = activityRepository;
    }

    public List<PaperResponse> getAllPapers() {
        User user = getCurrentUser();
        return paperRepository
                .findByOwnerId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PageResponse<PaperResponse> getPapersPaginated(String search, Integer year, int page, int size) {
        User user = getCurrentUser();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by("createdAt").descending());

        String sanitizedSearch = (search != null && !search.trim().isBlank()) ? search.trim() : null;
        Page<Paper> paperPage = paperRepository.findByOwnerIdWithFilters(user.getId(), sanitizedSearch, year, pageable);

        List<PaperResponse> content = paperPage.getContent().stream().map(this::toResponse).toList();
        return new PageResponse<>(
                content,
                paperPage.getNumber(),
                paperPage.getSize(),
                paperPage.getTotalElements(),
                paperPage.getTotalPages(),
                paperPage.isFirst(),
                paperPage.isLast()
        );
    }

    public PaperResponse getPaper(Long id) {
        User user = getCurrentUser();

        Paper paper = paperRepository
                .findByIdAndOwnerId(id, user.getId())
                .orElseThrow(() -> new PaperNotFoundException("Paper with ID " + id + " not found"));

        return toResponse(paper);
    }

    @Transactional
    public PaperResponse createPaper(CreatePaperRequest request) {
        User user = getCurrentUser();
        Paper paper = new Paper();
        paper.setTitle(request.getTitle());
        paper.setAbstractText(request.getAbstractText());
        paper.setPublicationYear(request.getPublicationYear());
        paper.setPdfPath("manual");
        paper.setOriginalFileName(request.getTitle() + ".pdf");
        paper.setStoredFileName("manual-" + System.currentTimeMillis() + ".pdf");
        paper.setOwner(user);
        paper.setStatus(PaperStatus.EXTRACTED);
        paper.setExtractedText((request.getTitle() != null ? request.getTitle() : "") + "\n\n" +
                (request.getAbstractText() != null ? request.getAbstractText() : ""));

        Paper savedPaper = paperRepository.save(paper);

        activityRepository.save(new ResearchActivity(
                user,
                "MANUAL_PAPER_ENTRY",
                "Created paper entry manually: " + savedPaper.getTitle()
        ));

        return toResponse(savedPaper);
    }

    @Transactional
    public PaperResponse uploadPaper(
            MultipartFile file,
            String title,
            Integer publicationYear) {

        User user = getCurrentUser();
        fileValidationService.validatePdf(file);

        String storedFileName = fileStorageService.storePdf(file);

        Paper paper = new Paper();
        paper.setTitle(title.trim());
        paper.setPublicationYear(publicationYear);
        paper.setPdfPath(storedFileName);
        paper.setOriginalFileName(file.getOriginalFilename());
        paper.setStoredFileName(storedFileName);
        paper.setStatus(PaperStatus.UPLOADED);
        paper.setOwner(user);

        Paper savedPaper = paperRepository.save(paper);

        activityRepository.save(new ResearchActivity(
                user,
                "PDF_UPLOAD",
                "Uploaded paper PDF: " + savedPaper.getTitle() + " (" + file.getOriginalFilename() + ")"
        ));

        // Extract PDF text synchronously after saving the paper
        pdfExtractionService.extractTextAsync(savedPaper.getId());

        log.info("Paper uploaded with ID: {}. PDF text extraction completed.", savedPaper.getId());

        return toResponse(savedPaper);
    }

    @Transactional
    public void deletePaper(Long id) {
        User user = getCurrentUser();

        Paper paper = paperRepository.findByIdAndOwnerId(id, user.getId())
                .orElseThrow(() -> new PaperNotFoundException("Paper with ID " + id + " not found"));

        log.info("Deleting paper ID: {} ('{}')", id, paper.getTitle());

        // 1. Remove related similarities
        similarityRepository.deleteBySourcePaperIdOrTargetPaperId(id);

        // 2. Remove related analysis
        analysisRepository.deleteByPaperId(id);

        // 3. Remove gap evidence references
        gapEvidenceRepository.deleteByPaperId(id);

        // 4. Remove physical file from disk
        fileStorageService.deletePdf(paper.getStoredFileName());

        // 5. Delete paper entity
        paperRepository.delete(paper);

        activityRepository.save(new ResearchActivity(
                user,
                "PAPER_DELETE",
                "Deleted paper: " + paper.getTitle()
        ));

        log.info("Successfully deleted paper ID: {}", id);
    }

    private PaperResponse toResponse(Paper paper) {
        boolean hasText = paper.getExtractedText() != null && !paper.getExtractedText().isBlank();
        return new PaperResponse(
                paper.getId(),
                paper.getTitle(),
                paper.getAbstractText(),
                paper.getPublicationYear(),
                paper.getStatus(),
                paper.getOriginalFileName(),
                hasText,
                paper.getCreatedAt(),
                paper.getUpdatedAt()
        );
    }

    private User getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}