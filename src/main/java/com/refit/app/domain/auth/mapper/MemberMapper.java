package com.refit.app.domain.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemberMapper {

    // int insert(Member m);
    //
    // Optional<Member> findByEmail(String email);

    Long findIdByEmail(String email);

    boolean existsByEmail(@Param("email") String email);

    boolean existsByNickname(@Param("nickname") String nickname);
}
