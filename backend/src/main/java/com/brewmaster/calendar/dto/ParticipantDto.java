package com.brewmaster.calendar.dto;

import java.util.UUID;

public record ParticipantDto(
        UUID userId,
        String username,
        String displayName,
        String rsvp
) {}
