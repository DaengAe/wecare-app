package com.example.wecare.routine.service;

import com.example.wecare.invitation.domain.Invitation;
import com.example.wecare.member.domain.Member;
import com.example.wecare.member.domain.Role;
import com.example.wecare.member.repository.MemberRepository;
import com.example.wecare.routine.domain.Routine;
import com.example.wecare.routine.domain.RoutineAlarmSetting;
import com.example.wecare.routine.dto.RoutineRequest;
import com.example.wecare.routine.dto.RoutineResponse;
import com.example.wecare.routine.dto.RoutineMemoRequest;
import com.example.wecare.routine.repository.RoutineRepository;
import com.example.wecare.routine.repository.RoutineAlarmSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.wecare.routine.domain.SoundType; // SoundType 임포트 추가

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineService {

    private final RoutineRepository routineRepository;
    private final MemberRepository memberRepository;
    private final RoutineAlarmSettingRepository routineAlarmSettingRepository;

    private Long getCurrentMemberId() {
        return Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private Member getCurrentMember() {
        Long currentMemberId = getCurrentMemberId();
        return memberRepository.findById(currentMemberId)
                .orElseThrow(() -> new IllegalArgumentException("현재 로그인된 사용자를 찾을 수 없습니다."));
    }

    @Transactional
    public RoutineResponse createRoutine(Long dependentId, RoutineRequest request) {
        Member guardian = getCurrentMember();
        if (guardian.getRole() != Role.GUARDIAN) {
            throw new AccessDeniedException("루틴을 생성할 수 있는 권한이 없습니다. 보호자만 루틴을 생성할 수 있습니다.");
        }

        Member dependent = memberRepository.findById(dependentId)
                .orElseThrow(() -> new IllegalArgumentException("피보호자를 찾을 수 없습니다."));

        if (dependent.getRole() != Role.DEPENDENT) {
            throw new IllegalArgumentException("루틴 대상은 피보호자여야 합니다.");
        }

        boolean isConnected = guardian.getDependentConnections().stream()
                .anyMatch(conn -> conn.getDependent().getId().equals(dependentId));

        if (!isConnected) {
            throw new AccessDeniedException("해당 피보호자에 대한 루틴을 생성할 권한이 없습니다.");
        }

        Routine routine = Routine.builder()
                .guardian(guardian)
                .dependent(dependent)
                .type(request.getType())
                .title(request.getTitle())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .is_repeat(request.isRepeat())
                .repeatDays(request.getRepeatDays())
                .completed(false)
                .guardianMemo(request.getGuardianMemo()) // 보호자 메모 추가
                .dependentMemo(request.getDependentMemo()) // 피보호자 메모 추가
                .build();

        Routine savedRoutine = routineRepository.save(routine);

        // isEnabled가 true이거나 null인 경우에만 알림 설정을 생성
        if (Boolean.TRUE.equals(request.getIsEnabled()) || request.getIsEnabled() == null) {
            if (request.getAlertBeforeStartMin() != null ||
                request.getAlertBeforeEndMin() != null ||
                request.getRepeatIntervalMin() != null ||
                request.getSoundType() != null || // SoundType 추가
                request.getVoiceMessageUrl() != null) { // VoiceMessageUrl 추가
                RoutineAlarmSetting alarmSetting = RoutineAlarmSetting.builder()
                    .routine(savedRoutine)
                    .alertBeforeStartMin(request.getAlertBeforeStartMin())
                    .alertBeforeEndMin(request.getAlertBeforeEndMin())
                    .repeatIntervalMin(request.getRepeatIntervalMin())
                    .isEnabled(true) // 기본값 true
                    .soundType(request.getSoundType()) // SoundType 추가
                    .voiceMessageUrl(request.getVoiceMessageUrl()) // VoiceMessageUrl 추가
                    .build();
                routineAlarmSettingRepository.save(alarmSetting);
                savedRoutine.setAlarmSetting(alarmSetting);
            }
        }

        return convertToResponse(savedRoutine);
    }

    public List<RoutineResponse> getRoutinesByGuardian() {
        Member currentMember = getCurrentMember();
        if (currentMember.getRole() != Role.GUARDIAN) {
            throw new AccessDeniedException("루틴을 생성할 수 있는 권한이 없습니다. 보호자만 루틴을 생성할 수 있습니다.");
        }
        List<Routine> routines = routineRepository.findByGuardianId(currentMember.getId());
        return routines.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<RoutineResponse> getRoutinesByDependent() {
        Member currentMember = getCurrentMember();
        if (currentMember.getRole() != Role.DEPENDENT) {
            throw new AccessDeniedException("피보호자만 루틴 목록을 조회할 수 있습니다.");
        }
        List<Routine> routines = routineRepository.findByDependentId(currentMember.getId());
        return routines.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public RoutineResponse getRoutineById(Long routineId) {
        Routine routine = findRoutineByIdAndCheckAccess(routineId);
        return convertToResponse(routine);
    }

    @Transactional
    public RoutineResponse updateRoutine(Long routineId, RoutineRequest request) {
        Routine routine = findRoutineByIdAndCheckAccess(routineId);
        Member currentMember = getCurrentMember();

        if (currentMember.getRole() != Role.GUARDIAN || !routine.getGuardian().getId().equals(currentMember.getId())) {
            throw new AccessDeniedException("루틴을 수정할 권한이 없습니다. 보호자만 루틴을 수정할 수 있습니다.");
        }

        routine.setType(request.getType());
        routine.setTitle(request.getTitle());
        routine.setDescription(request.getDescription());
        routine.setStartTime(request.getStartTime());
        routine.setEndTime(request.getEndTime());
        routine.set_repeat(request.isRepeat());
        if (request.getRepeatDays() != null) {
            routine.setRepeatDays(request.getRepeatDays());
        }

        RoutineAlarmSetting existingAlarmSetting = routine.getAlarmSetting();
        boolean hasAlarmSettingRequest = request.getAlertBeforeStartMin() != null ||
                                         request.getAlertBeforeEndMin() != null ||
                                         request.getRepeatIntervalMin() != null ||
                                         request.getSoundType() != null || // SoundType 추가
                                         request.getVoiceMessageUrl() != null; // VoiceMessageUrl 추가

        // 1. 알림 설정을 새로 생성해야 하는 경우
        if (existingAlarmSetting == null && (Boolean.TRUE.equals(request.getIsEnabled()) || (request.getIsEnabled() == null && hasAlarmSettingRequest))) {
            RoutineAlarmSetting newAlarmSetting = RoutineAlarmSetting.builder()
                .routine(routine)
                .alertBeforeStartMin(request.getAlertBeforeStartMin())
                .alertBeforeEndMin(request.getAlertBeforeEndMin())
                .repeatIntervalMin(request.getRepeatIntervalMin())
                .isEnabled(Boolean.TRUE.equals(request.getIsEnabled()) || request.getIsEnabled() == null) // 요청에 isEnabled가 없으면 true로 간주
                .soundType(request.getSoundType()) // SoundType 추가
                .voiceMessageUrl(request.getVoiceMessageUrl()) // VoiceMessageUrl 추가
                .build();
            routine.setAlarmSetting(newAlarmSetting);
            routineAlarmSettingRepository.save(newAlarmSetting);
        }
        // 2. 기존 알림 설정을 업데이트해야 하는 경우
        else if (existingAlarmSetting != null) {
            // isEnabled가 명시적으로 요청에 포함된 경우
            if (request.getIsEnabled() != null) {
                existingAlarmSetting.setIsEnabled(request.getIsEnabled());
            }
            // 알림 설정 필드들이 요청에 포함된 경우 업데이트
            if (hasAlarmSettingRequest) {
                existingAlarmSetting.setAlertBeforeStartMin(request.getAlertBeforeStartMin());
                existingAlarmSetting.setAlertBeforeEndMin(request.getAlertBeforeEndMin());
                existingAlarmSetting.setRepeatIntervalMin(request.getRepeatIntervalMin());
                existingAlarmSetting.setSoundType(request.getSoundType()); // SoundType 추가
                existingAlarmSetting.setVoiceMessageUrl(request.getVoiceMessageUrl()); // VoiceMessageUrl 추가
            }
            // 알림 관련 필드 요청이 없고, isEnabled가 false로 명시적으로 넘어오거나, isEnabled가 null이면서 알림 관련 필드도 없는 경우 삭제
            if (!hasAlarmSettingRequest && (Boolean.FALSE.equals(request.getIsEnabled()) || request.getIsEnabled() == null)) {
                routine.setAlarmSetting(null);
                routineAlarmSettingRepository.delete(existingAlarmSetting);
            } else {
                routineAlarmSettingRepository.save(existingAlarmSetting);
            }
        }


        Routine updatedRoutine = routineRepository.save(routine);
        return convertToResponse(updatedRoutine);
    }

    @Transactional
    public void deleteRoutine(Long routineId) {
        Routine routine = findRoutineByIdAndCheckAccess(routineId);
        Member currentMember = getCurrentMember();

        if (!routine.getGuardian().getId().equals(currentMember.getId())) {
            throw new AccessDeniedException("루틴을 삭제할 권한이 없습니다.");
        }
        routineRepository.delete(routine);
    }

    @Transactional
    public void completeRoutine(Long routineId) {
        Routine routine = findRoutineByIdAndCheckAccess(routineId);
        Member currentMember = getCurrentMember();

        if (currentMember.getRole() != Role.DEPENDENT) {
            throw new AccessDeniedException("루틴을 완료할 수 있는 권한이 없습니다. 피보호자만 가능합니다.");
        }

        if (!routine.getDependent().getId().equals(currentMember.getId())) {
            throw new AccessDeniedException("해당 루틴을 완료할 권한이 없습니다.");
        }

        routine.setCompleted(true);
        routineRepository.save(routine);
    }

    @Transactional
    public RoutineResponse updateRoutineMemo(Long routineId, RoutineMemoRequest request) {
        Routine routine = findRoutineByIdAndCheckAccess(routineId);
        Member currentMember = getCurrentMember();

        if (currentMember.getRole() == Role.GUARDIAN) {
            if (!routine.getGuardian().getId().equals(currentMember.getId())) {
                throw new AccessDeniedException("해당 루틴의 보호자 메모를 수정할 권한이 없습니다.");
            }
            if (request.getDependentMemo() != null) {
                throw new AccessDeniedException("보호자는 피보호자 메모를 수정할 수 없습니다.");
            }
            routine.setGuardianMemo(request.getGuardianMemo());
        } else if (currentMember.getRole() == Role.DEPENDENT) {
            if (!routine.getDependent().getId().equals(currentMember.getId())) {
                throw new AccessDeniedException("해당 루틴의 피보호자 메모를 수정할 권한이 없습니다.");
            }
            if (request.getGuardianMemo() != null) {
                throw new AccessDeniedException("피보호자는 보호자 메모를 수정할 수 없습니다.");
            }
            routine.setDependentMemo(request.getDependentMemo());
        } else {
            throw new AccessDeniedException("메모를 수정할 권한이 없습니다.");
        }

        Routine updatedRoutine = routineRepository.save(routine);
        return convertToResponse(updatedRoutine);
    }

    private Routine findRoutineByIdAndCheckAccess(Long routineId) {
        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new IllegalArgumentException("루틴을 찾을 수 없습니다."));
        Member currentMember = getCurrentMember();

        boolean isGuardian = currentMember.getRole() == Role.GUARDIAN &&
                             currentMember.getDependentConnections().stream()
                                     .anyMatch(conn -> conn.getDependent().getId().equals(routine.getDependent().getId()));

        boolean isDependent = currentMember.getRole() == Role.DEPENDENT &&
                              currentMember.getGuardianConnections().stream()
                                      .anyMatch(conn -> conn.getGuardian().getId().equals(routine.getGuardian().getId()));

        if (!isGuardian && !isDependent) {
            throw new AccessDeniedException("해당 루틴에 접근할 권한이 없습니다.");
        }
        return routine;
    }

    private RoutineResponse convertToResponse(Routine routine) {
        return RoutineResponse.builder()
                .id(routine.getId())
                .guardianId(routine.getGuardian().getId())
                .dependentId(routine.getDependent().getId())
                .type(routine.getType())
                .title(routine.getTitle())
                .description(routine.getDescription())
                .startTime(routine.getStartTime())
                .endTime(routine.getEndTime())
                .repeat(routine.is_repeat())
                .repeatDays(routine.getRepeatDays())
                .alertBeforeStartMin(routine.getAlarmSetting() != null ? routine.getAlarmSetting().getAlertBeforeStartMin() : null)
                .alertBeforeEndMin(routine.getAlarmSetting() != null ? routine.getAlarmSetting().getAlertBeforeEndMin() : null)
                .repeatIntervalMin(routine.getAlarmSetting() != null ? routine.getAlarmSetting().getRepeatIntervalMin() : null)
                .isEnabled(routine.getAlarmSetting() != null ? routine.getAlarmSetting().getIsEnabled() : false) // isEnabled 추가
                .soundType(routine.getAlarmSetting() != null ? routine.getAlarmSetting().getSoundType() : null) // SoundType 추가
                .voiceMessageUrl(routine.getAlarmSetting() != null ? routine.getAlarmSetting().getVoiceMessageUrl() : null) // VoiceMessageUrl 추가
                .completed(routine.isCompleted())
                .guardianMemo(routine.getGuardianMemo()) // 보호자 메모 추가
                .dependentMemo(routine.getDependentMemo()) // 피보호자 메모 추가
                .guardianName(routine.getGuardian().getName()) // 보호자 이름 추가
                .dependentName(routine.getDependent().getName()) // 피보호자 이름 추가
                .build();
    }
}

