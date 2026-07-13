package com.heichan.camera.camera.service;

import com.heichan.camera.camera.ai.QuestionAiClient;
import com.heichan.camera.camera.dto.AiQuestionItem;
import com.heichan.camera.camera.dto.AiQuestionResult;
import com.heichan.camera.camera.dto.CameraAnalysisRequest;
import com.heichan.camera.camera.dto.CameraAnalysisResponse;
import com.heichan.camera.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * 摄像头 AI 多题分析服务。
 */
@Service
@RequiredArgsConstructor
public class CameraAnalysisService {

    private static final Set<String> SUPPORTED_MODES =
            Set.of("simple", "balanced", "best");

    private final CameraAccessService cameraAccessService;
    private final CameraSnapshotService cameraSnapshotService;
    private final QuestionAiClient questionAiClient;

    @Value("${app.time-zone:Asia/Kuala_Lumpur}")
    private String timeZone;

    public CameraAnalysisResponse analyze(
            String username,
            String cameraCode,
            CameraAnalysisRequest request
    ) {
        validateUsername(username);
        validateCameraCode(cameraCode);

        String normalizedCameraCode = cameraCode.trim();

        cameraAccessService.checkAccessOrThrow(
                username,
                normalizedCameraCode
        );

        String answerMode = normalizeAnswerMode(
                request == null
                        ? null
                        : request.getAnswerMode()
        );

        byte[] imageBytes =
                cameraSnapshotService.captureSnapshot(
                        normalizedCameraCode
                );

        if (imageBytes == null || imageBytes.length == 0) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "CAMERA_SNAPSHOT_EMPTY",
                    "摄像头截图内容为空"
            );
        }

        String base64Image = Base64.getEncoder()
                .encodeToString(imageBytes);

        AiQuestionResult aiResult =
                questionAiClient.analyzeQuestion(
                        "image/jpeg",
                        base64Image,
                        answerMode
                );

        List<AiQuestionItem> questions =
                normalizeQuestions(aiResult);

        return CameraAnalysisResponse.builder()
                .cameraId(normalizedCameraCode)
                .capturedAt(
                        OffsetDateTime.now(resolveZoneId())
                )
                .confidence(
                        calculateAverageConfidence(questions)
                )
                .questionCount(questions.size())
                .summary(
                        buildSummary(
                                aiResult,
                                questions.size()
                        )
                )
                .questions(questions)
                .build();
    }

    private List<AiQuestionItem> normalizeQuestions(
            AiQuestionResult aiResult
    ) {
        if (aiResult == null) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "AI_EMPTY_RESULT",
                    "AI 未返回有效分析结果"
            );
        }

        List<AiQuestionItem> source =
                aiResult.getQuestions();

        if (source == null || source.isEmpty()) {
            return new ArrayList<>();
        }

        List<AiQuestionItem> result =
                new ArrayList<>();

        int index = 1;

        for (AiQuestionItem item : source) {
            if (item == null) {
                continue;
            }

            item.setQuestionNo(
                    defaultString(
                            item.getQuestionNo(),
                            "第 " + index + " 题"
                    )
            );

            item.setQuestionTitle(
                    defaultString(
                            item.getQuestionTitle(),
                            defaultString(
                                    item.getRecognizedText(),
                                    "未识别题目"
                            )
                    )
            );

            item.setRecognizedText(
                    defaultString(
                            item.getRecognizedText(),
                            item.getQuestionTitle()
                    )
            );

            item.setQuestionType(
                    defaultString(
                            item.getQuestionType(),
                            "unknown"
                    )
            );

            item.setSimpleAnswer(
                    defaultString(
                            item.getSimpleAnswer(),
                            "No clear answer detected."
                    )
            );

            item.setBalancedAnswer(
                    defaultString(
                            item.getBalancedAnswer(),
                            "Answer: "
                                    + item.getSimpleAnswer()
                                    + " 中文解释：AI 未返回额外解释。"
                    )
            );

            item.setBestAnswer(
                    defaultString(
                            item.getBestAnswer(),
                            "Final Answer: "
                                    + item.getSimpleAnswer()
                                    + " 中文详细解释：AI 未返回额外详细解释。"
                    )
            );

            item.setConfidence(
                    normalizeConfidence(
                            item.getConfidence()
                    )
            );

            result.add(item);
            index++;
        }

        return result;
    }

    private BigDecimal calculateAverageConfidence(
            List<AiQuestionItem> questions
    ) {
        if (questions == null || questions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;
        int count = 0;

        for (AiQuestionItem question : questions) {
            if (question == null
                    || question.getConfidence() == null) {
                continue;
            }

            total = total.add(
                    normalizeConfidence(
                            question.getConfidence()
                    )
            );
            count++;
        }

        if (count == 0) {
            return BigDecimal.ZERO;
        }

        return total.divide(
                BigDecimal.valueOf(count),
                4,
                RoundingMode.HALF_UP
        );
    }

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

    private String buildSummary(
            AiQuestionResult aiResult,
            int questionCount
    ) {
        if (aiResult != null
                && StringUtils.hasText(aiResult.getSummary())) {
            return aiResult.getSummary().trim();
        }

        if (questionCount == 0) {
            return "当前画面中未识别到明确题目。";
        }

        return "本次共识别到 "
                + questionCount
                + " 道题目，已生成英文答案和中文解释。";
    }

    private String normalizeAnswerMode(String value) {
        String mode = StringUtils.hasText(value)
                ? value.trim().toLowerCase()
                : "best";

        if (!SUPPORTED_MODES.contains(mode)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "ANSWER_MODE_INVALID",
                    "answerMode 只支持 simple、balanced 或 best"
            );
        }

        return mode;
    }

    private void validateUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_UNAUTHORIZED",
                    "请先登录"
            );
        }
    }

    private void validateCameraCode(String cameraCode) {
        if (!StringUtils.hasText(cameraCode)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "CAMERA_ID_REQUIRED",
                    "摄像头编号不能为空"
            );
        }
    }

    private String defaultString(
            String value,
            String defaultValue
    ) {
        return StringUtils.hasText(value)
                ? value.trim()
                : defaultValue;
    }

    private ZoneId resolveZoneId() {
        try {
            return ZoneId.of(timeZone);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "系统时区配置错误：" + timeZone,
                    exception
            );
        }
    }
}
