package com.brewmaster.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String token,
        long expiresIn,
        UserDto user
) {
    public record UserDto(
            UUID id,
            String username,
            String displayName,
            String role
    ) {}
}
