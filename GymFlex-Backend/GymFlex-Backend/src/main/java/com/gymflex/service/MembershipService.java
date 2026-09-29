package com.gymflex.service;

import com.gymflex.dto.MembershipRequest;
import com.gymflex.dto.MembershipResponse;
import com.gymflex.dto.RenewRequest;
import com.gymflex.entity.Member;
import com.gymflex.entity.Membership;
import com.gymflex.entity.MembershipStatus;
import com.gymflex.entity.Plan;
import com.gymflex.exception.BusinessRuleException;
import com.gymflex.exception.ResourceNotFoundException;
import com.gymflex.repository.MembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class MembershipService {

    /** Memberships expiring within this many days are "expiring soon". */
    public static final int EXPIRING_SOON_DAYS = 7;

    private final MembershipRepository membershipRepository;
    private final MemberService memberService;
    private final PlanService planService;

    public MembershipService(MembershipRepository membershipRepository,
                             MemberService memberService,
                             PlanService planService) {
        this.membershipRepository = membershipRepository;
        this.memberService = memberService;
        this.planService = planService;
    }

    public MembershipResponse create(MembershipRequest request) {
        Member member = memberService.find(request.memberId());
        Plan plan = planService.find(request.planId());
        requireActivePlan(plan);

        LocalDate today = LocalDate.now();
        if (membershipRepository.existsByMemberIdAndStatusAndExpiryDateGreaterThanEqual(
                member.getId(), MembershipStatus.ACTIVE, today)) {
            throw new BusinessRuleException(
                    "This member already has an active membership. Renew it instead of creating a new one.");
        }

        LocalDate start = request.startDate();
        LocalDate expiry = request.expiryDate() != null
                ? request.expiryDate()
                : start.plusMonths(plan.getDurationMonths());
        if (expiry.isBefore(start)) {
            throw new BusinessRuleException("Expiry date cannot be before the start date");
        }

        Membership membership = new Membership();
        membership.setMember(member);
        membership.setPlan(plan);
        membership.setStartDate(start);
        membership.setExpiryDate(expiry);
        membership.setStatus(MembershipStatus.ACTIVE);
        return toResponse(membershipRepository.save(membership));
    }

    @Transactional(readOnly = true)
    public List<MembershipResponse> list() {
        return membershipRepository.findAllByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MembershipResponse get(Long id) {
        return toResponse(find(id));
    }

    /**
     * RULE 2 - renewal.
     * - Not yet expired: extend from the CURRENT expiry date (NOT from today).
     *   e.g. expiry 15 Oct + 1 month -> 15 Nov.
     * - Already expired: the new period starts today.
     * - Cancelled memberships cannot be renewed.
     */
    public MembershipResponse renew(Long id, RenewRequest request) {
        Membership membership = find(id);
        if (membership.getStatus() == MembershipStatus.CANCELLED) {
            throw new BusinessRuleException(
                    "A cancelled membership cannot be renewed. Create a new membership instead.");
        }

        Plan plan = membership.getPlan();
        if (request != null && request.planId() != null) {
            plan = planService.find(request.planId());
            membership.setPlan(plan);
        }
        requireActivePlan(plan);

        LocalDate today = LocalDate.now();
        LocalDate base;
        if (!membership.getExpiryDate().isBefore(today)) {
            base = membership.getExpiryDate();      // still valid: extend from current expiry
        } else {
            base = today;                           // already expired: restart from today
            membership.setStartDate(today);
        }
        membership.setExpiryDate(base.plusMonths(plan.getDurationMonths()));
        membership.setStatus(MembershipStatus.ACTIVE);
        return toResponse(membershipRepository.save(membership));
    }

    /** RULE 3 - ACTIVE memberships expiring from today up to and including today + 7 days. */
    @Transactional(readOnly = true)
    public List<MembershipResponse> expiringSoon() {
        LocalDate today = LocalDate.now();
        return membershipRepository
                .findByStatusAndExpiryDateBetweenOrderByExpiryDateAsc(
                        MembershipStatus.ACTIVE, today, today.plusDays(EXPIRING_SOON_DAYS))
                .stream().map(this::toResponse).toList();
    }

    private Membership find(Long id) {
        return membershipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found with id " + id));
    }

    private void requireActivePlan(Plan plan) {
        if (!plan.isActive()) {
            throw new BusinessRuleException("Plan '" + plan.getName() + "' is not active");
        }
    }

    private MembershipResponse toResponse(Membership m) {
        return MembershipResponse.from(m, LocalDate.now());
    }
}
