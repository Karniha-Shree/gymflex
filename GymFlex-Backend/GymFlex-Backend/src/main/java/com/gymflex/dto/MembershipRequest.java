package com.gymflex.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * expiryDate is optional: when omitted it is calculated as startDate + plan duration.
 */
public record MembershipRequest(
        @NotNull(message = "Member is required")
        Long memberId,

        @NotNull(message = "Plan is required")
        Long planId,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        LocalDate expiryDate) {
}
