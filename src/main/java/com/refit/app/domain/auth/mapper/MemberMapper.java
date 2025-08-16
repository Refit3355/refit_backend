package com.refit.app.domain.auth.mapper;

import com.refit.app.domain.auth.dto.ConcernSummaryDto;
import com.refit.app.domain.auth.dto.MemberRowDto;
import java.time.LocalDate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemberMapper {


    void insertBasic(@Param("email") String email,
            @Param("nickName") String nickName,
            @Param("memberName") String memberName,
            @Param("passwordHash") String passwordHash,
            @Param("zipcode") Integer zipcode,
            @Param("roadAddress") String roadAddress,
            @Param("detailAddress") String detailAddress,
            @Param("gender") String gender,
            @Param("birthday") LocalDate birthday,
            @Param("phoneNumber") String phoneNumber,
            @Param("profileUrl") String profileUrl);

    Long findIdByEmail(String email);

    boolean existsByEmail(@Param("email") String email);

    boolean existsByNickname(@Param("nickname") String nickname);

    MemberRowDto findByEmail(@Param("email") String email);

    ConcernSummaryDto findHealthSummary(@Param("memberId") Long memberId);

    MemberRowDto findBasicById(@Param("memberId") Long memberId);

    int updateBasicById(@Param("memberId") Long memberId,
            @Param("email") String email,
            @Param("memberName") String memberName,
            @Param("passwordHash") String passwordHash,
            @Param("zipcode") Integer zipcode,
            @Param("roadAddress") String roadAddress,
            @Param("detailAddress") String detailAddress,
            @Param("gender") String gender,
            @Param("birthday") LocalDate birthday,
            @Param("phoneNumber") String phoneNumber);


}
