package zippick.domain.auth.service;

import zippick.domain.auth.dto.request.LoginRequest;
import zippick.domain.auth.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    void logout(String token);
}
