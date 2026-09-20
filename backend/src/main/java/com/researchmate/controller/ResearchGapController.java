package com.researchmate.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.researchmate.dto.response.ResearchGapResponse;
import com.researchmate.service.ResearchGapService;

@RestController
@RequestMapping("/api/research-gaps")
public class ResearchGapController {

    private final ResearchGapService researchGapService;

    public ResearchGapController(ResearchGapService researchGapService) {
        this.researchGapService = researchGapService;
    }

    @GetMapping
    public ResponseEntity<List<ResearchGapResponse>> getResearchGaps(
            @RequestParam(required = false, defaultValue = "false") boolean refresh) {

        if (refresh) {
            return ResponseEntity.ok(researchGapService.detectResearchGaps());
        }

        List<ResearchGapResponse> existing = researchGapService.getSavedResearchGaps();
        if (existing.isEmpty()) {
            existing = researchGapService.detectResearchGaps();
        }

        return ResponseEntity.ok(existing);
    }
}
