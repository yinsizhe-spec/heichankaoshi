package com.heichan.camera;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heichan.camera.camera.entity.Camera;
import com.heichan.camera.camera.mapper.CameraMapper;
import com.heichan.camera.user.entity.AppUser;
import com.heichan.camera.user.mapper.AppUserMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@SpringBootTest
class CameraAccessBackendApplicationTests {

	@Autowired
	private AppUserMapper appUserMapper;

	@Autowired
	private CameraMapper cameraMapper;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/*
	 * =========================================================
	 * 测试用户配置
	 * =========================================================
	 */

	private static final String TEST_USERNAME = "duanxu";
	private static final String TEST_EMAIL = "duanxu@gmail.com";
	private static final String TEST_PASSWORD = "123456";
	private static final String TEST_ROLE = "USER";
	private static final String TEST_USER_STATUS = "ACTIVE";

	/*
	 * =========================================================
	 * 测试摄像头配置
	 * =========================================================
	 */

	private static final String CAMERA_CODE = "cam_test";
	private static final String CAMERA_NAME = "系统测试摄像头";
	private static final String CAMERA_LOCATION = "马来西亚考试测试摄像头";
	private static final String CAMERA_STATUS = "ONLINE";
	private static final String CAMERA_STREAM_TYPE = "IFRAME";

	/*
	 * 前端浏览器播放地址。
	 *
	 * 根据你的实际服务器地址进行修改。
	 */
	private static final String CAMERA_BROWSER_URL =
			"http://64.176.57.254:8889/live/desktop";

	/*
	 * 后端 FFmpeg 截图使用的原始视频流地址。
	 *
	 * 如果后端与 MediaMTX 在同一台服务器，
	 * 建议使用 127.0.0.1。
	 */
	private static final String CAMERA_SOURCE_URL =
			"rtmp://64.176.57.254:1935/live/desktop";

	private static final String CAMERA_SNAPSHOT_URL =
			"http://64.176.57.254:7020/api/cameras/"
					+ CAMERA_CODE
					+ "/snapshot";

	private static final String CAMERA_DESCRIPTION =
			"用于考试监控、画面拍照和 AI 内容分析";

	/*
	 * =========================================================
	 * 权限配置
	 * =========================================================
	 */

	private static final LocalTime ACCESS_START_TIME =
			LocalTime.of(0, 0);

	private static final LocalTime ACCESS_END_TIME =
			LocalTime.of(23, 59);

	private static final LocalDate VALID_FROM =
			LocalDate.of(2026, 7, 13);

	private static final LocalDate VALID_UNTIL =
			LocalDate.of(2027, 7, 14);

	/**
	 * 一次创建或更新全部测试数据。
	 *
	 * 执行后会生成：
	 * 1. 一个测试用户；
	 * 2. 一个测试摄像头；
	 * 3. 一条用户摄像头权限；
	 * 4. 可用于登录的 BCrypt 密码。
	 *
	 * 本方法支持重复运行：
	 * 数据存在时更新，不存在时创建。
	 */
	@Test
	void createOrUpdateAllTestData() {
		System.out.println();
		System.out.println("========================================");
		System.out.println("开始创建完整测试数据");
		System.out.println("========================================");

		AppUser user = createOrUpdateUser(
				TEST_USERNAME,
				TEST_EMAIL,
				TEST_PASSWORD
		);

		Camera camera = createOrUpdateCamera();

		Long permissionId = createOrUpdatePermission(
				user.getId(),
				camera.getId(),
				ACCESS_START_TIME,
				ACCESS_END_TIME,
				VALID_FROM,
				VALID_UNTIL
		);

		verifyUser(user.getId(), TEST_PASSWORD);
		verifyCamera(camera.getId());
		verifyPermission(
				permissionId,
				user.getId(),
				camera.getId()
		);

		printCompleteResult(
				user,
				camera,
				permissionId,
				TEST_PASSWORD
		);
	}

