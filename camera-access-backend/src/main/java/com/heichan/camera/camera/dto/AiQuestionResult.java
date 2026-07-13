package com.heichan.camera.camera.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 返回的结构化题目结果。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiQuestionResult {

    private String questionNo;

    private String questionTitle;

    private String recognizedText;

    private String questionType;

    private String simpleAnswer;

    private String balancedAnswer;

    private String bestAnswer;

    private BigDecimal confidence;
}