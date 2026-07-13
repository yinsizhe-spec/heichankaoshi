package com.heichan.camera.camera.ai;

import com.heichan.camera.camera.dto.AiQuestionResult;

/**
 * AI 题目分析客户端。
 */
public interface QuestionAiClient {

    /**
     * 分析图片中的全部题目。
     *
     * @param mimeType 图片 MIME 类型
     * @param base64Image 图片 Base64 内容，不包含 data URL 前缀
     * @param answerMode 回答模式
     * @return 多题分析结果
     */
    AiQuestionResult analyzeQuestion(
            String mimeType,
            String base64Image,
            String answerMode
    );
}
