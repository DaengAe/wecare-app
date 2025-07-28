package com.example.wecare.invitation.dto;

import com.example.wecare.invitation.domain.RelationshipType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AcceptInvitationRequest {
    @NotBlank(message = "초대 코드는 필수 입력 값입니다.")
    private String invitationCode;

    @NotNull(message = "상대방과의 관계는 필수 입력 값입니다.")
    private RelationshipType relationshipType;
}