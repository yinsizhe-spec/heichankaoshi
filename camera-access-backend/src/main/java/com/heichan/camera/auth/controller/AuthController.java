package com.heichan.camera.auth.controller;

import com.heichan.camera.auth.dto.LoginRequest;
import com.heichan.camera.auth.dto.LoginResponse;
import com.heichan.camera.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录接口。
     *
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        return authService.login(request);
    }
}