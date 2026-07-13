package com.heichan.camera.camera.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 摄像头实体。
 */
@Data
@TableName("camera")
public class Camera {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 摄像头业务编号，例如 cam_001。
     */
    private String cameraCode;

    private String cameraName;

    private String location;

    /**
     * ONLINE、OFFLINE、MAINTENANCE。
     */
    private String status;

    /**
     * IFRAME、HLS、WEBRTC、MJPEG。
     */
    private String streamType;

    /**
     * 浏览器播放地址。
     */
    private String streamSourceUrl;

    /**
     * 后端 FFmpeg 截图使用的原始视频流地址。
     */
    private String sourceStreamUrl;

    private String snapshotUrl;

    private String description;

    /**
     * 1启用，0禁用。
     */
    private Integer isEnabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}