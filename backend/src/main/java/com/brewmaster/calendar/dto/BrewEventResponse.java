package com.brewmaster.calendar.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record BrewEventResponse(
        UUID id,
        String title,
        String description,
        LocalDate brewDate,
        LocalTime startTime,
        String location,
        UUID recipeId,
        UUID createdBy,
        String createdAt,
        List<ParticipantDto> participants
) {}
