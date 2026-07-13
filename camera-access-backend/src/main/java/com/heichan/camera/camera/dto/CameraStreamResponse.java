package com.heichan.camera.camera.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 摄像头播放信息响应。
 */
@Data
@Builder
public class CameraStreamResponse {

    /**
     * 摄像头业务编号，例如 cam_001。
     */
    private String cameraId;

    /**
     * 摄像头名称。
     */
    private String cameraName;

    /**
     * 视频流类型：
     * WEBRTC、HLS、IFRAME、MJPEG。
     */
    private String streamType;

    /**
     * 前端实际播放地址。
     */
    private String streamUrl;

    /**
     * 当前后端服务器时间。
     */
    private OffsetDateTime serverTime;

    /**
     * 当前播放地址的建议失效时间。
     */
    private OffsetDateTime expiresAt;
}