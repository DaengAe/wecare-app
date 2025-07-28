package com.example.wecare.routine.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "routine_alarm_setting", uniqueConstraints = {
    @UniqueConstraint(name = "pk_routine_alarm_setting", columnNames = {"id"}),
    @UniqueConstraint(name = "uk_routine_alarm_setting_routine_id", columnNames = {"routine_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutineAlarmSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "routine_id", foreignKey = @ForeignKey(name = "fk_routine_alarm_setting_routine_id"))
    private Routine routine;

    private Integer alertBeforeStartMin;
    private Integer alertBeforeEndMin;
    private Integer repeatIntervalMin;

    @Builder.Default
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "sound_type")
    private SoundType soundType; // 알림 사운드 타입

    @Column(name = "voice_message_url")
    private String voiceMessageUrl; // 음성 메시지 URL (VOICE_MESSAGE 타입일 경우)
}