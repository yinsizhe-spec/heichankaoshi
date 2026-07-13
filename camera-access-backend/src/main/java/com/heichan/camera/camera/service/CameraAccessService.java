package com.heichan.camera.camera.service;

import com.heichan.camera.camera.dto.CameraAccessCheckResponse;
import com.heichan.camera.camera.mapper.CameraMapper;
import com.heichan.camera.camera.model.CameraAccessRow;
import com.heichan.camera.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 摄像头访问权限校验服务。
 */
@Service
@RequiredArgsConstructor
public class CameraAccessService {

    private final CameraMapper cameraMapper;

    /**
     * 系统使用的统一时区。
     */
    @Value("${app.time-zone:Asia/Kuala_Lumpur}")
    private String timeZone;

    /**
     * 检查当前登录用户能否访问指定摄像头。
     *
     * @param username   JWT 中的当前用户名
     * @param cameraCode 摄像头业务编号，例如 cam_001
     * @return 访问校验结果
     */
    @Transactional(readOnly = true)
    public CameraAccessCheckResponse checkAccess(
            String username,
            String cameraCode
    ) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_UNAUTHORIZED",
                    "请先登录"
            );
        }

        if (!StringUtils.hasText(cameraCode)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "CAMERA_ID_REQUIRED",
                    "摄像头编号不能为空"
            );
        }

        String normalizedCameraCode = cameraCode.trim();

        CameraAccessRow row = cameraMapper.selectCameraAccess(
                username,
                normalizedCameraCode
        );

        /*
         * 该查询同时按用户名和摄像头编号查询。
         *
         * row 为 null 可能表示：
         * 1. 用户不存在；
         * 2. 摄像头不存在。
         *
         * 因为正常请求中的用户名来自有效 JWT，
         * 所以这里主要按摄像头不存在处理。
         */
        if (row == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "CAMERA_NOT_FOUND",
                    "摄像头不存在"
            );
        }

        OffsetDateTime serverTime = OffsetDateTime.now(
                resolveZoneId()
        );

        LocalDate currentDate = serverTime.toLocalDate();
        LocalTime currentTime = serverTime.toLocalTime();

        AccessDecision decision = evaluateAccess(
                row,
                currentDate,
                currentTime
        );

        return CameraAccessCheckResponse.builder()
                .allowed(decision.allowed())
                .cameraId(row.getCameraCode())
                .cameraName(row.getCameraName())
                .location(row.getLocation())
                .cameraStatus(row.getCameraStatus())
                .serverTime(serverTime)
                .accessStartTime(row.getAccessStartTime())
                .accessEndTime(row.getAccessEndTime())
                .validFrom(row.getValidFrom())
                .validUntil(row.getValidUntil())
                .reason(decision.reason())
                .message(decision.message())
                .build();
    }

    /**
     * 执行所有访问规则判断。
     */
    private AccessDecision evaluateAccess(
            CameraAccessRow row,
            LocalDate currentDate,
            LocalTime currentTime
    ) {
        /*
         * 1. 检查用户状态。
         */
        if ("DISABLED".equalsIgnoreCase(row.getUserStatus())) {
            return new AccessDecision(
                    false,
                    "USER_DISABLED",
                    "当前账号已被禁用"
            );
        }

        if ("LOCKED".equalsIgnoreCase(row.getUserStatus())) {
            return new AccessDecision(
                    false,
                    "USER_LOCKED",
                    "当前账号已被锁定"
            );
        }

        if (!"ACTIVE".equalsIgnoreCase(row.getUserStatus())) {
            return new AccessDecision(
                    false,
                    "USER_NOT_ACTIVE",
                    "当前账号状态不可用"
            );
        }

        /*
         * 2. 检查摄像头是否启用。
         */
        if (!Integer.valueOf(1).equals(row.getCameraEnabled())) {
            return new AccessDecision(
                    false,
                    "CAMERA_DISABLED",
                    "摄像头已被禁用"
            );
        }

        /*
         * 3. 检查摄像头运行状态。
         */
        if ("OFFLINE".equalsIgnoreCase(row.getCameraStatus())) {
            return new AccessDecision(
                    false,
                    "CAMERA_OFFLINE",
                    "摄像头当前离线，请稍后重试"
            );
        }

        if ("MAINTENANCE".equalsIgnoreCase(row.getCameraStatus())) {
            return new AccessDecision(
                    false,
                    "CAMERA_MAINTENANCE",
                    "摄像头正在维护"
            );
        }

        if (!"ONLINE".equalsIgnoreCase(row.getCameraStatus())) {
            return new AccessDecision(
                    false,
                    "CAMERA_UNAVAILABLE",
                    "摄像头当前不可访问"
            );
        }

        /*
         * 4. 检查用户是否拥有摄像头权限。
         */
        if (row.getPermissionId() == null) {
            return new AccessDecision(
                    false,
                    "NO_CAMERA_PERMISSION",
                    "你没有权限访问该摄像头"
            );
        }

        /*
         * 5. 检查权限是否启用。
         */
        if (!Integer.valueOf(1).equals(row.getPermissionEnabled())) {
            return new AccessDecision(
                    false,
                    "CAMERA_PERMISSION_DISABLED",
                    "摄像头访问权限已被禁用"
            );
        }

        /*
         * 6. 检查权限生效日期。
         */
        if (row.getValidFrom() != null
                && currentDate.isBefore(row.getValidFrom())) {
            return new AccessDecision(
                    false,
                    "PERMISSION_NOT_STARTED",
                    "摄像头访问权限尚未生效"
            );
        }

        /*
         * 7. 检查权限失效日期。
         */
        if (row.getValidUntil() != null
                && currentDate.isAfter(row.getValidUntil())) {
            return new AccessDecision(
                    false,
                    "PERMISSION_EXPIRED",
                    "摄像头访问权限已过期"
            );
        }

        /*
         * 8. 检查每天允许访问的时间段。
         */
        if (!isWithinAccessTime(
                currentTime,
                row.getAccessStartTime(),
                row.getAccessEndTime()
        )) {
            return new AccessDecision(
                    false,
                    "OUTSIDE_ACCESS_TIME",
                    "当前不在允许访问时间内"
            );
        }

        return new AccessDecision(
                true,
                "ACCESS_ALLOWED",
                "允许访问"
        );
    }

    /**
     * 判断当前时间是否处于允许访问时间段。
     *
     * 支持普通时间段：
     * 09:00 - 18:00
     *
     * 支持跨天时间段：
     * 22:00 - 06:00
     *
     * 开始时间等于结束时间时，按全天开放处理。
     */
    private boolean isWithinAccessTime(
            LocalTime now,
            LocalTime start,
            LocalTime end
    ) {
        if (now == null || start == null || end == null) {
            return false;
        }

        if (start.equals(end)) {
            return true;
        }

        /*
         * 普通时间段，例如 09:00 - 18:00。
         */
        if (start.isBefore(end)) {
            return !now.isBefore(start)
                    && !now.isAfter(end);
        }

        /*
         * 跨天时间段，例如 22:00 - 06:00。
         */
        return !now.isBefore(start)
                || !now.isAfter(end);
    }

    /**
     * 获取系统配置时区。
     */
    private ZoneId resolveZoneId() {
        try {
            return ZoneId.of(timeZone);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "系统时区配置错误：" + timeZone,
                    exception
            );
        }
    }

    /**
     * 服务内部访问判断结果。
     */
    private record AccessDecision(
            boolean allowed,
            String reason,
            String message
    ) {
    }

    /**
     * 校验访问权限。
     *
     * 如果不允许访问，直接抛出 403 业务异常。
     * 该方法供 stream、analysis 等受保护接口复用。
     */
    @Transactional(readOnly = true)
    public CameraAccessCheckResponse checkAccessOrThrow(
            String username,
            String cameraCode
    ) {
        CameraAccessCheckResponse result = checkAccess(
                username,
                cameraCode
        );

        if (!Boolean.TRUE.equals(result.getAllowed())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    result.getReason(),
                    result.getMessage()
            );
        }

        return result;
    }
}