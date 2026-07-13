package com.heichan.camera.camera.controller;

import com.heichan.camera.camera.dto.CameraAccessCheckResponse;
import com.heichan.camera.camera.dto.CameraListResponse;
import com.heichan.camera.camera.dto.CameraStreamResponse;
import com.heichan.camera.camera.service.CameraAccessService;
import com.heichan.camera.camera.service.CameraService;
import com.heichan.camera.camera.service.CameraStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 摄像头接口。
 */
@RestController
@RequestMapping("/api/cameras")
@RequiredArgsConstructor
public class CameraController {

    private final CameraService cameraService;

    private final CameraAccessService cameraAccessService;

    private final CameraStreamService cameraStreamService;

    /**
     * 获取当前用户的摄像头列表。
     *
     * GET /api/cameras
     */
    @GetMapping
    public List<CameraListResponse> getCurrentUserCameras(
            Authentication authentication
    ) {
        return cameraService.getCurrentUserCameras(
                authentication.getName()
        );
    }

    /**
     * 校验当前用户是否可以访问摄像头。
     *
     * GET /api/cameras/{cameraId}/access-check
     */
    @GetMapping("/{cameraId}/access-check")
    public CameraAccessCheckResponse checkCameraAccess(
            @PathVariable String cameraId,
            Authentication authentication
    ) {
        return cameraAccessService.checkAccess(
                authentication.getName(),
                cameraId
        );
    }

    /**
     * 获取摄像头播放地址。
     *
     * GET /api/cameras/{cameraId}/stream
     *
     * 示例：
     * GET /api/cameras/cam_001/stream
     */
    @GetMapping("/{cameraId}/stream")
    public CameraStreamResponse getCameraStream(
            @PathVariable String cameraId,
            Authentication authentication
    ) {
        return cameraStreamService.getStream(
                authentication.getName(),
                cameraId
        );
    }
}