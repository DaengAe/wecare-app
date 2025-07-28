package com.example.wecare.auth.service;

import com.example.wecare.auth.dto.LoginRequest;
import com.example.wecare.auth.dto.SignUpRequest;
import com.example.wecare.auth.dto.TokenDto;
import com.example.wecare.auth.jwt.JwtProperties;
import com.example.wecare.auth.jwt.JwtUtil;
import com.example.wecare.member.domain.Member;
import com.example.wecare.member.repository.MemberRepository;
import com.example.wecare.common.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtUtil jwtUtil;
    private final RedisService redisService;
    private final JwtProperties jwtProperties;

    @Transactional
    public void signUp(SignUpRequest request) {
        if (memberRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("이미 등록된 아이디입니다.");
        }

        Member member = new Member();
        member.setPassword(passwordEncoder.encode(request.getPassword()));
        member.setName(request.getName());
        member.setGender(request.getGender());
        member.setBirthDate(request.getBirthDate());
        member.setRole(request.getRole());
        member.setUsername(request.getUsername());

        memberRepository.save(member);
    }

    @Transactional
    public TokenDto login(LoginRequest loginRequest) {
        log.info("uesrname으로 로그인 시도 중입니다: {}", loginRequest.getUsername());
        // 1. Login ID/PW 를 기반으로 Authentication 객체 생성
        // 이때 authentication 는 인증 여부를 확인하는 authenticated 값이 false
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword());

        // 2. 실제 검증 (사용자 비밀번호 체크)이 이루어지는 부분
        // authenticate 매서드가 실행될 때 CustomUserDetailsService 에서 만든 loadUserByUsername 메서드가 실행
        Authentication authentication = null;
        try {
            authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
            log.info("username의 인증이 성공했습니다: {}", loginRequest.getUsername());
        } catch (Exception e) {
            log.error("username의 인증에 실패했습니다: {}. Error: {}", loginRequest.getUsername(), e.getMessage());
            throw e; // 예외를 다시 던져서 컨트롤러에서 처리하도록 함
        }

        // 3. 인증 정보를 기반으로 JWT 토큰 생성
        TokenDto tokenDto = TokenDto.builder()
                .accessToken(jwtUtil.generateAccessToken(authentication))
                .refreshToken(jwtUtil.generateRefreshToken(authentication))
                .build();

        // 4. RefreshToken Redis 저장 (expirationTime 설정을 통해 자동 삭제 처리)
        redisService.setValues(authentication.getName(), tokenDto.getRefreshToken(), jwtProperties.getRefreshExp(), TimeUnit.MILLISECONDS);

        return tokenDto;
    }

    @Transactional
    public void logout(String accessToken) {
        // 1. Access Token 검증
        if (!jwtUtil.validateToken(accessToken)) {
            throw new IllegalArgumentException("유효하지 않은 Access Token 입니다.");
        }

        // 2. Access Token 에서 Authentication 추출
        Authentication authentication = jwtUtil.getAuthentication(accessToken);

        // 3. Redis 에서 해당 User ID 로 저장된 Refresh Token 이 있는지 여부 확인 후 있을 경우 삭제
        if (redisService.getValues(authentication.getName()) != null) {
            redisService.deleteValues(authentication.getName());
        }

        // 4. 해당 Access Token 유효시간 가지고 와서 BlackList 로 저장
        long expiration = jwtUtil.getExpirationFromToken(accessToken).getTime() - System.currentTimeMillis();
        redisService.setValues(accessToken, "logout", expiration, TimeUnit.MILLISECONDS);
    }

    @Transactional
    public TokenDto reissue(String refreshToken) {
        // 1. Refresh Token 검증
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token 입니다.");
        }

        // 2. Refresh Token 에서 Authentication 추출
        Authentication authentication = jwtUtil.getAuthentication(refreshToken);

        // 3. Redis 에서 User ID 기반으로 저장된 Refresh Token 값 조회
        String redisRefreshToken = redisService.getValues(authentication.getName());
        if (redisRefreshToken == null || !redisRefreshToken.equals(refreshToken)) {
            throw new IllegalArgumentException("Refresh Token 정보가 일치하지 않습니다.");
        }

        // 4. 새로운 토큰 생성
        TokenDto tokenDto = TokenDto.builder()
                .accessToken(jwtUtil.generateAccessToken(authentication))
                .refreshToken(jwtUtil.generateRefreshToken(authentication))
                .build();

        // 5. RefreshToken Redis 업데이트
        redisService.setValues(authentication.getName(), tokenDto.getRefreshToken(), jwtProperties.getRefreshExp(), TimeUnit.MILLISECONDS);

        return tokenDto;
    }
}

