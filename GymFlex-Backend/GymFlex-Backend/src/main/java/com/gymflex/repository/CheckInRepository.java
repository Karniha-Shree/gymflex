package com.gymflex.repository;

import com.gymflex.entity.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CheckInRepository extends JpaRepository<CheckIn, Long> {

    List<CheckIn> findByMemberIdOrderByCheckInTimeDesc(Long memberId);

    boolean existsByMemberIdAndCheckInDate(Long memberId, LocalDate date);

    long countByMemberIdAndCheckInDateBetween(Long memberId, LocalDate from, LocalDate to);

    long countByCheckInDate(LocalDate date);

    long countByCheckInDateBetween(LocalDate from, LocalDate to);

    void deleteByMemberId(Long memberId);
}
