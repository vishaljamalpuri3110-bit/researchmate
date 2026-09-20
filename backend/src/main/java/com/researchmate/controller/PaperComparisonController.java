package com.researchmate.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.researchmate.dto.request.PaperComparisonRequest;
import com.researchmate.dto.response.PaperComparisonResponse;
import com.researchmate.service.PaperComparisonService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/papers")
public class PaperComparisonController {

    private final PaperComparisonService comparisonService;

    public PaperComparisonController(PaperComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    @PostMapping("/compare")
    public ResponseEntity<PaperComparisonResponse> comparePapers(
            @Valid @RequestBody PaperComparisonRequest request) {

        PaperComparisonResponse response = comparisonService.comparePapers(request);
        return ResponseEntity.ok(response);
    }
}
