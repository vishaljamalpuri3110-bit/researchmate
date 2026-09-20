package com.researchmate.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.researchmate.dto.request.CreatePaperRequest;
import com.researchmate.dto.response.PageResponse;
import com.researchmate.dto.response.PaperResponse;
import com.researchmate.service.PaperService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/papers")
public class PaperController {

    private final PaperService paperService;

    public PaperController(PaperService paperService) {
        this.paperService = paperService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<PaperResponse>> getPapers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                paperService.getPapersPaginated(search, year, page, size)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaperResponse> getPaper(@PathVariable Long id) {
        return ResponseEntity.ok(paperService.getPaper(id));
    }

    @PostMapping
    public ResponseEntity<PaperResponse> createPaper(@Valid @RequestBody CreatePaperRequest request) {
        PaperResponse response = paperService.createPaper(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<PaperResponse> uploadPaper(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("publicationYear") Integer publicationYear) {

        PaperResponse response = paperService.uploadPaper(file, title, publicationYear);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePaper(@PathVariable Long id) {
        paperService.deletePaper(id);
        return ResponseEntity.noContent().build();
    }
}