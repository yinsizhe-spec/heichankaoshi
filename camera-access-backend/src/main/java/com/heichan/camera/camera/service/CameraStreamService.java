package com.heichan.camera.camera.service;

import com.heichan.camera.camera.dto.CameraAccessCheckResponse;
import com.heichan.camera.camera.dto.CameraStreamResponse;
import com.heichan.camera.camera.entity.Camera;
import com.heichan.camera.camera.mapper.CameraMapper;
import com.heichan.camera.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 摄像头播放地址服务。
 */
@Service
@RequiredArgsConstructor
public class CameraStreamService {

    private final CameraMapper cameraMapper;

    private final CameraAccessService cameraAccessService;

    /**
     * 系统时区。
     */
    @Value("${app.time-zone:Asia/Kuala_Lumpur}")
    private String timeZone;

    /**
     * 播放地址默认有效时间，单位为秒。
     *
     * 默认5分钟。
     */
    @Value("${app.stream.expiration-seconds:300}")
    private long streamExpirationSeconds;

    /**
     * 获取当前用户可以访问的摄像头播放地址。
     *
     * @param username   当前登录用户名
     * @param cameraCode 摄像头业务编号
     */
    @Transactional(readOnly = true)
    public CameraStreamResponse getStream(
            String username,
            String cameraCode
    ) {
        if (!StringUtils.hasText(cameraCode)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "CAMERA_ID_REQUIRED",
                    "摄像头编号不能为空"
            );
        }

        String normalizedCameraCode = cameraCode.trim();

        /*
         * 必须重新执行完整访问校验。
         *
         * 不能因为前端之前调用过 access-check，
         * 就直接返回播放地址。
         */
        CameraAccessCheckResponse accessResult =
                cameraAccessService.checkAccessOrThrow(
                        username,
                        normalizedCameraCode
                );

        Camera camera = cameraMapper.selectByCameraCode(
                normalizedCameraCode
        );

        if (camera == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "CAMERA_NOT_FOUND",
                    "摄像头不存在"
            );
        }

        if (!Integer.valueOf(1).equals(camera.getIsEnabled())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "CAMERA_DISABLED",
                    "摄像头已被禁用"
            );
        }

        if (!"ONLINE".equalsIgnoreCase(camera.getStatus())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "CAMERA_OFFLINE",
                    "摄像头当前不可用"
            );
        }

        if (!StringUtils.hasText(camera.getStreamSourceUrl())) {
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "STREAM_NOT_CONFIGURED",
                    "摄像头播放地址尚未配置"
            );
        }

        if (!StringUtils.hasText(camera.getStreamType())) {
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "STREAM_TYPE_NOT_CONFIGURED",
                    "摄像头播放类型尚未配置"
            );
        }

        ZoneId zoneId = resolveZoneId();

        OffsetDateTime serverTime = OffsetDateTime.now(zoneId);

        /*
         * 播放失效时间不能超过用户权限结束时间。
         */
        OffsetDateTime defaultExpiresAt = serverTime.plusSeconds(
                streamExpirationSeconds
        );

        OffsetDateTime permissionExpiresAt =
                calculatePermissionExpiresAt(
                        serverTime,
                        accessResult
                );

        OffsetDateTime finalExpiresAt;

        if (permissionExpiresAt != null
                && permissionExpiresAt.isBefore(defaultExpiresAt)) {
            finalExpiresAt = permissionExpiresAt;
        } else {
            finalExpiresAt = defaultExpiresAt;
        }

        return CameraStreamResponse.builder()
                .cameraId(camera.getCameraCode())
                .cameraName(camera.getCameraName())
                .streamType(camera.getStreamType())
                .streamUrl(buildStreamUrl(
                        camera,
                        username,
                        finalExpiresAt
                ))
                .serverTime(serverTime)
                .expiresAt(finalExpiresAt)
                .build();
    }

    /**
     * 根据用户当天访问结束时间计算权限结束时间。
     */
    private OffsetDateTime calculatePermissionExpiresAt(
            OffsetDateTime serverTime,
            CameraAccessCheckResponse accessResult
    ) {
        if (accessResult.getAccessStartTime() == null
                || accessResult.getAccessEndTime() == null) {
            return null;
        }

        /*
         * 开始时间等于结束时间，按全天开放处理。
         */
        if (accessResult.getAccessStartTime()
                .equals(accessResult.getAccessEndTime())) {
            return null;
        }

        OffsetDateTime accessEndDateTime =
                serverTime
                        .toLocalDate()
                        .atTime(accessResult.getAccessEndTime())
                        .atZone(serverTime.getOffset())
                        .toOffsetDateTime();

        /*
         * 跨天访问时间，例如 22:00 - 06:00。
         *
         * 如果当前处于晚上22点以后，
         * 结束时间应该是第二天06点。
         */
        if (accessResult.getAccessStartTime()
                .isAfter(accessResult.getAccessEndTime())) {

            if (!serverTime.toLocalTime()
                    .isBefore(accessResult.getAccessStartTime())) {
                accessEndDateTime =
                        accessEndDateTime.plusDays(1);
            }
        }

        return accessEndDateTime;
    }

    /**
     * 构造最终返回给前端的播放地址。
     *
     * 当前第一版直接返回数据库中的 MediaMTX 播放地址。
     * 后续可以在这里增加签名参数。
     */
    private String buildStreamUrl(
            Camera camera,
            String username,
            OffsetDateTime expiresAt
    ) {
        return camera.getStreamSourceUrl();
    }

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
}