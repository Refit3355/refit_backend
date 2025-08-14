package com.refit.app.auth.mapper;

import com.refit.app.auth.domain.Member;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthMapper {

    int insert(Member m);

    Optional<Member> findByEmail(String email);

    Long findIdByEmail(String email);

}
