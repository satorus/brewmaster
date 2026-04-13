package com.brewmaster.ai;

import org.springframework.stereotype.Service;

// TODO: implement in AI Recipe Finder feature milestone
@Service
public class RecipeAiService {

    private final AnthropicClient anthropicClient;

    public RecipeAiService(AnthropicClient anthropicClient) {
        this.anthropicClient = anthropicClient;
    }
}
