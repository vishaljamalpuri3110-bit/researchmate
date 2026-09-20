package com.researchmate.dto.response;

import java.util.List;

public record PaperAnalysisDto(
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
        String summary
) {
}
