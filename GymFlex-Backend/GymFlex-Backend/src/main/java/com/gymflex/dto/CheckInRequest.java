package com.gymflex.dto;

import jakarta.validation.constraints.NotNull;

public record CheckInRequest(
        @NotNull(message = "Member is required")
        Long memberId) {
}
