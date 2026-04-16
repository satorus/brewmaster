package com.brewmaster.calendar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CreateEventRequest(
        @NotBlank @Size(max = 200) String title,
        String description,
        @NotNull LocalDate brewDate,
        LocalTime startTime,
        @Size(max = 200) String location,
        UUID recipeId,
        List<UUID> invitedUserIds
) {}
