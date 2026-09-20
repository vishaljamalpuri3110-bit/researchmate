package com.researchmate.ai;

public interface LlmClient {

    /**
     * Executes a completion request with timeout and retry handling.
     * @param systemPrompt Instructions specifying strict academic grounding and output schema.
     * @param userPrompt The chunked or aggregated paper text.
     * @return Raw model response, expected to be valid JSON.
     */
    String complete(String systemPrompt, String userPrompt);
}
