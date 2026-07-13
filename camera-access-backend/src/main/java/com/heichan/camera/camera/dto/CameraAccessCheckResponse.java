package com.heichan.camera.camera.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * 摄像头访问校验响应。
 */
@Data
@Builder
public class CameraAccessCheckResponse {

    /**
     * 是否允许当前用户访问。
     */
    private Boolean allowed;

    /**
     * 摄像头业务编号。
     */
    private String cameraId;

    private String cameraName;

    private String location;

    private String cameraStatus;

    /**
     * 后端服务器时间。
     */
    private OffsetDateTime serverTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime accessStartTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime accessEndTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate validFrom;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate validUntil;

    /**
     * 访问判断结果代码。
     */
    private String reason;

    /**
     * 给前端展示的中文提示。
     */
    private String message;
}