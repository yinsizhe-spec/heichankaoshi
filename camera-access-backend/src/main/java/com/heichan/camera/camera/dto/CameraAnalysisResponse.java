package com.heichan.camera.camera.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 摄像头 AI 搜题响应。
 */
@Data
@Builder
public class CameraAnalysisResponse {

    private String cameraId;

    private OffsetDateTime capturedAt;

    private String questionNo;

    private String questionTitle;

    private String recognizedText;

    private String questionType;

    private String simpleAnswer;

    private String balancedAnswer;

    private String bestAnswer;

    private BigDecimal confidence;
}