package com.gymflex.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PlanRequest(
        @NotBlank(message = "Plan name is required")
        @Size(max = 100, message = "Plan name must be at most 100 characters")
        String name,

        @Size(max = 255, message = "Description must be at most 255 characters")
        String description,

        @NotNull(message = "Duration (in months) is required")
        @Min(value = 1, message = "Duration must be at least 1 month")
        @Max(value = 120, message = "Duration must be at most 120 months")
        Integer durationMonths,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "Price can have up to 8 digits and 2 decimals")
        BigDecimal price,

        Boolean active) {
}
