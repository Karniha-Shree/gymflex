package com.gymflex.dto;

import com.gymflex.entity.Membership;
import com.gymflex.entity.MembershipStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record MembershipResponse(
        Long id,
        Long memberId,
        String memberName,
        Long planId,
        String planName,
        LocalDate startDate,
        LocalDate expiryDate,
        MembershipStatus status,
        long daysRemaining,
        LocalDateTime createdAt) {

    /**
     * The status shown to clients is the "effective" status: an ACTIVE membership whose
     * expiry date has passed is reported as EXPIRED even if the row was not updated yet.
     */
    public static MembershipResponse from(Membership m, LocalDate today) {
        MembershipStatus status = m.getStatus();
        if (status == MembershipStatus.ACTIVE && m.getExpiryDate().isBefore(today)) {
            status = MembershipStatus.EXPIRED;
        }
        long days = ChronoUnit.DAYS.between(today, m.getExpiryDate());
        return new MembershipResponse(
                m.getId(),
                m.getMember().getId(),
                m.getMember().getName(),
                m.getPlan().getId(),
                m.getPlan().getName(),
                m.getStartDate(),
                m.getExpiryDate(),
                status,
                days,
                m.getCreatedAt());
    }
}
