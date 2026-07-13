package com.heichan.camera.camera.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heichan.camera.camera.entity.Camera;
import com.heichan.camera.camera.model.CameraAccessRow;
import com.heichan.camera.camera.model.CameraPermissionRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 摄像头 Mapper。
 */
@Mapper
public interface CameraMapper extends BaseMapper<Camera> {

    @Select("""
            SELECT
                c.id AS camera_database_id,
                c.camera_code,
                c.camera_name,
                c.location,
                c.status,
                c.stream_type,
                p.access_start_time,
                p.access_end_time,
                p.valid_from,
                p.valid_until
            FROM user_camera_permission p
            INNER JOIN camera c
                ON c.id = p.camera_id
            WHERE p.user_id = #{userId}
              AND p.is_enabled = 1
              AND c.is_enabled = 1
            ORDER BY c.id ASC
            """)
    List<CameraPermissionRow> selectAvailableCamerasByUserId(
            @Param("userId") Long userId
    );

    @Select("""
            SELECT
                u.id AS user_id,
                u.username,
                u.status AS user_status,

                c.id AS camera_database_id,
                c.camera_code,
                c.camera_name,
                c.location,
                c.status AS camera_status,
                c.is_enabled AS camera_enabled,

                p.id AS permission_id,
                p.access_start_time,
                p.access_end_time,
                p.valid_from,
                p.valid_until,
                p.is_enabled AS permission_enabled

            FROM app_user u
            CROSS JOIN camera c

            LEFT JOIN user_camera_permission p
                ON p.user_id = u.id
                AND p.camera_id = c.id

            WHERE u.username = #{username}
              AND c.camera_code = #{cameraCode}

            LIMIT 1
            """)
    CameraAccessRow selectCameraAccess(
            @Param("username") String username,
            @Param("cameraCode") String cameraCode
    );

    /**
     * 根据业务编号查询摄像头完整信息。
     */
    @Select("""
            SELECT
                id,
                camera_code,
                camera_name,
                location,
                status,
                stream_type,
                stream_source_url,
                source_stream_url,
                snapshot_url,
                description,
                is_enabled,
                created_at,
                updated_at
            FROM camera
            WHERE camera_code = #{cameraCode}
            LIMIT 1
            """)
    Camera selectByCameraCode(
            @Param("cameraCode") String cameraCode
    );
}