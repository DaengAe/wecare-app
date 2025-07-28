package com.example.wecare.routine.dto;


import com.example.wecare.routine.domain.RepeatDay;
import com.example.wecare.routine.domain.RoutineType;

import com.example.wecare.routine.domain.SoundType;
import com.fasterxml.jackson.annotation.JsonFormat;
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
public class RoutineResponse {

    private Long id;
    private Long guardianId;
    private Long dependentId;
    private RoutineType type;
    private String title;
    private String description;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;
    private boolean repeat;
    private List<RepeatDay> repeatDays;
    private Integer alertBeforeStartMin;
    private Integer alertBeforeEndMin;
    private Integer repeatIntervalMin;
    private boolean completed;
    private Boolean isEnabled; // 알림 활성화 여부 필드 추가
    private SoundType soundType; // 알림 사운드 타입
    private String voiceMessageUrl; // 음성 메시지 URL
    private String guardianMemo; // 보호자 메모
    private String dependentMemo; // 피보호자 메모
    private String guardianName; // 보호자 이름
    private String dependentName; // 피보호자 이름
}