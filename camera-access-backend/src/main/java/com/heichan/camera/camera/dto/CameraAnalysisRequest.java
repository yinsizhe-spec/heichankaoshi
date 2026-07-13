package com.heichan.camera.camera.dto;

import lombok.Data;

/**
 * AI 搜题请求。
 *
 * 前端不上传图片，由后端从视频流截图。
 */
@Data
public class CameraAnalysisRequest {

    /**
     * simple、balanced、best。
     */
    private String answerMode = "best";
}