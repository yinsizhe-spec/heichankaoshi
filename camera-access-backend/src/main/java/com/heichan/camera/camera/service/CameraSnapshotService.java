package com.heichan.camera.camera.service;

import com.heichan.camera.camera.entity.Camera;
import com.heichan.camera.camera.mapper.CameraMapper;
import com.heichan.camera.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 摄像头截图服务。
 *
 * 使用 FFmpeg 从 camera.source_stream_url 中截取一帧 JPEG 图片。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CameraSnapshotService {

    private final CameraMapper cameraMapper;

    /**
     * FFmpeg 可执行文件路径。
     *
     * Windows 示例：
     * C:/ffmpeg/bin/ffmpeg.exe
     *
     * Linux 示例：
     * ffmpeg
     */
    @Value("${app.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    /**
     * 整个 FFmpeg 截图任务最长等待时间。
     */
    @Value("${app.ffmpeg.snapshot-timeout-seconds:20}")
    private long snapshotTimeoutSeconds;

    /**
     * 截图最大大小，默认 5MB。
     */
    @Value("${app.ffmpeg.max-image-size-bytes:5242880}")
    private long maxImageSizeBytes;

    /**
     * 从摄像头视频流中截取当前画面。
     *
     * @param cameraCode 摄像头业务编号
     * @return JPEG 图片字节数组
     */
    public byte[] captureSnapshot(String cameraCode) {
        String normalizedCameraCode = validateCameraCode(cameraCode);

        Camera camera = cameraMapper.selectByCameraCode(
                normalizedCameraCode
        );

        validateCamera(camera);

        String sourceStreamUrl =
                camera.getSourceStreamUrl().trim();

        List<String> command =
                buildFfmpegCommand(sourceStreamUrl);

        log.info(
                "开始截取摄像头画面，cameraCode={}, sourceStreamUrl={}",
                normalizedCameraCode,
                maskStreamUrl(sourceStreamUrl)
        );

        log.debug(
                "FFmpeg command={}",
                command
        );

        Process process = null;

        CompletableFuture<byte[]> imageFuture = null;
        CompletableFuture<String> errorFuture = null;

        long startTime = System.currentTimeMillis();

        try {
            ProcessBuilder processBuilder =
                    new ProcessBuilder(command);

            /*
             * stdout 用于读取 JPEG。
             * stderr 用于读取 FFmpeg 日志。
             *
             * 不能合并，否则错误日志会混入 JPEG 数据。
             */
            processBuilder.redirectErrorStream(false);

            process = processBuilder.start();

            Process runningProcess = process;

            /*
             * 异步读取 JPEG 数据。
             *
             * 不能在主线程直接 while(read)，否则 FFmpeg
             * 没有及时返回帧时会阻塞，无法进入超时判断。
             */
            imageFuture = CompletableFuture.supplyAsync(
                    () -> readImageBytes(
                            runningProcess.getInputStream()
                    )
            );

            /*
             * 同时读取错误输出，避免 stderr 缓冲区满后
             * 导致 FFmpeg 进程阻塞。
             */
            errorFuture = CompletableFuture.supplyAsync(
                    () -> readErrorText(
                            runningProcess.getErrorStream()
                    )
            );

            /*
             * 主线程等待 FFmpeg 退出。
             */
            boolean completed = process.waitFor(
                    snapshotTimeoutSeconds,
                    TimeUnit.SECONDS
            );

            if (!completed) {
                terminateProcess(process);

                cancelFuture(imageFuture);
                cancelFuture(errorFuture);

                throw new BusinessException(
                        HttpStatus.GATEWAY_TIMEOUT,
                        "SNAPSHOT_TIMEOUT",
                        "摄像头截图超时，请检查视频流是否正在推送"
                );
            }

            /*
             * FFmpeg 已退出，获取 stderr。
             */
            String errorText = getErrorText(
                    errorFuture
            );

            int exitCode = process.exitValue();

            if (exitCode != 0) {
                log.error(
                        "FFmpeg 截图失败，cameraCode={}, exitCode={}, error={}",
                        normalizedCameraCode,
                        exitCode,
                        errorText
                );

                throw new BusinessException(
                        HttpStatus.BAD_GATEWAY,
                        "SNAPSHOT_FAILED",
                        buildFfmpegErrorMessage(errorText)
                );
            }

            /*
             * FFmpeg 正常退出后，读取 JPEG 结果。
             */
            byte[] imageBytes = getImageBytes(
                    imageFuture
            );

            validateImageBytes(imageBytes);

            long elapsed =
                    System.currentTimeMillis() - startTime;

            log.info(
                    "摄像头截图成功，cameraCode={}, imageSize={} bytes, elapsed={} ms",
                    normalizedCameraCode,
                    imageBytes.length,
                    elapsed
            );

            return imageBytes;

        } catch (BusinessException exception) {
            throw exception;

        } catch (IOException exception) {
            log.error(
                    "启动 FFmpeg 失败，ffmpegPath={}",
                    ffmpegPath,
                    exception
            );

            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "FFMPEG_NOT_AVAILABLE",
                    "无法启动 FFmpeg，请确认 FFmpeg 已安装并且路径配置正确"
            );

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "SNAPSHOT_INTERRUPTED",
                    "摄像头截图任务被中断"
            );

        } finally {
            if (process != null && process.isAlive()) {
                terminateProcess(process);
            }
        }
    }

    /**
     * 构造 FFmpeg 命令。
     *
     * 等价于已经测试成功的命令：
     *
     * ffmpeg
     * -hide_banner
     * -loglevel info
     * -rtsp_transport tcp
     * -i rtsp://...
     * -map 0:v:0
     * -an
     * -frames:v 1
     * -f image2pipe
     * -vcodec mjpeg
     * pipe:1
     */
    private List<String> buildFfmpegCommand(
            String streamUrl
    ) {
        List<String> command = new ArrayList<>();

        command.add(ffmpegPath);

        command.add("-hide_banner");

        /*
         * 正式环境可以改成 error。
         * 当前排查阶段使用 info 更容易看到问题。
         */
        command.add("-loglevel");
        command.add("info");

        /*
         * 只有 RTSP 地址才加入该参数。
         */
        if (streamUrl.toLowerCase().startsWith("rtsp://")) {
            command.add("-rtsp_transport");
            command.add("tcp");
        }

        /*
         * 输入地址。
         */
        command.add("-i");
        command.add(streamUrl);

        /*
         * 明确选择第一个视频轨道。
         */
        command.add("-map");
        command.add("0:v:0");

        /*
         * 不处理音频。
         */
        command.add("-an");

        /*
         * 只截取一帧。
         */
        command.add("-frames:v");
        command.add("1");

        /*
         * JPEG 质量。
         * 数字越小质量越高。
         */
        command.add("-q:v");
        command.add("3");

        /*
         * 输出为图片流。
         */
        command.add("-f");
        command.add("image2pipe");

        command.add("-vcodec");
        command.add("mjpeg");

        /*
         * 输出到标准输出，由 Java 读取。
         */
        command.add("pipe:1");

        return command;
    }

    /**
     * 读取 FFmpeg 标准输出中的 JPEG 数据。
     */
    private byte[] readImageBytes(
            InputStream inputStream
    ) {
        try (
                InputStream stream = inputStream;
                ByteArrayOutputStream output =
                        new ByteArrayOutputStream()
        ) {
            byte[] buffer = new byte[8192];

            int length;
            long totalSize = 0;

            while ((length = stream.read(buffer)) != -1) {
                totalSize += length;

                if (totalSize > maxImageSizeBytes) {
                    throw new BusinessException(
                            HttpStatus.PAYLOAD_TOO_LARGE,
                            "SNAPSHOT_TOO_LARGE",
                            "摄像头截图大小超过限制"
                    );
                }

                output.write(
                        buffer,
                        0,
                        length
                );
            }

            return output.toByteArray();

        } catch (BusinessException exception) {
            throw exception;

        } catch (IOException exception) {
            throw new CompletionException(exception);
        }
    }

    /**
     * 读取 FFmpeg 错误输出。
     */
    private String readErrorText(
            InputStream inputStream
    ) {
        try (InputStream stream = inputStream) {
            return new String(
                    stream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            return "";
        }
    }

    /**
     * 获取异步读取的 JPEG。
     */
    private byte[] getImageBytes(
            CompletableFuture<byte[]> imageFuture
    ) {
        if (imageFuture == null) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "SNAPSHOT_EMPTY",
                    "摄像头没有返回截图数据"
            );
        }

        try {
            /*
             * FFmpeg 已退出，正常情况下应该立即完成。
             */
            return imageFuture.get(
                    5,
                    TimeUnit.SECONDS
            );

        } catch (TimeoutException exception) {
            imageFuture.cancel(true);

            throw new BusinessException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "SNAPSHOT_READ_TIMEOUT",
                    "读取摄像头截图数据超时"
            );

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "SNAPSHOT_INTERRUPTED",
                    "读取摄像头截图时任务被中断"
            );

        } catch (ExecutionException exception) {
            Throwable cause =
                    unwrapCause(exception);

            if (cause instanceof BusinessException businessException) {
                throw businessException;
            }

            log.error(
                    "读取 FFmpeg JPEG 数据失败",
                    cause
            );

            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "SNAPSHOT_READ_FAILED",
                    "读取摄像头截图数据失败"
            );
        }
    }

    /**
     * 获取 FFmpeg 错误日志。
     */
    private String getErrorText(
            CompletableFuture<String> errorFuture
    ) {
        if (errorFuture == null) {
            return "";
        }

        try {
            return errorFuture.get(
                    3,
                    TimeUnit.SECONDS
            );
        } catch (Exception exception) {
            return "";
        }
    }

    /**
     * 校验摄像头编号。
     */
    private String validateCameraCode(
            String cameraCode
    ) {
        if (!StringUtils.hasText(cameraCode)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "CAMERA_ID_REQUIRED",
                    "摄像头编号不能为空"
            );
        }

        return cameraCode.trim();
    }

    /**
     * 校验摄像头配置。
     */
    private void validateCamera(
            Camera camera
    ) {
        if (camera == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "CAMERA_NOT_FOUND",
                    "摄像头不存在"
            );
        }

        if (!Integer.valueOf(1).equals(
                camera.getIsEnabled()
        )) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "CAMERA_DISABLED",
                    "摄像头已被禁用"
            );
        }

        if (!"ONLINE".equalsIgnoreCase(
                camera.getStatus()
        )) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "CAMERA_OFFLINE",
                    "摄像头当前不在线"
            );
        }

        if (!StringUtils.hasText(
                camera.getSourceStreamUrl()
        )) {
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "SOURCE_STREAM_NOT_CONFIGURED",
                    "摄像头原始视频流地址尚未配置"
            );
        }
    }

    /**
     * 校验截图内容。
     */
    private void validateImageBytes(
            byte[] imageBytes
    ) {
        if (imageBytes == null
                || imageBytes.length == 0) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "SNAPSHOT_EMPTY",
                    "摄像头没有返回有效画面"
            );
        }

        /*
         * JPEG 文件头必须以 FF D8 开始。
         */
        boolean jpegHeaderValid =
                imageBytes.length >= 2
                        && (imageBytes[0] & 0xFF) == 0xFF
                        && (imageBytes[1] & 0xFF) == 0xD8;

        /*
         * JPEG 文件尾通常以 FF D9 结束。
         */
        boolean jpegFooterValid =
                imageBytes.length >= 2
                        && (imageBytes[
                        imageBytes.length - 2
                        ] & 0xFF) == 0xFF
                        && (imageBytes[
                        imageBytes.length - 1
                        ] & 0xFF) == 0xD9;

        if (!jpegHeaderValid) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "SNAPSHOT_FORMAT_INVALID",
                    "FFmpeg 返回的内容不是有效 JPEG 图片"
            );
        }

        if (!jpegFooterValid) {
            log.warn(
                    "JPEG 文件尾不完整，但保留当前截图，size={}",
                    imageBytes.length
            );
        }
    }

    /**
     * 终止 FFmpeg 进程。
     */
    private void terminateProcess(
            Process process
    ) {
        if (process == null
                || !process.isAlive()) {
            return;
        }

        process.destroy();

        try {
            boolean stopped = process.waitFor(
                    1,
                    TimeUnit.SECONDS
            );

            if (!stopped
                    && process.isAlive()) {
                process.destroyForcibly();

                process.waitFor(
                        1,
                        TimeUnit.SECONDS
                );
            }

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            process.destroyForcibly();
        }
    }

    private void cancelFuture(
            CompletableFuture<?> future
    ) {
        if (future != null
                && !future.isDone()) {
            future.cancel(true);
        }
    }

    /**
     * 解除 CompletableFuture 对异常的包装。
     */
    private Throwable unwrapCause(
            Throwable throwable
    ) {
        Throwable current = throwable;

        while (
                current.getCause() != null
                        && (
                        current instanceof CompletionException
                                || current instanceof ExecutionException
                )
        ) {
            current = current.getCause();
        }

        return current;
    }

    /**
     * 构造返回给前端的 FFmpeg 错误信息。
     */
    private String buildFfmpegErrorMessage(
            String errorText
    ) {
        if (!StringUtils.hasText(errorText)) {
            return "摄像头截图失败，请检查视频流地址和推流状态";
        }

        String normalized = errorText
                .replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.length() > 800) {
            normalized = normalized.substring(
                    0,
                    800
            );
        }

        return "摄像头截图失败：" + normalized;
    }

    /**
     * 日志中隐藏地址中的账号密码。
     */
    private String maskStreamUrl(
            String streamUrl
    ) {
        if (!StringUtils.hasText(streamUrl)) {
            return "";
        }

        /*
         * 例如：
         * rtsp://username:password@host/path
         *
         * 转为：
         * rtsp://***:***@host/path
         */
        return streamUrl.replaceFirst(
                "^(\\w+://)[^/@:]+:[^/@]+@",
                "$1***:***@"
        );
    }
}