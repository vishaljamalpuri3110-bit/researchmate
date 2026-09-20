package com.researchmate.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchmate.ai.LlmClient;
import com.researchmate.dto.response.PaperAnalysisResponse;
import com.researchmate.entity.Notification;
import com.researchmate.entity.Paper;
import com.researchmate.entity.PaperAnalysis;
import com.researchmate.entity.PaperStatus;
import com.researchmate.entity.ResearchActivity;
import com.researchmate.entity.User;
import com.researchmate.exception.PaperNotFoundException;
import com.researchmate.repository.NotificationRepository;
import com.researchmate.repository.PaperAnalysisRepository;
import com.researchmate.repository.PaperRepository;
import com.researchmate.repository.ResearchActivityRepository;
import com.researchmate.repository.UserRepository;
import com.researchmate.security.SecurityUtils;

@Service
@Transactional(readOnly = true)
public class PaperAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(PaperAnalysisService.class);
    private static final int CHUNK_SIZE = 12000;
    private static final int CHUNK_OVERLAP = 1000;

    private final PaperRepository paperRepository;
    private final PaperAnalysisRepository analysisRepository;
    private final UserRepository userRepository;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;
    private final ResearchActivityRepository activityRepository;

    public PaperAnalysisService(
            PaperRepository paperRepository,
            PaperAnalysisRepository analysisRepository,
            UserRepository userRepository,
            LlmClient llmClient,
            ObjectMapper objectMapper,
            NotificationRepository notificationRepository,
            ResearchActivityRepository activityRepository) {
        this.paperRepository = paperRepository;
        this.analysisRepository = analysisRepository;
        this.userRepository = userRepository;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.notificationRepository = notificationRepository;
        this.activityRepository = activityRepository;
    }

    @Transactional
    public PaperAnalysisResponse analyzePaper(Long paperId) {
        User currentUser = getCurrentUser();

        Paper paper = paperRepository.findByIdAndOwnerId(paperId, currentUser.getId())
                .orElseThrow(() -> new PaperNotFoundException(paperId));
            if (paper.getStatus() == PaperStatus.ANALYZING) {
        throw new IllegalStateException(
                "Paper analysis is already in progress"
        );
    }
            
        String paperText = paper.getExtractedText();
        if (paperText == null || paperText.isBlank() || paperText.trim().length() < 100) {
    throw new IllegalStateException(
            "Paper does not contain enough extracted text for analysis"
    );
}
    log.info("Initiating paper analysis for paper ID: {} ('{}')", paperId, paper.getTitle());

        paper.setStatus(PaperStatus.ANALYZING);
        paperRepository.saveAndFlush(paper);

        try {
            // Chunking for long papers if text exceeds threshold
            List<String> chunks = createChunks(paperText);

String textForAnalysis = chunks.get(0);

if (chunks.size() > 1) {
    log.info(
            "Paper ID {} contains {} text chunks. "
                    + "Analyzing primary chunk only in MVP mode.",
            paperId,
            chunks.size()
    );
} // Primary representation

            String systemPrompt = """
                    You are an academic research analysis engine.
                    Analyze ONLY information strictly supported by the supplied paper text.
                    Never invent authors, datasets, algorithms, numerical results, or research problems.
                    If any section is not explicitly present in the text, write: 'Information not explicitly specified in document'.
                    You must output ONLY valid, parsable JSON matching this exact structure:
                    {
                      "title": "String",
                      "authors": ["String"],
                      "abstract": "String",
                      "keywords": ["String"],
                      "researchProblem": "String",
                      "methodology": "String",
                      "dataset": "String",
                      "algorithms": ["String"],
                      "results": "String",
                      "limitations": ["String"],
                      "futureWork": ["String"],
                      "summary": "String"
                    }
                    """;

            String userPrompt = "Analyze this academic text and return the structured JSON:\n\n" + textForAnalysis;

            String rawResponse = llmClient.complete(systemPrompt, userPrompt);
            JsonNode rootNode = parseAndValidateJson(rawResponse);

            PaperAnalysis analysis = analysisRepository.findByPaperId(paper.getId())
                    .orElseGet(() -> {
                        PaperAnalysis pa = new PaperAnalysis();
                        pa.setPaper(paper);
                        return pa;
                    });

        String parsedTitle = getTextField(rootNode, "title", paper.getTitle());

analysis.setTitle(parsedTitle);
analysis.setAuthors(jsonArrayToDelimited(rootNode.get("authors")));
analysis.setAbstractText(getTextField(rootNode, "abstract", paper.getAbstractText()));
analysis.setKeywords(jsonArrayToDelimited(rootNode.get("keywords")));

analysis.setResearchProblem(
        getTextField(rootNode, "researchProblem",
                "Information not explicitly specified in document"));

analysis.setMethodology(
        getTextField(rootNode, "methodology",
                "Information not explicitly specified in document"));

analysis.setDataset(
        getTextField(rootNode, "dataset",
                "Information not explicitly specified in document"));

analysis.setAlgorithms(jsonArrayToDelimited(rootNode.get("algorithms")));

analysis.setResults(
        getTextField(rootNode, "results",
                "Information not explicitly specified in document"));

analysis.setLimitations(jsonArrayToDelimited(rootNode.get("limitations")));
analysis.setFutureWork(jsonArrayToDelimited(rootNode.get("futureWork")));

analysis.setSummary(
        getTextField(rootNode, "summary",
                "Information not explicitly specified in document"));

analysis.setAnalysisStatus("COMPLETED");

            PaperAnalysis saved = analysisRepository.save(analysis);

            paper.setStatus(PaperStatus.ANALYZED);
            paperRepository.save(paper);

            activityRepository.save(new ResearchActivity(
                    currentUser,
                    "PAPER_ANALYSIS",
                    "Completed structured AI analysis for paper: " + paper.getTitle()
            ));

            notificationRepository.save(new Notification(
                    currentUser,
                    "Analysis ready for paper: '" + paper.getTitle() + "'"
            ));

            log.info("Successfully analyzed and saved paper ID: {}", paperId);

            return toResponse(saved);

        } catch (Exception ex) {
            log.error("Failed to analyze paper ID {}: {}", paperId, ex.getMessage(), ex);
            paper.setStatus(PaperStatus.FAILED);
            paperRepository.save(paper);

            throw new RuntimeException("Paper analysis failed: " + ex.getMessage(), ex);
        }
    }

    public PaperAnalysisResponse getAnalysis(Long paperId) {
        User currentUser = getCurrentUser();

        PaperAnalysis analysis = analysisRepository.findByPaperIdAndOwnerId(paperId, currentUser.getId())
                .orElseThrow(() -> new PaperNotFoundException("Analysis not found for paper ID: " + paperId));

        return toResponse(analysis);
    }

    private List<String> createChunks(String text) {
        if (text == null || text.length() <= CHUNK_SIZE) {
            return Collections.singletonList(text != null ? text : "");
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            chunks.add(text.substring(start, end));
            if (end == text.length()) break;
            start += (CHUNK_SIZE - CHUNK_OVERLAP);
        }
        return chunks;
    }

    private JsonNode parseAndValidateJson(String raw) {
        try {
            // Strip markdown code fences if model enclosed JSON in ```json ... ```
            String clean = raw.trim();
            if (clean.startsWith("```json")) {
                clean = clean.substring(7);
            } else if (clean.startsWith("```")) {
                clean = clean.substring(3);
            }
            if (clean.endsWith("```")) {
                clean = clean.substring(0, clean.length() - 3);
            }
            clean = clean.trim();

            JsonNode node = objectMapper.readTree(clean);
            if (!node.isObject()) {
                throw new IllegalArgumentException("Parsed JSON is not an object");
            }
            return node;
        } catch (Exception ex) {
            log.error("Could not parse LLM output as valid JSON: {}", ex.getMessage());
    throw new IllegalArgumentException("LLM returned invalid JSON", ex);
        }
    }

    private String getTextField(JsonNode root, String fieldName, String fallback) {
        if (root != null && root.has(fieldName) && !root.get(fieldName).isNull()) {
            String text = root.get(fieldName).asText();
            if (!text.isBlank()) return text;
        }
        return fallback != null ? fallback : "";
    }

    private String jsonArrayToDelimited(JsonNode node) {
        if (node == null || !node.isArray() || node.isEmpty()) {
            return "";
        }
        List<String> items = new ArrayList<>();
        for (JsonNode item : node) {
            String s = item.asText().trim();
            if (!s.isBlank()) items.add(s);
        }
        return String.join(" || ", items);
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

    private PaperAnalysisResponse toResponse(PaperAnalysis pa) {
        return new PaperAnalysisResponse(
                pa.getId(),
                pa.getPaper().getId(),
                pa.getTitle(),
                delimitedToList(pa.getAuthors()),
                pa.getAbstractText(),
                delimitedToList(pa.getKeywords()),
                pa.getResearchProblem(),
                pa.getMethodology(),
                pa.getDataset(),
                delimitedToList(pa.getAlgorithms()),
                pa.getResults(),
                delimitedToList(pa.getLimitations()),
                delimitedToList(pa.getFutureWork()),
                pa.getSummary(),
                pa.getAnalysisStatus(),
                pa.getCreatedAt(),
                pa.getUpdatedAt()
        );
    }

    private User getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
