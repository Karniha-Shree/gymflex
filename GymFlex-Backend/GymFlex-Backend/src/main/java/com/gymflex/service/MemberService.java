package com.gymflex.service;

import com.gymflex.dto.MemberRequest;
import com.gymflex.dto.MemberResponse;
import com.gymflex.entity.Member;
import com.gymflex.exception.DuplicateResourceException;
import com.gymflex.exception.ResourceNotFoundException;
import com.gymflex.repository.CheckInRepository;
import com.gymflex.repository.MemberRepository;
import com.gymflex.repository.MembershipRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final MembershipRepository membershipRepository;
    private final CheckInRepository checkInRepository;

    public MemberService(MemberRepository memberRepository,
                         MembershipRepository membershipRepository,
                         CheckInRepository checkInRepository) {
        this.memberRepository = memberRepository;
        this.membershipRepository = membershipRepository;
        this.checkInRepository = checkInRepository;
    }

    public MemberResponse create(MemberRequest request) {
        if (memberRepository.findByEmailIgnoreCase(request.email()).isPresent()) {
            throw new DuplicateResourceException("A member with email " + request.email() + " already exists");
        }
        Member member = new Member();
        apply(member, request);
        return MemberResponse.from(memberRepository.save(member));
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> list(String search) {
        Sort sort = Sort.by("name");
        List<Member> members;
        if (search == null || search.isBlank()) {
            members = memberRepository.findAll(sort);
        } else {
            String term = search.trim();
            members = memberRepository
                    .findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContaining(
                            term, term, term, sort);
        }
        return members.stream().map(MemberResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MemberResponse get(Long id) {
        return MemberResponse.from(find(id));
    }

    public MemberResponse update(Long id, MemberRequest request) {
        Member member = find(id);
        memberRepository.findByEmailIgnoreCase(request.email())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new DuplicateResourceException(
                            "A member with email " + request.email() + " already exists");
                });
        apply(member, request);
        return MemberResponse.from(memberRepository.save(member));
    }

    /** Deleting a member also removes that member's memberships and check-ins. */
    public void delete(Long id) {
        Member member = find(id);
        checkInRepository.deleteByMemberId(id);
        membershipRepository.deleteByMemberId(id);
        memberRepository.delete(member);
    }

    /** Shared helper used by other services (public so Spring transaction proxies handle it safely). */
    public Member find(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id " + id));
    }

    private void apply(Member member, MemberRequest r) {
        member.setName(r.name().trim());
        member.setEmail(r.email().trim());
        member.setPhone(r.phone().trim());
        member.setDateOfBirth(r.dateOfBirth());
        member.setAddress(r.address());
        member.setEmergencyContact(r.emergencyContact());
    }
}
