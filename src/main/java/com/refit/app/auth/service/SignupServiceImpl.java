package com.refit.app.auth.service;

import com.refit.app.auth.domain.Member;
import com.refit.app.auth.dto.HealthRequest;
import com.refit.app.auth.dto.SignupRequest;
import com.refit.app.auth.mapper.ConcernMapper;
import com.refit.app.auth.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignupServiceImpl implements SignupService {

    private final MemberMapper memberMapper;
    private final ConcernMapper concernMapper;
    private final PasswordEncoder encoder;

    @Override
    @Transactional
    public Long signupBasic(SignupRequest signupRequest) {
        memberMapper.findByEmail(signupRequest.getEmail()).ifPresent(m -> {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        });

        Member m = new Member();
        m.setEmail(signupRequest.getEmail());
        m.setNickname(signupRequest.getNickName());
        m.setName(signupRequest.getMemberName());
        m.setPasswordHash(encoder.encode(signupRequest.getPassword()));
        m.setZipcode(signupRequest.getZipcode());
        m.setRoadAddress(signupRequest.getRoadAddress());
        m.setDetailAddress(signupRequest.getDetailAddress());
        m.setGender(signupRequest.getGender());
        m.setBirthday(signupRequest.getBirthday());
        m.setPhone(signupRequest.getPhoneNumber());
        m.setProfileImage(signupRequest.getProfileUrl());

        memberMapper.insert(m);
        return memberMapper.findIdByEmail(signupRequest.getEmail());
    }

    @Override
    public void upsertHealth(Long memberId, HealthRequest healthRequest) {
        concernMapper.mergeHealth(memberId, healthRequest);
    }
}

