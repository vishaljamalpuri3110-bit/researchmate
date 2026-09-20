package com.researchmate.dto.response;

import java.time.Instant;
import java.util.List;

import com.researchmate.entity.EvidenceSourceType;

public record ResearchGapResponse(
        Long id,
        String title,
        String description,
        Double confidence,
        Integer frequency,
        List<String> supportingPapers,
        List<ResearchGapEvidenceDto> evidence,
        String disclaimer,
        Instant createdAt
) {
    public record ResearchGapEvidenceDto(
            Long paperId,
            String paperTitle,
            String evidenceText,
            EvidenceSourceType sourceType
    ) {}
}
