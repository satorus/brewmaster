package com.brewmaster.calendar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RsvpRequest(
        @NotBlank @Pattern(regexp = "ACCEPTED|DECLINED") String status
) {}
