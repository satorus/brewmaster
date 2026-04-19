package com.brewmaster.ai.anthropic;

import com.brewmaster.ai.AIClient;
import com.brewmaster.ai.AIClientException;
import com.brewmaster.ai.AIRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "anthropic")
public class AnthropicAIClient implements AIClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicAIClient.class);
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String WEB_SEARCH_BETA = "web-search-2025-03-05";
    private static final int MAX_RETRIES = 2;

    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final long retryBaseMs;

    @Autowired
    public AnthropicAIClient(
            @Value("${anthropic.api-key}") String apiKey,
            @Value("${anthropic.model}") String model,
            @Value("${anthropic.max-tokens}") int maxTokens,
            ObjectMapper objectMapper) {
        this(apiKey, model, maxTokens, objectMapper,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(60)).build(), 1000L);
    }

    AnthropicAIClient(String apiKey, String model, int maxTokens,
                      ObjectMapper objectMapper, HttpClient httpClient, long retryBaseMs) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.retryBaseMs = retryBaseMs;
    }

    @Override
    public String sendWithWebSearch(AIRequest request) throws AIClientException {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", request.maxTokens() > 0 ? request.maxTokens() : maxTokens);
            body.put("system", request.systemPrompt());

            ArrayNode tools = body.putArray("tools");
            ObjectNode webSearch = tools.addObject();
            webSearch.put("type", "web_search_20250305");
            webSearch.put("name", "web_search");

            ArrayNode messages = body.putArray("messages");
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", request.userMessage());

            String requestBody = objectMapper.writeValueAsString(body);
            log.debug("Anthropic request: model={}, system_len={}, user_len={}",
                    model, request.systemPrompt().length(), request.userMessage().length());

            return sendWithRetry(requestBody);
        } catch (AIClientException e) {
            throw e;
        } catch (Exception e) {
            throw new AIClientException("Failed to build Anthropic request: " + e.getMessage(), e);
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
                        long delayMs = (long) Math.pow(2, attempt) * retryBaseMs;
                        log.warn("Anthropic overloaded (529), retry {}/{} in {}ms", attempt, MAX_RETRIES, delayMs);
                        Thread.sleep(delayMs);
                        continue;
                    }
                    throw new AIClientException("Anthropic API is temporarily overloaded. Please try again later.");
                }

                if (response.statusCode() != 200) {
                    log.error("Anthropic API error: status={}", response.statusCode());
                    throw new AIClientException(
                            "Anthropic API returned an error (status " + response.statusCode() + ").");
                }

                return extractTextBlocks(response.body());

            } catch (AIClientException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AIClientException("Request interrupted", e);
            } catch (Exception e) {
                if (attempt <= MAX_RETRIES) {
                    log.warn("Anthropic request failed (attempt {}), retrying: {}", attempt, e.getMessage());
                } else {
                    log.error("Anthropic API unreachable after {} attempts", attempt, e);
                    throw new AIClientException("Anthropic API is unreachable. Please try again later.", e);
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
            throw new AIClientException("Anthropic response contained no text content blocks");
        }
        return text.toString();
    }
}
