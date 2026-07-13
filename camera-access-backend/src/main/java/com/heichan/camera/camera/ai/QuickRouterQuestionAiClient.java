package com.heichan.camera.camera.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heichan.camera.camera.ai.dto.QuickRouterChatRequest;
import com.heichan.camera.camera.ai.dto.QuickRouterChatResponse;
import com.heichan.camera.camera.dto.AiQuestionItem;
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
import java.util.ArrayList;
import java.util.List;

/**
 * QuickRouter 视觉多题分析客户端。
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

    @Value("${ai.quickrouter.max-tokens:6000}")
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

        String normalizedMimeType =
                StringUtils.hasText(mimeType)
                        ? mimeType.trim()
                        : "image/jpeg";

        String imageDataUrl =
                "data:"
                        + normalizedMimeType
                        + ";base64,"
                        + base64Image;

        QuickRouterChatRequest requestBody =
                buildRequest(
                        imageDataUrl,
                        answerMode
                );

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

            return parseResult(
                    extractContent(response)
            );
        } catch (
                HttpClientErrorException.Unauthorized exception
        ) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "QUICKROUTER_UNAUTHORIZED",
                    "QuickRouter API Key 无效或已过期"
            );
        } catch (
                HttpClientErrorException.Forbidden exception
        ) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "QUICKROUTER_FORBIDDEN",
                    "QuickRouter 拒绝请求，请检查模型权限、账户额度或令牌分组"
            );
        } catch (
                HttpClientErrorException.TooManyRequests exception
        ) {
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
                                    exception
                                            .getResponseBodyAsString(),
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
                You are a visual exam question recognition and answering assistant.

                CRITICAL REQUIREMENT:
                Your primary task is to ANSWER every detected question.
                Do not only explain, translate, summarize or restate the question.
                A response is invalid if a detected question contains no concrete final answer.

                MULTIPLE QUESTIONS:
                1. Scan the entire image from top to bottom and left to right.
                2. Identify every visible or reasonably inferable question.
                3. Do not stop after the first question.
                4. Return one separate object for each question.
                5. Do not combine several questions into one object.
                6. Preserve the visual order.

                QUESTION NUMBER:
                7. questionNo must match the exact question number visible in the image.
                8. Preserve formats such as "1", "2.", "(3)", "Q4", "Question 5",
                   "6(a)", "6(b)", "7(i)" and "Section B - 2".
                9. Do not renumber a question when a visible number exists.
                10. Only generate a sequential number when no usable question number is visible.
                11. Preserve sub-question labels separately.

                UNCLEAR QUESTIONS:
                12. Do not omit a question only because it is blurry, cropped,
                    partially covered, tilted or low resolution.
                13. Use as much reasonable inference as possible from:
                    - visible keywords
                    - partial words and sentences
                    - answer choices
                    - formulas
                    - source code
                    - diagrams
                    - tables
                    - units
                    - nearby questions
                    - subject context
                    - common exam wording
                    - common textbook patterns
                14. Reconstruct the most likely question when sufficient clues exist.
                15. When several interpretations are possible, choose the most likely one.
                16. Mention uncertainty and alternatives in bestAnswer.
                17. Lower confidence when substantial inference is required.
                18. Do not invent unrelated content with no visual evidence.

                ANSWER LANGUAGE:
                19. The final answer must be written in English.
                20. The explanation must be written in Chinese.
                21. Important English terms, option text, formulas, code and identifiers
                    must remain in their original form.

                ANSWER RULES:
                22. simpleAnswer must contain the direct final answer in English.
                23. simpleAnswer must not explain, translate or restate the question.
                24. simpleAnswer must begin directly with the answer.
                25. Do not start simpleAnswer with:
                    - "The question asks..."
                    - "This question is about..."
                    - "You need to..."
                    - "The image shows..."
                    - "It appears to be..."

                26. Correct simpleAnswer examples:
                    - "B. extends"
                    - "True"
                    - "False"
                    - "42"
                    - "The time complexity is O(n log n)."
                    - "Encapsulation protects an object's internal state."
                    - "SELECT * FROM users;"

                27. Incorrect simpleAnswer examples:
                    - "This question asks about Java inheritance."
                    - "The image contains a programming question."
                    - "You need to select the correct option."
                    - "This is asking for the definition of polymorphism."

                28. For a single-choice question:
                    - select exactly one most likely option
                    - include the option letter
                    - include the original option text when visible

                29. For a multiple-choice question:
                    - include every selected option letter
                    - include option text when visible

                30. For a true-or-false question:
                    - answer "True" or "False"

                31. For a fill-in-the-blank question:
                    - provide the exact word, phrase, number or code

                32. For a calculation question:
                    - provide the final value and unit

                33. For a programming question:
                    - provide the requested output, code, keyword, algorithm,
                      syntax or conclusion

                34. For a short-answer question:
                    - provide a concise English answer that directly satisfies the question

                35. balancedAnswer must use this order:
                    - English final answer first
                    - Chinese explanation second

                36. balancedAnswer should follow:
                    "Answer: [English final answer]. 中文解释：[Chinese explanation]."

                37. bestAnswer must use this order:
                    - English final answer first
                    - detailed Chinese explanation second

                38. bestAnswer should follow:
                    "Final Answer: [English final answer]. 中文详细解释：[Detailed Chinese reasoning]."

                39. Even when uncertain:
                    - provide a concrete likely English answer first
                    - explain uncertainty in Chinese
                    - lower confidence

                40. Do not respond only with uncertainty statements when a plausible
                    answer can be inferred.

                OUTPUT:
                41. Return exactly one valid JSON object.
                42. Do not use Markdown code fences.
                43. Do not output any text before or after the JSON.
                44. Return exactly this structure:

                {
                  "summary": "中文总结",
                  "questions": [
                    {
                      "questionNo": "与画面匹配的原始题号",
                      "questionTitle": "识别出的题目标题",
                      "recognizedText": "完整题干和选项",
                      "questionType": "single_choice|multiple_choice|true_false|calculation|short_answer|fill_blank|programming|unknown",
                      "simpleAnswer": "Direct final answer in English",
                      "balancedAnswer": "Answer: [English final answer]. 中文解释：[Chinese explanation].",
                      "bestAnswer": "Final Answer: [English final answer]. 中文详细解释：[Detailed Chinese reasoning].",
                      "confidence": 0.0
                    }
                  ]
                }
                """;

        String userPrompt =
                buildUserPrompt(answerMode);

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
                                QuickRouterChatRequest.ImageUrl
                                        .builder()
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

    private String buildUserPrompt(
            String answerMode
    ) {
        return switch (
                normalizeAnswerMode(answerMode)
        ) {
            case "simple" -> """
                    Scan the complete image and identify every visible or reasonably
                    inferable question.

                    You must answer every question.
                    Do not only explain or restate the question.

                    Preserve the exact visible question number.

                    For every question:
                    - simpleAnswer: direct final answer in English
                    - balancedAnswer: English answer first, Chinese explanation second
                    - bestAnswer: English answer first, detailed Chinese explanation second

                    Prioritize concise answers, but still populate all three fields.
                    """;

            case "balanced" -> """
                    Scan the complete image and identify every visible or reasonably
                    inferable question.

                    You must solve and answer every question.
                    Do not only describe what the question means.

                    Preserve the exact visible question number.

                    For every question:
                    - provide the direct final answer in English
                    - then explain the reason in Chinese
                    - preserve English terms, formulas, code and option text

                    Use:
                    "Answer: [English final answer]. 中文解释：[Chinese explanation]."

                    Return one separate object for every question.
                    """;

            default -> """
                    Thoroughly scan the entire image and identify every visible,
                    partial, blurry, cropped or reasonably inferable question.

                    You must solve and answer every question.
                    Do not only explain, translate, summarize or restate the question.

                    Preserve the exact visible question number.
                    Never renumber visible questions.

                    For unclear questions, infer the most likely complete question
                    and answer from visible keywords, answer choices, formulas,
                    source code, diagrams, tables, units and surrounding context.

                    For every question:
                    - simpleAnswer: direct final answer in English
                    - balancedAnswer: English answer first, Chinese explanation second
                    - bestAnswer: English final answer first, detailed Chinese reasoning second

                    Use:
                    "Final Answer: [English final answer]. 中文详细解释：[Detailed Chinese reasoning]."

                    Even when uncertain, provide the most likely concrete English answer,
                    explain uncertainty in Chinese, and lower confidence.

                    Return one separate object for every detected question.
                    """;
        };
    }

    private String extractContent(
            QuickRouterChatResponse response
    ) {
        if (response == null
                || response.getChoices() == null
                || response.getChoices().isEmpty()
                || response.getChoices().get(0) == null
                || response.getChoices()
                        .get(0)
                        .getMessage() == null
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

    private AiQuestionResult parseResult(
            String content
    ) {
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
                    "AI 返回内容不是有效的多题 JSON："
                            + limitText(content, 500)
            );
        }
    }

    private String cleanJson(String content) {
        if (!StringUtils.hasText(content)) {
            return "{}";
        }

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

        if (firstBrace >= 0
                && lastBrace > firstBrace) {
            value = value.substring(
                    firstBrace,
                    lastBrace + 1
            );
        }

        return value;
    }

    private void normalizeResult(
            AiQuestionResult result
    ) {
        if (result == null) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "AI_EMPTY_RESULT",
                    "AI 未返回有效搜题结果"
            );
        }

        if (result.getQuestions() == null) {
            result.setQuestions(new ArrayList<>());
        }

        List<AiQuestionItem> normalized =
                new ArrayList<>();

        int index = 1;

        for (AiQuestionItem item
                : result.getQuestions()) {
            if (item == null) {
                continue;
            }

            if (!StringUtils.hasText(
                    item.getQuestionNo()
            )) {
                item.setQuestionNo(
                        "第 " + index + " 题"
                );
            } else {
                item.setQuestionNo(
                        item.getQuestionNo().trim()
                );
            }

            if (!StringUtils.hasText(
                    item.getQuestionTitle()
            )) {
                item.setQuestionTitle(
                        StringUtils.hasText(
                                item.getRecognizedText()
                        )
                                ? item.getRecognizedText()
                                : "未识别题目"
                );
            }

            if (!StringUtils.hasText(
                    item.getRecognizedText()
            )) {
                item.setRecognizedText(
                        item.getQuestionTitle()
                );
            }

            if (!StringUtils.hasText(
                    item.getQuestionType()
            )) {
                item.setQuestionType("unknown");
            }

            if (!StringUtils.hasText(
                    item.getSimpleAnswer()
            )) {
                item.setSimpleAnswer(
                        "No clear answer detected."
                );
            }

            if (!StringUtils.hasText(
                    item.getBalancedAnswer()
            )) {
                item.setBalancedAnswer(
                        "Answer: "
                                + item.getSimpleAnswer()
                                + " 中文解释：AI 未返回额外解释。"
                );
            }

            if (!StringUtils.hasText(
                    item.getBestAnswer()
            )) {
                item.setBestAnswer(
                        "Final Answer: "
                                + item.getSimpleAnswer()
                                + " 中文详细解释：AI 未返回额外详细解释。"
                );
            }

            item.setConfidence(
                    normalizeConfidence(
                            item.getConfidence()
                    )
            );

            normalized.add(item);
            index++;
        }

        result.setQuestions(normalized);

        if (!StringUtils.hasText(
                result.getSummary()
        )) {
            result.setSummary(
                    normalized.isEmpty()
                            ? "当前画面中未识别到明确题目。"
                            : "本次共识别到 "
                            + normalized.size()
                            + " 道题目，已生成英文答案和中文解释。"
            );
        }
    }

    private BigDecimal normalizeConfidence(
            BigDecimal confidence
    ) {
        if (confidence == null) {
            return BigDecimal.ZERO;
        }

        if (confidence.compareTo(
                BigDecimal.ZERO
        ) < 0) {
            return BigDecimal.ZERO;
        }

        if (confidence.compareTo(
                BigDecimal.ONE
        ) > 0) {
            return BigDecimal.ONE;
        }

        return confidence;
    }

    private String normalizeAnswerMode(
            String value
    ) {
        if (!StringUtils.hasText(value)) {
            return "best";
        }

        return switch (
                value.trim().toLowerCase()
        ) {
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

    private String limitText(
            String value,
            int maxLength
    ) {
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

        return normalized.substring(
                0,
                maxLength
        );
    }
}
