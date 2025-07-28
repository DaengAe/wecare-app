package com.example.wecare.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
public class SecurityUtil {

    private SecurityUtil() { }

    /**
     * SecurityContext에서 현재 인증된 사용자의 ID를 가져옵니다.
     * @return 현재 사용자의 ID (Long)
     */
    public static Long getCurrentMemberId() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Security Context에 인증 정보가 없습니다.");
        }

        // principal에서 사용자 ID를 Long 타입으로 파싱하여 반환
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            log.error("Security Context의 사용자 ID를 파싱할 수 없습니다: {}", authentication.getName());
            throw new RuntimeException("인증 정보가 올바르지 않습니다.");
        }
    }
}
