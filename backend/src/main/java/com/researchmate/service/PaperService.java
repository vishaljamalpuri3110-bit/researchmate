package com.researchmate.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.researchmate.dto.request.CreatePaperRequest;
import com.researchmate.dto.response.PaperResponse;
import com.researchmate.entity.Paper;
import com.researchmate.entity.User;
import com.researchmate.repository.PaperRepository;
import com.researchmate.repository.UserRepository;
import com.researchmate.security.SecurityUtils;
import com.researchmate.exception.PaperNotFoundException;

@Service
@Transactional(readOnly = true)
public class PaperService {

    private final PaperRepository paperRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final FileValidationService fileValidationService;

    public PaperService(PaperRepository paperRepository,UserRepository userRepository,
        FileStorageService fileStorageService,FileValidationService fileValidationService) {
        this.paperRepository = paperRepository;
        this.userRepository=userRepository;
        this.fileStorageService=fileStorageService;
        this.fileValidationService=fileValidationService;
    }

    public List<PaperResponse> getAllPapers() {

        User user=getCurrentUser();
        
        return paperRepository
                .findByOwnerId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PaperResponse getPaper(Long id) {

        User user=getCurrentUser();

        Paper paper = paperRepository
                .findByIdAndOwnerId(id,user.getId())
                .orElseThrow(() -> new PaperNotFoundException(id));

        return toResponse(paper);
    }

    @Transactional
    public PaperResponse createPaper(CreatePaperRequest request) {

        User user = getCurrentUser();
        Paper paper = new Paper();
        paper.setTitle(request.getTitle());
        paper.setAbstractText(request.getAbstractText());
        paper.setPublicationYear(request.getPublicationYear());
        paper.setPdfPath("pending");
        paper.setOwner(user);
        
        Paper savedPaper = paperRepository.save(paper);
        
        return toResponse(savedPaper);
    }

    private PaperResponse toResponse(Paper paper) {

        return new PaperResponse(
                paper.getId(),
                paper.getTitle(),
                paper.getAbstractText(),
                paper.getPublicationYear(),
                paper.getStatus(),
                paper.getCreatedAt(),
                paper.getUpdatedAt()
        );
    }

    private User getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        return userRepository.findByEmail(email)
            .orElseThrow(() ->
                    new RuntimeException("Authenticated user not found"));
                }

    @Transactional
public PaperResponse uploadPaper(
        MultipartFile file,
        String title,
        Integer publicationYear) {

    User user = getCurrentUser();

    fileValidationService.validatePdf(file);

    String storedFileName =
            fileStorageService.storePdf(file);

    Paper paper = new Paper();

    paper.setTitle(title);
    paper.setPublicationYear(publicationYear);
    paper.setPdfPath(storedFileName);
    paper.setOriginalFileName(
            file.getOriginalFilename());
    paper.setStoredFileName(storedFileName);
    paper.setOwner(user);

    Paper savedPaper =
            paperRepository.save(paper);

    return toResponse(savedPaper);
}
}