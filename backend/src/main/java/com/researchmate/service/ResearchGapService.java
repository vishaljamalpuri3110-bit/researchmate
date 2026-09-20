
package com.researchmate.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.researchmate.ai.EmbeddingService;
import com.researchmate.dto.response.ResearchGapResponse;
import com.researchmate.dto.response.ResearchGapResponse.ResearchGapEvidenceDto;
import com.researchmate.entity.EvidenceSourceType;
import com.researchmate.entity.Paper;
import com.researchmate.entity.ResearchActivity;
import com.researchmate.entity.ResearchGap;
import com.researchmate.entity.ResearchGapEvidence;
import com.researchmate.entity.User;
import com.researchmate.repository.PaperAnalysisRepository;
import com.researchmate.repository.PaperRepository;
import com.researchmate.repository.ResearchActivityRepository;
import com.researchmate.repository.ResearchGapRepository;
import com.researchmate.repository.UserRepository;
import com.researchmate.security.SecurityUtils;

@Service
@Transactional(readOnly = true)
public class ResearchGapService {

    private static final Logger log = LoggerFactory.getLogger(ResearchGapService.class);

    private static final String NOVELTY_DISCLAIMER =
            "Potential research direction. Novelty requires human verification, domain expert review, and systematic literature review.";

    private static final double SIMILARITY_THRESHOLD = 0.55;

    private final PaperRepository paperRepository;
    private final PaperAnalysisRepository analysisRepository;
    private final ResearchGapRepository gapRepository;
    private final UserRepository userRepository;
    private final EmbeddingService embeddingService;
    private final ResearchActivityRepository activityRepository;

    public ResearchGapService(
            PaperRepository paperRepository,
            PaperAnalysisRepository analysisRepository,
            ResearchGapRepository gapRepository,
            UserRepository userRepository,
            EmbeddingService embeddingService,
            ResearchActivityRepository activityRepository) {

        this.paperRepository = paperRepository;
        this.analysisRepository = analysisRepository;
        this.gapRepository = gapRepository;
        this.userRepository = userRepository;
        this.embeddingService = embeddingService;
        this.activityRepository = activityRepository;
    }

    @Transactional
    public List<ResearchGapResponse> detectResearchGaps() {

        User currentUser = getCurrentUser();

        List<Paper> userPapers =
                paperRepository.findByOwnerId(currentUser.getId());

        if (userPapers.isEmpty()) {
            return List.of();
        }

        // Collect valid limitation and future-work evidence.
        List<EvidenceCandidate> candidates = new ArrayList<>();

        for (Paper paper : userPapers) {

            analysisRepository.findByPaperId(paper.getId()).ifPresent(analysis -> {

                for (String lim : delimitedToList(analysis.getLimitations())) {

                    if (isValidEvidence(lim)) {

                        candidates.add(
                                new EvidenceCandidate(
                                        paper,
                                        lim,
                                        EvidenceSourceType.LIMITATION,
                                        embeddingService.generateEmbedding(lim)
                                )
                        );
                    }
                }

                for (String fut : delimitedToList(analysis.getFutureWork())) {

                    if (isValidEvidence(fut)) {

                        candidates.add(
                                new EvidenceCandidate(
                                        paper,
                                        fut,
                                        EvidenceSourceType.FUTURE_WORK,
                                        embeddingService.generateEmbedding(fut)
                                )
                        );
                    }
                }
            });
        }

        if (candidates.isEmpty()) {

            log.info(
                    "No valid limitation/future-work evidence found for user {}",
                    currentUser.getId()
            );

            return List.of();
        }

        // Semantic clustering into recurring thematic groups.
        List<List<EvidenceCandidate>> clusters =
                clusterCandidates(candidates);

        // Clear existing generated gaps for this user.
        gapRepository.deleteByOwnerId(currentUser.getId());

        List<ResearchGapResponse> responses = new ArrayList<>();

        for (List<EvidenceCandidate> cluster : clusters) {

            if (cluster.isEmpty()) {
                continue;
            }

            Set<Paper> supportingPapers = new HashSet<>();

            for (EvidenceCandidate candidate : cluster) {
                supportingPapers.add(candidate.paper);
            }

            int frequency = supportingPapers.size();

            // Confidence is a heuristic based on
            // cluster coherence and supporting paper frequency.
            double baseCoherence =
                    calculateClusterCoherence(cluster);

            double confidence =
                    Math.min(
                            0.95,
                            Math.max(
                                    0.60,
                                    Math.round(
                                            (
                                                    0.55
                                                    + (baseCoherence * 0.3)
                                                    + (Math.min(frequency, 5) * 0.05)
                                            ) * 100.0
                                    ) / 100.0
                            )
                    );

            String title =
                    synthesizeGapTitle(cluster);

            String description =
                    synthesizeGapDescription(
                            cluster,
                            supportingPapers.size()
                    );

            ResearchGap gap =
                    new ResearchGap(
                            title,
                            description,
                            confidence,
                            frequency,
                            currentUser
                    );

            for (EvidenceCandidate candidate : cluster) {

                gap.addEvidence(
                        new ResearchGapEvidence(
                                gap,
                                candidate.paper,
                                candidate.text,
                                candidate.sourceType
                        )
                );
            }

            ResearchGap savedGap =
                    gapRepository.save(gap);

            List<String> paperTitles =
                    supportingPapers.stream()
                            .map(Paper::getTitle)
                            .toList();

            List<ResearchGapEvidenceDto> evidenceDtos =
                    savedGap.getEvidenceList()
                            .stream()
                            .map(e ->
                                    new ResearchGapEvidenceDto(
                                            e.getPaper().getId(),
                                            e.getPaper().getTitle(),
                                            e.getEvidenceText(),
                                            e.getSourceType()
                                    )
                            )
                            .toList();

            responses.add(
                    new ResearchGapResponse(
                            savedGap.getId(),
                            savedGap.getTitle(),
                            savedGap.getDescription(),
                            savedGap.getConfidence(),
                            savedGap.getFrequency(),
                            paperTitles,
                            evidenceDtos,
                            NOVELTY_DISCLAIMER,
                            savedGap.getCreatedAt()
                    )
            );
        }

        responses.sort(
                (a, b) ->
                        Double.compare(
                                b.confidence(),
                                a.confidence()
                        )
        );

        activityRepository.save(
                new ResearchActivity(
                        currentUser,
                        "GAP_DETECTION",
                        "Synthesized "
                                + responses.size()
                                + " candidate research directions from "
                                + userPapers.size()
                                + " papers"
                )
        );

        return responses;
    }

