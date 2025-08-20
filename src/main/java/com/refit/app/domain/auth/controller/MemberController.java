package com.refit.app.domain.auth.controller;

import com.refit.app.domain.auth.dto.ConcernSummaryDto;
import com.refit.app.domain.auth.dto.ReissueResultDto;
import com.refit.app.domain.auth.dto.request.LoginRequest;
import com.refit.app.domain.auth.dto.request.SignupAllRequest;
import com.refit.app.domain.auth.dto.request.UpdateBasicRequest;
import com.refit.app.domain.auth.dto.response.LoginResponse;
import com.refit.app.domain.auth.dto.response.SignupResponse;
import com.refit.app.domain.auth.dto.response.UtilResponse;
import com.refit.app.domain.auth.service.MemberService;
import com.refit.app.global.config.JwtProvider;
import com.refit.app.infra.file.s3.S3Uploader;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final JwtProvider jwtProvider;
    private final S3Uploader s3Uploader;

    @PostMapping(value = "/join", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public UtilResponse<SignupResponse> signupAll(
            @Valid @RequestBody SignupAllRequest req
    ) {

        Long id = memberService.signupAll(req);
        return new UtilResponse<>("SUCCESS", "회원가입이 완료되었습니다.", new SignupResponse(id));
    }


    @GetMapping("/check/email")
    public UtilResponse<Boolean> checkEmail(@RequestParam String email) {
        boolean available = memberService.isEmailAvailable(email);
        return new UtilResponse<>("SUCCESS", "이메일 중복검사를 완료했습니다.", available);
    }

    @GetMapping("/check/nickname")
    public UtilResponse<Boolean> checkNickname(@RequestParam String nickname) {
        boolean available = memberService.isNicknameAvailable(nickname);
        return new UtilResponse<>("SUCCESS", "닉네임 중복검사를 완료했습니다.", available);
    }

    @Value("${jwt.refresh-exp-days}")
    private long refreshExpDays;

    @PostMapping("/login")
    public UtilResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletResponse httpResp
    ) {
        LoginResponse data = memberService.login(req.getEmail(), req.getPassword());

        // access token 헤더에 추가
        String accessToken = jwtProvider.createAccessToken(
                data.getMemberId(), req.getEmail());
        httpResp.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        // refresh token HttpOnly 쿠키에 저장
        String refreshToken = jwtProvider.createRefreshToken(data.getMemberId());
        data.setRefreshToken(refreshToken);

        return new UtilResponse<>("SUCCESS", "로그인에 성공했습니다.", data);
    }

    @GetMapping("/refresh")
    public UtilResponse<LoginResponse> refreshToken(
            @RequestParam("refreshToken") String refreshToken,
            HttpServletResponse response) {
        ReissueResultDto res = memberService.reissueAccessToken(refreshToken);

        // 새 access 토큰 Authorization 헤더
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + res.getAccessToken());

        // 새 refresh 토큰 바디 전달
        LoginResponse data = new LoginResponse();
        data.setMemberId(res.getUserId());
        data.setRefreshToken(res.getRotatedRefreshToken());

        return new UtilResponse<>("SUCCESS", "토큰을 재발급했습니다.", data);
    }

    @PutMapping("/basic")
    public UtilResponse<Void> updateMyBasic(
            @Valid @RequestBody UpdateBasicRequest req,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        memberService.updateMyBasicInfo(userId, req);
        return new UtilResponse<>("SUCCESS", "기본 정보 수정을 완료했습니다.", null);
    }

    @PutMapping("/health")
    public UtilResponse<Void> updateMyConcerns(
            @Valid @RequestBody ConcernSummaryDto req,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        memberService.updateMyConcerns(userId, req);
        return new UtilResponse<>("SUCCESS", "관심(건강/헤어/피부) 정보를 수정했습니다.", null);
    }

}
