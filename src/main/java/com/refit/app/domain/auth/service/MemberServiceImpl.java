package com.refit.app.domain.auth.service;

import com.refit.app.domain.auth.dto.ConcernSummaryDto;
import com.refit.app.domain.auth.dto.HairInfoDto;
import com.refit.app.domain.auth.dto.HealthInfoDto;
import com.refit.app.domain.auth.dto.MemberRowDto;
import com.refit.app.domain.auth.dto.SkinInfoDto;
import com.refit.app.domain.auth.dto.request.ConcernRequest;
import com.refit.app.domain.auth.dto.request.SignupAllRequest;
import com.refit.app.domain.auth.dto.request.SignupRequest;
import com.refit.app.domain.auth.dto.response.LoginResponse;
import com.refit.app.domain.auth.mapper.ConcernMapper;
import com.refit.app.domain.auth.mapper.MemberMapper;
import com.refit.app.global.config.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberMapper memberMapper;

    private final ConcernMapper concernMapper;

    private final PasswordEncoder encoder;
    private final JwtProvider jwtProvider;

    @Override
    @Transactional
    public Long signupAll(SignupAllRequest req) {

        if (req.getSignup() == null || req.getConcerns() == null
                || req.getConcerns().getHealth() == null
                || req.getConcerns().getHair() == null
                || req.getConcerns().getSkin() == null) {
            throw new IllegalArgumentException("기본정보와 건강/헤어/피부 고민 정보를 모두 입력해야 합니다.");
        }

        SignupRequest s = req.getSignup();

        if (memberMapper.existsByEmail(s.getEmail())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        if (memberMapper.existsByNickname(s.getNickName())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 기본 타입 데이터 insert
        memberMapper.insertBasic(
                s.getEmail(),
                s.getNickName(),
                s.getMemberName(),
                encoder.encode(s.getPassword()),
                s.getZipcode(),
                s.getRoadAddress(),
                s.getDetailAddress(),
                s.getGender(),
                s.getBirthday(),
                s.getPhoneNumber(),
                s.getProfileUrl()
        );

        Long memberId = memberMapper.findIdByEmail(s.getEmail());

        // 건강, 헤어, 피부 관련 데이터 merge
        ConcernRequest c = req.getConcerns();
        concernMapper.mergeHealth(memberId, c.getHealth());
        concernMapper.mergeHair(memberId, c.getHair());
        concernMapper.mergeSkin(memberId, c.getSkin());

        return memberId;
    }

    @Override
    public boolean isEmailAvailable(String email) {

        return !memberMapper.existsByEmail(email);
    }

    @Override
    public boolean isNicknameAvailable(String nickname) {

        return !memberMapper.existsByNickname(nickname);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(String email, String rawPassword) {
        MemberRowDto m = memberMapper.findByEmail(email);
        if (m == null || !encoder.matches(rawPassword, m.password)) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        ConcernSummaryDto summary = memberMapper.findHealthSummary(m.memberId);
        if (summary == null) {
            summary = new ConcernSummaryDto(
                    new HealthInfoDto(0, 0, 0, 0, 0, 0, 0),
                    new HairInfoDto(0, 0, 0, 0),
                    new SkinInfoDto(0, 0, 0, 0, 0, 0, 0, 0, 0)
            );
        } else {
            if (summary.getHealth() == null) {
                summary.setHealth(new HealthInfoDto(0, 0, 0, 0, 0, 0, 0));
            }
            if (summary.getHair() == null) {
                summary.setHair(new HairInfoDto(0, 0, 0, 0));
            }
            if (summary.getSkin() == null) {
                summary.setSkin(new SkinInfoDto(0, 0, 0, 0, 0, 0, 0, 0, 0));
            }
        }

        LoginResponse res = new LoginResponse();
        res.setMemberId(m.memberId);
        res.setEmail(m.email);
        res.setNickname(m.nickname);
        res.setName(m.memberName);
        res.setConcerns(summary);
        return res;
    }


    public String issueRefreshTokenCookieValue(Long userId) {
        return jwtProvider.createRefreshToken(userId);
    }


}

