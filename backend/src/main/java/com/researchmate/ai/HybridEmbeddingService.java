package com.researchmate.ai;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.LongBuffer;
import java.util.Map;

@Service
public class HybridEmbeddingService implements EmbeddingService {

    private static final int MAX_LENGTH = 256;

    private final OrtEnvironment environment;
    private final OrtSession session;
    private final HuggingFaceTokenizer tokenizer;

    public HybridEmbeddingService() {
        try {
            environment = OrtEnvironment.getEnvironment();

            ClassPathResource modelResource =
                    new ClassPathResource(
                            "models/all-MiniLM-L6-v2/model.onnx");

            session = environment.createSession(
                    modelResource.getFile().getAbsolutePath(),
                    new OrtSession.SessionOptions()
            );

            ClassPathResource tokenizerResource =
                    new ClassPathResource(
                            "models/all-MiniLM-L6-v2/tokenizer.json");

            tokenizer = HuggingFaceTokenizer.newInstance(
                    tokenizerResource.getFile().toPath()
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to initialize MiniLM embedding model",
                    e
            );
        }
    }

    @Override
    public float[] generateEmbedding(String text) {

        if (text == null || text.isBlank()) {
            return new float[0];
        }

        try {
            /*
             * Real MiniLM tokenization
             */
            var encoding = tokenizer.encode(text);

            long[] encodedIds = encoding.getIds();

            /*
             * Limit sequence length.
             */
            int length =
                    Math.min(encodedIds.length, MAX_LENGTH);

            long[] inputIds =
                    new long[MAX_LENGTH];

            long[] attentionMask =
                    new long[MAX_LENGTH];

            long[] tokenTypeIds =
                    new long[MAX_LENGTH];

            for (int i = 0; i < length; i++) {

                inputIds[i] =
                        encodedIds[i];

                attentionMask[i] = 1;

                tokenTypeIds[i] = 0;
            }

            /*
             * ONNX tensors
             */
            try (
                    OnnxTensor inputIdsTensor =
                            OnnxTensor.createTensor(
                                    environment,
                                    LongBuffer.wrap(inputIds),
                                    new long[]{1, MAX_LENGTH}
                            );

                    OnnxTensor attentionMaskTensor =
                            OnnxTensor.createTensor(
                                    environment,
                                    LongBuffer.wrap(attentionMask),
                                    new long[]{1, MAX_LENGTH}
                            );

                    OnnxTensor tokenTypeIdsTensor =
                            OnnxTensor.createTensor(
                                    environment,
                                    LongBuffer.wrap(tokenTypeIds),
                                    new long[]{1, MAX_LENGTH}
                            )
            ) {

                Map<String, OnnxTensor> inputs =
                        Map.of(
                                "input_ids",
                                inputIdsTensor,

                                "attention_mask",
                                attentionMaskTensor,

                                "token_type_ids",
                                tokenTypeIdsTensor
                        );

                try (OrtSession.Result result =
                             session.run(inputs)) {

                    Object output =
                            result.get(0).getValue();

                    if (!(output instanceof float[][][] tokenEmbeddings)) {

                        throw new RuntimeException(
                                "Unexpected MiniLM output format"
                        );
                    }

                    float[] embedding =
                            meanPool(
                                    tokenEmbeddings,
                                    attentionMask
                            );

                    return normalize(embedding);
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "MiniLM embedding generation failed",
                    e
            );
        }
    }

    private float[] meanPool(
            float[][][] tokenEmbeddings,
            long[] attentionMask) {

        int dimensions =
                tokenEmbeddings[0][0].length;

        float[] result =
                new float[dimensions];

        int validTokens = 0;

        for (int token = 0;
             token < tokenEmbeddings[0].length;
             token++) {

            if (attentionMask[token] == 0) {
                continue;
            }

            for (int dimension = 0;
                 dimension < dimensions;
                 dimension++) {

                result[dimension] +=
                        tokenEmbeddings[0][token][dimension];
            }

            validTokens++;
        }

        if (validTokens > 0) {

            for (int i = 0;
                 i < dimensions;
                 i++) {

                result[i] /=
                        validTokens;
            }
        }

        return result;
    }

    private float[] normalize(float[] vector) {

        double norm = 0.0;

        for (float value : vector) {

            norm +=
                    value * value;
        }

        norm = Math.sqrt(norm);

        if (norm == 0.0) {
            return vector;
        }

        for (int i = 0;
             i < vector.length;
             i++) {

            vector[i] /=
                    (float) norm;
        }

        return vector;
    }

    @Override
    public double cosineSimilarity(
            float[] vectorA,
            float[] vectorB) {

        if (vectorA == null ||
                vectorB == null ||
                vectorA.length == 0 ||
                vectorB.length == 0) {

            return 0.0;
        }

        if (vectorA.length != vectorB.length) {

            throw new IllegalArgumentException(
                    "Embedding dimensions do not match: "
                            + vectorA.length
                            + " vs "
                            + vectorB.length
            );
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0;
             i < vectorA.length;
             i++) {

            dotProduct +=
                    vectorA[i] *
                    vectorB[i];

            normA +=
                    vectorA[i] *
                    vectorA[i];

            normB +=
                    vectorB[i] *
                    vectorB[i];
        }

        if (normA == 0.0 ||
                normB == 0.0) {

            return 0.0;
        }

        return dotProduct /
                (Math.sqrt(normA) *
                 Math.sqrt(normB));
    }
}