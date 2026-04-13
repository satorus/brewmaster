package com.brewmaster.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "must contain only letters, digits, or underscores")
        String username,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, message = "must be at least 8 characters")
        String password,

        String displayName
) {}
