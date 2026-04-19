package com.brewmaster.ai;

import com.brewmaster.ai.dto.AiRecipeSearchResponse;
import com.brewmaster.ai.dto.TasteProfileRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeAiServiceTest {

    @Mock
    private AIClient aiClient;

    private RecipeAiService service;

    private static final String VALID_JSON = """
            {
              "recipes": [
                {
                  "name": "Pacific IPA",
                  "style": "American IPA",
                  "description": "A bright citrusy IPA.",
                  "sourceUrl": "https://homebrewtalk.com/recipe/pacific-ipa",
                  "baseVolumeL": 20,
                  "originalGravity": 1.062,
                  "finalGravity": 1.012,
                  "abv": 6.5,
                  "ibu": 65,
                  "srm": 6,
                  "mashTempC": 66.5,
                  "mashDurationMin": 60,
                  "boilDurationMin": 60,
                  "fermentationTempC": 19.0,
                  "fermentationDays": 14,
                  "ingredients": [
                    {"name":"Pale Malt","category":"MALT","amount":4.5,"unit":"kg","additionTime":null,"notes":null,"sortOrder":0}
                  ],
                  "steps": [
                    {"stepNumber":1,"phase":"MASHING","title":"Mash In","instructions":"Add grain to strike water.","durationMin":60,"targetTempC":66.5,"timerRequired":false,"notes":null}
                  ]
                }
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        service = new RecipeAiService(aiClient, new ObjectMapper());
    }

    @Test
    void findRecipes_parsesValidJsonResponseIntoRecipes() {
        when(aiClient.sendWithWebSearch(any())).thenReturn(VALID_JSON);

        AiRecipeSearchResponse result = service.findRecipes(buildProfile());

        assertThat(result.recipes()).hasSize(1);
        assertThat(result.recipes().get(0).name()).isEqualTo("Pacific IPA");
        assertThat(result.recipes().get(0).abv()).isEqualByComparingTo(new BigDecimal("6.5"));
        assertThat(result.recipes().get(0).ingredients()).hasSize(1);
        assertThat(result.recipes().get(0).steps()).hasSize(1);
    }

    @Test
    void findRecipes_handlesMarkdownFencedJson() {
        String fenced = "```json\n" + VALID_JSON + "\n```";
        when(aiClient.sendWithWebSearch(any())).thenReturn(fenced);

        AiRecipeSearchResponse result = service.findRecipes(buildProfile());

        assertThat(result.recipes()).hasSize(1);
        assertThat(result.recipes().get(0).style()).isEqualTo("American IPA");
    }

    @Test
    void findRecipes_propagates503WhenClientThrows() {
        when(aiClient.sendWithWebSearch(any()))
                .thenThrow(new AIClientException("AI overloaded"));

        assertThatThrownBy(() -> service.findRecipes(buildProfile()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void findRecipes_throws503WhenJsonUnparseable() {
        when(aiClient.sendWithWebSearch(any())).thenReturn("not valid json at all");

        assertThatThrownBy(() -> service.findRecipes(buildProfile()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    private TasteProfileRequest buildProfile() {
        return new TasteProfileRequest(
                "IPA", 4, 2, "Pale",
                new BigDecimal("5.5"), new BigDecimal("7.0"),
                List.of("Citrus", "Pine"),
                new BigDecimal("20"),
                "Something summery");
    }
}
