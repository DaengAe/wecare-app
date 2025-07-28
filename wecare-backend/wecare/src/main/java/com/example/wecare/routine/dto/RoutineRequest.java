package com.example.wecare.routine.dto;


import com.example.wecare.routine.domain.RepeatDay;
import com.example.wecare.routine.domain.RoutineType;

import com.example.wecare.routine.domain.SoundType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutineRequest {

    @NotNull(message = "루틴 타입은 필수입니다.")
    private RoutineType type;

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    private String description;

    @NotNull(message = "시작 시간은 필수입니다.")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endTime;

    private boolean repeat;

    private List<RepeatDay> repeatDays;

    private Integer alertBeforeStartMin;
    private Integer alertBeforeEndMin;
    private Integer repeatIntervalMin;
    private Boolean isEnabled; // 알림 활성화 여부 필드 추가
    private SoundType soundType; // 알림 사운드 타입
    private String voiceMessageUrl; // 음성 메시지 URL
    private String guardianMemo; // 보호자 메모
    private String dependentMemo; // 피보호자 메모
}