package com.example.wecare.unit.service;

import com.example.wecare.invitation.domain.Invitation;
import com.example.wecare.invitation.domain.InvitationId;
import com.example.wecare.invitation.domain.RelationshipType;
import com.example.wecare.invitation.repository.InvitationRepository;
import com.example.wecare.invitation.service.InvitationService;
import com.example.wecare.member.domain.Member;
import com.example.wecare.member.domain.Role;
import com.example.wecare.member.repository.MemberRepository;
import com.example.wecare.member.service.MemberService; // MemberService 추가
import com.example.wecare.common.service.RedisService;
import jakarta.persistence.EntityManager; // EntityManager 임포트 추가
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @InjectMocks
    private InvitationService invitationService;

    @Mock
    private RedisService redisService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private MemberService memberService; // MemberService Mock 추가

    @Mock
    private EntityManager entityManager; // EntityManager Mock 추가

    private Member guardian;
    private Member dependent;
    private Member otherGuardian;

    @BeforeEach
    void setUp() {
        // SecurityContextHolder 관련 Mocking 제거

        guardian = Member.builder().id(1L).username("guardian1").name("보호자1").role(Role.GUARDIAN).birthDate(LocalDate.of(1980, 1, 1)).build();
        dependent = Member.builder().id(2L).username("dependent1").name("피보호자1").role(Role.DEPENDENT).birthDate(LocalDate.of(2000, 10, 10)).build();
        otherGuardian = Member.builder().id(3L).username("guardian2").name("보호자2").role(Role.GUARDIAN).birthDate(LocalDate.of(1985, 5, 5)).build();

        // EntityManager.refresh() 호출 Mocking
        lenient().doNothing().when(entityManager).refresh(any());
    }

    // mockAuthentication 메소드 제거

    private void mockMemberFindByIdWithPessimisticLock(Member member) {
        when(memberRepository.findByIdWithPessimisticLock(member.getId())).thenReturn(Optional.of(member));
    }

    @Test
    @DisplayName("역할에 관계없이 초대 코드 생성 성공")
    void generateInvitationCode_byAnyRole_success() {
        // given
        when(memberService.getCurrentMember()).thenReturn(guardian); // memberService Mocking

        // when
        String invitationCode = invitationService.generateInvitationCode();

        // then
        assertThat(invitationCode).isNotNull();
        assertThat(invitationCode.length()).isEqualTo(8);
        verify(redisService, times(1)).setValues(anyString(), eq(guardian.getId().toString()), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("피보호자가 보호자의 코드를 수락하여 연결 성공")
    void acceptInvitationCode_dependentAcceptsGuardianCode_success() {
        // given
        String validCode = "VALID123";
        when(memberService.getCurrentMember()).thenReturn(dependent); // memberService Mocking
        mockMemberFindByIdWithPessimisticLock(guardian);

        when(redisService.getValues("INVITE:" + validCode)).thenReturn(guardian.getId().toString());
        when(invitationRepository.existsByGuardianIdAndDependentIdAndIsActiveTrue(anyLong(), anyLong())).thenReturn(false);

        // when
        invitationService.acceptInvitationCode(validCode, RelationshipType.FRIEND);

        // then
        ArgumentCaptor<Invitation> invitationCaptor = ArgumentCaptor.forClass(Invitation.class);
        verify(invitationRepository, times(1)).save(invitationCaptor.capture());
        Invitation savedInvitation = invitationCaptor.getValue();

        assertThat(savedInvitation.getGuardian()).isEqualTo(guardian);
        assertThat(savedInvitation.getDependent()).isEqualTo(dependent);
        assertThat(savedInvitation.isActive()).isTrue();
        assertThat(savedInvitation.getRelationshipType()).isEqualTo(RelationshipType.FRIEND);
        verify(redisService, times(1)).deleteValues("INVITE:" + validCode);
    }
    
    @Test
    @DisplayName("보호자가 피보호자의 코드를 수락하여 연결 성공")
    void acceptInvitationCode_guardianAcceptsDependentCode_success() {
        // given
        String validCode = "VALID456";
        when(memberService.getCurrentMember()).thenReturn(guardian); // memberService Mocking
        mockMemberFindByIdWithPessimisticLock(dependent);

        when(redisService.getValues("INVITE:" + validCode)).thenReturn(dependent.getId().toString());
        when(invitationRepository.existsByGuardianIdAndDependentIdAndIsActiveTrue(anyLong(), anyLong())).thenReturn(false);

        // when
        invitationService.acceptInvitationCode(validCode, RelationshipType.PARENT);

        // then
        ArgumentCaptor<Invitation> invitationCaptor = ArgumentCaptor.forClass(Invitation.class);
        verify(invitationRepository, times(1)).save(invitationCaptor.capture());
        Invitation savedInvitation = invitationCaptor.getValue();

        assertThat(savedInvitation.getGuardian()).isEqualTo(guardian);
        assertThat(savedInvitation.getDependent()).isEqualTo(dependent);
        assertThat(savedInvitation.isActive()).isTrue();
        assertThat(savedInvitation.getRelationshipType()).isEqualTo(RelationshipType.PARENT);
        verify(redisService, times(1)).deleteValues("INVITE:" + validCode);
    }

    @Test
    @DisplayName("유효하지 않은 코드로 수락 시도 시 실패")
    void acceptInvitationCode_fail_withInvalidCode() {
        // given
        String invalidCode = "INVALID123";
        when(memberService.getCurrentMember()).thenReturn(dependent); // memberService Mocking

        when(redisService.getValues("INVITE:" + invalidCode)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> invitationService.acceptInvitationCode(invalidCode, RelationshipType.FRIEND))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 초대 코드입니다.");
    }

    @Test
    @DisplayName("이미 활성화된 연결이 존재할 경우 수락 시도 시 실패")
    void acceptInvitationCode_fail_whenAlreadyActiveConnected() {
        // given
        String validCode = "VALID123";
        when(memberService.getCurrentMember()).thenReturn(dependent); // memberService Mocking
        mockMemberFindByIdWithPessimisticLock(guardian);

        when(redisService.getValues("INVITE:" + validCode)).thenReturn(guardian.getId().toString());
        when(invitationRepository.existsByGuardianIdAndDependentIdAndIsActiveTrue(anyLong(), anyLong())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> invitationService.acceptInvitationCode(validCode, RelationshipType.FRIEND))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 연결된 관계입니다.");
    }
    
    @Test
    @DisplayName("동일한 역할끼리 연결 시도 시 실패")
    void acceptInvitationCode_fail_withSameRole() {
        // given
        String validCode = "VALID789";
        when(memberService.getCurrentMember()).thenReturn(guardian); // memberService Mocking
        mockMemberFindByIdWithPessimisticLock(otherGuardian);

        when(redisService.getValues("INVITE:" + validCode)).thenReturn(otherGuardian.getId().toString());

        // when & then
        assertThatThrownBy(() -> invitationService.acceptInvitationCode(validCode, RelationshipType.FRIEND))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("동일한 역할의 사용자와는 연결할 수 없습니다.");
    }

    @Test
    @DisplayName("자신의 초대 코드를 수락 시도 시 실패")
    void acceptInvitationCode_fail_withOwnCode() {
        // given
        String ownCode = "OWNCODE1";
        when(memberService.getCurrentMember()).thenReturn(guardian); // memberService Mocking

        when(redisService.getValues("INVITE:" + ownCode)).thenReturn(guardian.getId().toString());

        // when & then
        assertThatThrownBy(() -> invitationService.acceptInvitationCode(ownCode, RelationshipType.FRIEND))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자신의 초대 코드는 수락할 수 없습니다.");
    }

    @Test
    @DisplayName("연결 삭제 성공 - isActive가 false로 변경되어야 한다.")
    void deleteConnection_success() {
        // given
        when(memberService.getCurrentMember()).thenReturn(guardian); // memberService Mocking
        mockMemberFindByIdWithPessimisticLock(dependent);

        Invitation existingInvitation = Invitation.builder()
                .guardian(guardian)
                .dependent(dependent)
                .isActive(true)
                .relationshipType(RelationshipType.FRIEND)
                .build();

        when(invitationRepository.findById(new InvitationId(guardian.getId(), dependent.getId()))).thenReturn(Optional.of(existingInvitation));

        // when
        invitationService.deleteConnection(dependent.getId());

        // then
        ArgumentCaptor<Invitation> invitationCaptor = ArgumentCaptor.forClass(Invitation.class);
        verify(invitationRepository, times(1)).save(invitationCaptor.capture());
        Invitation savedInvitation = invitationCaptor.getValue();

        assertThat(savedInvitation.isActive()).isFalse();
    }

    @Test
    @DisplayName("연결 삭제 실패 - 존재하지 않는 연결")
    void deleteConnection_fail_notExist() {
        // given
        when(memberService.getCurrentMember()).thenReturn(guardian); // memberService Mocking
        mockMemberFindByIdWithPessimisticLock(dependent);

        when(invitationRepository.findById(new InvitationId(guardian.getId(), dependent.getId()))).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> invitationService.deleteConnection(dependent.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 연결입니다.");
    }

    @Test
    @DisplayName("연결 삭제 실패 - 이미 비활성화된 연결")
    void deleteConnection_fail_alreadyInactive() {
        // given
        when(memberService.getCurrentMember()).thenReturn(guardian); // memberService Mocking
        mockMemberFindByIdWithPessimisticLock(dependent);

        Invitation existingInvitation = Invitation.builder()
                .guardian(guardian)
                .dependent(dependent)
                .isActive(false)
                .relationshipType(RelationshipType.FRIEND)
                .build();

        when(invitationRepository.findById(new InvitationId(guardian.getId(), dependent.getId()))).thenReturn(Optional.of(existingInvitation));

        // when & then
        assertThatThrownBy(() -> invitationService.deleteConnection(dependent.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 비활성화된 연결입니다.");
    }
}