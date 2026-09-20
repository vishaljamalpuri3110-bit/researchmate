package com.researchmate.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.researchmate.dto.response.PaperSimilarityResponse;
import com.researchmate.entity.SimilarityMethod;
import com.researchmate.service.PaperSimilarityService;

@RestController
@RequestMapping("/api/papers")
public class PaperSimilarityController {

    private final PaperSimilarityService similarityService;

    public PaperSimilarityController(PaperSimilarityService similarityService) {
        this.similarityService = similarityService;
    }

    @GetMapping("/{id}/similar")
    public ResponseEntity<List<PaperSimilarityResponse>> getSimilarPapers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "TF_IDF") SimilarityMethod method) {

        List<PaperSimilarityResponse> response = similarityService.getSimilarPapers(id, method);
        return ResponseEntity.ok(response);
    }
}
