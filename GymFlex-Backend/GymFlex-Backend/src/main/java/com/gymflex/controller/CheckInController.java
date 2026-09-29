package com.gymflex.controller;

import com.gymflex.dto.AttendanceCountResponse;
import com.gymflex.dto.CheckInRequest;
import com.gymflex.dto.CheckInResponse;
import com.gymflex.service.CheckInService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/checkins")
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CheckInResponse checkIn(@Valid @RequestBody CheckInRequest request) {
        return checkInService.checkIn(request.memberId());
    }

    @GetMapping("/member/{memberId}")
    public List<CheckInResponse> listForMember(@PathVariable Long memberId) {
        return checkInService.listForMember(memberId);
    }

    @GetMapping("/member/{memberId}/current-month")
    public AttendanceCountResponse currentMonth(@PathVariable Long memberId) {
        return checkInService.currentMonthCount(memberId);
    }
}