	/**
	 * 单独测试修改密码和摄像头访问权限。
	 *
	 * 执行前必须先运行：
	 * createOrUpdateAllTestData()
	 */
	@Test
	void updatePasswordAndCameraPermission() {
		String newPassword = "NewCamera@654321";

		LocalTime newStartTime = LocalTime.of(9, 0);
		LocalTime newEndTime = LocalTime.of(23, 0);

		LocalDate newValidFrom = LocalDate.now();
		LocalDate newValidUntil = LocalDate.now().plusYears(1);

		AppUser user = findUserByUsername(TEST_USERNAME);

		Assertions.assertNotNull(
				user,
				"测试用户不存在，请先运行 createOrUpdateAllTestData()"
		);

		Camera camera = cameraMapper.selectByCameraCode(CAMERA_CODE);

		Assertions.assertNotNull(
				camera,
				"测试摄像头不存在，请先运行 createOrUpdateAllTestData()"
		);

		/*
		 * 修改密码。
		 */
		user.setPassword(passwordEncoder.encode(newPassword));
		user.setUpdatedAt(LocalDateTime.now());

		int updatedUserRows = appUserMapper.updateById(user);

		Assertions.assertEquals(
				1,
				updatedUserRows,
				"修改密码失败"
		);

		/*
		 * 修改权限。
		 */
		Long permissionId = createOrUpdatePermission(
				user.getId(),
				camera.getId(),
				newStartTime,
				newEndTime,
				newValidFrom,
				newValidUntil
		);

		/*
		 * 验证新密码。
		 */
		AppUser updatedUser = appUserMapper.selectById(user.getId());

		Assertions.assertNotNull(updatedUser);

		Assertions.assertTrue(
				passwordEncoder.matches(
						newPassword,
						updatedUser.getPassword()
				),
				"新密码 BCrypt 验证失败"
		);

		/*
		 * 验证新权限。
		 */
		Map<String, Object> permission =
				findPermission(user.getId(), camera.getId());

		Assertions.assertNotNull(permission);
		Assertions.assertEquals(
				newStartTime,
				toLocalTime(permission.get("access_start_time"))
		);
		Assertions.assertEquals(
				newEndTime,
				toLocalTime(permission.get("access_end_time"))
		);
		Assertions.assertEquals(
				newValidFrom,
				toLocalDate(permission.get("valid_from"))
		);
		Assertions.assertEquals(
				newValidUntil,
				toLocalDate(permission.get("valid_until"))
		);

		System.out.println();
		System.out.println("========================================");
		System.out.println("密码和摄像头权限修改成功");
		System.out.println("----------------------------------------");
		System.out.println("用户 ID：" + user.getId());
		System.out.println("用户名：" + user.getUsername());
		System.out.println("新密码：" + newPassword);
		System.out.println("摄像头 ID：" + camera.getId());
		System.out.println("摄像头编号：" + camera.getCameraCode());
		System.out.println("权限 ID：" + permissionId);
		System.out.println(
				"访问时间：" + newStartTime + " - " + newEndTime
		);
		System.out.println(
				"有效日期：" + newValidFrom + " - " + newValidUntil
		);
		System.out.println("========================================");
	}

	/**
	 * 单独关闭用户摄像头权限。
	 */
	@Test
	void disableCameraPermission() {
		AppUser user = findUserByUsername(TEST_USERNAME);

		Assertions.assertNotNull(
				user,
				"测试用户不存在"
		);

		Camera camera =
				cameraMapper.selectByCameraCode(CAMERA_CODE);

		Assertions.assertNotNull(
				camera,
				"测试摄像头不存在"
		);

		int updatedRows = jdbcTemplate.update(
				"""
                UPDATE user_camera_permission
                SET is_enabled = 0,
                    updated_at = ?
                WHERE user_id = ?
                  AND camera_id = ?
                """,
				Timestamp.valueOf(LocalDateTime.now()),
				user.getId(),
				camera.getId()
		);

		Assertions.assertEquals(
				1,
				updatedRows,
				"关闭摄像头权限失败"
		);

		Map<String, Object> permission =
				findPermission(user.getId(), camera.getId());

		Assertions.assertNotNull(permission);

		Assertions.assertEquals(
				0,
				((Number) permission.get("is_enabled")).intValue()
		);

		System.out.println(
				"已关闭用户 "
						+ TEST_USERNAME
						+ " 对摄像头 "
						+ CAMERA_CODE
						+ " 的访问权限"
		);
	}

	/**
	 * 单独重新启用摄像头权限。
	 */
	@Test
	void enableCameraPermission() {
		AppUser user = findUserByUsername(TEST_USERNAME);

		Assertions.assertNotNull(user, "测试用户不存在");

		Camera camera =
				cameraMapper.selectByCameraCode(CAMERA_CODE);

		Assertions.assertNotNull(camera, "测试摄像头不存在");

		int updatedRows = jdbcTemplate.update(
				"""
                UPDATE user_camera_permission
                SET is_enabled = 1,
                    updated_at = ?
                WHERE user_id = ?
                  AND camera_id = ?
                """,
				Timestamp.valueOf(LocalDateTime.now()),
				user.getId(),
				camera.getId()
		);

		Assertions.assertEquals(
				1,
				updatedRows,
				"启用摄像头权限失败"
		);

		System.out.println(
				"已启用用户 "
						+ TEST_USERNAME
						+ " 对摄像头 "
						+ CAMERA_CODE
						+ " 的访问权限"
		);
	}

