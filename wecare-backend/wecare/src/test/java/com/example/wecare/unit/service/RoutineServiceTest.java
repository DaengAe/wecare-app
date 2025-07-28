package com.example.wecare.unit.service;

import com.example.wecare.invitation.domain.Invitation;
import com.example.wecare.invitation.repository.InvitationRepository;
import com.example.wecare.member.domain.Member;
import com.example.wecare.member.domain.Role;
import com.example.wecare.member.repository.MemberRepository;
import com.example.wecare.routine.domain.RepeatDay;
import com.example.wecare.routine.domain.Routine;
import com.example.wecare.routine.domain.RoutineAlarmSetting;
import com.example.wecare.routine.domain.RoutineType;
import com.example.wecare.routine.dto.RoutineRequest;
import com.example.wecare.routine.dto.RoutineResponse;
import com.example.wecare.routine.dto.RoutineMemoRequest;
import com.example.wecare.routine.repository.RoutineAlarmSettingRepository;
import com.example.wecare.routine.repository.RoutineRepository;
import com.example.wecare.routine.service.RoutineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoutineServiceTest {

    @InjectMocks
    private RoutineService routineService;

    @Mock
    private RoutineRepository routineRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RoutineAlarmSettingRepository routineAlarmSettingRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private InvitationRepository invitationRepository;

    private Member guardian;
    private Member dependent;
    private Member otherMember;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);

        guardian = Member.builder().id(1L).role(Role.GUARDIAN).build();
        dependent = Member.builder().id(2L).role(Role.DEPENDENT).build();
        otherMember = Member.builder().id(3L).role(Role.GUARDIAN).build();

        // 모든 Member 객체의 연결 Set 초기화
        guardian.setDependentConnections(new HashSet<>());
        guardian.setGuardianConnections(new HashSet<>());
        dependent.setDependentConnections(new HashSet<>());
        dependent.setGuardianConnections(new HashSet<>());
        otherMember.setDependentConnections(new HashSet<>());
        otherMember.setGuardianConnections(new HashSet<>());

        // Invitation 엔티티를 통해 관계 설정
        Invitation guardianDependentConnection = new Invitation();
        guardianDependentConnection.setGuardian(guardian);
        guardianDependentConnection.setDependent(dependent);

        guardian.getDependentConnections().add(guardianDependentConnection);
        dependent.getGuardianConnections().add(guardianDependentConnection);
    }

    private void mockCurrentUser(Member member) {
        when(securityContext.getAuthentication()).thenReturn(new UsernamePasswordAuthenticationToken(member.getId().toString(), null));
        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
    }

    // --- 루틴 생성 테스트 ---
    @Test
    @DisplayName("성공: 보호자가 자신의 피보호자에게 루틴 생성 (알림 설정 포함)")
    void createRoutine_Success_WithAlarmSetting() {
        // given
        mockCurrentUser(guardian);
        when(memberRepository.findById(dependent.getId())).thenReturn(Optional.of(dependent));
        when(routineRepository.save(any(Routine.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(routineAlarmSettingRepository.save(any(RoutineAlarmSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoutineRequest request = RoutineRequest.builder()
                .type(RoutineType.MEDICATION)
                .title("약 먹기 루틴")
                .startTime(LocalDateTime.now())
                .repeat(true)
                .repeatDays(Arrays.asList(RepeatDay.MON, RepeatDay.WED, RepeatDay.FRI))
                .alertBeforeStartMin(10)
                .alertBeforeEndMin(5)
                .repeatIntervalMin(30)
                .build();

        // when
        routineService.createRoutine(dependent.getId(), request);

        // then
        ArgumentCaptor<Routine> routineCaptor = ArgumentCaptor.forClass(Routine.class);
        verify(routineRepository, times(1)).save(routineCaptor.capture());
        Routine savedRoutine = routineCaptor.getValue();

        assertThat(savedRoutine.getAlarmSetting()).isNotNull();
        assertThat(savedRoutine.getAlarmSetting().getAlertBeforeStartMin()).isEqualTo(10);
        assertThat(savedRoutine.getAlarmSetting().getAlertBeforeEndMin()).isEqualTo(5);
        assertThat(savedRoutine.getAlarmSetting().getRepeatIntervalMin()).isEqualTo(30);
        assertThat(savedRoutine.getRepeatDays()).containsExactlyInAnyOrder(RepeatDay.MON, RepeatDay.WED, RepeatDay.FRI);
    }

    @Test
    @DisplayName("성공: 보호자가 자신의 피보호자에게 루틴 생성 (알림 설정 없음)")
    void createRoutine_Success_NoAlarmSetting() {
        // given
        mockCurrentUser(guardian);
        when(memberRepository.findById(dependent.getId())).thenReturn(Optional.of(dependent));
        when(routineRepository.save(any(Routine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoutineRequest request = RoutineRequest.builder()
                .type(RoutineType.ACTIVITY)
                .title("운동 루틴")
                .startTime(LocalDateTime.now())
                .repeat(false)
                .build();

        // when
        routineService.createRoutine(dependent.getId(), request);

        // then
        ArgumentCaptor<Routine> routineCaptor = ArgumentCaptor.forClass(Routine.class);
        verify(routineRepository, times(1)).save(routineCaptor.capture());
        Routine savedRoutine = routineCaptor.getValue();

        assertThat(savedRoutine.getAlarmSetting()).isNull();
    }

    @Test
    @DisplayName("실패: 피보호자가 루틴 생성을 시도")
    void createRoutine_Fail_WhenDependentTries() {
        // given
        mockCurrentUser(dependent);
        RoutineRequest request = new RoutineRequest();

        // when & then
        assertThatThrownBy(() -> routineService.createRoutine(dependent.getId(), request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("루틴을 생성할 수 있는 권한이 없습니다. 보호자만 루틴을 생성할 수 있습니다.");
    }

    @Test
    @DisplayName("실패: 보호자가 자신의 피보호자가 아닌 다른 사용자에게 루틴 생성 시도")
    void createRoutine_Fail_WhenNotMyDependent() {
        // given
        mockCurrentUser(guardian);
        Member anotherDependent = Member.builder().id(4L).role(Role.DEPENDENT).build();
        // 이 피보호자는 현재 보호자와 연결되어 있지 않음

        when(memberRepository.findById(anotherDependent.getId())).thenReturn(Optional.of(anotherDependent));

        RoutineRequest request = new RoutineRequest();

        // when & then
        assertThatThrownBy(() -> routineService.createRoutine(anotherDependent.getId(), request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("해당 피보호자에 대한 루틴을 생성할 권한이 없습니다.");
    }

    // --- 루틴 수정 테스트 ---
    @Test
    @DisplayName("성공: 보호자가 자신의 루틴을 수정 (알림 설정 업데이트)")
    void updateRoutine_Success_UpdateAlarmSetting() {
        // given
        mockCurrentUser(guardian);
        Routine existingRoutine = Routine.builder()
                .id(1L)
                .guardian(guardian)
                .dependent(dependent)
                .type(RoutineType.MEDICATION)
                .title("기존 루틴")
                .startTime(LocalDateTime.now().minusHours(1))
                .is_repeat(true)
                .repeatDays(Arrays.asList(RepeatDay.MON))
                .guardianMemo("기존 보호자 메모")
                .dependentMemo("기존 피보호자 메모")
                .build();
        RoutineAlarmSetting existingAlarmSetting = RoutineAlarmSetting.builder()
                .id(100L)
                .routine(existingRoutine)
                .alertBeforeStartMin(10)
                .build();
        existingRoutine.setAlarmSetting(existingAlarmSetting);

        when(routineRepository.findById(existingRoutine.getId())).thenReturn(Optional.of(existingRoutine));
        when(routineRepository.save(any(Routine.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(routineAlarmSettingRepository.save(any(RoutineAlarmSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoutineRequest request = RoutineRequest.builder()
                .type(RoutineType.MEDICATION)
                .title("수정된 루틴")
                .startTime(LocalDateTime.now())
                .repeat(true)
                .repeatDays(Arrays.asList(RepeatDay.TUE, RepeatDay.THU))
                .alertBeforeStartMin(20)
                .alertBeforeEndMin(15)
                .repeatIntervalMin(60)
                .build();

        // when
        routineService.updateRoutine(existingRoutine.getId(), request);

        // then
        ArgumentCaptor<Routine> routineCaptor = ArgumentCaptor.forClass(Routine.class);
        verify(routineRepository, times(1)).save(routineCaptor.capture());
        Routine updatedRoutine = routineCaptor.getValue();

        assertThat(updatedRoutine.getTitle()).isEqualTo("수정된 루틴");
        assertThat(updatedRoutine.getAlarmSetting()).isNotNull();
        assertThat(updatedRoutine.getAlarmSetting().getAlertBeforeStartMin()).isEqualTo(20);
        assertThat(updatedRoutine.getAlarmSetting().getAlertBeforeEndMin()).isEqualTo(15);
        assertThat(updatedRoutine.getAlarmSetting().getRepeatIntervalMin()).isEqualTo(60);
        assertThat(updatedRoutine.getRepeatDays()).containsExactlyInAnyOrder(RepeatDay.TUE, RepeatDay.THU);
        assertThat(updatedRoutine.getGuardianMemo()).isEqualTo("기존 보호자 메모"); // 메모는 변경되지 않아야 함
        assertThat(updatedRoutine.getDependentMemo()).isEqualTo("기존 피보호자 메모"); // 메모는 변경되지 않아야 함
        verify(routineAlarmSettingRepository, times(1)).save(any(RoutineAlarmSetting.class));
    }

    @Test
    @DisplayName("성공: 보호자가 자신의 루틴을 수정 (알림 설정 추가)")
    void updateRoutine_Success_AddAlarmSetting() {
        // given
        mockCurrentUser(guardian);
        Routine existingRoutine = Routine.builder()
                .id(1L)
                .guardian(guardian)
                .dependent(dependent)
                .type(RoutineType.MEDICATION)
                .title("기존 루틴")
                .startTime(LocalDateTime.now().minusHours(1))
                .is_repeat(false)
                .guardianMemo("기존 보호자 메모")
                .dependentMemo("기존 피보호자 메모")
                .build();

        when(routineRepository.findById(existingRoutine.getId())).thenReturn(Optional.of(existingRoutine));
        when(routineRepository.save(any(Routine.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(routineAlarmSettingRepository.save(any(RoutineAlarmSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoutineRequest request = RoutineRequest.builder()
                .type(RoutineType.MEDICATION)
                .title("수정된 루틴")
                .startTime(LocalDateTime.now())
                .repeat(false)
                .alertBeforeStartMin(10)
                .build();

        // when
        routineService.updateRoutine(existingRoutine.getId(), request);

        // then
        ArgumentCaptor<Routine> routineCaptor = ArgumentCaptor.forClass(Routine.class);
        verify(routineRepository, times(1)).save(routineCaptor.capture());
        Routine updatedRoutine = routineCaptor.getValue();

        assertThat(updatedRoutine.getAlarmSetting()).isNotNull();
        assertThat(updatedRoutine.getAlarmSetting().getAlertBeforeStartMin()).isEqualTo(10);
        assertThat(updatedRoutine.getGuardianMemo()).isEqualTo("기존 보호자 메모"); // 메모는 변경되지 않아야 함
        assertThat(updatedRoutine.getDependentMemo()).isEqualTo("기존 피보호자 메모"); // 메모는 변경되지 않아야 함
        verify(routineAlarmSettingRepository, times(1)).save(any(RoutineAlarmSetting.class));
    }

    @Test
    @DisplayName("성공: 보호자가 자신의 루틴을 수정 (알림 설정 제거)")
    void updateRoutine_Success_RemoveAlarmSetting() {
        // given
        mockCurrentUser(guardian);
        Routine existingRoutine = Routine.builder()
                .id(1L)
                .guardian(guardian)
                .dependent(dependent)
                .type(RoutineType.MEDICATION)
                .title("기존 루틴")
                .startTime(LocalDateTime.now().minusHours(1))
                .is_repeat(false)
                .guardianMemo("기존 보호자 메모")
                .dependentMemo("기존 피보호자 메모")
                .build();
        RoutineAlarmSetting existingAlarmSetting = RoutineAlarmSetting.builder()
                .id(100L)
                .routine(existingRoutine)
                .alertBeforeStartMin(10)
                .build();
        existingRoutine.setAlarmSetting(existingAlarmSetting);

        when(routineRepository.findById(existingRoutine.getId())).thenReturn(Optional.of(existingRoutine));
        when(routineRepository.save(any(Routine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoutineRequest request = RoutineRequest.builder()
                .type(RoutineType.MEDICATION)
                .title("수정된 루틴")
                .startTime(LocalDateTime.now())
                .repeat(false)
                .build();

        // when
        routineService.updateRoutine(existingRoutine.getId(), request);

        // then
        ArgumentCaptor<Routine> routineCaptor = ArgumentCaptor.forClass(Routine.class);
        verify(routineRepository, times(1)).save(routineCaptor.capture());
        Routine updatedRoutine = routineCaptor.getValue();

        assertThat(updatedRoutine.getAlarmSetting()).isNull();
        assertThat(updatedRoutine.getGuardianMemo()).isEqualTo("기존 보호자 메모"); // 메모는 변경되지 않아야 함
        assertThat(updatedRoutine.getDependentMemo()).isEqualTo("기존 피보호자 메모"); // 메모는 변경되지 않아야 함
        verify(routineAlarmSettingRepository, times(1)).delete(existingAlarmSetting);
    }



    // --- 루틴 조회 테스트 ---
    @Test
    @DisplayName("성공: 보호자가 자신의 루틴 목록 조회")
    void getRoutinesByGuardian_Success() {
        // given
        mockCurrentUser(guardian);
        Routine routine1 = Routine.builder().id(1L).guardian(guardian).dependent(dependent).build();
        Routine routine2 = Routine.builder().id(2L).guardian(guardian).dependent(dependent).build();
        when(routineRepository.findByGuardianId(guardian.getId())).thenReturn(Arrays.asList(routine1, routine2));

        // when
        List<com.example.wecare.routine.dto.RoutineResponse> routines = routineService.getRoutinesByGuardian();

        // then
        assertThat(routines).hasSize(2);
        verify(routineRepository, times(1)).findByGuardianId(guardian.getId());
    }

    @Test
    @DisplayName("성공: 피보호자가 자신의 루틴 목록 조회")
    void getRoutinesByDependent_Success() {
        // given
        mockCurrentUser(dependent);
        Routine routine1 = Routine.builder().id(1L).guardian(guardian).dependent(dependent).build();
        Routine routine2 = Routine.builder().id(2L).guardian(guardian).dependent(dependent).build();
        when(routineRepository.findByDependentId(dependent.getId())).thenReturn(Arrays.asList(routine1, routine2));

        // when
        List<com.example.wecare.routine.dto.RoutineResponse> routines = routineService.getRoutinesByDependent();

        // then
        assertThat(routines).hasSize(2);
        verify(routineRepository, times(1)).findByDependentId(dependent.getId());
    }

    @Test
    @DisplayName("성공: 보호자가 자신의 루틴 상세 조회")
    void getRoutineById_Success_Guardian() {
        // given
        mockCurrentUser(guardian);
        Routine routine = Routine.builder().id(1L).guardian(guardian).dependent(dependent).build();
        when(routineRepository.findById(routine.getId())).thenReturn(Optional.of(routine));

        // when
        com.example.wecare.routine.dto.RoutineResponse response = routineService.getRoutineById(routine.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(routine.getId());
    }

    @Test
    @DisplayName("성공: 피보호자가 자신의 루틴 상세 조회")
    void getRoutineById_Success_Dependent() {
        // given
        mockCurrentUser(dependent);
        Routine routine = Routine.builder().id(1L).guardian(guardian).dependent(dependent).build();
        when(routineRepository.findById(routine.getId())).thenReturn(Optional.of(routine));

        // when
        com.example.wecare.routine.dto.RoutineResponse response = routineService.getRoutineById(routine.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(routine.getId());
    }

    @Test
    @DisplayName("실패: 제3자가 특정 루틴 정보 조회 시도")
    void getRoutineById_Fail_WhenAccessedByOthers() {
        // given
        mockCurrentUser(otherMember);
        Routine routine = Routine.builder().id(1L).guardian(guardian).dependent(dependent).build();
        when(routineRepository.findById(routine.getId())).thenReturn(Optional.of(routine));

        // when & then
        assertThatThrownBy(() -> routineService.getRoutineById(routine.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("해당 루틴에 접근할 권한이 없습니다.");
    }

    // --- 루틴 삭제 테스트 ---
    @Test
    @DisplayName("성공: 보호자가 자신의 루틴 삭제")
    void deleteRoutine_Success() {
        // given
        mockCurrentUser(guardian);
        Routine routine = Routine.builder().id(1L).guardian(guardian).dependent(dependent).build();
        when(routineRepository.findById(routine.getId())).thenReturn(Optional.of(routine));

        // when
        routineService.deleteRoutine(routine.getId());

        // then
        verify(routineRepository, times(1)).delete(routine);
    }

    @Test
    @DisplayName("실패: 피보호자가 루틴 삭제 시도")
    void deleteRoutine_Fail_WhenDependentTries() {
        // given
        mockCurrentUser(dependent);
        Routine routine = Routine.builder().id(1L).guardian(guardian).dependent(dependent).build();
        when(routineRepository.findById(routine.getId())).thenReturn(Optional.of(routine));

        // when & then
        assertThatThrownBy(() -> routineService.deleteRoutine(routine.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("루틴을 삭제할 권한이 없습니다.");
    }



    // --- 루틴 완료 테스트 ---
    @Test
    @DisplayName("성공: 피보호자가 자신의 루틴을 완료 처리")
    void completeRoutine_Success() {
        // given
        mockCurrentUser(dependent);
        Routine routine = Routine.builder().id(1L).guardian(guardian).dependent(dependent).completed(false).build();
        when(routineRepository.findById(routine.getId())).thenReturn(Optional.of(routine));

        // when
        routineService.completeRoutine(routine.getId());

        // then
        ArgumentCaptor<Routine> routineCaptor = ArgumentCaptor.forClass(Routine.class);
        verify(routineRepository, times(1)).save(routineCaptor.capture());
        Routine savedRoutine = routineCaptor.getValue();
        assertThat(savedRoutine.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("실패: 보호자가 루틴 완료 처리 시도")
    void completeRoutine_Fail_WhenGuardianTries() {
        // given
        mockCurrentUser(guardian);
        Routine routine = Routine.builder().id(1L).guardian(guardian).dependent(dependent).build();
        when(routineRepository.findById(routine.getId())).thenReturn(Optional.of(routine));

        // when & then
        assertThatThrownBy(() -> routineService.completeRoutine(routine.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("루틴을 완료할 수 있는 권한이 없습니다. 피보호자만 가능합니다.");
    }

    // --- 메모 업데이트 테스트 ---
    @Test
    @DisplayName("성공: 보호자가 자신의 루틴의 보호자 메모를 수정")
    void updateRoutineMemo_Success_GuardianUpdatesGuardianMemo() {
        // given
        mockCurrentUser(guardian);
        Routine existingRoutine = Routine.builder()
                .id(1L)
                .guardian(guardian)
                .dependent(dependent)
                .guardianMemo("기존 보호자 메모")
                .dependentMemo("기존 피보호자 메모")
                .build();
        when(routineRepository.findById(existingRoutine.getId())).thenReturn(Optional.of(existingRoutine));
        when(routineRepository.save(any(Routine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoutineMemoRequest request = new RoutineMemoRequest();
        request.setGuardianMemo("새로운 보호자 메모");

        // when
        routineService.updateRoutineMemo(existingRoutine.getId(), request);

        // then
        ArgumentCaptor<Routine> routineCaptor = ArgumentCaptor.forClass(Routine.class);
        verify(routineRepository, times(1)).save(routineCaptor.capture());
        Routine updatedRoutine = routineCaptor.getValue();

        assertThat(updatedRoutine.getGuardianMemo()).isEqualTo("새로운 보호자 메모");
        assertThat(updatedRoutine.getDependentMemo()).isEqualTo("기존 피보호자 메모"); // 피보호자 메모는 변경되지 않아야 함
    }

    @Test
    @DisplayName("성공: 피보호자가 자신의 루틴의 피보호자 메모를 수정")
    void updateRoutineMemo_Success_DependentUpdatesDependentMemo() {
        // given
        mockCurrentUser(dependent);
        Routine existingRoutine = Routine.builder()
                .id(1L)
                .guardian(guardian)
                .dependent(dependent)
                .guardianMemo("기존 보호자 메모")
                .dependentMemo("기존 피보호자 메모")
                .build();
        when(routineRepository.findById(existingRoutine.getId())).thenReturn(Optional.of(existingRoutine));
        when(routineRepository.save(any(Routine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoutineMemoRequest request = new RoutineMemoRequest();
        request.setDependentMemo("새로운 피보호자 메모");

        // when
        routineService.updateRoutineMemo(existingRoutine.getId(), request);

        // then
        ArgumentCaptor<Routine> routineCaptor = ArgumentCaptor.forClass(Routine.class);
        verify(routineRepository, times(1)).save(routineCaptor.capture());
        Routine updatedRoutine = routineCaptor.getValue();

        assertThat(updatedRoutine.getGuardianMemo()).isEqualTo("기존 보호자 메모"); // 보호자 메모는 변경되지 않아야 함
        assertThat(updatedRoutine.getDependentMemo()).isEqualTo("새로운 피보호자 메모");
    }

    @Test
    @DisplayName("실패: 보호자가 피보호자 메모를 수정 시도")
    void updateRoutineMemo_Fail_GuardianTriesToUpdateDependentMemo() {
        // given
        mockCurrentUser(guardian);
        Routine existingRoutine = Routine.builder()
                .id(1L)
                .guardian(guardian)
                .dependent(dependent)
                .build();
        when(routineRepository.findById(existingRoutine.getId())).thenReturn(Optional.of(existingRoutine));

        RoutineMemoRequest request = new RoutineMemoRequest();
        request.setDependentMemo("보호자가 수정하려는 피보호자 메모");

        // when & then
        assertThatThrownBy(() -> routineService.updateRoutineMemo(existingRoutine.getId(), request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("보호자는 피보호자 메모를 수정할 수 없습니다.");
    }

    @Test
    @DisplayName("실패: 피보호자가 보호자 메모를 수정 시도")
    void updateRoutineMemo_Fail_DependentTriesToUpdateGuardianMemo() {
        // given
        mockCurrentUser(dependent);
        Routine existingRoutine = Routine.builder()
                .id(1L)
                .guardian(guardian)
                .dependent(dependent)
                .build();
        when(routineRepository.findById(existingRoutine.getId())).thenReturn(Optional.of(existingRoutine));

        RoutineMemoRequest request = new RoutineMemoRequest();
        request.setGuardianMemo("피보호자가 수정하려는 보호자 메모");

        // when & then
        assertThatThrownBy(() -> routineService.updateRoutineMemo(existingRoutine.getId(), request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("피보호자는 보호자 메모를 수정할 수 없습니다.");
    }

    @Test
    @DisplayName("실패: 제3자가 메모 수정 시도")
    void updateRoutineMemo_Fail_OtherMemberTriesToUpdateMemo() {
        // given
        mockCurrentUser(otherMember);
        Routine existingRoutine = Routine.builder()
                .id(1L)
                .guardian(guardian)
                .dependent(dependent)
                .build();
        when(routineRepository.findById(existingRoutine.getId())).thenReturn(Optional.of(existingRoutine));

        RoutineMemoRequest request = new RoutineMemoRequest();
        request.setGuardianMemo("제3자가 수정하려는 메모");

        // when & then
        assertThatThrownBy(() -> routineService.updateRoutineMemo(existingRoutine.getId(), request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("해당 루틴에 접근할 권한이 없습니다.");
    }
}
