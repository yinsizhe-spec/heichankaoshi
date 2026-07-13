package com.heichan.camera.camera.model;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 摄像头访问权限联合查询结果。
 *
 * 用于接收 app_user、camera、
 * user_camera_permission 三张表的联合查询结果。
 */
@Data
public class CameraAccessRow {

    /**
     * 用户数据库主键。
     */
    private Long userId;

    private String username;

    private String userStatus;

    /**
     * 摄像头数据库主键。
     */
    private Long cameraDatabaseId;

    /**
     * 摄像头业务编号，例如 cam_001。
     */
    private String cameraCode;

    private String cameraName;

    private String location;

    /**
     * ONLINE、OFFLINE、MAINTENANCE。
     */
    private String cameraStatus;

    /**
     * 摄像头是否启用。
     */
    private Integer cameraEnabled;

    /**
     * 权限数据库主键。
     *
     * 如果用户没有权限，该字段为 null。
     */
    private Long permissionId;

    private LocalTime accessStartTime;

    private LocalTime accessEndTime;

    private LocalDate validFrom;

    private LocalDate validUntil;

    /**
     * 权限是否启用。
     */
    private Integer permissionEnabled;
}