package com.brewmaster.ai;

public record AIRequest(String systemPrompt, String userMessage, int maxTokens) {}
