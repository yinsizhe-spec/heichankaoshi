package com.heichan.camera.camera.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 摄像头 AI 多题分析响应。
 */
@Data
@Builder
public class CameraAnalysisResponse {

    private String cameraId;

    private OffsetDateTime capturedAt;

    private BigDecimal confidence;

    private Integer questionCount;

    private String summary;

    private List<AiQuestionItem> questions;
}
