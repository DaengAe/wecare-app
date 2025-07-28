package com.example.wecare.unit.service;

import com.example.wecare.auth.dto.LoginRequest;
import com.example.wecare.auth.dto.SignUpRequest;
import com.example.wecare.auth.dto.TokenDto;
import com.example.wecare.auth.jwt.JwtProperties;
import com.example.wecare.auth.jwt.JwtUtil;
import com.example.wecare.auth.service.AuthService;
import com.example.wecare.member.domain.Gender;
import com.example.wecare.member.domain.Member;
import com.example.wecare.member.domain.Role;
import com.example.wecare.member.repository.MemberRepository;
import com.example.wecare.common.service.RedisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManagerBuilder authenticationManagerBuilder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisService redisService;

    @Mock
    private Authentication authentication;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("회원가입 성공")
    void signUp_success() {
        // Given
        SignUpRequest request = SignUpRequest.builder()
                .username("testuser")
                .password("password123")
                .name("테스트")
                .gender(Gender.MALE)
                .birthDate(LocalDate.parse("1990-01-01"))
                .role(Role.GUARDIAN)
                .build();

        when(memberRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");

        // When
        authService.signUp(request);

        // Then
        verify(memberRepository, times(1)).findByUsername(request.getUsername());
        verify(passwordEncoder, times(1)).encode(request.getPassword());
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 등록된 아이디")
    void signUp_fail_alreadyExists() {
        // Given
        SignUpRequest request = SignUpRequest.builder()
                .username("existinguser")
                .password("password123")
                .name("테스트")
                .gender(Gender.MALE)
                .birthDate(LocalDate.parse("1990-01-01"))
                .role(Role.GUARDIAN)
                .build();

        when(memberRepository.findByUsername(request.getUsername())).thenReturn(Optional.of(new Member()));

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> authService.signUp(request));
        assertEquals("이미 등록된 아이디입니다.", exception.getMessage());

        verify(memberRepository, times(1)).findByUsername(request.getUsername());
        verify(passwordEncoder, never()).encode(anyString());
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("로그인 성공 - 토큰 발급")
    void login_success() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        TokenDto expectedTokenDto = TokenDto.builder()
                .accessToken("mockAccessToken")
                .refreshToken("mockRefreshToken")
                .build();

        when(authenticationManagerBuilder.getObject()).thenReturn(authenticationManager);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtUtil.generateAccessToken(authentication)).thenReturn("mockAccessToken");
        when(jwtUtil.generateRefreshToken(authentication)).thenReturn("mockRefreshToken");
        when(jwtProperties.getRefreshExp()).thenReturn(3600000L); // 1 hour
        when(authentication.getName()).thenReturn("testuser");

        // When
        TokenDto result = authService.login(loginRequest);

        // Then
        assertNotNull(result);
        assertEquals(expectedTokenDto.getAccessToken(), result.getAccessToken());
        assertEquals(expectedTokenDto.getRefreshToken(), result.getRefreshToken());

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil, times(1)).generateAccessToken(authentication);
        verify(jwtUtil, times(1)).generateRefreshToken(authentication);
        verify(redisService, times(1)).setValues(eq("testuser"), eq("mockRefreshToken"), eq(3600000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 자격 증명")
    void login_fail_badCredentials() {
        // Given
        LoginRequest loginRequest = LoginRequest.builder()
                .username("wronguser")
                .password("wrongpassword")
                .build();

        when(authenticationManagerBuilder.getObject()).thenReturn(authenticationManager);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // When & Then
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
        assertEquals("Bad credentials", exception.getMessage());

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil, never()).generateAccessToken(any(Authentication.class));
        verify(redisService, never()).setValues(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_success() {
        // Given
        String accessToken = "validAccessToken";
        Date expirationTime = new Date(System.currentTimeMillis()+36000L); // 1 second

        when(jwtUtil.validateToken(accessToken)).thenReturn(true);
        when(jwtUtil.getAuthentication(accessToken)).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(redisService.getValues("testuser")).thenReturn("someRefreshToken");
        when(jwtUtil.getExpirationFromToken(accessToken)).thenReturn(expirationTime);

        // When
        authService.logout(accessToken);

        // Then
        verify(jwtUtil, times(1)).validateToken(accessToken);
        verify(jwtUtil, times(1)).getAuthentication(accessToken);
        verify(redisService, times(1)).getValues("testuser");
        verify(redisService, times(1)).deleteValues("testuser");
        verify(jwtUtil, times(1)).getExpirationFromToken(accessToken);
        verify(redisService, times(1)).setValues(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("로그아웃 실패 - 유효하지 않은 Access Token")
    void logout_fail_invalidAccessToken() {
        // Given
        String accessToken = "invalidAccessToken";

        when(jwtUtil.validateToken(accessToken)).thenReturn(false);

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> authService.logout(accessToken));
        assertEquals("유효하지 않은 Access Token 입니다.", exception.getMessage());

        verify(jwtUtil, times(1)).validateToken(accessToken);
        verify(jwtUtil, never()).getAuthentication(anyString());
        verify(redisService, never()).getValues(anyString());
        verify(redisService, never()).deleteValues(anyString());
        verify(jwtUtil, never()).getExpirationFromToken(anyString());
        verify(redisService, never()).setValues(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void reissue_success() {
        // Given
        String refreshToken = "validRefreshToken";
        TokenDto expectedTokenDto = TokenDto.builder()
                .accessToken("newAccessToken")
                .refreshToken("newRefreshToken")
                .build();

        when(jwtUtil.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtil.getAuthentication(refreshToken)).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(redisService.getValues("testuser")).thenReturn(refreshToken);
        when(jwtUtil.generateAccessToken(authentication)).thenReturn("newAccessToken");
        when(jwtUtil.generateRefreshToken(authentication)).thenReturn("newRefreshToken");
        when(jwtProperties.getRefreshExp()).thenReturn(3600000L);

        // When
        TokenDto result = authService.reissue(refreshToken);

        // Then
        assertNotNull(result);
        assertEquals(expectedTokenDto.getAccessToken(), result.getAccessToken());
        assertEquals(expectedTokenDto.getRefreshToken(), result.getRefreshToken());

        verify(jwtUtil, times(1)).validateToken(refreshToken);
        verify(jwtUtil, times(1)).getAuthentication(refreshToken);
        verify(redisService, times(1)).getValues("testuser");
        verify(jwtUtil, times(1)).generateRefreshToken(authentication);
        verify(jwtUtil, times(1)).generateRefreshToken(authentication);
        verify(redisService, times(1)).setValues(eq("testuser"), eq("newRefreshToken"), eq(3600000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않은 Refresh Token")
    void reissue_fail_invalidRefreshToken() {
        // Given
        String refreshToken = "invalidRefreshToken";

        when(jwtUtil.validateToken(refreshToken)).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.reissue(refreshToken));
        assertEquals("유효하지 않은 Refresh Token 입니다.", exception.getMessage());

        verify(jwtUtil, times(1)).validateToken(refreshToken);
        verify(jwtUtil, never()).getAuthentication(anyString());
        verify(redisService, never()).getValues(anyString());
        verify(jwtUtil, never()).generateAccessToken(any(Authentication.class));
    }

    @Test
    @DisplayName("토큰 재발급 실패 - Redis Refresh Token 불일치")
    void reissue_fail_redisMismatch() {
        // Given
        String refreshToken = "validRefreshToken";
        String redisRefreshToken = "differentRefreshToken";

        when(jwtUtil.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtil.getAuthentication(refreshToken)).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(redisService.getValues("testuser")).thenReturn(redisRefreshToken);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.reissue(refreshToken));
        assertEquals("Refresh Token 정보가 일치하지 않습니다.", exception.getMessage());

        verify(jwtUtil, times(1)).validateToken(refreshToken);
        verify(jwtUtil, times(1)).getAuthentication(refreshToken);
        verify(redisService, times(1)).getValues("testuser");
        verify(jwtUtil, never()).generateAccessToken(any(Authentication.class));
    }
}