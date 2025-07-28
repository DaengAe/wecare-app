package com.example.wecare.auth.dto;

import com.example.wecare.member.domain.Gender;
import com.example.wecare.member.domain.Role;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SignUpRequest {

    @NotBlank(message = "아이디는 필수 입력 값입니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,20}$", message = "아이디는 6~20자, 영문, 숫자를 포함해야 합니다.")
    private String username;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[~!@#$%^&*()+|=])[A-Za-z\\d~!@#$%^&*()+|=]{8,20}$", message = "비밀번호는 8~20자, 영문, 숫자, 특수문자를 포함해야 합니다.")
    private String password;

    @NotBlank(message = "이름은 필수 입력 값입니다.")
    private String name;

    @NotBlank(message = "휴대폰 번호는 필수 입력 값입니다.")
    @Pattern(
            regexp = "^01[016-9]-\\d{3,4}-\\d{4}$",
            message = "유효한 휴대폰 번호 형식(예: 010-1234-5678)이어야 합니다."
    )
    @Size(min = 13, max = 13, message = "휴대폰 번호는 13자리여야 합니다.")
    private String phone;

    @NotNull(message = "성별은 필수 입력 값입니다.")
    private Gender gender;

    @NotNull(message = "생년월일은 필수 입력 값입니다.")
    @Past(message = "생년월일은 과거 날짜여야 합니다.")
    private LocalDate birthDate;

    @NotNull(message = "역할은 필수 입력 값입니다.")
    private Role role;
}