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
 *
 * 支持三种回答模式：
 *
 * simple：
 * 追求最快响应，只生成直接答案，不进行详细解释。
 *
 * balanced：
 * 响应速度和回答质量之间保持平衡，提供简短中文解释。
 *
 * best：
 * 使用高精度图片分析，尽可能识别所有题目，并生成详细解释。
 */
@Component
@RequiredArgsConstructor
public class QuickRouterQuestionAiClient implements QuestionAiClient {

    @Qualifier("quickRouterRestClient")
    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    @Value("${ai.quickrouter.base-url:https://api.quickrouter.ai/v1}")
    private String baseUrl;

    @Value("${ai.quickrouter.api-key:}")
    private String apiKey;

    @Value("${ai.quickrouter.model:gpt-4o}")
    private String model;

    /**
     * 最优回答模式允许使用的最大输出长度。
     */
    @Value("${ai.quickrouter.max-tokens:6000}")
    private int maxTokens;

    /**
     * 最优回答模式使用的默认温度。
     */
    @Value("${ai.quickrouter.temperature:0.2}")
    private double temperature;

    /**
     * 分析摄像头截图中的问题。
     *
     * @param mimeType    图片 MIME 类型
     * @param base64Image Base64 图片数据
     * @param answerMode  回答模式：simple、balanced、best
     * @return 多题分析结果
     */
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

        String normalizedMimeType = StringUtils.hasText(mimeType)
                ? mimeType.trim()
                : "image/jpeg";

        String imageDataUrl =
                "data:" + normalizedMimeType + ";base64," + base64Image;

        QuickRouterChatRequest requestBody =
                buildRequest(imageDataUrl, answerMode);

        try {
            QuickRouterChatResponse response = restClient
                    .post()
                    .uri(normalizeBaseUrl() + "/chat/completions")
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + apiKey
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(QuickRouterChatResponse.class);

            return parseResult(extractContent(response));

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

    /**
     * 根据回答模式构建不同的 AI 请求。
     */
    private QuickRouterChatRequest buildRequest(
            String imageDataUrl,
            String answerMode
    ) {
        AnswerModeConfig config =
                getAnswerModeConfig(answerMode);

        String systemPrompt =
                buildSystemPrompt(config.mode());

        String userPrompt =
                buildUserPrompt(config.mode());

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
                                        .detail(config.imageDetail())
                                        .build()
                        )
                        .build();

        QuickRouterChatRequest.Message userMessage =
                QuickRouterChatRequest.Message.builder()
                        .role("user")
                        .content(List.of(textPart, imagePart))
                        .build();

        return QuickRouterChatRequest.builder()
                .model(model)
                .messages(
                        List.of(
                                systemMessage,
                                userMessage
                        )
                )
                .temperature(config.temperature())
                .maxTokens(config.maxTokens())
                .stream(false)
                .build();
    }

    /**
     * 获取不同回答模式的运行参数。
     *
     * simple：
     * 使用较低图片分析精度和较少输出 token，
     * 只要求模型快速生成答案。
     *
     * balanced：
     * 使用高精度图片识别，但限制解释长度。
     *
     * best：
     * 使用完整的配置参数，追求识别率和解释质量。
     */
    private AnswerModeConfig getAnswerModeConfig(
            String answerMode
    ) {
        String normalizedMode =
                normalizeAnswerMode(answerMode);

        return switch (normalizedMode) {
            case "simple" -> new AnswerModeConfig(
                    "simple",
                    "auto",
                    Math.min(maxTokens, 800),
                    0.0
            );

            case "balanced" -> new AnswerModeConfig(
                    "balanced",
                    "high",
                    Math.min(maxTokens, 2200),
                    0.1
            );

            default -> new AnswerModeConfig(
                    "best",
                    "high",
                    maxTokens,
                    temperature
            );
        };
    }

    /**
     * 根据回答模式生成系统提示词。
     */
    private String buildSystemPrompt(String mode) {
        return switch (mode) {
            case "simple" -> buildSimpleSystemPrompt();
            case "balanced" -> buildBalancedSystemPrompt();
            default -> buildBestSystemPrompt();
        };
    }

