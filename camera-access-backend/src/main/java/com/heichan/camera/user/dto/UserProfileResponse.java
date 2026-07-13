package com.heichan.camera.user.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 当前登录用户资料响应。
 */
@Data
@Builder
public class UserProfileResponse {

    /**
     * 用户主键。
     */
    private Long id;

    /**
     * 登录用户名。
     */
    private String username;

    /**
     * 用户邮箱。
     */
    private String email;

    /**
     * 用户角色，例如 USER、ADMIN。
     */
    private String role;

    /**
     * 用户状态，例如 ACTIVE、DISABLED、LOCKED。
     */
    private String status;

    /**
     * 用户头像地址。
     */
    private String avatarUrl;
}