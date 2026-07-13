package com.heichan.camera.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    /**
     * JWT 字符串。
     */
    private String token;

    /**
     * 固定为 Bearer。
     */
    private String tokenType;

    /**
     * Token 剩余有效时间，单位为秒。
     */
    private Long expiresIn;

    private LoginUserResponse user;
}