    /**
     * 最简回答模式提示词。
     *
     * 重点是降低输出长度和推理复杂度，
     * 尽快返回所有题目的直接答案。
     */
    private String buildSimpleSystemPrompt() {
        return """
                You are a fast visual exam answering assistant.

                Your highest priority is response speed.

                CRITICAL REQUIREMENTS:

                1. Scan the complete image once from top to bottom and left to right.
                2. Identify every clearly visible question.
                3. Do not stop after the first question.
                4. Return one separate JSON object for each question.
                5. Preserve the visible question number.
                6. Answer every detected question directly.
                7. Questions are most likely written in English.
                8. Final answers must be written in English.
                9. Do not provide long reasoning.
                10. Do not translate or restate the complete question.
                11. When text is slightly unclear, make the most likely guess quickly.
                12. Do not spend output tokens discussing uncertainty.
                13. Keep all output fields concise.

                ANSWER RULES:

                For single-choice questions:
                - Return the option letter and option text when visible.
                - Example: "B. Encapsulation"

                For multiple-choice questions:
                - Return all selected option letters.
                - Example: "A, C and D"

                For true-or-false questions:
                - Return "True" or "False".

                For calculation questions:
                - Return the final result and unit.
                - Do not include complete calculation steps.

                For fill-in-the-blank questions:
                - Return the exact missing word, phrase, number or code.

                For programming questions:
                - Return the requested code, output, syntax, keyword or conclusion.

                For short-answer questions:
                - Return one concise English sentence.

                OUTPUT REQUIREMENTS:

                Return exactly one valid JSON object.
                Do not use Markdown.
                Do not use code fences.
                Do not output any text before or after the JSON.

                Return exactly this structure:

                {
                  "summary": "简短中文总结",
                  "questions": [
                    {
                      "questionNo": "画面中的原始题号",
                      "questionTitle": "简短题目标题",
                      "recognizedText": "简短识别内容",
                      "questionType": "single_choice|multiple_choice|true_false|calculation|short_answer|fill_blank|programming|unknown",
                      "simpleAnswer": "Direct final answer in English",
                      "balancedAnswer": "Answer: [English final answer]. 中文解释：简短解释。",
                      "bestAnswer": "Final Answer: [English final answer]. 中文详细解释：简短解释。",
                      "confidence": 0.0
                    }
                  ]
                }

                simpleAnswer, balancedAnswer and bestAnswer must contain
                the same final answer.

                Keep the complete JSON response as short as possible.
                """;
    }

    /**
     * 均衡回答模式提示词。
     */
    private String buildBalancedSystemPrompt() {
        return """
                You are a visual exam question recognition and answering assistant.

                Your goal is to balance response speed and answer quality.

                CRITICAL REQUIREMENTS:

                1. Scan the entire image from top to bottom and left to right.
                2. Identify every visible or reasonably inferable question.
                3. Do not stop after the first question.
                4. Return one separate object for each question.
                5. Preserve the visual order.
                6. Preserve the exact question number visible in the image.
                7. Preserve sub-question labels such as 2(a), 2(b), 3(i) and 3(ii).
                8. Answer every detected question.
                9. Do not only explain, translate, summarize or restate the question.
                10. Final answers must be written in English.
                11. Explanations must be written in Chinese.
                12. Preserve English technical terms, formulas, code and identifiers.
                13. Keep explanations concise.
                14. Do not provide unnecessary background knowledge.

                UNCLEAR QUESTIONS:

                15. Do not immediately omit a slightly blurry or cropped question.
                16. Infer the most likely question from visible keywords,
                    answer choices, formulas, source code, units and surrounding context.
                17. When several interpretations are possible,
                    select the most likely answer and lower confidence.
                18. Do not invent unrelated content without visual evidence.

                ANSWER RULES:

                simpleAnswer:
                - Direct final answer in English only.
                - No explanation.
                - Do not restate the question.

                balancedAnswer:
                - English answer first.
                - Brief Chinese explanation second.
                - Use this format:
                  "Answer: [English final answer]. 中文解释：[Brief Chinese explanation]."

                bestAnswer:
                - English answer first.
                - Moderately detailed Chinese explanation second.
                - Use this format:
                  "Final Answer: [English final answer]. 中文详细解释：[Chinese reasoning]."

                For single-choice questions:
                - Select exactly one most likely option.
                - Include the option letter.
                - Include the visible option text when possible.

                For multiple-choice questions:
                - Include every selected option letter.
                - Include option text when possible.

                For true-or-false questions:
                - Answer "True" or "False".

                For calculations:
                - Include the final result and unit.
                - Include only the essential calculation method in the explanation.

                For programming questions:
                - Provide the requested output, code, syntax,
                  keyword, algorithm or conclusion.

                OUTPUT REQUIREMENTS:

                Return exactly one valid JSON object.
                Do not use Markdown code fences.
                Do not output any text before or after the JSON.

                Return exactly this structure:

                {
                  "summary": "中文总结",
                  "questions": [
                    {
                      "questionNo": "与画面匹配的原始题号",
                      "questionTitle": "识别出的题目标题",
                      "recognizedText": "识别出的题干和选项",
                      "questionType": "single_choice|multiple_choice|true_false|calculation|short_answer|fill_blank|programming|unknown",
                      "simpleAnswer": "Direct final answer in English",
                      "balancedAnswer": "Answer: [English final answer]. 中文解释：[Brief Chinese explanation].",
                      "bestAnswer": "Final Answer: [English final answer]. 中文详细解释：[Moderately detailed Chinese reasoning].",
                      "confidence": 0.0
                    }
                  ]
                }
                """;
    }

