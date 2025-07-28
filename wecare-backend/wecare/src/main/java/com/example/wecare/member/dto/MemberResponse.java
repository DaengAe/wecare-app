package com.example.wecare.member.dto;

import com.example.wecare.member.domain.Gender;
import com.example.wecare.member.domain.Role;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
public class MemberResponse {
    private Long id;
    private String username;
    private String name;
    private Gender gender;
    private LocalDate birthDate;
    private Role role;
    private List<MemberResponse> guardians; // 피보호자의 경우 연결된 보호자 목록 (MemberResponse로 통일)
    private List<MemberResponse> dependents; // 보호자의 경우 연결된 피보호자 목록
    @JsonProperty("isActive") // JSON 직렬화/역직렬화 시 필드명 지정
    private boolean isActive; // 연결 상태 추가
}