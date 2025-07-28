package com.example.wecare.member.controller;

import com.example.wecare.member.dto.MemberResponse;
import com.example.wecare.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // MyPage 접근 시
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMe() {
        MemberResponse memberResponse = memberService.getMe();
        return ResponseEntity.ok(memberResponse);
    }

    // 연결된 피보호자 목록 조회
    
}