    /**
     * 最优回答模式提示词。
     *
     * 重点是尽可能识别所有题目，
     * 包括模糊、裁切和不完整的题目。
     */
    private String buildBestSystemPrompt() {
        return """
                You are a high-recall visual exam question recognition
                and answering assistant.

                CRITICAL REQUIREMENT:

                Your primary task is to ANSWER every detected question.

                Do not only explain, translate, summarize or restate the question.
                A response is invalid if a detected question contains
                no concrete final answer.

                MULTIPLE QUESTIONS:

                1. Scan the entire image from top to bottom and left to right.
                2. Inspect all areas of the image carefully.
                3. Identify every visible or reasonably inferable question.
                4. Do not stop after the first question.
                5. Return one separate object for each question.
                6. Do not combine several questions into one object.
                7. Preserve the visual order.

                QUESTION NUMBERS:

                8. questionNo must match the exact question number
                   visible in the image.
                9. Preserve formats such as:
                   "1", "2.", "(3)", "Q4", "Question 5",
                   "6(a)", "6(b)", "7(i)" and "Section B - 2".
                10. Do not renumber a question when a visible number exists.
                11. Only generate a sequential number when no usable
                    question number is visible.
                12. Preserve sub-question labels separately.

                UNCLEAR QUESTIONS:

                13. Do not omit a question only because it is blurry,
                    cropped, partially covered, tilted or low resolution.
                14. Use as much reasonable inference as possible from:
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
                15. Reconstruct the most likely question
                    when sufficient clues exist.
                16. When several interpretations are possible,
                    choose the most likely interpretation.
                17. Mention uncertainty and reasonable alternatives
                    in bestAnswer.
                18. Lower confidence when substantial inference is required.
                19. Do not invent unrelated content with no visual evidence.

                ANSWER LANGUAGE:

                20. The final answer must be written in English.
                21. The explanation must be written in Chinese.
                22. Important English terms, option text, formulas,
                    code and identifiers must remain in their original form.

                ANSWER RULES:

                23. simpleAnswer must contain the direct final answer in English.
                24. simpleAnswer must not explain, translate
                    or restate the question.
                25. simpleAnswer must begin directly with the answer.

                Do not begin simpleAnswer with:
                - "The question asks..."
                - "This question is about..."
                - "You need to..."
                - "The image shows..."
                - "It appears to be..."

                Correct simpleAnswer examples:
                - "B. extends"
                - "True"
                - "False"
                - "42 cm"
                - "The time complexity is O(n log n)."
                - "Encapsulation protects an object's internal state."
                - "SELECT * FROM users;"

                Incorrect simpleAnswer examples:
                - "This question asks about Java inheritance."
                - "The image contains a programming question."
                - "You need to select the correct option."
                - "This is asking for the definition of polymorphism."

                26. For a single-choice question:
                    - select exactly one most likely option
                    - include the option letter
                    - include the original option text when visible

                27. For a multiple-choice question:
                    - include every selected option letter
                    - include option text when visible

                28. For a true-or-false question:
                    - answer "True" or "False"

                29. For a fill-in-the-blank question:
                    - provide the exact word, phrase, number or code

                30. For a calculation question:
                    - provide the final value and unit

                31. For a programming question:
                    - provide the requested output, code, keyword,
                      algorithm, syntax or conclusion

                32. For a short-answer question:
                    - provide a concise English answer
                      that directly satisfies the question

                33. balancedAnswer must use this order:
                    - English final answer first
                    - Chinese explanation second

                34. balancedAnswer must follow:
                    "Answer: [English final answer]. 中文解释：[Chinese explanation]."

                35. bestAnswer must use this order:
                    - English final answer first
                    - detailed Chinese explanation second

                36. bestAnswer must follow:
                    "Final Answer: [English final answer]. 中文详细解释：[Detailed Chinese reasoning]."

                37. Even when uncertain:
                    - provide a concrete likely English answer first
                    - explain uncertainty in Chinese
                    - lower confidence

                38. Do not respond only with uncertainty statements
                    when a plausible answer can be inferred.

                OUTPUT REQUIREMENTS:

                39. Return exactly one valid JSON object.
                40. Do not use Markdown code fences.
                41. Do not output any text before or after the JSON.

                Return exactly this structure:

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
    }

    /**
     * 根据回答模式生成用户提示词。
     */
    private String buildUserPrompt(String answerMode) {
        return switch (normalizeAnswerMode(answerMode)) {
            case "simple" -> """
                    Quickly scan this image and answer all clearly visible questions.

                    Requirements:

                    - Return the direct final answers immediately.
                    - Answers must be in English.
                    - Preserve visible question numbers.
                    - Keep every field very short.
                    - Do not provide detailed reasoning.
                    - Do not repeat complete question text.
                    - For slightly unclear text, make the most likely quick guess.
                    - Populate all three answer fields using the same final answer.
                    - Return the JSON immediately.
                    """;

            case "balanced" -> """
                    Scan the complete image and identify every visible
                    or reasonably inferable question.

                    For every question:

                    - preserve the exact visible question number;
                    - provide the direct final answer in English;
                    - provide a brief Chinese explanation;
                    - preserve English terms, formulas, code and option text;
                    - infer slightly unclear content from visible clues;
                    - keep the explanation useful but concise;
                    - return one separate object for every question.
                    """;

            default -> """
                    Thoroughly inspect the entire image.

                    Identify every visible, partial, blurry, cropped
                    or reasonably inferable question.

                    You must solve and answer every question.
                    Do not only explain, translate, summarize
                    or restate the question.

                    Preserve the exact visible question number.
                    Never renumber visible questions.

                    For unclear questions, infer the most likely complete
                    question and answer from:

                    - visible keywords;
                    - answer choices;
                    - formulas;
                    - source code;
                    - diagrams;
                    - tables;
                    - units;
                    - surrounding context;
                    - common exam patterns.

                    For every question:

                    - simpleAnswer: direct final answer in English;
                    - balancedAnswer: English answer first,
                      brief Chinese explanation second;
                    - bestAnswer: English final answer first,
                      detailed Chinese reasoning second.

                    Even when uncertain, provide the most likely
                    concrete English answer first, explain uncertainty
                    in Chinese, and lower confidence.

                    Return one separate object for every detected question.
                    """;
        };
    }

    /**
     * 提取 QuickRouter 返回的正文。
     */
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

    /**
     * 将 AI 返回内容转换为后端结果对象。
     */
    private AiQuestionResult parseResult(String content) {
        String json = cleanJson(content);

        try {
            AiQuestionResult result = objectMapper.readValue(
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

    /**
     * 清除模型可能返回的 Markdown 标记。
     */
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

        if (firstBrace >= 0 && lastBrace > firstBrace) {
            value = value.substring(
                    firstBrace,
                    lastBrace + 1
            );
        }

        return value;
    }

    /**
     * 补全模型可能遗漏的字段。
     */
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

        for (AiQuestionItem item : result.getQuestions()) {
            if (item == null) {
                continue;
            }

            if (!StringUtils.hasText(item.getQuestionNo())) {
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

        if (!StringUtils.hasText(result.getSummary())) {
            result.setSummary(
                    normalized.isEmpty()
                            ? "当前画面中未识别到明确题目。"
                            : "本次共识别到 "
                              + normalized.size()
                              + " 道题目，已生成英文答案和中文解释。"
            );
        }
    }

    /**
     * 将置信度限制在 0 到 1 之间。
     */
    private BigDecimal normalizeConfidence(
            BigDecimal confidence
    ) {
        if (confidence == null) {
            return BigDecimal.ZERO;
        }

        if (confidence.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }

        if (confidence.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }

        return confidence;
    }

    /**
     * 统一回答模式。
     */
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

    /**
     * 检查 AI 配置。
     */
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

    /**
     * 删除基础地址末尾多余的斜杠。
     */
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

    /**
     * 限制错误信息长度。
     */
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

    /**
     * 每种回答模式对应的运行参数。
     *
     * @param mode        回答模式
     * @param imageDetail 图片分析精度
     * @param maxTokens   最大输出 token
     * @param temperature 生成温度
     */
    private record AnswerModeConfig(
            String mode,
            String imageDetail,
            int maxTokens,
            double temperature
    ) {
    }
}