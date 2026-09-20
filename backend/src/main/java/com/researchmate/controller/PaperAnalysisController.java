package com.researchmate.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.researchmate.dto.response.PaperAnalysisResponse;
import com.researchmate.service.PaperAnalysisService;

@RestController
@RequestMapping("/api/papers")
public class PaperAnalysisController {

    private final PaperAnalysisService paperAnalysisService;

    public PaperAnalysisController(PaperAnalysisService paperAnalysisService) {
        this.paperAnalysisService = paperAnalysisService;
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<PaperAnalysisResponse> analyzePaper(@PathVariable Long id) {
        PaperAnalysisResponse response = paperAnalysisService.analyzePaper(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/analysis")
    public ResponseEntity<PaperAnalysisResponse> getAnalysis(@PathVariable Long id) {
        PaperAnalysisResponse response = paperAnalysisService.getAnalysis(id);
        return ResponseEntity.ok(response);
    }
}
