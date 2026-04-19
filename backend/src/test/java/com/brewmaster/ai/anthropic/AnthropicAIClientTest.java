package com.brewmaster.ai.anthropic;

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
class AnthropicAIClientTest {

    @Mock
    private HttpClient httpClient;

    private AnthropicAIClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SUCCESS_BODY =
            "{\"content\":[{\"type\":\"text\",\"text\":\"{\\\"key\\\":\\\"value\\\"}\"}]}";

    @BeforeEach
    void setUp() {
        client = new AnthropicAIClient("test-api-key", "claude-test", 1024, objectMapper, httpClient, 1L);
    }

    @Test
    void sendsCorrectHeadersAndModel() throws Exception {
        HttpResponse response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(SUCCESS_BODY);
        doReturn(response).when(httpClient).send(any(HttpRequest.class), any());

        String result = client.sendWithWebSearch(new AIRequest("system prompt", "user message", 1024));

        assertThat(result).isEqualTo("{\"key\":\"value\"}");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        HttpRequest sent = captor.getValue();

        assertThat(sent.headers().firstValue("x-api-key")).contains("test-api-key");
        assertThat(sent.headers().firstValue("anthropic-version")).contains("2023-06-01");
        assertThat(sent.headers().firstValue("anthropic-beta")).contains("web-search-2025-03-05");
        assertThat(sent.uri().toString()).isEqualTo("https://api.anthropic.com/v1/messages");
    }

    @Test
    void retriesOn529AndSucceeds() throws Exception {
        HttpResponse overloaded = mock(HttpResponse.class);
        when(overloaded.statusCode()).thenReturn(529);
        when(overloaded.body()).thenReturn("overloaded");

        HttpResponse success = mock(HttpResponse.class);
        when(success.statusCode()).thenReturn(200);
        when(success.body()).thenReturn(SUCCESS_BODY);

        doReturn(overloaded, overloaded, success).when(httpClient).send(any(HttpRequest.class), any());

        String result = client.sendWithWebSearch(new AIRequest("sys", "msg", 1024));

        assertThat(result).isEqualTo("{\"key\":\"value\"}");
        verify(httpClient, times(3)).send(any(), any());
    }

    @Test
    void throwsAIClientExceptionAfterExhaustedRetries() throws Exception {
        HttpResponse overloaded = mock(HttpResponse.class);
        when(overloaded.statusCode()).thenReturn(529);
        when(overloaded.body()).thenReturn("overloaded");

        doReturn(overloaded).when(httpClient).send(any(HttpRequest.class), any());

        assertThatThrownBy(() -> client.sendWithWebSearch(new AIRequest("sys", "msg", 1024)))
                .isInstanceOf(AIClientException.class)
                .hasMessageContaining("overloaded");

        verify(httpClient, times(3)).send(any(), any());
    }
}
