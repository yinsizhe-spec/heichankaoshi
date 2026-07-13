package com.heichan.camera.camera.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 摄像头 AI 分析请求。
 */
@Data
public class CameraAnalysisRequest {

    /**
     * 回答模式：
     * simple、balanced、best。
     */
    @Pattern(
            regexp = "simple|balanced|best",
            message = "answerMode 只支持 simple、balanced 或 best"
    )
    private String answerMode = "best";
}
