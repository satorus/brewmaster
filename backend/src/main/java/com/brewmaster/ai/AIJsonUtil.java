package com.brewmaster.ai;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class AIJsonUtil {

    private AIJsonUtil() {}

    public static String extractJson(String text, ObjectMapper objectMapper) {
        String trimmed = text.trim();

        // Already valid JSON — return immediately
        try {
            objectMapper.readTree(trimmed);
            return trimmed;
        } catch (Exception ignored) {}

        // Strip ```json ... ``` fences — use lastIndexOf for closing fence so
        // any ``` sequences inside the JSON content don't terminate early
        int start = trimmed.indexOf("```json");
        if (start >= 0) {
            String inner = trimmed.substring(start + 7);
            int end = inner.lastIndexOf("```");
            if (end >= 0) {
                return inner.substring(0, end).trim();
            }
        }

        // Strip plain ``` ... ``` fences
        start = trimmed.indexOf("```");
        if (start >= 0) {
            String inner = trimmed.substring(start + 3);
            int end = inner.lastIndexOf("```");
            if (end >= 0) {
                return inner.substring(0, end).trim();
            }
        }

        throw new AIClientException(
                "Could not extract valid JSON from AI response. Preview: " +
                trimmed.substring(0, Math.min(200, trimmed.length())));
    }
}
