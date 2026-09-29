package com.gymflex.service;

import com.gymflex.dto.DashboardResponse;
import com.gymflex.entity.MembershipStatus;
import com.gymflex.repository.CheckInRepository;
import com.gymflex.repository.MemberRepository;
import com.gymflex.repository.MembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final MemberRepository memberRepository;
    private final MembershipRepository membershipRepository;
    private final CheckInRepository checkInRepository;

    public DashboardService(MemberRepository memberRepository,
                            MembershipRepository membershipRepository,
                            CheckInRepository checkInRepository) {
        this.memberRepository = memberRepository;
        this.membershipRepository = membershipRepository;
        this.checkInRepository = checkInRepository;
    }

    public DashboardResponse summary() {
        LocalDate today = LocalDate.now();
        YearMonth month = YearMonth.now();

        long active = membershipRepository.countByStatusAndExpiryDateGreaterThanEqual(
                MembershipStatus.ACTIVE, today);
        // Expired = stored as EXPIRED, or still marked ACTIVE but the expiry date has passed.
        long expired = membershipRepository.countByStatus(MembershipStatus.EXPIRED)
                + membershipRepository.countByStatusAndExpiryDateLessThan(MembershipStatus.ACTIVE, today);
        long expiringSoon = membershipRepository
                .findByStatusAndExpiryDateBetweenOrderByExpiryDateAsc(
                        MembershipStatus.ACTIVE, today, today.plusDays(MembershipService.EXPIRING_SOON_DAYS))
                .size();

        return new DashboardResponse(
                memberRepository.count(),
                active,
                expired,
                expiringSoon,
                checkInRepository.countByCheckInDate(today),
                checkInRepository.countByCheckInDateBetween(month.atDay(1), month.atEndOfMonth()));
    }
}
