package com.brewmaster.ai;

public interface AIClient {
    String sendWithWebSearch(AIRequest request) throws AIClientException;
}
