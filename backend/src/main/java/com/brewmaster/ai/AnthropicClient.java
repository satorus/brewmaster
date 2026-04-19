package com.brewmaster.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class AnthropicClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicClient.class);
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String WEB_SEARCH_BETA = "web-search-2025-03-05";
    private static final int MAX_RETRIES = 2;

    private final String model;
    private final int maxTokens;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AnthropicClient(
            @Value("${anthropic.api-key}") String apiKey,
            @Value("${anthropic.model}") String model,
            @Value("${anthropic.max-tokens}") int maxTokens,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(60))
                .build();
    }

    public String sendMessage(String systemPrompt, String userMessage) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", maxTokens);
            body.put("system", systemPrompt);

            ArrayNode tools = body.putArray("tools");
            ObjectNode webSearch = tools.addObject();
            webSearch.put("type", "web_search_20250305");
            webSearch.put("name", "web_search");

            ArrayNode messages = body.putArray("messages");
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);

            String requestBody = objectMapper.writeValueAsString(body);
            log.debug("Anthropic request: model={}, system_len={}, user_len={}",
                    model, systemPrompt.length(), userMessage.length());

            return sendWithRetry(requestBody);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Failed to build Anthropic request: " + e.getMessage());
        }
    }

    private String sendWithRetry(String requestBody) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .timeout(Duration.ofSeconds(60))
                        .header("x-api-key", apiKey)
                        .header("anthropic-version", ANTHROPIC_VERSION)
                        .header("anthropic-beta", WEB_SEARCH_BETA)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                log.debug("Anthropic response: status={}, body_len={}", response.statusCode(), response.body().length());

                if (response.statusCode() == 529) {
                    if (attempt <= MAX_RETRIES) {
                        long delayMs = (long) Math.pow(2, attempt) * 1000L;
                        log.warn("Anthropic overloaded (529), retry {}/{} in {}ms", attempt, MAX_RETRIES, delayMs);
                        Thread.sleep(delayMs);
                        continue;
                    }
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "Anthropic API is temporarily overloaded. Please try again later.");
                }

                if (response.statusCode() != 200) {
                    log.error("Anthropic API error: status={}", response.statusCode());
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "Anthropic API returned an error (status " + response.statusCode() + ").");
                }

                return extractTextBlocks(response.body());

            } catch (ResponseStatusException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Request interrupted");
            } catch (Exception e) {
                if (attempt <= MAX_RETRIES) {
                    log.warn("Anthropic request failed (attempt {}), retrying: {}", attempt, e.getMessage());
                } else {
                    log.error("Anthropic API unreachable after {} attempts", attempt, e);
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "Anthropic API is unreachable. Please try again later.");
                }
            }
        }
    }

    private String extractTextBlocks(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        StringBuilder text = new StringBuilder();
        for (JsonNode block : root.path("content")) {
            if ("text".equals(block.path("type").asText())) {
                text.append(block.path("text").asText());
            }
        }
        if (text.isEmpty()) {
            throw new IllegalStateException("Anthropic response contained no text content blocks");
        }
        return text.toString();
    }

    public String extractJson(String text) {
        String trimmed = text.trim();

        try {
            objectMapper.readTree(trimmed);
            return trimmed;
        } catch (Exception ignored) {}

        int start = trimmed.indexOf("```json");
        if (start >= 0) {
            String inner = trimmed.substring(start + 7);
            int end = inner.indexOf("```");
            if (end >= 0) {
                String candidate = inner.substring(0, end).trim();
                try {
                    objectMapper.readTree(candidate);
                    return candidate;
                } catch (Exception ignored) {}
            }
        }

        start = trimmed.indexOf("```");
        if (start >= 0) {
            String inner = trimmed.substring(start + 3);
            int end = inner.indexOf("```");
            if (end >= 0) {
                String candidate = inner.substring(0, end).trim();
                try {
                    objectMapper.readTree(candidate);
                    return candidate;
                } catch (Exception ignored) {}
            }
        }

        throw new IllegalStateException(
                "Could not extract valid JSON from AI response. Preview: " +
                trimmed.substring(0, Math.min(200, trimmed.length())));
    }
}
