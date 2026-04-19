package com.brewmaster.brew;

import com.brewmaster.brew.dto.*;
import com.brewmaster.recipe.RecipeService;
import com.brewmaster.recipe.dto.IngredientDto;
import com.brewmaster.recipe.dto.ScaleRequest;
import com.brewmaster.recipe.dto.ScaledRecipeResponse;
import com.brewmaster.recipe.dto.StepDto;
import com.brewmaster.user.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BrewSessionService {

    private static final BigDecimal DEFAULT_BOIL_OFF = BigDecimal.TEN;
    private static final BigDecimal DEFAULT_WATER_GRAIN = new BigDecimal("3.0");

    private final BrewSessionRepository sessionRepository;
    private final BrewSessionStepLogRepository stepLogRepository;
    private final RecipeService recipeService;
    private final ObjectMapper objectMapper;

    public BrewSessionService(BrewSessionRepository sessionRepository,
                              BrewSessionStepLogRepository stepLogRepository,
                              RecipeService recipeService,
                              ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.stepLogRepository = stepLogRepository;
        this.recipeService = recipeService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public BrewSessionResponse startSession(StartSessionRequest req, User user) {
        BigDecimal boilOff = req.boilOffRatePercent() != null ? req.boilOffRatePercent() : DEFAULT_BOIL_OFF;
        BigDecimal wgRatio = req.waterToGrainRatio() != null ? req.waterToGrainRatio() : DEFAULT_WATER_GRAIN;

        ScaledRecipeResponse scaled = recipeService.scaleRecipe(
                req.recipeId(), new ScaleRequest(req.targetVolumeL(), boilOff, wgRatio));

        String ingredientsJson;
        String stepsJson;
        try {
            ingredientsJson = objectMapper.writeValueAsString(scaled.recipe().ingredients());
            stepsJson = objectMapper.writeValueAsString(scaled.recipe().steps());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize recipe snapshot");
        }

        BrewSession session = new BrewSession(
                req.recipeId(), req.eventId(), req.targetVolumeL(), user.getId(),
                ingredientsJson, stepsJson, boilOff, wgRatio,
                scaled.strikeWaterL(), scaled.spargeVolumeL(), scaled.preBoilVolumeL(),
                req.notes());

        BrewSession saved = sessionRepository.save(session);
        return toResponse(saved, scaled.recipe().ingredients(), scaled.recipe().steps(), List.of());
    }

    @Transactional(readOnly = true)
    public BrewSessionResponse getSession(UUID id) {
        BrewSession session = findOrThrow(id);
        List<IngredientDto> ingredients = deserializeIngredients(session.getScaledIngredientsJson());
        List<StepDto> steps = deserializeSteps(session.getScaledStepsJson());
        List<StepLogEntry> logs = session.getStepLogs().stream().map(this::toLogEntry).toList();
        return toResponse(session, ingredients, steps, logs);
    }

    @Transactional
    public BrewSessionResponse advanceStep(UUID id, AdvanceStepRequest req, User user) {
        BrewSession session = findOrThrow(id);
        requireOwner(session, user);

        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session is not in progress");
        }

        if (!stepLogRepository.existsBySessionIdAndStepNumber(id, req.stepNumber())) {
            stepLogRepository.save(new BrewSessionStepLog(id, req.stepNumber(), req.actualTempC(), req.notes()));
        }

        if (req.stepNumber() >= session.getCurrentStep()) {
            session.setCurrentStep(req.stepNumber() + 1);
        }

        BrewSession saved = sessionRepository.save(session);
        List<IngredientDto> ingredients = deserializeIngredients(saved.getScaledIngredientsJson());
        List<StepDto> steps = deserializeSteps(saved.getScaledStepsJson());
        List<StepLogEntry> logs = stepLogRepository.findBySessionIdOrderByStepNumberAsc(id)
                .stream().map(this::toLogEntry).toList();
        return toResponse(saved, ingredients, steps, logs);
    }

    @Transactional
    public BrewSessionResponse completeSession(UUID id, CompleteSessionRequest req, User user) {
        BrewSession session = findOrThrow(id);
        requireOwner(session, user);

        session.setStatus("COMPLETED");
        session.setCompletedAt(Instant.now());
        if (req.notes() != null) {
            session.setNotes(req.notes());
        }

        BrewSession saved = sessionRepository.save(session);
        List<IngredientDto> ingredients = deserializeIngredients(saved.getScaledIngredientsJson());
        List<StepDto> steps = deserializeSteps(saved.getScaledStepsJson());
        List<StepLogEntry> logs = saved.getStepLogs().stream().map(this::toLogEntry).toList();
        return toResponse(saved, ingredients, steps, logs);
    }

    @Transactional
    public void abandonSession(UUID id, User user) {
        BrewSession session = findOrThrow(id);
        requireOwner(session, user);
        session.setStatus("ABANDONED");
        sessionRepository.save(session);
    }

    // --- helpers ---

    private BrewSession findOrThrow(UUID id) {
        return sessionRepository.findByIdWithStepLogs(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brew session not found"));
    }

    private void requireOwner(BrewSession session, User user) {
        if (!session.getStartedBy().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the session owner can modify it");
        }
    }

    private List<IngredientDto> deserializeIngredients(String json) {
        if (json == null) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<IngredientDto>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<StepDto> deserializeSteps(String json) {
        if (json == null) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<StepDto>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private StepLogEntry toLogEntry(BrewSessionStepLog log) {
        return new StepLogEntry(log.getStepNumber(), log.getCompletedAt().toString(),
                log.getActualTempC(), log.getNotes());
    }

    private BrewSessionResponse toResponse(BrewSession s, List<IngredientDto> ingredients,
                                           List<StepDto> steps, List<StepLogEntry> logs) {
        return new BrewSessionResponse(
                s.getId(), s.getRecipeId(), s.getEventId(), s.getVolumeL(),
                s.getCurrentStep(), s.getStatus(), s.getNotes(),
                s.getStrikeWaterL(), s.getSpargeVolumeL(), s.getPreBoilVolumeL(),
                s.getBoilOffRatePercent(), s.getWaterToGrainRatio(),
                s.getStartedAt().toString(),
                s.getCompletedAt() != null ? s.getCompletedAt().toString() : null,
                ingredients, steps, logs);
    }
}
