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

    /**
     * 数据库主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 摄像头业务编号，例如 cam_001。
     */
    private String cameraCode;

    /**
     * 摄像头名称。
     */
    private String cameraName;

    /**
     * 安装位置。
     */
    private String location;

    /**
     * ONLINE、OFFLINE、MAINTENANCE。
     */
    private String status;

    /**
     * WEBRTC、HLS、IFRAME、MJPEG。
     */
    private String streamType;

    /**
     * 原始播放地址。
     *
     * 注意：不能直接返回给普通用户。
     */
    private String streamSourceUrl;

    /**
     * 截图地址。
     */
    private String snapshotUrl;

    /**
     * 摄像头说明。
     */
    private String description;

    /**
     * 是否启用。
     */
    private Integer isEnabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}