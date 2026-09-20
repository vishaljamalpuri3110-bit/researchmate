package com.researchmate.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.researchmate.ai.EmbeddingService;
import com.researchmate.dto.response.PaperSimilarityResponse;
import com.researchmate.entity.Paper;
import com.researchmate.entity.PaperSimilarity;
import com.researchmate.entity.ResearchActivity;
import com.researchmate.entity.SimilarityMethod;
import com.researchmate.entity.User;
import com.researchmate.exception.PaperNotFoundException;
import com.researchmate.nlp.TextPreprocessor;
import com.researchmate.nlp.TfIdfService;
import com.researchmate.repository.PaperRepository;
import com.researchmate.repository.PaperSimilarityRepository;
import com.researchmate.repository.ResearchActivityRepository;
import com.researchmate.repository.UserRepository;
import com.researchmate.security.SecurityUtils;

@Service
@Transactional(readOnly = true)
public class PaperSimilarityService {

    private static final Logger log = LoggerFactory.getLogger(PaperSimilarityService.class);

    private final PaperRepository paperRepository;
    private final UserRepository userRepository;
    private final PaperSimilarityRepository similarityRepository;
    private final TfIdfService tfIdfService;
    private final TextPreprocessor textPreprocessor;
    private final EmbeddingService embeddingService;
    private final ResearchActivityRepository activityRepository;

    public PaperSimilarityService(
            PaperRepository paperRepository,
            UserRepository userRepository,
            PaperSimilarityRepository similarityRepository,
            TfIdfService tfIdfService,
            TextPreprocessor textPreprocessor,
            EmbeddingService embeddingService,
            ResearchActivityRepository activityRepository) {
        this.paperRepository = paperRepository;
        this.userRepository = userRepository;
        this.similarityRepository = similarityRepository;
        this.tfIdfService = tfIdfService;
        this.textPreprocessor = textPreprocessor;
        this.embeddingService = embeddingService;
        this.activityRepository = activityRepository;
    }

    @Transactional
    public List<PaperSimilarityResponse> getSimilarPapers(Long paperId, SimilarityMethod method) {
        User currentUser = getCurrentUser();

        Paper sourcePaper = paperRepository.findByIdAndOwnerId(paperId, currentUser.getId())
                .orElseThrow(() -> new PaperNotFoundException(paperId));

        // Get all authorized papers for this user, excluding source paper
        List<Paper> allUserPapers = paperRepository.findByOwnerId(currentUser.getId()).stream()
                .filter(p -> !p.getId().equals(paperId))
                .toList();

        if (allUserPapers.isEmpty()) {
            return List.of();
        }

        // Recompute similarities for fresh results across current user library
        List<PaperSimilarityResponse> results = new ArrayList<>();

        if (method == SimilarityMethod.TF_IDF) {
            results = computeTfIdfSimilarities(sourcePaper, allUserPapers);
        } else {
            results = computeEmbeddingSimilarities(sourcePaper, allUserPapers);
        }

        // Persist or update calculated similarities
        for (PaperSimilarityResponse res : results) {
            Paper targetPaper = paperRepository.findById(res.paperId()).orElse(null);
            if (targetPaper != null) {
                PaperSimilarity sim = similarityRepository
                        .findBySourcePaperIdAndTargetPaperIdAndMethod(sourcePaper.getId(), targetPaper.getId(), method)
                        .orElseGet(() -> new PaperSimilarity(sourcePaper, targetPaper, method, res.score()));
                sim.setScore(res.score());
                similarityRepository.save(sim);
            }
        }

        activityRepository.save(new ResearchActivity(
                currentUser,
                "SIMILARITY_SEARCH",
                "Queried related papers for '" + sourcePaper.getTitle() + "' using " + method
        ));

        return results;
    }

    private List<PaperSimilarityResponse> computeTfIdfSimilarities(Paper sourcePaper, List<Paper> candidates) {
        // Build mini corpus with source paper + candidates
        String sourceText = getFullPaperText(sourcePaper);
        List<String> sourceTokens = textPreprocessor.preprocess(sourceText);

        List<List<String>> corpusTokens = new ArrayList<>();
        corpusTokens.add(sourceTokens);

        List<List<String>> candidateTokensList = new ArrayList<>();
        for (Paper candidate : candidates) {
            List<String> candidateTokens = textPreprocessor.preprocess(getFullPaperText(candidate));
            candidateTokensList.add(candidateTokens);
            corpusTokens.add(candidateTokens);
        }

        Map<String, Double> idfMap = tfIdfService.computeIdf(corpusTokens);
        Map<String, Double> sourceVector = tfIdfService.computeTfIdfVector(sourceTokens, idfMap);

        List<PaperSimilarityResponse> scoredList = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            Paper candidate = candidates.get(i);
            List<String> cTokens = candidateTokensList.get(i);
            Map<String, Double> candidateVector = tfIdfService.computeTfIdfVector(cTokens, idfMap);

            double score = tfIdfService.cosineSimilarity(sourceVector, candidateVector);
            // Round to 4 decimal places
            double roundedScore = Math.round(score * 10000.0) / 10000.0;

            scoredList.add(new PaperSimilarityResponse(
                    candidate.getId(),
                    candidate.getTitle(),
                    candidate.getPublicationYear(),
                    SimilarityMethod.TF_IDF,
                    roundedScore
            ));
        }

        scoredList.sort(Comparator.comparing(PaperSimilarityResponse::score).reversed());
        return scoredList;
    }

    private List<PaperSimilarityResponse> computeEmbeddingSimilarities(Paper sourcePaper, List<Paper> candidates) {
        String sourceText = getFullPaperText(sourcePaper);
        float[] sourceVector = embeddingService.generateEmbedding(sourceText);

        List<PaperSimilarityResponse> scoredList = new ArrayList<>();
        for (Paper candidate : candidates) {
            String candidateText = getFullPaperText(candidate);
            float[] candidateVector = embeddingService.generateEmbedding(candidateText);

            double score = embeddingService.cosineSimilarity(sourceVector, candidateVector);
            double roundedScore = Math.round(score * 10000.0) / 10000.0;

            scoredList.add(new PaperSimilarityResponse(
                    candidate.getId(),
                    candidate.getTitle(),
                    candidate.getPublicationYear(),
                    SimilarityMethod.EMBEDDING,
                    roundedScore
            ));
        }

        scoredList.sort(Comparator.comparing(PaperSimilarityResponse::score).reversed());
        return scoredList;
    }

    private String getFullPaperText(Paper paper) {
        StringBuilder sb = new StringBuilder();
        if (paper.getTitle() != null) sb.append(paper.getTitle()).append(". ");
        if (paper.getAbstractText() != null) sb.append(paper.getAbstractText()).append(". ");
        if (paper.getExtractedText() != null && !paper.getExtractedText().isBlank()) {
            sb.append(paper.getExtractedText());
        }
        return sb.toString();
    }

    private User getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
