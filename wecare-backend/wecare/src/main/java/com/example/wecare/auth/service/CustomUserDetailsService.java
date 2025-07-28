package com.example.wecare.auth.service;

import com.example.wecare.member.domain.Member;
import com.example.wecare.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    @Transactional(readOnly = true) // 읽기 전용 트랜잭션 설정
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return memberRepository.findByUsername(username)
                .map(this::convertToUserDetails)
                .orElseThrow(() ->
                        // 보안상의 이유로 상세 ID 노출 제한
                        new UsernameNotFoundException("해당 사용자를 찾을 수 없습니다."));
    }

    private UserDetails convertToUserDetails(Member member) {
        // 여러 권한 확장 가능하도록 구조 변경
        GrantedAuthority authority = new SimpleGrantedAuthority(member.getRole().name());

        return new User(
                String.valueOf(member.getId()),
                member.getPassword(),
                Collections.singleton(authority)
        );
    }
}

