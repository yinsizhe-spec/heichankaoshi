package com.heichan.camera.camera.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 识别出的单道题目。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiQuestionItem {

    /**
     * 题号，必须尽量与画面中的原始题号保持一致。
     */
    private String questionNo;

    /**
     * 题目标题或题目简要内容。
     */
    private String questionTitle;

    /**
     * 从画面中识别出的完整题目内容。
     */
    private String recognizedText;

    /**
     * 题目类型。
     */
    private String questionType;

    /**
     * 英文直接答案。
     */
    private String simpleAnswer;

    /**
     * 英文答案 + 中文解释。
     */
    private String balancedAnswer;

    /**
     * 英文最终答案 + 中文详细解释。
     */
    private String bestAnswer;

    /**
     * 当前题目的置信度，范围 0 到 1。
     */
    private BigDecimal confidence;
}
