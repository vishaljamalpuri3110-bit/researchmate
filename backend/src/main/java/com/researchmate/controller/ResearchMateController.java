package com.researchmate.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
public class ResearchMateController {
    
    @GetMapping("/api/health")
    public String healthCheck() {
        return "ResearchMate backend is running";
    }
    
}
