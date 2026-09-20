package com.researchmate.nlp;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TfIdfService {

    private static final Logger log = LoggerFactory.getLogger(TfIdfService.class);
    private final TextPreprocessor textPreprocessor;

    public TfIdfService(TextPreprocessor textPreprocessor) {
        this.textPreprocessor = textPreprocessor;
    }

    /**
     * Computes term frequencies (normalized by document length) for a list of tokens.
     */
    public Map<String, Double> computeTf(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Integer> counts = new HashMap<>();
        for (String token : tokens) {
            counts.put(token, counts.getOrDefault(token, 0) + 1);
        }

        double totalTokens = tokens.size();
        Map<String, Double> tfMap = new HashMap<>(counts.size());
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            tfMap.put(entry.getKey(), entry.getValue() / totalTokens);
        }
        return tfMap;
    }

    /**
     * Computes Inverse Document Frequencies across a corpus of tokenized documents:
     * IDF(t) = ln(1 + (N / DF(t))) + 1.0
     */
    public Map<String, Double> computeIdf(List<List<String>> corpusTokens) {
        if (corpusTokens == null || corpusTokens.isEmpty()) {
            return Collections.emptyMap();
        }

        int totalDocs = corpusTokens.size();
        Map<String, Integer> docFrequencies = new HashMap<>();

        for (List<String> docTokens : corpusTokens) {
            Set<String> uniqueTokens = new HashSet<>(docTokens);
            for (String token : uniqueTokens) {
                docFrequencies.put(token, docFrequencies.getOrDefault(token, 0) + 1);
            }
        }

        Map<String, Double> idfMap = new HashMap<>(docFrequencies.size());
        for (Map.Entry<String, Integer> entry : docFrequencies.entrySet()) {
            double idf = Math.log(1.0 + ((double) totalDocs / entry.getValue())) + 1.0;
            idfMap.put(entry.getKey(), idf);
        }
        return idfMap;
    }

    /**
     * Computes TF-IDF vector: TF(t, d) * IDF(t)
     */
    public Map<String, Double> computeTfIdfVector(List<String> docTokens, Map<String, Double> idfMap) {
        Map<String, Double> tfMap = computeTf(docTokens);
        Map<String, Double> tfIdfVector = new HashMap<>(tfMap.size());

        for (Map.Entry<String, Double> entry : tfMap.entrySet()) {
            String term = entry.getKey();
            double idf = idfMap.getOrDefault(term, 1.0);
            tfIdfVector.put(term, entry.getValue() * idf);
        }
        return tfIdfVector;
    }

    /**
     * Computes standard Cosine Similarity between two sparse TF-IDF vectors:
     * cos(A, B) = (A . B) / (||A|| * ||B||)
     */
    public double cosineSimilarity(Map<String, Double> vectorA, Map<String, Double> vectorB) {
        if (vectorA == null || vectorB == null || vectorA.isEmpty() || vectorB.isEmpty()) {
            return 0.0;
        }

        // Iterate through the smaller vector for performance
        Map<String, Double> smaller = vectorA.size() < vectorB.size() ? vectorA : vectorB;
        Map<String, Double> larger = (smaller == vectorA) ? vectorB : vectorA;

        double dotProduct = 0.0;
        for (Map.Entry<String, Double> entry : smaller.entrySet()) {
            Double valB = larger.get(entry.getKey());
            if (valB != null) {
                dotProduct += entry.getValue() * valB;
            }
        }

        if (dotProduct == 0.0) {
            return 0.0;
        }

        double normA = computeNorm(vectorA);
        double normB = computeNorm(vectorB);

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        double similarity = dotProduct / (normA * normB);
        // Bound result between 0.0 and 1.0
        return Math.min(1.0, Math.max(0.0, similarity));
    }

    public double calculateSimilarityBetweenTexts(String textA, String textB) {
        List<String> tokensA = textPreprocessor.preprocess(textA);
        List<String> tokensB = textPreprocessor.preprocess(textB);

        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return 0.0;
        }

        List<List<String>> miniCorpus = List.of(tokensA, tokensB);
        Map<String, Double> idfMap = computeIdf(miniCorpus);

        Map<String, Double> vecA = computeTfIdfVector(tokensA, idfMap);
        Map<String, Double> vecB = computeTfIdfVector(tokensB, idfMap);

        return cosineSimilarity(vecA, vecB);
    }

    private double computeNorm(Map<String, Double> vector) {
        double sumSquares = 0.0;
        for (double val : vector.values()) {
            sumSquares += val * val;
        }
        return Math.sqrt(sumSquares);
    }
}
