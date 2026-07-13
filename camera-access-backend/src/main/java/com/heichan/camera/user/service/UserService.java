package com.heichan.camera.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heichan.camera.common.exception.BusinessException;
import com.heichan.camera.user.dto.UserProfileResponse;
import com.heichan.camera.user.entity.AppUser;
import com.heichan.camera.user.mapper.AppUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户业务服务。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserMapper appUserMapper;

    /**
     * 根据当前认证用户名查询用户资料。
     *
     * @param username 当前 JWT 中的用户名
     * @return 当前用户资料
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser(String username) {

        if (!StringUtils.hasText(username)) {
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_UNAUTHORIZED",
                    "请先登录"
            );
        }

        AppUser user = appUserMapper.selectOne(
                new LambdaQueryWrapper<AppUser>()
                        .eq(AppUser::getUsername, username)
                        .last("LIMIT 1")
        );

        if (user == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "USER_NOT_FOUND",
                    "当前用户不存在"
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

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}