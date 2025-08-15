package com.refit.app.domain.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.refit.app.domain.auth.dto.request.ConcernRequest;
import com.refit.app.domain.auth.dto.request.SignupAllRequest;
import com.refit.app.domain.auth.dto.request.SignupRequest;
import com.refit.app.domain.auth.mapper.ConcernMapper;
import com.refit.app.domain.auth.mapper.MemberMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SignupServiceImpl implements SignupService {

	private final MemberMapper memberMapper;

	private final ConcernMapper concernMapper;

	private final PasswordEncoder encoder;

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

		if (memberMapper.existsByEmail(s.getEmail()))
			throw new IllegalArgumentException("이미 가입된 이메일입니다.");
		if (memberMapper.existsByNickname(s.getNickName()))
			throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");

		// 1) MEMBER insert (profileUrl null이면 DB default)
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

		// 2) 관심사 3종 MERGE
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
}

