package com.gymflex.service;

import com.gymflex.dto.PlanRequest;
import com.gymflex.dto.PlanResponse;
import com.gymflex.entity.Plan;
import com.gymflex.exception.DuplicateResourceException;
import com.gymflex.exception.ResourceNotFoundException;
import com.gymflex.repository.PlanRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PlanService {

    private final PlanRepository planRepository;

    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    public PlanResponse create(PlanRequest request) {
        if (planRepository.findByNameIgnoreCase(request.name().trim()).isPresent()) {
            throw new DuplicateResourceException("A plan named '" + request.name().trim() + "' already exists");
        }
        Plan plan = new Plan();
        apply(plan, request);
        return PlanResponse.from(planRepository.save(plan));
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> list() {
        return planRepository.findAll(Sort.by("durationMonths")).stream().map(PlanResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PlanResponse get(Long id) {
        return PlanResponse.from(find(id));
    }

    public PlanResponse update(Long id, PlanRequest request) {
        Plan plan = find(id);
        planRepository.findByNameIgnoreCase(request.name().trim())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new DuplicateResourceException(
                            "A plan named '" + request.name().trim() + "' already exists");
                });
        apply(plan, request);
        return PlanResponse.from(planRepository.save(plan));
    }

    public Plan find(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id " + id));
    }

    private void apply(Plan plan, PlanRequest r) {
        plan.setName(r.name().trim());
        plan.setDescription(r.description());
        plan.setDurationMonths(r.durationMonths());
        plan.setPrice(r.price());
        plan.setActive(r.active() == null || r.active());
    }
}
