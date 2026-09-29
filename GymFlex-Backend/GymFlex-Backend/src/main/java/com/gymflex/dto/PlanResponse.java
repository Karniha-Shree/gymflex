package com.gymflex.dto;

import com.gymflex.entity.Plan;

import java.math.BigDecimal;

public record PlanResponse(
        Long id,
        String name,
        String description,
        Integer durationMonths,
        BigDecimal price,
        boolean active) {

    public static PlanResponse from(Plan p) {
        return new PlanResponse(p.getId(), p.getName(), p.getDescription(),
                p.getDurationMonths(), p.getPrice(), p.isActive());
    }
}
