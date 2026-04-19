package com.brewmaster.ai.gemini;

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
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemini")
public class GeminiAIClient implements AIClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiAIClient.class);
    private static final String API_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";
    private static final int MAX_RETRIES = 2;

    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final long retryBaseMs;

    @Autowired
    public GeminiAIClient(
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model,
            @Value("${gemini.max-tokens}") int maxTokens,
            ObjectMapper objectMapper) {
        this(apiKey, model, maxTokens, objectMapper,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(120)).build(), 1000L);
    }

    GeminiAIClient(String apiKey, String model, int maxTokens,
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

            ObjectNode systemInstruction = body.putObject("system_instruction");
            ArrayNode sysParts = systemInstruction.putArray("parts");
            sysParts.addObject().put("text", request.systemPrompt());

            ArrayNode contents = body.putArray("contents");
            ObjectNode userContent = contents.addObject();
            userContent.put("role", "user");
            ArrayNode userParts = userContent.putArray("parts");
            userParts.addObject().put("text", request.userMessage());

            ArrayNode tools = body.putArray("tools");
            tools.addObject().putObject("googleSearch");

            ObjectNode generationConfig = body.putObject("generationConfig");
            generationConfig.put("maxOutputTokens",
                    request.maxTokens() > 0 ? request.maxTokens() : maxTokens);
            // Disable thinking to avoid multi-minute response times on 2.5 Flash
            generationConfig.putObject("thinkingConfig").put("thinkingBudget", 0);

            String requestBody = objectMapper.writeValueAsString(body);
            log.debug("Gemini request: model={}, system_len={}, user_len={}",
                    model, request.systemPrompt().length(), request.userMessage().length());

            return sendWithRetry(requestBody);
        } catch (AIClientException e) {
            throw e;
        } catch (Exception e) {
            throw new AIClientException("Failed to build Gemini request: " + e.getMessage(), e);
        }
    }

    private String sendWithRetry(String requestBody) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                String url = String.format(API_URL_TEMPLATE, model, apiKey);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(120))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                log.debug("Gemini response: status={}, body_len={}", response.statusCode(), response.body().length());

                if (response.statusCode() == 429 || response.statusCode() == 503) {
                    if (attempt <= MAX_RETRIES) {
                        long delayMs = (long) Math.pow(2, attempt) * retryBaseMs;
                        log.warn("Gemini rate limited/overloaded ({}), retry {}/{} in {}ms",
                                response.statusCode(), attempt, MAX_RETRIES, delayMs);
                        Thread.sleep(delayMs);
                        continue;
                    }
                    throw new AIClientException("Gemini API is temporarily unavailable. Please try again later.");
                }

                if (response.statusCode() != 200) {
                    log.error("Gemini API error: status={}", response.statusCode());
                    throw new AIClientException(
                            "Gemini API returned an error (status " + response.statusCode() + ").");
                }

                return extractText(response.body());

            } catch (AIClientException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AIClientException("Request interrupted", e);
            } catch (Exception e) {
                if (attempt <= MAX_RETRIES) {
                    log.warn("Gemini request failed (attempt {}), retrying: {}", attempt, e.getMessage());
                } else {
                    log.error("Gemini API unreachable after {} attempts", attempt, e);
                    throw new AIClientException("Gemini API is unreachable. Please try again later.", e);
                }
            }
        }
    }

    private String extractText(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        StringBuilder text = new StringBuilder();
        for (JsonNode part : parts) {
            if (part.path("thought").asBoolean(false)) continue;
            JsonNode textNode = part.path("text");
            if (!textNode.isMissingNode()) {
                text.append(textNode.asText());
            }
        }
        if (text.isEmpty()) {
            throw new AIClientException("Gemini response contained no text content");
        }
        return text.toString();
    }
}
