package zippick.domain.auth.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthTokenDTO {
    private String token;
    private LocalDateTime expiredAt;
    private Long memberId;
}
