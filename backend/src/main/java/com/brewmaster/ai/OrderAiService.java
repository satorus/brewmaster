package com.brewmaster.ai;

import org.springframework.stereotype.Service;

// TODO: implement in Order & Price Finder feature milestone
@Service
public class OrderAiService {

    private final AIClient aiClient;

    public OrderAiService(AIClient aiClient) {
        this.aiClient = aiClient;
    }
}
