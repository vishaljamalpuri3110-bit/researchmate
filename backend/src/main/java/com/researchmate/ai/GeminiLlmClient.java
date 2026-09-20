package com.researchmate.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class GeminiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmClient.class);

    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeminiLlmClient(
            @Value("${GEMINI_API_KEY:}") String apiKey,
            ObjectMapper objectMapper) {

        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.objectMapper = objectMapper;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
public String complete(String systemPrompt, String userPrompt) {

    if (apiKey.isBlank()) {
        throw new IllegalStateException("GEMINI_API_KEY is not configured");
    }

    String prompt = """
            %s

            USER TASK:
            %s

            Return ONLY valid JSON.
            Do not use markdown code fences.
            Do not invent information that is not supported by the paper text.
            If information is unavailable, use an empty string or empty array.
            """.formatted(systemPrompt, userPrompt);

    Map<String, Object> payload = Map.of(
            "contents", new Object[]{
                    Map.of(
                            "parts", new Object[]{
                                    Map.of("text", prompt)
                            }
                    )
            },
            "generationConfig", Map.of(
                    "temperature", 0.1,
                    "maxOutputTokens", 4096
            )
    );

    try {
        String requestBody = objectMapper.writeValueAsString(payload);

        String endpoint =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent";

        int maxAttempts = 3;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Content-Type", "application/json")
                        .header("x-goog-api-key", apiKey)
                        .timeout(Duration.ofSeconds(60))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                log.info("Dispatching Gemini LLM request (attempt {}/{})",
                        attempt, maxAttempts);

                HttpResponse<String> response =
                        httpClient.send(
                                request,
                                HttpResponse.BodyHandlers.ofString()
                        );

                int status = response.statusCode();

                if (status >= 200 && status < 300) {
                    return extractContent(response.body());
                }

                boolean retryable = status == 429 || status >= 500;

                log.warn(
                        "Gemini API returned HTTP {} on attempt {}/{}",
                        status, attempt, maxAttempts
                );

                if (!retryable || attempt == maxAttempts) {
                    throw new RuntimeException(
                            "Gemini API returned HTTP " + status
                    );
                }

            } catch (java.net.http.HttpTimeoutException ex) {

                log.warn(
                        "Gemini request timed out on attempt {}/{}",
                        attempt, maxAttempts
                );

                if (attempt == maxAttempts) {
                    throw new RuntimeException(
                            "Gemini request timed out after " + maxAttempts + " attempts",
                            ex
                    );
                }

            } catch (java.io.IOException ex) {

                log.warn(
                        "Gemini network error on attempt {}/{}: {}",
                        attempt, maxAttempts, ex.getMessage()
                );

                if (attempt == maxAttempts) {
                    throw new RuntimeException(
                            "Gemini network request failed after "
                                    + maxAttempts + " attempts",
                            ex
                    );
                }
            }

            long backoffMillis = 1000L * attempt;

            log.info(
                    "Retrying Gemini request after {} ms",
                    backoffMillis
            );

            Thread.sleep(backoffMillis);
        }

        throw new RuntimeException("Gemini request failed after retries");

    } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Gemini request interrupted", ex);

    } catch (Exception ex) {
        log.error("Gemini LLM request failed: {}", ex.getMessage());
        throw new RuntimeException("Gemini LLM request failed", ex);
    }
}
    private String extractContent(String responseJson) throws Exception {

        JsonNode root = objectMapper.readTree(responseJson);

        JsonNode candidates = root.path("candidates");

        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new RuntimeException("Gemini returned no candidates");
        }

        JsonNode parts = candidates
                .get(0)
                .path("content")
                .path("parts");

        if (!parts.isArray() || parts.isEmpty()) {
            throw new RuntimeException("Gemini returned no text content");
        }

        String text = parts.get(0).path("text").asText();

        if (text == null || text.isBlank()) {
            throw new RuntimeException("Gemini returned empty content");
        }

        return cleanJson(text);
    }

    private String cleanJson(String text) {

        String cleaned = text.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }
}