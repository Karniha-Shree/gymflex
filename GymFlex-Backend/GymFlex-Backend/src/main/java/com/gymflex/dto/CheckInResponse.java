package com.gymflex.dto;

import com.gymflex.entity.CheckIn;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CheckInResponse(
        Long id,
        Long memberId,
        String memberName,
        LocalDateTime checkInTime,
        LocalDate checkInDate) {

    public static CheckInResponse from(CheckIn c) {
        return new CheckInResponse(c.getId(), c.getMember().getId(), c.getMember().getName(),
                c.getCheckInTime(), c.getCheckInDate());
    }
}
