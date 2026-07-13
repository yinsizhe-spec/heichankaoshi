package com.heichan.camera.camera.service;

import com.heichan.camera.camera.ai.QuestionAiClient;
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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Set;

/**
 * 摄像头 AI 搜题服务。
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
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_UNAUTHORIZED",
                    "请先登录"
            );
        }

        if (!StringUtils.hasText(cameraCode)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "CAMERA_ID_REQUIRED",
                    "摄像头编号不能为空"
            );
        }

        String normalizedCameraCode =
                cameraCode.trim();

        /*
         * 每次搜题都重新校验摄像头访问权限。
         */
        cameraAccessService.checkAccessOrThrow(
                username,
                normalizedCameraCode
        );

        String answerMode = normalizeAnswerMode(
                request == null
                        ? null
                        : request.getAnswerMode()
        );

        /*
         * 从原始流截取当前帧。
         */
        byte[] imageBytes =
                cameraSnapshotService.captureSnapshot(
                        normalizedCameraCode
                );

        String base64Image =
                Base64.getEncoder()
                        .encodeToString(imageBytes);

        /*
         * 调用 QuickRouter 视觉模型。
         */
        AiQuestionResult aiResult =
                questionAiClient.analyzeQuestion(
                        "image/jpeg",
                        base64Image,
                        answerMode
                );

        return CameraAnalysisResponse.builder()
                .cameraId(normalizedCameraCode)
                .capturedAt(
                        OffsetDateTime.now(
                                resolveZoneId()
                        )
                )
                .questionNo(
                        defaultString(
                                aiResult.getQuestionNo(),
                                "未识别题号"
                        )
                )
                .questionTitle(
                        defaultString(
                                aiResult.getQuestionTitle(),
                                "未识别题目"
                        )
                )
                .recognizedText(
                        defaultString(
                                aiResult.getRecognizedText(),
                                ""
                        )
                )
                .questionType(
                        defaultString(
                                aiResult.getQuestionType(),
                                "unknown"
                        )
                )
                .simpleAnswer(
                        defaultString(
                                aiResult.getSimpleAnswer(),
                                "暂时无法确定答案"
                        )
                )
                .balancedAnswer(
                        defaultString(
                                aiResult.getBalancedAnswer(),
                                "暂时无法生成简要解析"
                        )
                )
                .bestAnswer(
                        defaultString(
                                aiResult.getBestAnswer(),
                                "暂时无法生成完整解析"
                        )
                )
                .confidence(
                        aiResult.getConfidence() == null
                                ? BigDecimal.ZERO
                                : aiResult.getConfidence()
                )
                .build();
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

    private String defaultString(
            String value,
            String defaultValue
    ) {
        return StringUtils.hasText(value)
                ? value
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