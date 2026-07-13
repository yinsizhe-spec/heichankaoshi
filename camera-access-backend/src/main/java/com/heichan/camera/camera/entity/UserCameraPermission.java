package com.heichan.camera.permission.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 用户摄像头访问权限实体。
 */
@Data
@TableName("user_camera_permission")
public class UserCameraPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户数据库主键。
     */
    private Long userId;

    /**
     * 摄像头数据库主键。
     */
    private Long cameraId;

    /**
     * 每天允许访问的开始时间。
     */
    private LocalTime accessStartTime;

    /**
     * 每天允许访问的结束时间。
     */
    private LocalTime accessEndTime;

    /**
     * 权限开始生效日期。
     */
    private LocalDate validFrom;

    /**
     * 权限失效日期。
     */
    private LocalDate validUntil;

    /**
     * 权限是否启用。
     */
    private Integer isEnabled;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}