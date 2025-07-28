package com.example.wecare.unit.service;

import com.example.wecare.invitation.domain.Invitation;
import com.example.wecare.member.domain.Member;
import com.example.wecare.member.domain.Role;
import com.example.wecare.member.dto.MemberResponse;
import com.example.wecare.member.repository.MemberRepository;
import com.example.wecare.member.service.MemberService;
import com.example.wecare.common.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    // SecurityUtil은 static 메소드를 가지고 있으므로 Mockito.mockStatic으로 처리
    // @Mock private SecurityUtil securityUtil; // 더 이상 필요 없음

    @Mock
    private EntityManager entityManager;

    private Member guardian1;
    private Member guardian2;
    private Member dependent1;
    private Member dependent2;

    @BeforeEach
    void setUp() {
        guardian1 = Member.builder().id(1L).username("guardian1").name("보호자1").role(Role.GUARDIAN).birthDate(LocalDate.of(1980, 1, 1)).build();
        guardian2 = Member.builder().id(2L).username("guardian2").name("보호자2").role(Role.GUARDIAN).birthDate(LocalDate.of(1985, 5, 5)).build();
        dependent1 = Member.builder().id(3L).username("dependent1").name("피보호자1").role(Role.DEPENDENT).birthDate(LocalDate.of(2000, 10, 10)).build();
        dependent2 = Member.builder().id(4L).username("dependent2").name("피보호자2").role(Role.DEPENDENT).birthDate(LocalDate.of(2005, 12, 12)).build();

        // 관계 설정
        // guardian1 -> dependent1
        Invitation conn1 = new Invitation();
        conn1.setGuardian(guardian1);
        conn1.setDependent(dependent1);
        conn1.setActive(true); // 활성 상태 설정
        Set<Invitation> g1_dep_conns = new HashSet<>();
        g1_dep_conns.add(conn1);
        guardian1.setDependentConnections(g1_dep_conns);
        Set<Invitation> d1_gua_conns = new HashSet<>();
        d1_gua_conns.add(conn1);
        dependent1.setGuardianConnections(d1_gua_conns);

        // guardian1 -> dependent2
        Invitation conn2 = new Invitation();
        conn2.setGuardian(guardian1);
        conn2.setDependent(dependent2);
        conn2.setActive(true); // 활성 상태 설정
        g1_dep_conns.add(conn2);
        guardian1.setDependentConnections(g1_dep_conns);
        Set<Invitation> d2_gua_conns = new HashSet<>();
        d2_gua_conns.add(conn2);
        dependent2.setGuardianConnections(d2_gua_conns);

        // guardian2 -> dependent1 (dependent1이 여러 보호자를 가질 수 있음을 테스트)
        Invitation conn3 = new Invitation();
        conn3.setGuardian(guardian2);
        conn3.setDependent(dependent1);
        conn3.setActive(true); // 활성 상태 설정
        Set<Invitation> g2_dep_conns = new HashSet<>();
        g2_dep_conns.add(conn3);
        guardian2.setDependentConnections(g2_dep_conns);
        d1_gua_conns.add(conn3);
        dependent1.setGuardianConnections(d1_gua_conns);

        // EntityManager.refresh() 호출 Mocking
        doNothing().when(entityManager).refresh(any());
    }

    private void mockCurrentUser(Member member) {
        // SecurityUtil.getCurrentMemberId()는 static 메소드이므로 mockStatic으로 처리
        // 이 메소드는 테스트 메소드 내에서 try-with-resources로 감싸져야 합니다.
        // when(securityUtil.getCurrentMemberId()).thenReturn(member.getId()); // 더 이상 필요 없음
        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
    }

    @Test
    @DisplayName("성공: 현재 로그인된 사용자 정보 조회")
    void getMe_Success() {
        // given
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(guardian1.getId());
            mockCurrentUser(guardian1);

            // when
            MemberResponse response = memberService.getMe();

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(guardian1.getId());
            assertThat(response.getUsername()).isEqualTo(guardian1.getUsername());
            assertThat(response.getName()).isEqualTo(guardian1.getName());
        }
    }

    @Test
    @DisplayName("성공: 보호자가 자신의 피보호자 목록 조회")
    void getDependents_Success_Guardian() {
        // given
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(guardian1.getId());
            mockCurrentUser(guardian1);

            // when
            MemberResponse response = memberService.getMe();

            // then
            assertThat(response).isNotNull();
            assertThat(response.getRole()).isEqualTo(Role.GUARDIAN);
            assertThat(response.getDependents()).hasSize(2);
            assertThat(response.getDependents().stream().map(MemberResponse::getId).collect(Collectors.toList()))
                    .containsExactlyInAnyOrder(dependent1.getId(), dependent2.getId());
        }
    }

    @Test
    @DisplayName("실패: 피보호자가 피보호자 목록 조회 시도")
    void getDependents_Fail_DependentTries() {
        // given
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(dependent1.getId());
            mockCurrentUser(dependent1);

            // when
            MemberResponse response = memberService.getMe();

            // then
            assertThat(response).isNotNull();
            assertThat(response.getRole()).isEqualTo(Role.DEPENDENT);
            assertThat(response.getDependents()).isNull(); // 피보호자는 dependents 필드가 null이어야 함
        }
    }

    @Test
    @DisplayName("성공: 피보호자 정보 조회 시 보호자 목록 포함")
    void getMe_DependentWithGuardians() {
        // given
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(dependent1.getId());
            mockCurrentUser(dependent1);

            // when
            MemberResponse response = memberService.getMe();

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(dependent1.getId());
            assertThat(response.getGuardians()).hasSize(2);
            assertThat(response.getGuardians().stream().map(g -> g.getId()).collect(Collectors.toList()))
                    .containsExactlyInAnyOrder(guardian1.getId(), guardian2.getId());
        }
    }

    @Test
    @DisplayName("성공: 보호자 정보 조회 시 보호자 목록은 null")
    void getMe_GuardianWithoutGuardians() {
        // given
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(guardian1.getId());
            mockCurrentUser(guardian1);

            // when
            MemberResponse response = memberService.getMe();

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(guardian1.getId());
            assertThat(response.getGuardians()).isNull(); // 보호자는 guardians 필드가 null이어야 함
        }
    }
}