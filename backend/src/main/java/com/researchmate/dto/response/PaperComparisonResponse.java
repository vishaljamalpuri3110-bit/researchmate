package com.researchmate.dto.response;

import java.time.Instant;
import java.util.List;

public record PaperComparisonResponse(
        List<PaperComparisonItem> papers,
        String comparativeSynthesis,
        Instant generatedAt
) {
    public record PaperComparisonItem(
            Long paperId,
            String title,
            Integer publicationYear,
            String researchProblem,
            String methodology,
            String dataset,
            List<String> algorithms,
            String results,
            List<String> limitations,
            List<String> futureWork
    ) {}
}
