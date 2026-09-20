package com.researchmate.dto.response;

import java.util.List;
import java.util.Map;

public record ExperimentResultResponse(
        String researchQuestion,
        String datasetDescription,
        int totalPapersInCorpus,
        int totalEvaluationQueries,
        int evaluationDepthK,
        MethodMetrics tfIdfMetrics,
        MethodMetrics embeddingMetrics,
        List<QueryEvaluationDetail> queryBreakdown,
        String empiricalConclusion
) {
    public record MethodMetrics(
            double precisionAtK,
            double recallAtK,
            double f1ScoreAtK,
            double meanAveragePrecision, // MAP
            double meanReciprocalRank,   // MRR
            long executionTimeMs
    ) {}

    public record QueryEvaluationDetail(
            String queryTitle,
            List<String> groundTruthRelevantTitles,
            List<RankedItem> tfIdfTopK,
            List<RankedItem> embeddingTopK,
            double tfIdfAveragePrecision,
            double embeddingAveragePrecision
    ) {}

    public record RankedItem(
            String title,
            double score,
            boolean isRelevant
    ) {}
}
