package com.researchmate.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.researchmate.dto.request.CreatePaperRequest;
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
    public ResponseEntity<List<PaperResponse>> getAllPapers() {

        return ResponseEntity.ok(
                paperService.getAllPapers()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaperResponse> getPaper(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                paperService.getPaper(id)
        );
    }

    @PostMapping
    public ResponseEntity<PaperResponse> createPaper(
            @Valid @RequestBody CreatePaperRequest request) {

        PaperResponse response =
                paperService.createPaper(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping(
        value = "/upload",
        consumes = "multipart/form-data"
)
public ResponseEntity<PaperResponse> uploadPaper(

        @RequestParam("file")
        MultipartFile file,

        @RequestParam("title")
        String title,

        @RequestParam("publicationYear")
        Integer publicationYear) {

    PaperResponse response =
            paperService.uploadPaper(
                    file,
                    title,
                    publicationYear);

    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
}
}