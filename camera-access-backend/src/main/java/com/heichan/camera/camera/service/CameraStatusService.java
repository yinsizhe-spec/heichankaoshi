package com.heichan.camera.camera.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.heichan.camera.camera.entity.Camera;
import com.heichan.camera.camera.mapper.CameraMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class CameraStatusService {

    private final CameraMapper cameraMapper;

    public CameraStatusService(CameraMapper cameraMapper) {
        this.cameraMapper = cameraMapper;
    }

    /**
     * 根据摄像头编号修改摄像头在线状态。
     *
     * @param cameraCode 摄像头编号
     * @param status     ONLINE 或 OFFLINE
     * @return 修改后的摄像头信息
     */
    @Transactional
    public Camera updateCameraStatus(
            String cameraCode,
            String status
    ) {
        if (!StringUtils.hasText(cameraCode)) {
            throw new IllegalArgumentException("摄像头编号不能为空");
        }

        String normalizedStatus = normalizeStatus(status);

        Camera camera =
                cameraMapper.selectByCameraCode(cameraCode.trim());

        if (camera == null) {
            throw new IllegalArgumentException(
                    "摄像头不存在：" + cameraCode
            );
        }

        LocalDateTime now = LocalDateTime.now();

        /*
         * 使用条件更新，不依赖 updateById 的实体字段状态。
         */
        int updatedRows = cameraMapper.update(
                null,
                new LambdaUpdateWrapper<Camera>()
                        .eq(Camera::getId, camera.getId())
                        .set(Camera::getStatus, normalizedStatus)
                        .set(Camera::getUpdatedAt, now)
        );

        /*
         * 某些 MySQL 配置下，如果状态原本相同，
         * updatedRows 可能返回 0，所以最终以查询结果为准。
         */
        Camera updatedCamera =
                cameraMapper.selectByCameraCode(cameraCode.trim());

        if (updatedCamera == null) {
            throw new IllegalStateException(
                    "修改状态后无法查询摄像头：" + cameraCode
            );
        }

        if (!normalizedStatus.equalsIgnoreCase(
                updatedCamera.getStatus()
        )) {
            throw new IllegalStateException(
                    "摄像头状态修改失败：" + cameraCode
            );
        }

        return updatedCamera;
    }

    /**
     * 验证并标准化摄像头状态。
     */
    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new IllegalArgumentException(
                    "摄像头状态不能为空"
            );
        }

        String normalizedStatus =
                status.trim().toUpperCase();

        if (!"ONLINE".equals(normalizedStatus)
                && !"OFFLINE".equals(normalizedStatus)) {
            throw new IllegalArgumentException(
                    "摄像头状态只支持 ONLINE 或 OFFLINE"
            );
        }

        return normalizedStatus;
    }
}