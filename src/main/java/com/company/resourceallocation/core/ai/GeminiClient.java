package com.company.resourceallocation.core.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Thin HTTP wrapper around Gemini REST API.
 * Sends a plain-text prompt and returns the first text response.
 */
@Component
@Slf4j
public class GeminiClient {

    private final RestClient restClient;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public GeminiClient(
            @Value("${ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent}") String baseUrl,
            @Value("${ai.gemini.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Send a prompt to Gemini and return the plain text content of the first candidate.
     */
    public String call(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API key is not configured; returning fallback response.");
            return "{" + "\"recommendedResources\": []" + "}";
        }

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        try {
            String responseJson = restClient.post()
                    .uri("?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            return root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            throw new RuntimeException("Failed to call Gemini AI: " + e.getMessage(), e);
        }
    }
}
