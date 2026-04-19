package com.brewmaster.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AIJsonUtilTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returnsRawStringWhenAlreadyValidJson() {
        String json = "{\"key\": \"value\"}";
        assertThat(AIJsonUtil.extractJson(json, objectMapper)).isEqualTo(json);
    }

    @Test
    void extractsJsonFromCodeFences() {
        String json = "{\"key\": \"value\"}";
        String fenced = "```json\n" + json + "\n```";
        assertThat(AIJsonUtil.extractJson(fenced, objectMapper)).isEqualTo(json);
    }

    @Test
    void extractsJsonFromPlainCodeFences() {
        String json = "{\"key\": \"value\"}";
        String fenced = "```\n" + json + "\n```";
        assertThat(AIJsonUtil.extractJson(fenced, objectMapper)).isEqualTo(json);
    }

    @Test
    void throwsAIClientExceptionForGarbage() {
        assertThatThrownBy(() -> AIJsonUtil.extractJson("not valid json at all", objectMapper))
                .isInstanceOf(AIClientException.class)
                .hasMessageContaining("Could not extract valid JSON");
    }
}
