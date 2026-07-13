package com.heichan.camera.user.controller;

import com.heichan.camera.user.dto.UserProfileResponse;
import com.heichan.camera.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口。
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前登录用户资料。
     *
     * GET /api/users/me
     *
     * 请求头：
     * Authorization: Bearer JWT
     */
    @GetMapping("/me")
    public UserProfileResponse getCurrentUser(
            Authentication authentication
    ) {
        /*
         * authentication.getName() 的值来自
         * JwtAuthenticationFilter 中设置的 username。
         */
        String username = authentication.getName();

        return userService.getCurrentUser(username);
    }
}