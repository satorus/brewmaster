package com.brewmaster.ai;

import org.springframework.stereotype.Service;

// TODO: implement in Order & Price Finder feature milestone
@Service
public class OrderAiService {

    private final AnthropicClient anthropicClient;

    public OrderAiService(AnthropicClient anthropicClient) {
        this.anthropicClient = anthropicClient;
    }
}
