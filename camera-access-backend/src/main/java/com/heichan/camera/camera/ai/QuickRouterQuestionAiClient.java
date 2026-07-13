package com.heichan.camera.camera.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heichan.camera.camera.ai.dto.QuickRouterChatRequest;
import com.heichan.camera.camera.ai.dto.QuickRouterChatResponse;
import com.heichan.camera.camera.dto.AiQuestionResult;
import com.heichan.camera.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;

/**
 * QuickRouter 视觉搜题客户端。
 */
@Component
@RequiredArgsConstructor
public class QuickRouterQuestionAiClient
        implements QuestionAiClient {

    @Qualifier("quickRouterRestClient")
    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    @Value("${ai.quickrouter.base-url:https://api.quickrouter.ai/v1}")
    private String baseUrl;

    @Value("${ai.quickrouter.api-key:}")
    private String apiKey;

    @Value("${ai.quickrouter.model:gpt-4o}")
    private String model;

    @Value("${ai.quickrouter.max-tokens:2500}")
    private int maxTokens;

    @Value("${ai.quickrouter.temperature:0.2}")
    private double temperature;

    @Override
    public AiQuestionResult analyzeQuestion(
            String mimeType,
            String base64Image,
            String answerMode
    ) {
        validateConfiguration();

        if (!StringUtils.hasText(base64Image)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "AI_IMAGE_REQUIRED",
                    "待分析图片不能为空"
            );
        }

        String imageDataUrl =
                "data:"
                        + mimeType
                        + ";base64,"
                        + base64Image;

        QuickRouterChatRequest requestBody =
                buildRequest(imageDataUrl, answerMode);

        try {
            QuickRouterChatResponse response =
                    restClient
                            .post()
                            .uri(
                                    normalizeBaseUrl()
                                            + "/chat/completions"
                            )
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + apiKey
                            )
                            .contentType(
                                    MediaType.APPLICATION_JSON
                            )
                            .accept(
                                    MediaType.APPLICATION_JSON
                            )
                            .body(requestBody)
                            .retrieve()
                            .body(
                                    QuickRouterChatResponse.class
                            );

            String content = extractContent(response);

            return parseResult(content);

        } catch (HttpClientErrorException.Unauthorized exception) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "QUICKROUTER_UNAUTHORIZED",
                    "QuickRouter API Key 无效或已过期"
            );

        } catch (HttpClientErrorException.Forbidden exception) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "QUICKROUTER_FORBIDDEN",
                    "QuickRouter 拒绝请求，请检查模型权限、账户额度或令牌分组"
            );

        } catch (HttpClientErrorException.TooManyRequests exception) {
            throw new BusinessException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "QUICKROUTER_RATE_LIMITED",
                    "AI 请求过于频繁，请稍后重试"
            );

        } catch (HttpClientErrorException exception) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "QUICKROUTER_REQUEST_ERROR",
                    "QuickRouter 请求失败："
                            + limitText(
                            exception.getResponseBodyAsString(),
                            500
                    )
            );

        } catch (HttpServerErrorException exception) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "QUICKROUTER_SERVER_ERROR",
                    "QuickRouter 服务暂时异常"
            );

        } catch (ResourceAccessException exception) {
            throw new BusinessException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "QUICKROUTER_TIMEOUT",
                    "连接 QuickRouter 超时"
            );

        } catch (RestClientException exception) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "QUICKROUTER_CONNECTION_ERROR",
                    "无法连接 QuickRouter 服务"
            );
        }
    }

    private QuickRouterChatRequest buildRequest(
            String imageDataUrl,
            String answerMode
    ) {
        String systemPrompt = """
            You are a high-recall visual question recognition and solving assistant.

            Your main task is to examine the entire image, identify as many visible questions as possible, and provide an answer for every detected question.

            The questions are most likely written in English. They may also contain:
            - mathematical formulas;
            - numbers and units;
            - diagrams;
            - tables;
            - source code;
            - charts;
            - handwritten content;
            - a small amount of text in other languages.

            You must not focus only on the clearest, largest, central, or most obvious question.

            IMAGE SCANNING RULES:

            1. Scan the complete image carefully from top to bottom and from left to right.

            2. Check every visible area, including:
               - the center;
               - the upper and lower areas;
               - the left and right edges;
               - all four corners;
               - background screens;
               - projected content;
               - partially covered areas;
               - blurry areas;
               - tilted areas;
               - reflective areas;
               - cropped areas;
               - small or low-resolution text.

            3. Try to detect every visible question or question fragment.

            4. Do not ignore a question only because:
               - part of it is blurry;
               - part of it is outside the image;
               - some words are blocked;
               - only a few keywords are visible;
               - the answer choices are incomplete;
               - the question number cannot be clearly read.

            5. Process multiple questions in visual order:
               - top to bottom;
               - left to right.

            QUESTION RECOGNITION RULES:

            6. For each detected question, try to identify:
               - question number;
               - original English question statement;
               - answer choices;
               - formulas;
               - values;
               - units;
               - diagrams;
               - tables;
               - source code;
               - visible keywords;
               - any important context.

            7. Preserve the original English wording whenever it is visible.

            8. Do not translate the recognized English question or answer choices into Chinese.

            9. Preserve:
               - option letters such as A, B, C, and D;
               - technical terms;
               - programming keywords;
               - formulas;
               - variable names;
               - code;
               - units;
               - proper nouns.

            UNCLEAR OR INCOMPLETE QUESTION RULES:

            10. If a question is blurry, incomplete, partially covered, cropped, or difficult to read, do not immediately stop.

            11. Use all available clues to infer the most likely question and answer, including:
                - visible English keywords;
                - partial sentences;
                - visible answer choices;
                - question structure;
                - common exam patterns;
                - subject knowledge;
                - formulas;
                - numerical values;
                - units;
                - nearby text;
                - surrounding questions;
                - page layout;
                - diagrams;
                - code fragments;
                - context from the visible screen.

            12. Even when the full question cannot be recognized with complete certainty, still provide the most likely answer whenever useful clues exist.

            13. Do not respond only with:
                - "Unable to determine";
                - "The image is unclear";
                - "Insufficient information";
                - "Cannot read the question";
                - "No answer can be provided".

            14. When the answer is inferred from incomplete information:
                - clearly identify the visible keywords;
                - clearly state which content was inferred;
                - explain why the proposed answer is the most likely;
                - mention a reasonable alternative answer when applicable;
                - lower the confidence value appropriately.

            15. Mark uncertain recognized text with labels such as:
                - [unclear];
                - [partially visible];
                - [inferred: ...];
                - [likely text: ...].

            ANSWER LANGUAGE RULES:

            16. The recognized question and answer choices must remain in their original English wording.

            17. simpleAnswer must provide the final answer in English.

            18. For multiple questions, simpleAnswer must list every answer clearly, for example:
                "Q1: B. Encapsulation; Q2: True; Q3: 42 cm"

            19. For a single-choice question:
                - provide the option letter;
                - include the original English option text when visible.

            20. For a multiple-choice question:
                - provide all likely option letters;
                - include the original English option text when visible.

            21. For a true-or-false question:
                - answer "True" or "False".

            22. For a calculation question:
                - provide the final result;
                - include the correct unit when applicable.

            23. For a short-answer question:
                - provide the central conclusion in English.

            24. balancedAnswer and bestAnswer must explain the reasoning in Chinese.

            25. In Chinese explanations, preserve important English terms, technical words, formulas, option text, code, variable names, and visible keywords.

            26. Do not translate option letters or code.

            MULTIPLE-QUESTION OUTPUT RULES:

            27. Return answers for as many detected questions as possible.

            28. Since the output schema contains only one set of fields, place all detected questions into the same fields using a clear question-by-question format.

            29. In recognizedText, separate questions like this:

                Q1:
                Original English question and options

                Q2:
                Original English question and options

            30. In simpleAnswer, separate answers like this:
                "Q1: B. Encapsulation; Q2: False; Q3: 25 m/s"

            31. In balancedAnswer, explain each question separately:
                "Q1：答案是 B. Encapsulation。原因是……"
                "Q2：答案是 False。原因是……"

            32. In bestAnswer, provide detailed question-by-question reasoning and a final answer for every detected question.

            CONFIDENCE RULES:

            33. confidence must be a number from 0 to 1.

            34. Use the following approximate ranges:
                - 0.85 to 1.00: the questions and answers are clear;
                - 0.55 to 0.84: some details are unclear, but answers can be reasonably inferred;
                - 0.20 to 0.54: answers are mainly guessed from keywords or partial context;
                - 0.00 to 0.19: almost no useful question information is visible.

            35. When several questions are detected, confidence should represent the overall reliability of all returned answers.

            NO-QUESTION RULE:

            36. If the image truly contains no recognizable question and no useful question-related keywords:
                - set questionType to "unknown";
                - set simpleAnswer to "No clear question detected";
                - explain the situation briefly in Chinese;
                - use a low confidence value.

            OUTPUT FORMAT RULES:

            37. Return exactly one valid JSON object.

            38. Do not use Markdown code fences.

            39. Do not output any text before or after the JSON.

            40. All JSON field names must exactly match the following schema.

            Return JSON in this exact structure:

            {
              "questionNo": "List all detected question numbers, for example: Q1; Q2; Q3",
              "questionTitle": "A short English summary of all detected questions",
              "recognizedText": "All detected English questions and answer choices in visual order. Separate them as Q1, Q2, Q3. Mark unclear or inferred content explicitly.",
              "questionType": "Use mixed when multiple question types are present; otherwise use single_choice|multiple_choice|true_false|calculation|short_answer|unknown",
              "simpleAnswer": "Give the final answer for every detected question in English. Include option letters and original English answer text whenever possible.",
              "balancedAnswer": "Explain every answer in Chinese. Preserve important English terms, visible keywords, formulas, code, option letters, and original answer text. Mention uncertain or inferred parts.",
              "bestAnswer": "Provide detailed question-by-question reasoning in Chinese. For every question, include recognized information, inferred information, visible keywords, reasoning, alternatives, uncertainty, and the final English answer.",
              "confidence": 0.0
            }
            """;

        String userPrompt = switch (
                normalizeAnswerMode(answerMode)
                ) {
            case "simple" -> """
                Carefully scan the entire image and identify as many questions as possible.

                Do not focus only on the clearest question.

                For every detected question:
                - preserve the original English question wording;
                - provide a direct final answer in English;
                - include the option letter and original answer text when visible;
                - make a reasonable guess from visible keywords when the question is incomplete;
                - clearly mark inferred or uncertain content.

                Keep balancedAnswer and bestAnswer relatively concise,
                but still return every required JSON field.
                """;

            case "balanced" -> """
                Carefully scan the entire image and identify as many questions as possible.

                For every detected question:
                - preserve the original English question and options;
                - give the final answer in English;
                - explain the main reasoning in Chinese;
                - preserve important English keywords and technical terms;
                - identify visible clues;
                - state which parts are inferred;
                - provide a reasonable answer even when the question is incomplete.

                Do not omit blurry or partially visible questions when useful keywords are available.
                """;

            default -> """
                Thoroughly scan every area of the image and identify as many questions as possible.

                Do not answer only the clearest or most central question.

                For every detected question:
                1. preserve the original English wording and visible options;
                2. identify the visible keywords, formulas, values, units, diagrams, or code;
                3. infer missing content when necessary;
                4. provide the most likely final answer in English;
                5. explain the reasoning in Chinese;
                6. preserve important English technical terms and answer text;
                7. clearly distinguish recognized content from inferred content;
                8. mention reasonable alternatives when uncertainty exists;
                9. include every detected question in recognizedText, simpleAnswer, balancedAnswer, and bestAnswer.

                Even when a question is blurry or incomplete, use visible keywords and context to make a reasonable best-effort guess instead of omitting it.
                """;
        };

        QuickRouterChatRequest.Message systemMessage =
                QuickRouterChatRequest.Message.builder()
                        .role("system")
                        .content(systemPrompt)
                        .build();

        QuickRouterChatRequest.ContentPart textPart =
                QuickRouterChatRequest.ContentPart.builder()
                        .type("text")
                        .text(userPrompt)
                        .build();

        QuickRouterChatRequest.ContentPart imagePart =
                QuickRouterChatRequest.ContentPart.builder()
                        .type("image_url")
                        .imageUrl(
                                QuickRouterChatRequest.ImageUrl.builder()
                                        .url(imageDataUrl)
                                        .detail("high")
                                        .build()
                        )
                        .build();

        QuickRouterChatRequest.Message userMessage =
                QuickRouterChatRequest.Message.builder()
                        .role("user")
                        .content(
                                List.of(
                                        textPart,
                                        imagePart
                                )
                        )
                        .build();

        return QuickRouterChatRequest.builder()
                .model(model)
                .messages(
                        List.of(
                                systemMessage,
                                userMessage
                        )
                )
                .temperature(temperature)
                .maxTokens(maxTokens)
                .stream(false)
                .build();
    }

    private String extractContent(
            QuickRouterChatResponse response
    ) {
        if (response == null
                || response.getChoices() == null
                || response.getChoices().isEmpty()
                || response.getChoices().get(0) == null
                || response.getChoices().get(0).getMessage() == null
                || !StringUtils.hasText(
                response.getChoices()
                        .get(0)
                        .getMessage()
                        .getContent()
        )) {

            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "QUICKROUTER_EMPTY_RESPONSE",
                    "QuickRouter 未返回有效结果"
            );
        }

        return response.getChoices()
                .get(0)
                .getMessage()
                .getContent()
                .trim();
    }

    private AiQuestionResult parseResult(String content) {
        String json = cleanJson(content);

        try {
            AiQuestionResult result =
                    objectMapper.readValue(
                            json,
                            AiQuestionResult.class
                    );

            normalizeResult(result);

            return result;

        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "AI_RESPONSE_FORMAT_INVALID",
                    "AI 返回内容不是有效 JSON："
                            + limitText(content, 300)
            );
        }
    }

    private String cleanJson(String content) {
        String value = content.trim();

        if (value.startsWith("```json")) {
            value = value.substring(7);
        } else if (value.startsWith("```")) {
            value = value.substring(3);
        }

        if (value.endsWith("```")) {
            value = value.substring(
                    0,
                    value.length() - 3
            );
        }

        value = value.trim();

        int firstBrace = value.indexOf('{');
        int lastBrace = value.lastIndexOf('}');

        if (firstBrace >= 0 && lastBrace > firstBrace) {
            value = value.substring(
                    firstBrace,
                    lastBrace + 1
            );
        }

        return value;
    }

    private void normalizeResult(AiQuestionResult result) {
        if (result == null) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "AI_EMPTY_RESULT",
                    "AI 未返回有效搜题结果"
            );
        }

        if (!StringUtils.hasText(result.getQuestionNo())) {
            result.setQuestionNo("未识别题号");
        }

        if (!StringUtils.hasText(result.getQuestionTitle())) {
            result.setQuestionTitle("未识别题目");
        }

        if (result.getRecognizedText() == null) {
            result.setRecognizedText("");
        }

        if (!StringUtils.hasText(result.getQuestionType())) {
            result.setQuestionType("unknown");
        }

        if (!StringUtils.hasText(result.getSimpleAnswer())) {
            result.setSimpleAnswer("暂时无法确定答案");
        }

        if (!StringUtils.hasText(result.getBalancedAnswer())) {
            result.setBalancedAnswer("暂时无法生成简要解析");
        }

        if (!StringUtils.hasText(result.getBestAnswer())) {
            result.setBestAnswer("暂时无法生成完整解析");
        }

        BigDecimal confidence = result.getConfidence();

        if (confidence == null) {
            result.setConfidence(BigDecimal.ZERO);
        } else if (confidence.compareTo(BigDecimal.ZERO) < 0) {
            result.setConfidence(BigDecimal.ZERO);
        } else if (confidence.compareTo(BigDecimal.ONE) > 0) {
            result.setConfidence(BigDecimal.ONE);
        }
    }

    private String normalizeAnswerMode(String value) {
        if (!StringUtils.hasText(value)) {
            return "best";
        }

        return switch (value.trim().toLowerCase()) {
            case "simple" -> "simple";
            case "balanced" -> "balanced";
            default -> "best";
        };
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "QUICKROUTER_API_KEY_NOT_CONFIGURED",
                    "QuickRouter API Key 尚未配置"
            );
        }

        if (!StringUtils.hasText(model)) {
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "QUICKROUTER_MODEL_NOT_CONFIGURED",
                    "QuickRouter 模型尚未配置"
            );
        }
    }

    private String normalizeBaseUrl() {
        String value = baseUrl.trim();

        while (value.endsWith("/")) {
            value = value.substring(
                    0,
                    value.length() - 1
            );
        }

        return value;
    }

    private String limitText(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "未知错误";
        }

        String normalized = value
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();

        if (normalized.length() <= maxLength) {
            return normalized;
        }

        return normalized.substring(0, maxLength);
    }
}