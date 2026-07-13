package com.heichan.camera.config;

import com.heichan.camera.security.JsonAuthenticationEntryPoint;
import com.heichan.camera.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * JWT 认证过滤器。
     */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 未登录或 Token 无效时返回 JSON。
     */
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;

    /**
     * Spring Security 核心配置。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                /*
                 * 前后端分离项目使用 JWT，
                 * 不依赖浏览器 Session 和表单 CSRF Token。
                 */
                .csrf(csrf -> csrf.disable())

                /*
                 * 启用下面定义的 CORS 配置。
                 */
                .cors(Customizer.withDefaults())

                /*
                 * JWT 是无状态认证，不创建服务端 Session。
                 */
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                /*
                 * 未登录访问受保护接口时，
                 * 返回自定义 JSON，而不是跳转登录页面。
                 */
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                authenticationEntryPoint
                        )
                )

                /*
                 * 接口权限配置。
                 */
                .authorizeHttpRequests(authorize -> authorize

                        /*
                         * 放行所有浏览器跨域预检请求。
                         */
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        /*
                         * 登录接口公开访问。
                         */
                        .requestMatchers(
                                "/api/auth/login"
                        ).permitAll()

                        /*
                         * Swagger 接口文档公开访问。
                         */
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        /*
                         * 开发测试接口公开访问。
                         * 正式上线前建议删除。
                         */
                        .requestMatchers(
                                "/api/test/**"
                        ).permitAll()

                        /*
                         * 其他所有接口必须登录。
                         */
                        .anyRequest()
                        .authenticated()
                )

                /*
                 * 在 Spring Security 默认用户名密码过滤器之前，
                 * 执行 JWT 认证过滤器。
                 */
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * CORS 跨域配置。
     *
     * 当前配置为全部放行：
     * 1. 允许所有来源
     * 2. 允许所有请求方法
     * 3. 允许所有请求头
     *
     * 当前项目使用 Authorization 请求头传递 JWT，
     * 不使用跨域 Cookie，因此 allowCredentials 设置为 false。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        /*
         * 允许所有来源。
         *
         * 例如：
         * http://localhost:5173
         * http://192.168.1.100:5173
         * https://example.com
         */
        configuration.setAllowedOriginPatterns(
                List.of("*")
        );

        /*
         * 允许所有 HTTP 请求方法。
         */
        configuration.setAllowedMethods(
                List.of("*")
        );

        /*
         * 允许所有请求头。
         *
         * 包括：
         * Authorization
         * Content-Type
         */
        configuration.setAllowedHeaders(
                List.of("*")
        );

        /*
         * 允许前端读取所有响应头。
         */
        configuration.setExposedHeaders(
                List.of("*")
        );

        /*
         * 不允许跨域 Cookie。
         *
         * JWT 放在 Authorization 请求头中，
         * 所以不需要开启 Cookie 凭证。
         */
        configuration.setAllowCredentials(false);

        /*
         * 浏览器缓存跨域预检结果一小时。
         */
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        /*
         * 让此 CORS 配置对所有后端路径生效。
         */
        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    /**
     * BCrypt 密码加密器。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}