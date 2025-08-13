package com.refit.app.domain.example.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zippick.domain.auth.dto.request.LoginRequest;
import zippick.domain.auth.dto.response.LoginResponse;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements zippick.domain.auth.service.AuthService {

    @Override
    public LoginResponse login(LoginRequest request) {
        return null;
    }

    @Override
    public void logout(String token) {

    }
}
