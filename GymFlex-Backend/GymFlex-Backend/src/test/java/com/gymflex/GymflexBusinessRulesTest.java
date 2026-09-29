package com.gymflex;

import com.gymflex.dto.AttendanceCountResponse;
import com.gymflex.dto.CheckInResponse;
import com.gymflex.dto.MembershipResponse;
import com.gymflex.dto.RenewRequest;
import com.gymflex.entity.CheckIn;
import com.gymflex.entity.Member;
import com.gymflex.entity.Membership;
import com.gymflex.entity.MembershipStatus;
import com.gymflex.entity.Plan;
import com.gymflex.exception.BusinessRuleException;
import com.gymflex.repository.CheckInRepository;
import com.gymflex.repository.MemberRepository;
import com.gymflex.repository.MembershipRepository;
import com.gymflex.repository.PlanRepository;
import com.gymflex.service.CheckInService;
import com.gymflex.service.MembershipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GymflexBusinessRulesTest {

    @Autowired private MemberRepository memberRepository;
    @Autowired private PlanRepository planRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private CheckInRepository checkInRepository;
    @Autowired private CheckInService checkInService;
    @Autowired private MembershipService membershipService;
    @Autowired private MockMvc mockMvc;

    private Plan monthly;

    @BeforeEach
    void cleanDatabase() {
        checkInRepository.deleteAll();
        membershipRepository.deleteAll();
        memberRepository.deleteAll();
        planRepository.deleteAll();
        monthly = savePlan("Monthly", 1);
    }

    // ---------- helpers ----------
    private Plan savePlan(String name, int months) {
        Plan p = new Plan();
        p.setName(name);
        p.setDescription("test plan");
        p.setDurationMonths(months);
        p.setPrice(new BigDecimal("1000.00"));
        p.setActive(true);
        return planRepository.save(p);
    }

    private Member saveMember(String email) {
        Member m = new Member();
        m.setName("Test " + email);
        m.setEmail(email);
        m.setPhone("9876543210");
        return memberRepository.save(m);
    }

    private Membership saveMembership(Member member, LocalDate start, LocalDate expiry, MembershipStatus status) {
        Membership ms = new Membership();
        ms.setMember(member);
        ms.setPlan(monthly);
        ms.setStartDate(start);
        ms.setExpiryDate(expiry);
        ms.setStatus(status);
        return membershipRepository.save(ms);
    }

    // ---------- 1. Active member can check in ----------
    @Test
    void activeMemberCanCheckIn() {
        LocalDate today = LocalDate.now();
        Member member = saveMember("active@test.com");
        saveMembership(member, today.minusDays(5), today.plusDays(25), MembershipStatus.ACTIVE);

        CheckInResponse response = checkInService.checkIn(member.getId());

        assertEquals(member.getId(), response.memberId());
        assertEquals(today, response.checkInDate());
        assertEquals(1, checkInRepository.count());
    }

    // ---------- 2. Expired membership cannot check in ----------
    @Test
    void expiredMembershipCannotCheckIn() {
        LocalDate today = LocalDate.now();
        Member member = saveMember("expired@test.com");
        saveMembership(member, today.minusDays(40), today.minusDays(8), MembershipStatus.ACTIVE);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> checkInService.checkIn(member.getId()));

        assertTrue(ex.getMessage().contains("expired"));
        assertEquals(0, checkInRepository.count(), "Nothing must be saved when the check-in is rejected");
    }

    // ---------- 3. Renewal extends from the existing expiry date ----------
    @Test
    void renewalExtendsFromCurrentExpiryNotFromToday() {
        LocalDate today = LocalDate.now();
        LocalDate currentExpiry = today.plusDays(17);
        Member member = saveMember("renew@test.com");
        Membership ms = saveMembership(member, today.minusDays(13), currentExpiry, MembershipStatus.ACTIVE);

        MembershipResponse renewed = membershipService.renew(ms.getId(), null);

        assertEquals(currentExpiry.plusMonths(1), renewed.expiryDate());
        assertNotEquals(today.plusMonths(1), renewed.expiryDate());
        assertEquals(MembershipStatus.ACTIVE, renewed.status());
    }

    @Test
    void renewalOfExpiredMembershipStartsFromToday() {
        LocalDate today = LocalDate.now();
        Member member = saveMember("renew-expired@test.com");
        Membership ms = saveMembership(member, today.minusDays(40), today.minusDays(10), MembershipStatus.EXPIRED);

        MembershipResponse renewed = membershipService.renew(ms.getId(), new RenewRequest(null));

        assertEquals(today.plusMonths(1), renewed.expiryDate());
        assertEquals(MembershipStatus.ACTIVE, renewed.status());
    }

    // ---------- 4. Expiring-soon query ----------
    @Test
    void expiringSoonReturnsOnlyMembershipsExpiringWithinSevenDays() {
        LocalDate today = LocalDate.now();
        Member soon = saveMember("soon@test.com");
        Member later = saveMember("later@test.com");
        Member gone = saveMember("gone@test.com");
        saveMembership(soon, today.minusDays(27), today.plusDays(3), MembershipStatus.ACTIVE);
        saveMembership(later, today.minusDays(5), today.plusDays(25), MembershipStatus.ACTIVE);
        saveMembership(gone, today.minusDays(40), today.minusDays(1), MembershipStatus.ACTIVE);

        List<MembershipResponse> result = membershipService.expiringSoon();

        assertEquals(1, result.size());
        assertEquals(soon.getId(), result.get(0).memberId());
    }

    // ---------- 5. Current-month attendance count ----------
    @Test
    void currentMonthAttendanceCountsOnlyThisMonth() {
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
        Member member = saveMember("attendance@test.com");
        saveCheckIn(member, firstOfMonth);
        saveCheckIn(member, firstOfMonth.plusDays(1));
        saveCheckIn(member, firstOfMonth.minusDays(1)); // previous month - must not be counted

        AttendanceCountResponse response = checkInService.currentMonthCount(member.getId());

        assertEquals(2, response.checkIns());
        assertEquals(firstOfMonth.getMonthValue(), response.month());
        assertEquals(firstOfMonth.getYear(), response.year());
    }

    private void saveCheckIn(Member member, LocalDate date) {
        CheckIn c = new CheckIn();
        c.setMember(member);
        c.setCheckInDate(date);
        c.setCheckInTime(LocalDateTime.of(date, java.time.LocalTime.of(7, 30)));
        checkInRepository.save(c);
    }

    // ---------- REST layer: clean JSON errors ----------
    @Test
    void expiredCheckInReturnsCleanJsonError() throws Exception {
        LocalDate today = LocalDate.now();
        Member member = saveMember("api-expired@test.com");
        saveMembership(member, today.minusDays(40), today.minusDays(8), MembershipStatus.ACTIVE);

        mockMvc.perform(post("/api/checkins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\": " + member.getId() + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("expired")));
    }

    @Test
    void invalidMemberReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"email\": \"not-an-email\", \"phone\": \"9876543210\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void dashboardEndpointResponds() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMembers").value(0));
    }
}
