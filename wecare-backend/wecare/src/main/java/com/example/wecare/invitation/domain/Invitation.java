package com.example.wecare.invitation.domain;

import com.example.wecare.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "invitations", uniqueConstraints = @UniqueConstraint(name = "pk_invitations", columnNames = {"dependent_id", "guardian_id"}))
@IdClass(InvitationId.class)
public class Invitation {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id", foreignKey = @ForeignKey(name = "fk_invitations_guardian_id"))
    private Member guardian;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependent_id", foreignKey = @ForeignKey(name = "fk_invitations_dependent_id"))
    private Member dependent;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @Column(name = "is_active", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean isActive = true;

    @Enumerated(EnumType.STRING) // Enum 값을 문자열로 저장
    @Column(name = "relationship_type", nullable = false) // 관계 필드 추가
    private RelationshipType relationshipType;

}