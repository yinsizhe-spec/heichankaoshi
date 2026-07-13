package com.heichan.camera.camera.model;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 摄像头和访问权限关联查询结果。
 *
 * 该类不是接口响应 DTO。
 */
@Data
public class CameraPermissionRow {

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

    private String status;

    private String streamType;

    private LocalTime accessStartTime;

    private LocalTime accessEndTime;

    private LocalDate validFrom;

    private LocalDate validUntil;
}