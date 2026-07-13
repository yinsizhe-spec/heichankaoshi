package com.heichan.camera.camera.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 返回的多题分析结果。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiQuestionResult {

    /**
     * 本次画面分析总结。
     */
    private String summary;

    /**
     * 识别出的全部题目。
     */
    private List<AiQuestionItem> questions = new ArrayList<>();
}
