package com.gymflex.service;

import com.gymflex.dto.AttendanceCountResponse;
import com.gymflex.dto.CheckInResponse;
import com.gymflex.entity.CheckIn;
import com.gymflex.entity.Member;
import com.gymflex.entity.Membership;
import com.gymflex.entity.MembershipStatus;
import com.gymflex.exception.BusinessRuleException;
import com.gymflex.exception.DuplicateResourceException;
import com.gymflex.repository.CheckInRepository;
import com.gymflex.repository.MembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@Transactional
public class CheckInService {

    private final CheckInRepository checkInRepository;
    private final MembershipRepository membershipRepository;
    private final MemberService memberService;

    public CheckInService(CheckInRepository checkInRepository,
                          MembershipRepository membershipRepository,
                          MemberService memberService) {
        this.checkInRepository = checkInRepository;
        this.membershipRepository = membershipRepository;
        this.memberService = memberService;
    }

    /**
     * RULE 1 - the membership is validated here, in the service layer, BEFORE anything is saved.
     */
    public CheckInResponse checkIn(Long memberId) {
        Member member = memberService.find(memberId);
        LocalDate today = LocalDate.now();

        boolean hasValidMembership = membershipRepository
                .existsByMemberIdAndStatusAndStartDateLessThanEqualAndExpiryDateGreaterThanEqual(
                        memberId, MembershipStatus.ACTIVE, today, today);
        if (!hasValidMembership) {
            throw new BusinessRuleException(explainRejection(memberId, today));
        }

        if (checkInRepository.existsByMemberIdAndCheckInDate(memberId, today)) {
            throw new DuplicateResourceException(member.getName() + " has already checked in today");
        }

        CheckIn checkIn = new CheckIn();
        checkIn.setMember(member);
        LocalDateTime now = LocalDateTime.now();
        checkIn.setCheckInTime(now);
        checkIn.setCheckInDate(now.toLocalDate());
        return CheckInResponse.from(checkInRepository.save(checkIn));
    }

    @Transactional(readOnly = true)
    public List<CheckInResponse> listForMember(Long memberId) {
        memberService.find(memberId);
        return checkInRepository.findByMemberIdOrderByCheckInTimeDesc(memberId)
                .stream().map(CheckInResponse::from).toList();
    }

    /** RULE 4 - number of check-ins in the current calendar month. */
    @Transactional(readOnly = true)
    public AttendanceCountResponse currentMonthCount(Long memberId) {
        Member member = memberService.find(memberId);
        YearMonth month = YearMonth.now();
        long count = checkInRepository.countByMemberIdAndCheckInDateBetween(
                memberId, month.atDay(1), month.atEndOfMonth());
        return new AttendanceCountResponse(memberId, member.getName(),
                month.getYear(), month.getMonthValue(), count);
    }

    private String explainRejection(Long memberId, LocalDate today) {
        List<Membership> memberships = membershipRepository.findByMemberIdOrderByExpiryDateDesc(memberId);
        if (memberships.isEmpty()) {
            return "Check-in rejected: this member has no membership. Please create a membership first.";
        }
        Membership latest = memberships.get(0);
        if (latest.getStatus() == MembershipStatus.CANCELLED) {
            return "Check-in rejected: the membership was cancelled.";
        }
        if (latest.getExpiryDate().isBefore(today)) {
            return "Check-in rejected: membership expired on " + latest.getExpiryDate()
                    + ". Please renew the membership.";
        }
        if (latest.getStartDate().isAfter(today)) {
            return "Check-in rejected: membership starts on " + latest.getStartDate() + ".";
        }
        return "Check-in rejected: no active membership found.";
    }
}