    public List<ResearchGapResponse> getSavedResearchGaps() {

        User currentUser = getCurrentUser();

        List<ResearchGap> gaps =
                gapRepository.findByOwnerIdWithEvidence(
                        currentUser.getId()
                );

        return gaps.stream()
                .map(gap -> {

                    List<String> paperTitles =
                            gap.getEvidenceList()
                                    .stream()
                                    .map(e -> e.getPaper().getTitle())
                                    .distinct()
                                    .toList();

                    List<ResearchGapEvidenceDto> evidenceDtos =
                            gap.getEvidenceList()
                                    .stream()
                                    .map(e ->
                                            new ResearchGapEvidenceDto(
                                                    e.getPaper().getId(),
                                                    e.getPaper().getTitle(),
                                                    e.getEvidenceText(),
                                                    e.getSourceType()
                                            )
                                    )
                                    .toList();

                    return new ResearchGapResponse(
                            gap.getId(),
                            gap.getTitle(),
                            gap.getDescription(),
                            gap.getConfidence(),
                            gap.getFrequency(),
                            paperTitles,
                            evidenceDtos,
                            NOVELTY_DISCLAIMER,
                            gap.getCreatedAt()
                    );
                })
                .toList();
    }

    private List<List<EvidenceCandidate>> clusterCandidates(
            List<EvidenceCandidate> candidates) {

        List<List<EvidenceCandidate>> clusters =
                new ArrayList<>();

        boolean[] assigned =
                new boolean[candidates.size()];

        for (int i = 0; i < candidates.size(); i++) {

            if (assigned[i]) {
                continue;
            }

            List<EvidenceCandidate> cluster =
                    new ArrayList<>();

            cluster.add(candidates.get(i));
            assigned[i] = true;

            for (int j = i + 1; j < candidates.size(); j++) {

                if (!assigned[j]) {

                    double similarity =
                            embeddingService.cosineSimilarity(
                                    candidates.get(i).vector,
                                    candidates.get(j).vector
                            );

                    if (similarity >= SIMILARITY_THRESHOLD) {

                        cluster.add(candidates.get(j));
                        assigned[j] = true;
                    }
                }
            }

            clusters.add(cluster);
        }

        return clusters;
    }

    private double calculateClusterCoherence(
            List<EvidenceCandidate> cluster) {

        if (cluster.size() <= 1) {
            return 0.85;
        }

        double totalSimilarity = 0.0;
        int pairs = 0;

        for (int i = 0; i < cluster.size(); i++) {

            for (int j = i + 1; j < cluster.size(); j++) {

                totalSimilarity +=
                        embeddingService.cosineSimilarity(
                                cluster.get(i).vector,
                                cluster.get(j).vector
                        );

                pairs++;
            }
        }

        return pairs > 0
                ? totalSimilarity / pairs
                : 0.85;
    }

    private String synthesizeGapTitle(
            List<EvidenceCandidate> cluster) {

        EvidenceCandidate representative =
                cluster.get(0);

        String text =
                representative.text;

        if (text.length() > 75) {
            text = text.substring(0, 75).trim();
        }

        return "Candidate Direction: "
                + text.replaceAll("[\\.\\;:]", "");
    }

    private String synthesizeGapDescription(
            List<EvidenceCandidate> cluster,
            int paperCount) {

        StringBuilder sb =
                new StringBuilder();

        sb.append(
                "A recurring theme across "
        ).append(paperCount)
                .append(" papers highlights: ");

        sb.append(cluster.get(0).text)
                .append(". ");

        if (cluster.size() > 1) {

            sb.append(
                    "Additionally, supporting investigations indicate: "
            ).append(cluster.get(1).text)
                    .append(". ");
        }

        sb.append(
                "Bridging this area offers an opportunity "
                        + "to address demonstrated computational "
                        + "or empirical bottlenecks."
        );

        return sb.toString();
    }

    /**
     * Rejects placeholder or missing evidence generated when
     * the LLM cannot find a limitation or future-work statement.
     */
    private boolean isValidEvidence(String text) {

        if (text == null || text.isBlank()) {
            return false;
        }

        String normalized =
                text.trim().toLowerCase();

        return normalized.length() > 15
                && !normalized.equals(
                        "information not explicitly specified in document"
                )
                && !normalized.contains(
                        "not explicitly specified in document"
                );
    }

    private List<String> delimitedToList(
            String delimited) {

        if (delimited == null || delimited.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(
                        delimited.split("\\|\\|")
                )
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private User getCurrentUser() {

        String email =
                SecurityUtils.getCurrentUserEmail();

        return userRepository.findByEmail(email)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Authenticated user not found"
                        )
                );
    }

    private record EvidenceCandidate(
            Paper paper,
            String text,
            EvidenceSourceType sourceType,
            float[] vector
    ) {}
}

