package com.brewmaster.ai.gemini;

import com.brewmaster.ai.AIClientException;
import com.brewmaster.ai.AIRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class GeminiAIClientTest {

    @Mock
    private HttpClient httpClient;

    private GeminiAIClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Includes a thought part (should be skipped) followed by the real text part
    private static final String SUCCESS_BODY =
            "{\"candidates\":[{\"content\":{\"parts\":[" +
            "{\"thought\":true,\"text\":\"Let me think...\"}," +
            "{\"text\":\"{\\\"key\\\":\\\"value\\\"}\"}" +
            "]}}]}";

    @BeforeEach
    void setUp() {
        client = new GeminiAIClient("test-gemini-key", "gemini-test", 1024, objectMapper, httpClient, 1L);
    }

    @Test
    void sendsCorrectRequestStructure() throws Exception {
        HttpResponse response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(SUCCESS_BODY);
        doReturn(response).when(httpClient).send(any(HttpRequest.class), any());

        String result = client.sendWithWebSearch(new AIRequest("system", "user", 1024));

        assertThat(result).isEqualTo("{\"key\":\"value\"}");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        HttpRequest sent = captor.getValue();

        assertThat(sent.uri().toString()).contains("generativelanguage.googleapis.com");
        assertThat(sent.uri().toString()).contains("test-gemini-key");
        assertThat(sent.uri().toString()).contains("gemini-test");
    }

    @Test
    void retriesOn429AndSucceeds() throws Exception {
        HttpResponse rateLimited = mock(HttpResponse.class);
        when(rateLimited.statusCode()).thenReturn(429);
        when(rateLimited.body()).thenReturn("rate limited");

        HttpResponse success = mock(HttpResponse.class);
        when(success.statusCode()).thenReturn(200);
        when(success.body()).thenReturn(SUCCESS_BODY);

        doReturn(rateLimited, rateLimited, success).when(httpClient).send(any(HttpRequest.class), any());

        String result = client.sendWithWebSearch(new AIRequest("sys", "msg", 1024));

        assertThat(result).isEqualTo("{\"key\":\"value\"}");
        verify(httpClient, times(3)).send(any(), any());
    }

    @Test
    void throwsAIClientExceptionAfterExhaustedRetries() throws Exception {
        HttpResponse overloaded = mock(HttpResponse.class);
        when(overloaded.statusCode()).thenReturn(503);
        when(overloaded.body()).thenReturn("unavailable");

        doReturn(overloaded).when(httpClient).send(any(HttpRequest.class), any());

        assertThatThrownBy(() -> client.sendWithWebSearch(new AIRequest("sys", "msg", 1024)))
                .isInstanceOf(AIClientException.class)
                .hasMessageContaining("unavailable");

        verify(httpClient, times(3)).send(any(), any());
    }
}
