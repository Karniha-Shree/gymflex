package com.gymflex.controller;

import com.gymflex.dto.MembershipRequest;
import com.gymflex.dto.MembershipResponse;
import com.gymflex.dto.RenewRequest;
import com.gymflex.service.MembershipService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/memberships")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipResponse create(@Valid @RequestBody MembershipRequest request) {
        return membershipService.create(request);
    }

    @GetMapping
    public List<MembershipResponse> list() {
        return membershipService.list();
    }

    // Declared before "/{id}" for readability; Spring matches the literal path first anyway.
    @GetMapping("/expiring-soon")
    public List<MembershipResponse> expiringSoon() {
        return membershipService.expiringSoon();
    }

    @GetMapping("/{id}")
    public MembershipResponse get(@PathVariable Long id) {
        return membershipService.get(id);
    }

    /** Body is optional: send {"planId": 2} to renew with a different plan. */
    @PutMapping("/{id}/renew")
    public MembershipResponse renew(@PathVariable Long id,
                                    @RequestBody(required = false) RenewRequest request) {
        return membershipService.renew(id, request);
    }
}
