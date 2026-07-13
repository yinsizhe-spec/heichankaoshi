package com.heichan.camera.camera.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * 当前用户摄像头列表响应。
 */
@Data
@Builder
public class CameraListResponse {

    /**
     * 返回摄像头业务编号，而不是数据库主键。
     *
     * 例如 cam_001。
     */
    private String cameraId;

    private String cameraName;

    private String location;

    /**
     * ONLINE、OFFLINE、MAINTENANCE。
     */
    private String status;

    /**
     * WEBRTC、HLS、IFRAME、MJPEG。
     */
    private String streamType;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime accessStartTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime accessEndTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate validFrom;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate validUntil;

    /**
     * 当前是否可以访问。
     */
    private Boolean canAccessNow;

    /**
     * 当前访问状态说明。
     */
    private String accessMessage;

    /**
     * 后端服务器时间。
     */
    private OffsetDateTime serverTime;
}