package com.heichan.camera.camera.controller;

import com.heichan.camera.camera.entity.Camera;
import com.heichan.camera.camera.service.CameraStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cameras")
public class CameraStatusController {

    private final CameraStatusService cameraStatusService;

    public CameraStatusController(
            CameraStatusService cameraStatusService
    ) {
        this.cameraStatusService = cameraStatusService;
    }

    /**
     * 修改指定摄像头的在线状态。
     *
     * 调用示例：
     * GET /api/cameras/G660555SUMYU/status/ONLINE
     * GET /api/cameras/G660555SUMYU/status/OFFLINE
     */
    @GetMapping("/{cameraCode}/status/{status}")
    public ResponseEntity<Map<String, Object>> updateCameraStatus(
            @PathVariable String cameraCode,
            @PathVariable String status
    ) {
        Camera camera =
                cameraStatusService.updateCameraStatus(
                        cameraCode,
                        status
                );

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("success", true);
        result.put("message", buildMessage(camera.getStatus()));
        result.put("cameraId", camera.getId());
        result.put("cameraCode", camera.getCameraCode());
        result.put("cameraName", camera.getCameraName());
        result.put("status", camera.getStatus());
        result.put("isOnline",
                "ONLINE".equalsIgnoreCase(camera.getStatus()));
        result.put("updatedAt", camera.getUpdatedAt());
        result.put("serverTime", LocalDateTime.now());

        return ResponseEntity.ok(result);
    }

    private String buildMessage(String status) {
        if ("ONLINE".equalsIgnoreCase(status)) {
            return "摄像头已设置为在线";
        }

        return "摄像头已设置为离线";
    }
}