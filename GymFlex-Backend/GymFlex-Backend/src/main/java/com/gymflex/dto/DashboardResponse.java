package com.gymflex.dto;

public record DashboardResponse(
        long totalMembers,
        long activeMemberships,
        long expiredMemberships,
        long expiringSoon,
        long todayCheckIns,
        long currentMonthCheckIns) {
}
