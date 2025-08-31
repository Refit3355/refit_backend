package com.refit.app.domain.auth.service;

import com.refit.app.domain.auth.dto.HealthInfoDto;
import com.refit.app.domain.auth.dto.MemberRowDto;
import com.refit.app.domain.auth.dto.request.SignupAllRequest;
import com.refit.app.domain.auth.dto.response.KakaoVerifyResponse;
import com.refit.app.domain.auth.dto.response.LoginResponse;
import com.refit.app.domain.auth.mapper.MemberMapper;
import com.refit.app.global.config.JwtProvider;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class KakaoOAuthServiceImpl implements KakaoOAuthService {

    private final WebClient kakaoWebClient;
    private final MemberMapper memberMapper;
    private final MemberService memberService;
    private final JwtProvider jwtProvider;

    public record KakaoUser(String id, String email, String nickname, String profileImageUrl) {

    }

    private KakaoUser fetchKakaoUser(String kakaoAccessToken) {
        try {
            Map<String, Object> resp = kakaoWebClient.get()
                    .uri("/v2/user/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, r ->
                            r.bodyToMono(String.class).flatMap(body ->
                                    Mono.error(new IllegalArgumentException(
                                            "카카오 토큰이 유효하지 않습니다: " + r.statusCode()))))
                    .onStatus(HttpStatusCode::is5xxServerError, r ->
                            r.bodyToMono(String.class).flatMap(body ->
                                    Mono.error(new IllegalStateException(
                                            "카카오 서버 오류: " + r.statusCode()))))
                    .bodyToMono(Map.class)
                    .block();

            if (resp == null || resp.get("id") == null) {
                throw new IllegalArgumentException("카카오 사용자 정보를 가져오지 못했습니다.");
            }

            String id = String.valueOf(resp.get("id"));
            Map<String, Object> account = safeMap(resp.get("kakao_account"));
            String email = account != null ? (String) account.get("email") : null;
            Map<String, Object> profile = account != null ? safeMap(account.get("profile")) : null;
            String nickname = profile != null ? (String) profile.get("nickname") : null;
            String profileImg = profile != null ? (String) profile.get("profile_image_url") : null;

            return new KakaoUser(id, email, nickname, profileImg);
        } catch (WebClientResponseException e) {
            throw new IllegalStateException("카카오 API 통신 실패: " + e.getStatusCode(), e);
        } catch (Exception e) {
            throw new IllegalStateException("카카오 API 호출 중 예외", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeMap(Object o) {
        return (o instanceof Map<?, ?> m) ? (Map<String, Object>) m : null;
    }

    @Override
    @Transactional(readOnly = true)
    public KakaoVerifyResponse verify(String kakaoAccessToken) {
        KakaoUser u = fetchKakaoUser(kakaoAccessToken);
        Long memberId = memberMapper.findIdByOauthId(u.id());

        if (memberId == null) {
            return new KakaoVerifyResponse(
                    true, u.id(), u.email(), u.nickname(), null,
                    null, null, null, null, null,
                    null, null, null
            );
        }

        MemberRowDto m = memberMapper.findBasicById(memberId);
        String refresh = jwtProvider.createRefreshToken(memberId);

        var summary = memberMapper.findHealthSummary(memberId);

        KakaoVerifyResponse res = new KakaoVerifyResponse();
        res.setNeedSignup(false);
        res.setKakaoId(u.id());
        res.setEmail(u.email());
        res.setNickname(u.nickname());
        res.setProfileImageUrl(m.getProfileImageUrl());
        res.setMemberId(memberId);
        res.setUserEmail(m.getEmail());
        res.setUserNickname(m.getNickname());
        res.setName(m.getMemberName());
        res.setRefreshToken(refresh);

        if (summary != null) {
            res.setHealth(summary.getHealth());
            res.setHair(summary.getHair());
            res.setSkin(summary.getSkin());
        }

        return res;
    }

    @Override
    @Transactional
    public LoginResponse signupWithKakao(String kakaoAccessToken, SignupAllRequest signupAll) {
        KakaoUser u = fetchKakaoUser(kakaoAccessToken);

        // 중복 연결 방지
        Long exists = memberMapper.findIdByOauthId(u.id());
        if (exists != null) {
            throw new IllegalStateException("이미 다른 계정에 연결된 카카오 계정입니다.");
        }

        Long memberId = memberService.signupAll(signupAll);

        // 레이스 방지 재확인 후 연결
        Long exists2 = memberMapper.findIdByOauthId(u.id());
        if (exists2 != null) {
            throw new IllegalStateException("이미 다른 계정에 연결된 카카오 계정입니다.");
        }
        memberMapper.updateOauthIdByMemberId(memberId, u.id());

        MemberRowDto m = memberMapper.findBasicById(memberId);

        // --- health만 세팅 ---
        HealthInfoDto health;
        {
            // 기존 요약 쿼리 재사용 (ConcernSummaryDto에서 health만 사용)
            var summary = memberMapper.findHealthSummary(memberId);
            if (summary == null || summary.getHealth() == null) {
                health = new HealthInfoDto(0, 0, 0, 0, 0, 0, 0);
            } else {
                health = summary.getHealth();
            }

        }

        LoginResponse lr = new LoginResponse();
        lr.setMemberId(memberId);
        lr.setNickname(m.getNickname());
        lr.setHealth(health);
        lr.setProfileImageUrl(m.getProfileImageUrl());
        return lr;
    }
}
