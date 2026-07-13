package com.heichan.camera.camera.ai;

import com.heichan.camera.camera.dto.AiQuestionResult;

/**
 * AI 搜题客户端。
 */
public interface QuestionAiClient {

    AiQuestionResult analyzeQuestion(
            String mimeType,
            String base64Image,
            String answerMode
    );
}