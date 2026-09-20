package com.researchmate.dto.response;

import java.time.Instant;
import java.util.List;

public record PaperAnalysisResponse(
        Long id,
        Long paperId,
        String title,
        List<String> authors,
        String abstractText,
        List<String> keywords,
        String researchProblem,
        String methodology,
        String dataset,
        List<String> algorithms,
        String results,
        List<String> limitations,
        List<String> futureWork,
        String summary,
        String analysisStatus,
        Instant createdAt,
        Instant updatedAt
) {
}
