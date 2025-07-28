package com.example.wecare.integration.security;

import com.example.wecare.auth.dto.LoginRequest;
import com.example.wecare.auth.dto.LoginResponse;
import com.example.wecare.auth.dto.SignUpRequest;
import com.example.wecare.member.domain.Gender;
import com.example.wecare.member.domain.Role;
import com.example.wecare.member.repository.MemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    // 테스트용 회원가입 데이터
    private SignUpRequest validSignUpRequest;
    private LoginRequest validLoginRequest;

    @BeforeEach
    void setUp() {
        validSignUpRequest = SignUpRequest.builder()
                .username("testuser123")
                .password("password123!")
                .phone("010-1234-5678")
                .name("테스트유저")
                .gender(Gender.MALE)
                .birthDate(LocalDate.of(1990, 1, 1))
                .role(Role.GUARDIAN)
                .build();

        validLoginRequest = LoginRequest.builder()
                .username("testuser123")
                .password("password123!")
                .build();
    }

    @AfterEach
    void cleanUp() {
        memberRepository.deleteAll();
    }

    // --- 회원가입 테스트 ---
    @Test
    @DisplayName("유효한 정보로 회원가입을 요청하면 성공(201 Created)해야 한다.")
    void signUp_Success() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSignUpRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("이미 등록된 아이디로 회원가입을 요청하면 실패(400 Bad Request)해야 한다.")
    void signUp_Fail_DuplicateMemberId() throws Exception {
        // 먼저 회원가입 성공
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSignUpRequest)))
                .andExpect(status().isCreated());

        // 동일한 아이디로 다시 회원가입 요청
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSignUpRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("아이디 형식이 올바르지 않으면 회원가입이 실패(400 Bad Request)해야 한다.")
    void signUp_Fail_InvalidMemberIdFormat() throws Exception {
        SignUpRequest request = SignUpRequest.builder()
                .username("abc") // 6자 미만
                .password("password123!")
                .phone("010-1234-5678")
                .name("테스트")
                .gender(Gender.MALE)
                .birthDate(LocalDate.of(1990, 1, 1))
                .role(Role.GUARDIAN)
                .build();

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        request.setUsername("한글아이디"); // 한글 포함 (백엔드 정규식은 영숫자만 허용)
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비밀번호 형식이 올바르지 않으면 회원가입이 실패(400 Bad Request)해야 한다.")
    void signUp_Fail_InvalidPasswordFormat() throws Exception {
        SignUpRequest request = SignUpRequest.builder()
                .username("newuser123")
                .password("short") // 8자 미만
                .phone("010-1234-5678")
                .name("테스트")
                .gender(Gender.MALE)
                .birthDate(LocalDate.of(1990, 1, 1))
                .role(Role.GUARDIAN)
                .build();

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        request.setPassword("onlyletters"); // 숫자 없음
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        request.setPassword("12345678"); // 영문자 없음
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- 로그인 테스트 ---
    @Test
    @DisplayName("유효한 아이디와 비밀번호로 로그인을 요청하면 성공(200 OK)하고 토큰을 반환해야 한다.")
    void login_Success() throws Exception {
        // 회원가입 먼저
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSignUpRequest)))
                .andExpect(status().isCreated());

        // 로그인 요청
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    @DisplayName("존재하지 않는 아이디로 로그인을 요청하면 실패(403 Forbidden)해야 한다.")
    void login_Fail_UserNotFound() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("nonexistent")
                .password("anypassword")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인을 요청하면 실패(403 Forbidden)해야 한다.")
    void login_Fail_BadCredentials() throws Exception {
        // 회원가입 먼저
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSignUpRequest)))
                .andExpect(status().isCreated());

        LoginRequest request = LoginRequest.builder()
                .username(validLoginRequest.getUsername())
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // --- 로그아웃 테스트 ---
    @Test
    @DisplayName("유효한 Access Token으로 로그아웃을 요청하면 성공(200 OK)해야 한다.")
    void logout_Success() throws Exception {
        // 회원가입 및 로그인하여 토큰 발급
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSignUpRequest)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), LoginResponse.class);
        String accessToken = loginResponse.getAccessToken();

        // 로그아웃 요청
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("유효하지 않은 Access Token으로 로그아웃을 요청하면 실패(400 Bad Request)해야 한다.")
    void logout_Fail_InvalidToken() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Access Token 없이 로그아웃을 요청하면 실패(400 Bad Request)해야 한다.")
    void logout_Fail_NoToken() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isBadRequest());
    }

    // --- 토큰 재발급 테스트 ---

    @Test
    @DisplayName("유효한 Refresh Token으로 토큰 재발급을 요청하면 성공(200 OK)하고 새로운 토큰을 반환해야 한다.")
    void reissue_Success() throws Exception {
        // 회원가입 및 로그인하여 토큰 발급
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSignUpRequest)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), LoginResponse.class);
        String refreshToken = loginResponse.getRefreshToken();

        // 재발급 요청
        mockMvc.perform(post("/auth/reissue")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    @DisplayName("유효하지 않은 Refresh Token으로 토큰 재발급을 요청하면 실패(400 Bad Request)해야 한다.")
    void reissue_Fail_InvalidToken() throws Exception {
        mockMvc.perform(post("/auth/reissue")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Refresh Token 없이 토큰 재발급을 요청하면 실패(400 Bad Request)해야 한다.")
    void reissue_Fail_NoToken() throws Exception {
        mockMvc.perform(post("/auth/reissue"))
                .andExpect(status().isBadRequest());
    }
}