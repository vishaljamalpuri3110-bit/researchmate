package com.researchmate.ai;

public interface EmbeddingService {

    /**
     * Generates a dense numerical vector embedding representation for the given text.
     */
    float[] generateEmbedding(String text);

    /**
     * Computes Cosine Similarity between two dense embedding vectors.
     */
    double cosineSimilarity(float[] vectorA, float[] vectorB);
}
