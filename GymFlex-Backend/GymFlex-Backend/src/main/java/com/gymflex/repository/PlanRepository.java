package com.gymflex.repository;

import com.gymflex.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findByName(String name);

    Optional<Plan> findByNameIgnoreCase(String name);
}
