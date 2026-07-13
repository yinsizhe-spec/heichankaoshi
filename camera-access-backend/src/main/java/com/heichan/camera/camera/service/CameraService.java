package com.heichan.camera.camera.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heichan.camera.camera.dto.CameraListResponse;
import com.heichan.camera.camera.mapper.CameraMapper;
import com.heichan.camera.camera.model.CameraPermissionRow;
import com.heichan.camera.common.exception.BusinessException;
import com.heichan.camera.user.entity.AppUser;
import com.heichan.camera.user.mapper.AppUserMapper;
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
import java.util.List;

/**
 * 摄像头业务服务。
 */
@Service
@RequiredArgsConstructor
public class CameraService {

    private final AppUserMapper appUserMapper;

    private final CameraMapper cameraMapper;

    /**
     * 系统统一时区。
     *
     * 默认使用马来西亚时区。
     */
    @Value("${app.time-zone:Asia/Kuala_Lumpur}")
    private String timeZone;

    /**
     * 获取当前登录用户可访问的摄像头列表。
     *
     * @param username JWT 中的当前用户名
     * @return 摄像头列表
     */
    @Transactional(readOnly = true)
    public List<CameraListResponse> getCurrentUserCameras(
            String username
    ) {
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

        validateUserStatus(user);

        List<CameraPermissionRow> rows =
                cameraMapper.selectAvailableCamerasByUserId(
                        user.getId()
                );

        ZoneId zoneId;

        try {
            zoneId = ZoneId.of(timeZone);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "系统时区配置错误：" + timeZone,
                    exception
            );
        }

        OffsetDateTime serverTime = OffsetDateTime.now(zoneId);
        LocalDate currentDate = serverTime.toLocalDate();
        LocalTime currentTime = serverTime.toLocalTime();

        return rows.stream()
                .map(row -> convertToResponse(
                        row,
                        currentDate,
                        currentTime,
                        serverTime
                ))
                .toList();
    }

    /**
     * 检查用户账号状态。
     */
    private void validateUserStatus(AppUser user) {
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
    }

    /**
     * 将数据库查询结果转换成接口响应。
     */
    private CameraListResponse convertToResponse(
            CameraPermissionRow row,
            LocalDate currentDate,
            LocalTime currentTime,
            OffsetDateTime serverTime
    ) {
        AccessResult accessResult = calculateAccessResult(
                row,
                currentDate,
                currentTime
        );

        return CameraListResponse.builder()
                .cameraId(row.getCameraCode())
                .cameraName(row.getCameraName())
                .location(row.getLocation())
                .status(row.getStatus())
                .streamType(row.getStreamType())
                .accessStartTime(row.getAccessStartTime())
                .accessEndTime(row.getAccessEndTime())
                .validFrom(row.getValidFrom())
                .validUntil(row.getValidUntil())
                .canAccessNow(accessResult.allowed())
                .accessMessage(accessResult.message())
                .serverTime(serverTime)
                .build();
    }

    /**
     * 计算当前摄像头是否允许访问。
     */
    private AccessResult calculateAccessResult(
            CameraPermissionRow row,
            LocalDate currentDate,
            LocalTime currentTime
    ) {
        /*
         * 先检查摄像头状态。
         */
        if ("OFFLINE".equalsIgnoreCase(row.getStatus())) {
            return new AccessResult(
                    false,
                    "摄像头当前离线"
            );
        }

        if ("MAINTENANCE".equalsIgnoreCase(row.getStatus())) {
            return new AccessResult(
                    false,
                    "摄像头正在维护"
            );
        }

        if (!"ONLINE".equalsIgnoreCase(row.getStatus())) {
            return new AccessResult(
                    false,
                    "摄像头当前不可访问"
            );
        }

        /*
         * 检查权限生效日期。
         */
        if (row.getValidFrom() != null
                && currentDate.isBefore(row.getValidFrom())) {
            return new AccessResult(
                    false,
                    "摄像头访问权限尚未生效"
            );
        }

        /*
         * 检查权限失效日期。
         */
        if (row.getValidUntil() != null
                && currentDate.isAfter(row.getValidUntil())) {
            return new AccessResult(
                    false,
                    "摄像头访问权限已过期"
            );
        }

        /*
         * 检查每天允许访问的时间段。
         */
        boolean withinAccessTime = isWithinAccessTime(
                currentTime,
                row.getAccessStartTime(),
                row.getAccessEndTime()
        );

        if (!withinAccessTime) {
            return new AccessResult(
                    false,
                    "当前不在允许访问时间内"
            );
        }

        return new AccessResult(
                true,
                "当前可以访问"
        );
    }

    /**
     * 判断当前时间是否在允许访问时间范围内。
     *
     * 支持普通时间段：
     * 09:00 - 18:00
     *
     * 也支持跨天时间段：
     * 22:00 - 06:00
     */
    private boolean isWithinAccessTime(
            LocalTime now,
            LocalTime start,
            LocalTime end
    ) {
        if (start == null || end == null) {
            return false;
        }

        /*
         * 开始时间等于结束时间时，按全天开放处理。
         */
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
     * 内部访问判断结果。
     */
    private record AccessResult(
            boolean allowed,
            String message
    ) {
    }
}