	/**
	 * 创建或更新用户。
	 */
	private AppUser createOrUpdateUser(
			String username,
			String email,
			String rawPassword
	) {
		AppUser user = findUserByUsername(username);
		LocalDateTime now = LocalDateTime.now();

		if (user == null) {
			user = new AppUser();
			user.setUsername(username);
			user.setEmail(email);
			user.setPassword(passwordEncoder.encode(rawPassword));
			user.setRole(TEST_ROLE);
			user.setStatus(TEST_USER_STATUS);
			user.setAvatarUrl(null);
			user.setCreatedAt(now);
			user.setUpdatedAt(now);

			int insertedRows = appUserMapper.insert(user);

			Assertions.assertEquals(
					1,
					insertedRows,
					"创建用户失败"
			);

			Assertions.assertNotNull(
					user.getId(),
					"创建用户后未返回用户 ID"
			);

			System.out.println(
					"已创建测试用户：" + username
			);
		} else {
			user.setEmail(email);
			user.setPassword(passwordEncoder.encode(rawPassword));
			user.setRole(TEST_ROLE);
			user.setStatus(TEST_USER_STATUS);
			user.setUpdatedAt(now);

			int updatedRows = appUserMapper.updateById(user);

			Assertions.assertEquals(
					1,
					updatedRows,
					"更新用户失败"
			);

			System.out.println(
					"测试用户已存在，已更新：" + username
			);
		}

		return appUserMapper.selectById(user.getId());
	}

	/**
	 * 创建或更新摄像头。
	 */
	private Camera createOrUpdateCamera() {
		Camera camera =
				cameraMapper.selectByCameraCode(CAMERA_CODE);

		LocalDateTime now = LocalDateTime.now();

		if (camera == null) {
			camera = new Camera();
			camera.setCameraCode(CAMERA_CODE);
			camera.setCreatedAt(now);

			setCameraFields(camera, now);

			int insertedRows = cameraMapper.insert(camera);

			Assertions.assertEquals(
					1,
					insertedRows,
					"创建摄像头失败"
			);

			Assertions.assertNotNull(
					camera.getId(),
					"创建摄像头后未返回摄像头 ID"
			);

			System.out.println(
					"已创建测试摄像头：" + CAMERA_CODE
			);
		} else {
			setCameraFields(camera, now);

			int updatedRows = cameraMapper.updateById(camera);

			Assertions.assertEquals(
					1,
					updatedRows,
					"更新摄像头失败"
			);

			System.out.println(
					"测试摄像头已存在，已更新：" + CAMERA_CODE
			);
		}

		return cameraMapper.selectByCameraCode(CAMERA_CODE);
	}

	/**
	 * 设置摄像头完整字段。
	 */
	private void setCameraFields(
			Camera camera,
			LocalDateTime updatedAt
	) {
		camera.setCameraName(CAMERA_NAME);
		camera.setLocation(CAMERA_LOCATION);
		camera.setStatus(CAMERA_STATUS);
		camera.setStreamType(CAMERA_STREAM_TYPE);
		camera.setStreamSourceUrl(CAMERA_BROWSER_URL);
		camera.setSourceStreamUrl(CAMERA_SOURCE_URL);
		camera.setSnapshotUrl(CAMERA_SNAPSHOT_URL);
		camera.setDescription(CAMERA_DESCRIPTION);
		camera.setIsEnabled(1);
		camera.setUpdatedAt(updatedAt);
	}

