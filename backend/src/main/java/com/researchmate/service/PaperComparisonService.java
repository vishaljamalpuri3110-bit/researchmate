package com.researchmate.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.researchmate.dto.request.PaperComparisonRequest;
import com.researchmate.dto.response.PaperComparisonResponse;
import com.researchmate.dto.response.PaperComparisonResponse.PaperComparisonItem;
import com.researchmate.entity.Paper;
import com.researchmate.entity.PaperAnalysis;
import com.researchmate.entity.ResearchActivity;
import com.researchmate.entity.User;
import com.researchmate.exception.PaperNotFoundException;
import com.researchmate.repository.PaperAnalysisRepository;
import com.researchmate.repository.PaperRepository;
import com.researchmate.repository.ResearchActivityRepository;
import com.researchmate.repository.UserRepository;
import com.researchmate.security.SecurityUtils;

@Service
@Transactional(readOnly = true)
public class PaperComparisonService {


    private final PaperRepository paperRepository;
    private final PaperAnalysisRepository analysisRepository;
    private final UserRepository userRepository;
    private final ResearchActivityRepository activityRepository;

    public PaperComparisonService(
            PaperRepository paperRepository,
            PaperAnalysisRepository analysisRepository,
            UserRepository userRepository,
            PaperAnalysisService paperAnalysisService,
            ResearchActivityRepository activityRepository) {
        this.paperRepository = paperRepository;
        this.analysisRepository = analysisRepository;
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
    }

    @Transactional
    public PaperComparisonResponse comparePapers(PaperComparisonRequest request) {
        User currentUser = getCurrentUser();

        List<Long> paperIds = request.getPaperIds();
        if (paperIds == null || paperIds.size() < 2 || paperIds.size() > 5) {
            throw new IllegalArgumentException("Comparison requires between 2 and 5 paper IDs");
        }

        List<PaperComparisonItem> items = new ArrayList<>();

        for (Long paperId : paperIds) {
            Paper paper = paperRepository.findByIdAndOwnerId(paperId, currentUser.getId())
                    .orElseThrow(() -> new PaperNotFoundException("Paper with ID " + paperId + " not found or unauthorized"));

            // Ensure analysis exists, otherwise run analysis
            PaperAnalysis analysis = analysisRepository.findByPaperId(paper.getId())
        .orElseThrow(() -> new IllegalStateException(
                "Paper '" + paper.getTitle() +
                "' must be analyzed before it can be compared."
        ));

            List<String> algos = delimitedToList(analysis.getAlgorithms());
            List<String> limits = delimitedToList(analysis.getLimitations());
            List<String> futures = delimitedToList(analysis.getFutureWork());

            items.add(new PaperComparisonItem(
                    paper.getId(),
                    paper.getTitle(),
                    paper.getPublicationYear(),
                    analysis.getResearchProblem(),
                    analysis.getMethodology(),
                    analysis.getDataset(),
                    algos,
                    analysis.getResults(),
                    limits,
                    futures
            ));

            
        }

        String synthesis = generateComparativeSynthesis(items);

        activityRepository.save(new ResearchActivity(
                currentUser,
                "PAPER_COMPARISON",
                "Compared " + paperIds.size() + " papers: " +
                        items.stream().map(PaperComparisonItem::title).limit(2).toList() + "..."
        ));

        return new PaperComparisonResponse(items, synthesis, Instant.now());
    }

    private String generateComparativeSynthesis(List<PaperComparisonItem> items) {
    StringBuilder synthesis = new StringBuilder();

    synthesis.append("Comparative Analysis Synthesis:\n\n");

    synthesis.append("The selected papers address different research problems ");
    synthesis.append("using their respective methodologies and algorithms. ");

    if (!items.isEmpty()) {
        synthesis.append("The comparison includes ");
        synthesis.append(items.size());
        synthesis.append(" analyzed papers.\n\n");
    }

    synthesis.append("Methodological Comparison:\n");
    for (PaperComparisonItem item : items) {
        synthesis.append("- ")
                .append(item.title())
                .append(" uses ")
                .append(item.algorithms().isEmpty()
                        ? "the algorithms or methods described in its analysis."
                        : String.join(", ", item.algorithms()))
                .append("\n");
    }

    synthesis.append("\nLimitations and Future Work:\n");
    for (PaperComparisonItem item : items) {
        if (!item.limitations().isEmpty()) {
            synthesis.append("- ")
                    .append(item.title())
                    .append(" limitations: ")
                    .append(String.join("; ", item.limitations()))
                    .append("\n");
        }

        if (!item.futureWork().isEmpty()) {
            synthesis.append("- ")
                    .append(item.title())
                    .append(" future work: ")
                    .append(String.join("; ", item.futureWork()))
                    .append("\n");
        }
    }

    return synthesis.toString();
}

    private List<String> delimitedToList(String delimited) {
        if (delimited == null || delimited.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(delimited.split("\\|\\|"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private User getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
