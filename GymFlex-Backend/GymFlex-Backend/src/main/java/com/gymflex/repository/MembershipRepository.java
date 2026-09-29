package com.gymflex.repository;

import com.gymflex.entity.Membership;
import com.gymflex.entity.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    List<Membership> findAllByOrderByIdDesc();

    List<Membership> findByMemberIdOrderByExpiryDateDesc(Long memberId);

    /** True if the member has a membership that is ACTIVE and covers the given day. */
    boolean existsByMemberIdAndStatusAndStartDateLessThanEqualAndExpiryDateGreaterThanEqual(
            Long memberId, MembershipStatus status, LocalDate startOnOrBefore, LocalDate expiryOnOrAfter);

    /** True if the member still has an unexpired ACTIVE membership. */
    boolean existsByMemberIdAndStatusAndExpiryDateGreaterThanEqual(
            Long memberId, MembershipStatus status, LocalDate expiryOnOrAfter);

    /** ACTIVE memberships whose expiry date is between the two dates (inclusive). */
    List<Membership> findByStatusAndExpiryDateBetweenOrderByExpiryDateAsc(
            MembershipStatus status, LocalDate from, LocalDate to);

    long countByStatusAndExpiryDateGreaterThanEqual(MembershipStatus status, LocalDate date);

    long countByStatusAndExpiryDateLessThan(MembershipStatus status, LocalDate date);

    long countByStatus(MembershipStatus status);

    void deleteByMemberId(Long memberId);
}
