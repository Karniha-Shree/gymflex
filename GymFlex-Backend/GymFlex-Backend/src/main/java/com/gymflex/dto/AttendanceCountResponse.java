package com.gymflex.dto;

public record AttendanceCountResponse(
        Long memberId,
        String memberName,
        int year,
        int month,
        long checkIns) {
}
