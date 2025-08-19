package com.refit.app.domain.auth.service;

import com.refit.app.domain.auth.dto.ConcernSummaryDto;
import com.refit.app.domain.auth.dto.HairInfoDto;
import com.refit.app.domain.auth.dto.HealthInfoDto;
import com.refit.app.domain.auth.dto.MemberRowDto;
import com.refit.app.domain.auth.dto.ReissueResultDto;
import com.refit.app.domain.auth.dto.SkinInfoDto;
import com.refit.app.domain.auth.dto.request.ConcernRequest;
import com.refit.app.domain.auth.dto.request.SignupAllRequest;
import com.refit.app.domain.auth.dto.request.SignupRequest;
import com.refit.app.domain.auth.dto.request.UpdateBasicRequest;
import com.refit.app.domain.auth.dto.response.LoginResponse;
import com.refit.app.domain.auth.mapper.ConcernMapper;
import com.refit.app.domain.auth.mapper.MemberMapper;
import com.refit.app.global.config.JwtProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import java.time.LocalDate;
import java.util.Date;
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

    @Override
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

    @Override
    public String issueRefreshTokenCookieValue(Long userId) {
        return jwtProvider.createRefreshToken(userId);
    }

    @Override
    public ReissueResultDto reissueAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("리프레시 토큰이 없습니다.");
        }

        Jws<Claims> jws = jwtProvider.parseAndValidate(refreshToken);
        if (!jwtProvider.isRefreshToken(jws)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        Long userId = jwtProvider.getUserId(jws);
        MemberRowDto m = memberMapper.findBasicById(userId);
        if (m == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }

        // 새 액세스 토큰 발급
        String newAccess = jwtProvider.createAccessToken(m.getMemberId(), m.getEmail(),
                m.getNickname());

        // 리프레시 토큰 만료 임박 시 새 리프레시도 함께 발급
        Date exp = jwtProvider.getExpiration(jws);
        long remains = exp.getTime() - System.currentTimeMillis();
        String newRefresh = null;

        // 남은 시간이 3일 미만이면 재발급
        long threeDaysMillis = 3L * 24 * 60 * 60 * 1000L;
        if (remains < threeDaysMillis) {
            newRefresh = jwtProvider.createRefreshToken(userId);
        }

        return new ReissueResultDto(userId, newAccess, newRefresh);
    }

    @Override
    @Transactional
    public void updateMyBasicInfo(Long memberId, UpdateBasicRequest req) {

        // 비번 해시
        String passwordHash = null;
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            passwordHash = encoder.encode(req.getPassword());
        }

        // birthday 파싱
        LocalDate birthday = null;
        if (req.getBirthday() != null && !req.getBirthday().isBlank()) {
            birthday = LocalDate.parse(req.getBirthday());
        }

        memberMapper.updateBasicById(
                memberId,
                req.getEmail(),
                req.getName(),
                passwordHash,
                req.getZipcode(),
                req.getRoadAddress(),
                req.getDetailAddress(),
                req.getGender(),
                birthday,
                req.getPhone()
        );
    }

    @Override
    @Transactional
    public void updateMyConcerns(Long memberId, ConcernSummaryDto req) {
        if (req.getHealth() != null) {
            concernMapper.mergeHealth(memberId, req.getHealth());
        }
        if (req.getHair() != null) {
            concernMapper.mergeHair(memberId, req.getHair());
        }
        if (req.getSkin() != null) {
            concernMapper.mergeSkin(memberId, req.getSkin());
        }
    }


}

