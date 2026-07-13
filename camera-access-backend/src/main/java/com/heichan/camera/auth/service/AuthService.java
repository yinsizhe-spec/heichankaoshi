package com.heichan.camera.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heichan.camera.auth.dto.LoginRequest;
import com.heichan.camera.auth.dto.LoginResponse;
import com.heichan.camera.auth.dto.LoginUserResponse;
import com.heichan.camera.common.exception.BusinessException;
import com.heichan.camera.security.JwtTokenProvider;
import com.heichan.camera.user.entity.AppUser;
import com.heichan.camera.user.mapper.AppUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 用户登录。
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername().trim();

        AppUser user = appUserMapper.selectOne(
                new LambdaQueryWrapper<AppUser>()
                        .eq(AppUser::getUsername, username)
                        .last("LIMIT 1")
        );

        /*
         * 用户不存在和密码错误统一提示，
         * 防止攻击者判断某个用户名是否存在。
         */
        if (user == null
                || !StringUtils.hasText(user.getPassword())
                || !passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_INVALID_CREDENTIALS",
                    "用户名或密码错误"
            );
        }

        if ("DISABLED".equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "USER_DISABLED",
                    "当前账号已被禁用"
            );
        }

        if ("LOCKED".equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "USER_LOCKED",
                    "当前账号已被锁定"
            );
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "USER_NOT_ACTIVE",
                    "当前账号状态不可用"
            );
        }

        String token = jwtTokenProvider.generateToken(user);

        LoginUserResponse userResponse = LoginUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .avatarUrl(user.getAvatarUrl())
                .build();

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationSeconds())
                .user(userResponse)
                .build();
    }
}