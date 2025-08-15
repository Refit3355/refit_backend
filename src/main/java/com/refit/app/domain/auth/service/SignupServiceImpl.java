package com.refit.app.domain.auth.service;


import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.refit.app.domain.auth.dto.HealthRequest;
import com.refit.app.domain.auth.dto.SignupRequest;
import com.refit.app.domain.auth.mapper.ConcernMapper;
import com.refit.app.domain.auth.mapper.MemberMapper;

@Service
@RequiredArgsConstructor
public class SignupServiceImpl implements SignupService {

    private final MemberMapper memberMapper;
    private final ConcernMapper concernMapper;
    private final PasswordEncoder encoder;

    @Override
    public Long signupBasic(SignupRequest signupRequest) {

        return 0L;
    }

    @Override
    public void upsertHealth(Long memberId, HealthRequest healthRequest) {
        concernMapper.mergeHealth(memberId, healthRequest);
    }


    @Override
    public boolean isEmailAvailable(String email) {
        return !memberMapper.existsByEmail(email);
    }

    @Override
    public boolean isNicknameAvailable(String nickname) {
        return !memberMapper.existsByNickname(nickname);
    }
}

