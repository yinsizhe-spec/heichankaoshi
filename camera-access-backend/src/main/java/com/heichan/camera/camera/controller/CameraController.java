package com.heichan.camera.camera.controller;

import com.heichan.camera.camera.dto.CameraAccessCheckResponse;
import com.heichan.camera.camera.dto.CameraAnalysisRequest;
import com.heichan.camera.camera.dto.CameraAnalysisResponse;
import com.heichan.camera.camera.dto.CameraListResponse;
import com.heichan.camera.camera.dto.CameraStreamResponse;
import com.heichan.camera.camera.service.CameraAccessService;
import com.heichan.camera.camera.service.CameraAnalysisService;
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

    private final CameraAnalysisService cameraAnalysisService;

    @GetMapping
    public List<CameraListResponse> getCurrentUserCameras(
            Authentication authentication
    ) {
        return cameraService.getCurrentUserCameras(
                authentication.getName()
        );
    }

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

    /**
     * AI 搜题。
     *
     * POST /api/cameras/{cameraId}/analysis
     */
    @PostMapping("/{cameraId}/analysis")
    public CameraAnalysisResponse analyzeCameraSnapshot(
            @PathVariable String cameraId,

            @RequestBody(required = false)
            CameraAnalysisRequest request,

            Authentication authentication
    ) {
        return cameraAnalysisService.analyze(
                authentication.getName(),
                cameraId,
                request
        );
    }
}