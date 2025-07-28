package com.example.wecare.member.service;

import com.example.wecare.common.util.SecurityUtil;
import com.example.wecare.invitation.domain.Invitation;
import com.example.wecare.member.domain.Member;
import com.example.wecare.member.domain.Role;
import com.example.wecare.member.dto.MemberResponse;
import com.example.wecare.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager; // EntityManager 임포트 추가

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final EntityManager entityManager; // EntityManager 주입

    // 현재 로그인한 사용자의 전체 정보(Member 엔티티 객체)를 데이터베이스에서 조회
    public Member getCurrentMember() { // private -> public 변경
        Long memberId = SecurityUtil.getCurrentMemberId(); // SecurityUtil을 통해 memberId 가져오기
        log.info("getCurrentMember (MemberService) - 조회할 사용자 ID: {}", memberId);
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("현재 로그인된 사용자를 찾을 수 없습니다. ID: " + memberId));
    }

    // 현재 로그인한 사용자 본인의 정보를 조회
    public MemberResponse getMe() {
        log.info("getMe - 메소드 호출됨");
        Member currentMember = getCurrentMember();
        // 영속성 컨텍스트에서 엔티티를 새로고침하여 최신 상태를 반영
        entityManager.refresh(currentMember);
        log.info("getMe - 현재 사용자 정보 조회 완료: {}", currentMember.getUsername());
        return convertToResponse(currentMember);
    }

    // 엔티티(Entity) → DTO(Data Transfer Object)
    private MemberResponse convertToResponse(Member member) {
        List<MemberResponse> guardians = null; // GuardianInfo -> MemberResponse로 변경
        List<MemberResponse> dependents = null;

        if (member.getRole() == Role.DEPENDENT) {
            log.debug("convertToResponse - 피보호자 역할, 보호자 목록 조회 시작");
            guardians = getMyGuardians(member);
            log.debug("convertToResponse - 보호자 목록 조회 완료: {}명", guardians != null ? guardians.size() : 0);
        } else if (member.getRole() == Role.GUARDIAN) {
            log.debug("convertToResponse - 보호자 역할, 피보호자 목록 조회 시작");
            dependents = getMyDependentsList(member);
            log.debug("convertToResponse - 피보호자 목록 조회 완료: {}명", dependents != null ? dependents.size() : 0);
        }

        return MemberResponse.builder()
                .id(member.getId())
                .username(member.getUsername())
                .name(member.getName())
                .gender(member.getGender())
                .birthDate(member.getBirthDate())
                .role(member.getRole())
                .guardians(guardians)
                .dependents(dependents)
                .build();
    }

    // 특정 피보호자에게 연결된 모든 보호자 정보를 조회
    private List<MemberResponse> getMyGuardians(Member dependent) { // GuardianInfo -> MemberResponse로 변경
        log.debug("getMyGuardians - 메소드 호출됨 (dependent ID: {})", dependent.getId());
        if (dependent.getRole() != Role.DEPENDENT) {
            log.warn("getMyGuardians - 호출된 멤버가 피보호자가 아님 (ID: {})", dependent.getId());
            return Collections.emptyList(); // null 대신 빈 리스트 반환
        }
        List<MemberResponse> result = dependent.getGuardianConnections().stream()
                .map(invitation -> {
                    Member guardianMember = invitation.getGuardian();
                    return MemberResponse.builder()
                            .id(guardianMember.getId())
                            .username(guardianMember.getUsername())
                            .name(guardianMember.getName())
                            .gender(guardianMember.getGender())
                            .birthDate(guardianMember.getBirthDate())
                            .role(guardianMember.getRole())
                            .isActive(invitation.isActive()) // isActive 추가
                            .build();
                })
                .collect(Collectors.toList());
        log.debug("getMyGuardians - 반환되는 보호자 수: {}", result.size());
        return result;
    }

    // 특정 보호자에게 연결된 모든 피보호자 정보를 조회
    private List<MemberResponse> getMyDependentsList(Member guardian) {
        log.debug("getMyDependentsList - 메소드 호출됨 (guardian ID: {})", guardian.getId());
        if (guardian.getRole() != Role.GUARDIAN) {
            log.warn("getMyDependentsList - 호출된 멤버가 보호자가 아님 (ID: {})", guardian.getId());
            return Collections.emptyList();
        }
        List<MemberResponse> result = guardian.getDependentConnections().stream()
                .map(invitation -> {
                    Member dependentMember = invitation.getDependent();
                    return MemberResponse.builder()
                            .id(dependentMember.getId())
                            .username(dependentMember.getUsername())
                            .name(dependentMember.getName())
                            .gender(dependentMember.getGender())
                            .birthDate(dependentMember.getBirthDate())
                            .role(dependentMember.getRole())
                            .isActive(invitation.isActive()) // isActive 추가
                            .build();
                })
                .collect(Collectors.toList());
        log.debug("getMyDependentsList - 반환되는 피보호자 수: {}", result.size());
        return result;
    }
}