	/**
	 * 创建或更新用户摄像头权限。
	 *
	 * 由于当前仓库没有单独的权限实体和 Mapper，
	 * 这里使用 JdbcTemplate 操作 user_camera_permission 表。
	 */
	private Long createOrUpdatePermission(
			Long userId,
			Long cameraId,
			LocalTime accessStartTime,
			LocalTime accessEndTime,
			LocalDate validFrom,
			LocalDate validUntil
	) {
		Map<String, Object> existing =
				findPermission(userId, cameraId);

		LocalDateTime now = LocalDateTime.now();

		if (existing == null) {
			int insertedRows = jdbcTemplate.update(
					"""
                    INSERT INTO user_camera_permission
                    (
                        user_id,
                        camera_id,
                        access_start_time,
                        access_end_time,
                        valid_from,
                        valid_until,
                        is_enabled,
                        created_at,
                        updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
					userId,
					cameraId,
					Time.valueOf(accessStartTime),
					Time.valueOf(accessEndTime),
					Date.valueOf(validFrom),
					Date.valueOf(validUntil),
					1,
					Timestamp.valueOf(now),
					Timestamp.valueOf(now)
			);

			Assertions.assertEquals(
					1,
					insertedRows,
					"创建摄像头权限失败"
			);

			System.out.println(
					"已创建用户摄像头权限"
			);
		} else {
			int updatedRows = jdbcTemplate.update(
					"""
                    UPDATE user_camera_permission
                    SET access_start_time = ?,
                        access_end_time = ?,
                        valid_from = ?,
                        valid_until = ?,
                        is_enabled = 1,
                        updated_at = ?
                    WHERE user_id = ?
                      AND camera_id = ?
                    """,
					Time.valueOf(accessStartTime),
					Time.valueOf(accessEndTime),
					Date.valueOf(validFrom),
					Date.valueOf(validUntil),
					Timestamp.valueOf(now),
					userId,
					cameraId
			);

			Assertions.assertEquals(
					1,
					updatedRows,
					"更新摄像头权限失败"
			);

			System.out.println(
					"用户摄像头权限已存在，已更新"
			);
		}

		Map<String, Object> saved =
				findPermission(userId, cameraId);

		Assertions.assertNotNull(
				saved,
				"保存后无法查询摄像头权限"
		);

		return ((Number) saved.get("id")).longValue();
	}

	/**
	 * 根据用户名查找用户。
	 */
	private AppUser findUserByUsername(String username) {
		List<AppUser> users = appUserMapper.selectList(
				new LambdaQueryWrapper<AppUser>()
						.eq(AppUser::getUsername, username)
						.last("LIMIT 1")
		);

		if (users == null || users.isEmpty()) {
			return null;
		}

		return users.get(0);
	}

	/**
	 * 查询用户摄像头权限。
	 */
	private Map<String, Object> findPermission(
			Long userId,
			Long cameraId
	) {
		List<Map<String, Object>> rows =
				jdbcTemplate.queryForList(
						"""
                        SELECT
                            id,
                            user_id,
                            camera_id,
                            access_start_time,
                            access_end_time,
                            valid_from,
                            valid_until,
                            is_enabled,
                            created_at,
                            updated_at
                        FROM user_camera_permission
                        WHERE user_id = ?
                          AND camera_id = ?
                        LIMIT 1
                        """,
						userId,
						cameraId
				);

		if (rows.isEmpty()) {
			return null;
		}

		return rows.get(0);
	}

	/**
	 * 验证用户数据。
	 */
	private void verifyUser(
			Long userId,
			String rawPassword
	) {
		AppUser user = appUserMapper.selectById(userId);

		Assertions.assertNotNull(
				user,
				"用户数据不存在"
		);

		Assertions.assertEquals(
				TEST_USERNAME,
				user.getUsername()
		);

		Assertions.assertEquals(
				TEST_EMAIL,
				user.getEmail()
		);

		Assertions.assertEquals(
				TEST_ROLE,
				user.getRole()
		);

		Assertions.assertEquals(
				TEST_USER_STATUS,
				user.getStatus()
		);

		Assertions.assertTrue(
				passwordEncoder.matches(
						rawPassword,
						user.getPassword()
				),
				"用户密码 BCrypt 验证失败"
		);

		Assertions.assertNotNull(
				user.getCreatedAt()
		);

		Assertions.assertNotNull(
				user.getUpdatedAt()
		);
	}

	/**
	 * 验证摄像头数据。
	 */
	private void verifyCamera(Long cameraId) {
		Camera camera = cameraMapper.selectById(cameraId);

		Assertions.assertNotNull(
				camera,
				"摄像头数据不存在"
		);

		Assertions.assertEquals(
				CAMERA_CODE,
				camera.getCameraCode()
		);

		Assertions.assertEquals(
				CAMERA_NAME,
				camera.getCameraName()
		);

		Assertions.assertEquals(
				CAMERA_LOCATION,
				camera.getLocation()
		);

		Assertions.assertEquals(
				CAMERA_STATUS,
				camera.getStatus()
		);

		Assertions.assertEquals(
				CAMERA_STREAM_TYPE,
				camera.getStreamType()
		);

		Assertions.assertEquals(
				CAMERA_BROWSER_URL,
				camera.getStreamSourceUrl()
		);

		Assertions.assertEquals(
				CAMERA_SOURCE_URL,
				camera.getSourceStreamUrl()
		);

		Assertions.assertEquals(
				CAMERA_SNAPSHOT_URL,
				camera.getSnapshotUrl()
		);

		Assertions.assertEquals(
				1,
				camera.getIsEnabled()
		);

		Assertions.assertNotNull(
				camera.getCreatedAt()
		);

		Assertions.assertNotNull(
				camera.getUpdatedAt()
		);
	}

	/**
	 * 验证权限数据。
	 */
	private void verifyPermission(
			Long permissionId,
			Long userId,
			Long cameraId
	) {
		Map<String, Object> permission =
				findPermission(userId, cameraId);

		Assertions.assertNotNull(
				permission,
				"摄像头权限不存在"
		);

		Assertions.assertEquals(
				permissionId,
				((Number) permission.get("id")).longValue()
		);

		Assertions.assertEquals(
				userId,
				((Number) permission.get("user_id")).longValue()
		);

		Assertions.assertEquals(
				cameraId,
				((Number) permission.get("camera_id")).longValue()
		);

		Assertions.assertEquals(
				ACCESS_START_TIME,
				toLocalTime(permission.get("access_start_time"))
		);

		Assertions.assertEquals(
				ACCESS_END_TIME,
				toLocalTime(permission.get("access_end_time"))
		);

		Assertions.assertEquals(
				VALID_FROM,
				toLocalDate(permission.get("valid_from"))
		);

		Assertions.assertEquals(
				VALID_UNTIL,
				toLocalDate(permission.get("valid_until"))
		);

		Assertions.assertEquals(
				1,
				((Number) permission.get("is_enabled")).intValue()
		);
	}

	/**
	 * 输出完整创建结果。
	 */
	private void printCompleteResult(
			AppUser user,
			Camera camera,
			Long permissionId,
			String rawPassword
	) {
		System.out.println();
		System.out.println("========================================");
		System.out.println("完整测试数据创建成功");
		System.out.println("========================================");

		System.out.println("【用户信息】");
		System.out.println("用户 ID：" + user.getId());
		System.out.println("用户名：" + user.getUsername());
		System.out.println("邮箱：" + user.getEmail());
		System.out.println("登录密码：" + rawPassword);
		System.out.println("角色：" + user.getRole());
		System.out.println("状态：" + user.getStatus());

		System.out.println("----------------------------------------");

		System.out.println("【摄像头信息】");
		System.out.println("摄像头 ID：" + camera.getId());
		System.out.println("摄像头编号：" + camera.getCameraCode());
		System.out.println("摄像头名称：" + camera.getCameraName());
		System.out.println("位置：" + camera.getLocation());
		System.out.println("状态：" + camera.getStatus());
		System.out.println("播放类型：" + camera.getStreamType());
		System.out.println(
				"浏览器播放地址：" + camera.getStreamSourceUrl()
		);
		System.out.println(
				"原始流地址：" + camera.getSourceStreamUrl()
		);
		System.out.println(
				"截图地址：" + camera.getSnapshotUrl()
		);

		System.out.println("----------------------------------------");

		System.out.println("【权限信息】");
		System.out.println("权限 ID：" + permissionId);
		System.out.println(
				"每天访问时间："
						+ ACCESS_START_TIME
						+ " - "
						+ ACCESS_END_TIME
		);
		System.out.println(
				"有效日期："
						+ VALID_FROM
						+ " - "
						+ VALID_UNTIL
		);
		System.out.println("权限状态：已启用");

		System.out.println("========================================");
	}

	/**
	 * 数据库 TIME 类型转 LocalTime。
	 */
	private LocalTime toLocalTime(Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof Time time) {
			return time.toLocalTime();
		}

		if (value instanceof LocalTime localTime) {
			return localTime;
		}

		return LocalTime.parse(value.toString());
	}

	/**
	 * 数据库 DATE 类型转 LocalDate。
	 */
	private LocalDate toLocalDate(Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof Date date) {
			return date.toLocalDate();
		}

		if (value instanceof LocalDate localDate) {
			return localDate;
		}

		return LocalDate.parse(value.toString());
	}

}
