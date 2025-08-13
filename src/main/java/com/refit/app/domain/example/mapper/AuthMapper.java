package zippick.domain.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import zippick.domain.auth.dto.AuthTokenDTO;
import zippick.domain.member.dto.MemberDTO;

@Mapper
public interface AuthMapper {
    MemberDTO getMemberById(String loginId);
    void insertToken(AuthTokenDTO token);
    void deleteToken(String token);
    AuthTokenDTO getToken(String token);

}
