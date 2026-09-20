package com.researchmate.dto.response;

import com.researchmate.entity.SimilarityMethod;

public record PaperSimilarityResponse(
        Long paperId,
        String title,
        Integer publicationYear,
        SimilarityMethod method,
        Double score
) {
}
