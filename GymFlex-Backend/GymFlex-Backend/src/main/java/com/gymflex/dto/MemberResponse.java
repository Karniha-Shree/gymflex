package com.gymflex.dto;

import com.gymflex.entity.Member;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MemberResponse(
        Long id,
        String name,
        String email,
        String phone,
        LocalDate dateOfBirth,
        String address,
        String emergencyContact,
        LocalDateTime createdAt) {

    public static MemberResponse from(Member m) {
        return new MemberResponse(m.getId(), m.getName(), m.getEmail(), m.getPhone(),
                m.getDateOfBirth(), m.getAddress(), m.getEmergencyContact(), m.getCreatedAt());
    }
}
