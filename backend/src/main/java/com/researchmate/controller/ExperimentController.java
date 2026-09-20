package com.researchmate.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.researchmate.dto.response.ExperimentResultResponse;
import com.researchmate.service.ExperimentService;

@RestController
@RequestMapping("/api/experiment")
public class ExperimentController {

    private final ExperimentService experimentService;

    public ExperimentController(ExperimentService experimentService) {
        this.experimentService = experimentService;
    }

    @GetMapping("/evaluate")
    public ResponseEntity<ExperimentResultResponse> evaluate(
            @RequestParam(defaultValue = "3") int k) {
        return ResponseEntity.ok(experimentService.runEvaluation(k));
    }
}
