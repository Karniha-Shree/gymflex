package com.gymflex.config;

import com.gymflex.entity.CheckIn;
import com.gymflex.entity.Member;
import com.gymflex.entity.Membership;
import com.gymflex.entity.MembershipStatus;
import com.gymflex.entity.Plan;
import com.gymflex.repository.CheckInRepository;
import com.gymflex.repository.MemberRepository;
import com.gymflex.repository.MembershipRepository;
import com.gymflex.repository.PlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Inserts sample data for development.
 * Each table is seeded ONLY when it is empty, so restarting the app never creates duplicates.
 * Disable completely with: gymflex.seed.enabled=false
 */
@Component
@ConditionalOnProperty(name = "gymflex.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final PlanRepository planRepository;
    private final MemberRepository memberRepository;
    private final MembershipRepository membershipRepository;
    private final CheckInRepository checkInRepository;

    public DataSeeder(PlanRepository planRepository,
                      MemberRepository memberRepository,
                      MembershipRepository membershipRepository,
                      CheckInRepository checkInRepository) {
        this.planRepository = planRepository;
        this.memberRepository = memberRepository;
        this.membershipRepository = membershipRepository;
        this.checkInRepository = checkInRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (planRepository.count() == 0) {
            seedPlans();
        }
        if (memberRepository.count() == 0) {
            seedMembers();
        }
        if (membershipRepository.count() == 0) {
            seedMemberships();
        }
        if (checkInRepository.count() == 0) {
            seedCheckIns();
        }
        log.info("GymFlex seed check complete (plans={}, members={}, memberships={}, check-ins={})",
                planRepository.count(), memberRepository.count(),
                membershipRepository.count(), checkInRepository.count());
    }

    private void seedPlans() {
        savePlan("Monthly", "1 month access to all equipment", 1, "1000.00");
        savePlan("Quarterly", "3 months access, save 10%", 3, "2700.00");
        savePlan("Half Yearly", "6 months access, save 17%", 6, "5000.00");
        savePlan("Yearly", "12 months access, best value", 12, "9000.00");
    }

    private void savePlan(String name, String description, int months, String price) {
        Plan p = new Plan();
        p.setName(name);
        p.setDescription(description);
        p.setDurationMonths(months);
        p.setPrice(new BigDecimal(price));
        p.setActive(true);
        planRepository.save(p);
    }

    private void seedMembers() {
        saveMember("Arjun Kumar", "arjun.kumar@example.com", "9876500001", LocalDate.of(1998, 4, 12),
                "12 Gandhi Road, Tiruchengode", "Ramesh Kumar - 9876500101");
        saveMember("Priya Sharma", "priya.sharma@example.com", "9876500002", LocalDate.of(1999, 8, 23),
                "45 Lake View Street, Salem", "Anita Sharma - 9876500102");
        saveMember("Karthik Raj", "karthik.raj@example.com", "9876500003", LocalDate.of(1995, 1, 5),
                "7 Temple Street, Namakkal", "Raj Kumar - 9876500103");
        saveMember("Divya Lakshmi", "divya.lakshmi@example.com", "9876500004", LocalDate.of(2000, 11, 30),
                "88 Anna Nagar, Erode", "Lakshmi Devi - 9876500104");
        saveMember("Suresh Babu", "suresh.babu@example.com", "9876500005", LocalDate.of(1992, 6, 17),
                "3 Market Road, Tiruchengode", "Babu Rao - 9876500105");
        saveMember("Meena Iyer", "meena.iyer@example.com", "9876500006", LocalDate.of(1997, 2, 9),
                "21 Park Avenue, Coimbatore", "Gopal Iyer - 9876500106");
    }

    private void saveMember(String name, String email, String phone, LocalDate dob,
                            String address, String emergencyContact) {
        Member m = new Member();
        m.setName(name);
        m.setEmail(email);
        m.setPhone(phone);
        m.setDateOfBirth(dob);
        m.setAddress(address);
        m.setEmergencyContact(emergencyContact);
        memberRepository.save(m);
    }

    private void seedMemberships() {
        LocalDate today = LocalDate.now();
        // Healthy active memberships
        saveMembership("arjun.kumar@example.com", "Monthly", today.minusDays(10), today.plusDays(20), MembershipStatus.ACTIVE);
        saveMembership("priya.sharma@example.com", "Quarterly", today.minusDays(60), today.plusDays(30), MembershipStatus.ACTIVE);
        saveMembership("meena.iyer@example.com", "Yearly", today.minusDays(65), today.plusDays(300), MembershipStatus.ACTIVE);
        // Expiring soon (within 7 days)
        saveMembership("karthik.raj@example.com", "Monthly", today.minusDays(27), today.plusDays(3), MembershipStatus.ACTIVE);
        saveMembership("divya.lakshmi@example.com", "Half Yearly", today.minusMonths(6).plusDays(6), today.plusDays(6), MembershipStatus.ACTIVE);
        // Already expired
        saveMembership("suresh.babu@example.com", "Monthly", today.minusDays(40), today.minusDays(10), MembershipStatus.EXPIRED);
    }

    private void saveMembership(String email, String planName, LocalDate start, LocalDate expiry,
                                MembershipStatus status) {
        Member member = memberRepository.findByEmailIgnoreCase(email).orElse(null);
        Plan plan = planRepository.findByName(planName).orElse(null);
        if (member == null || plan == null) {
            return; // sample data not present (e.g. it was edited) - skip safely
        }
        Membership ms = new Membership();
        ms.setMember(member);
        ms.setPlan(plan);
        ms.setStartDate(start);
        ms.setExpiryDate(expiry);
        ms.setStatus(status);
        membershipRepository.save(ms);
    }

    private void seedCheckIns() {
        LocalDate today = LocalDate.now();
        saveCheckIns("arjun.kumar@example.com", today, today.minusDays(1), today.minusDays(3), today.minusDays(5));
        saveCheckIns("priya.sharma@example.com", today, today.minusDays(2));
        saveCheckIns("karthik.raj@example.com", today.minusDays(1), today.minusDays(4));
        saveCheckIns("meena.iyer@example.com", today, today.minusDays(1), today.minusDays(2));
    }

    private void saveCheckIns(String email, LocalDate... dates) {
        Member member = memberRepository.findByEmailIgnoreCase(email).orElse(null);
        if (member == null) {
            return;
        }
        for (LocalDate date : dates) {
            CheckIn c = new CheckIn();
            c.setMember(member);
            c.setCheckInDate(date);
            c.setCheckInTime(date.atTime(7, 30));
            checkInRepository.save(c);
        }
    }
}
