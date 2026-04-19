package com.brewmaster.ai;

import com.brewmaster.ai.dto.AiRecipeSearchResponse;
import com.brewmaster.ai.dto.TasteProfileRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RecipeAiService {

    private static final Logger log = LoggerFactory.getLogger(RecipeAiService.class);

    private final AnthropicClient anthropicClient;
    private final ObjectMapper objectMapper;

    public RecipeAiService(AnthropicClient anthropicClient, ObjectMapper objectMapper) {
        this.anthropicClient = anthropicClient;
        this.objectMapper = objectMapper;
    }

    public AiRecipeSearchResponse findRecipes(TasteProfileRequest profile) {
        log.debug("AI recipe search: style={}, volume={}L", profile.style(), profile.batchVolumeL());

        String rawResponse = anthropicClient.sendMessage(buildSystemPrompt(), buildUserMessage(profile));
        String json = anthropicClient.extractJson(rawResponse);

        try {
            AiRecipeSearchResponse result = objectMapper.readValue(json, AiRecipeSearchResponse.class);
            log.debug("AI recipe search returned {} recipes", result.recipes().size());
            return result;
        } catch (Exception e) {
            log.error("Failed to deserialize AI recipe response: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI returned an unexpected response format. Please try again.");
        }
    }

    private String buildSystemPrompt() {
        return """
                You are an expert homebrewing advisor with deep knowledge of all beer styles, ingredients, and \
                brewing techniques.

                TASK: Find 2-4 real, proven homebrew recipes that match the user's taste profile.

                INSTRUCTIONS:
                1. Use the web_search tool to search for actual homebrew recipes from reputable sources: \
                homebrewtalk.com, brewersfriend.com, homebrewing.org, brulosophy.com
                2. Select 2-4 recipes that best match the requested style, ABV range, bitterness, \
                sweetness, colour, and aroma notes
                3. Scale ALL ingredient amounts to the requested batch volume
                4. Write complete step-by-step brewing instructions covering phases: \
                PREPARATION, MASHING, LAUTERING, BOILING, COOLING, FERMENTATION, CONDITIONING, PACKAGING
                5. Record the source URL for each recipe

                CRITICAL OUTPUT REQUIREMENT:
                Your response must be ONLY a valid JSON object. No markdown. No prose. No code fences. \
                No explanations before or after the JSON. The entire response must be parseable as JSON.

                Required JSON schema (use exactly these field names):
                {
                  "recipes": [{
                    "name": "string",
                    "style": "string",
                    "description": "string (2-3 sentences)",
                    "sourceUrl": "string (URL where recipe was found)",
                    "baseVolumeL": number,
                    "originalGravity": number,
                    "finalGravity": number,
                    "abv": number,
                    "ibu": integer,
                    "srm": number,
                    "mashTempC": number,
                    "mashDurationMin": integer,
                    "boilDurationMin": integer,
                    "fermentationTempC": number,
                    "fermentationDays": integer,
                    "ingredients": [{
                      "name": "string",
                      "category": "MALT|HOP|YEAST|ADJUNCT|WATER_TREATMENT|OTHER",
                      "amount": number,
                      "unit": "kg|g|l|ml|pcs|tsp|tbsp",
                      "additionTime": "string or null",
                      "notes": "string or null",
                      "sortOrder": integer
                    }],
                    "steps": [{
                      "stepNumber": integer,
                      "phase": "PREPARATION|MASHING|LAUTERING|BOILING|COOLING|FERMENTATION|CONDITIONING|PACKAGING",
                      "title": "string",
                      "instructions": "string",
                      "durationMin": integer or null,
                      "targetTempC": number or null,
                      "timerRequired": boolean,
                      "notes": "string or null"
                    }]
                  }]
                }
                """;
    }

    private String buildUserMessage(TasteProfileRequest p) {
        var sb = new StringBuilder("Find 2-4 real homebrew recipes matching these preferences:\n");

        if (p.style() != null && !p.style().isBlank()) {
            sb.append("Style: ").append(p.style()).append("\n");
        }
        if (p.bitternessLevel() != null) {
            sb.append("Bitterness: ").append(p.bitternessLevel())
              .append("/5 (").append(bitternessLabel(p.bitternessLevel())).append(")\n");
        }
        if (p.sweetnessLevel() != null) {
            sb.append("Sweetness: ").append(p.sweetnessLevel())
              .append("/5 (").append(sweetnessLabel(p.sweetnessLevel())).append(")\n");
        }
        if (p.colour() != null && !p.colour().isBlank()) {
            sb.append("Colour: ").append(p.colour()).append("\n");
        }
        if (p.targetAbvMin() != null || p.targetAbvMax() != null) {
            sb.append("ABV range: ")
              .append(p.targetAbvMin() != null ? p.targetAbvMin() + "%" : "any")
              .append(" - ")
              .append(p.targetAbvMax() != null ? p.targetAbvMax() + "%" : "any")
              .append("\n");
        }
        if (p.aromaNotes() != null && !p.aromaNotes().isEmpty()) {
            sb.append("Desired aromas: ").append(String.join(", ", p.aromaNotes())).append("\n");
        }
        sb.append("Batch volume: ").append(p.batchVolumeL()).append(" litres\n");
        if (p.additionalNotes() != null && !p.additionalNotes().isBlank()) {
            sb.append("Additional notes: ").append(p.additionalNotes()).append("\n");
        }
        sb.append("\nScale all ingredients to ").append(p.batchVolumeL())
          .append("L. Respond with ONLY the JSON object.");

        return sb.toString();
    }

    private String bitternessLabel(int level) {
        return switch (level) {
            case 1 -> "Very Low";
            case 2 -> "Low";
            case 3 -> "Medium";
            case 4 -> "High";
            default -> "Very High";
        };
    }

    private String sweetnessLabel(int level) {
        return switch (level) {
            case 1 -> "Very Dry";
            case 2 -> "Dry";
            case 3 -> "Medium";
            case 4 -> "Sweet";
            default -> "Very Sweet";
        };
    }
}
