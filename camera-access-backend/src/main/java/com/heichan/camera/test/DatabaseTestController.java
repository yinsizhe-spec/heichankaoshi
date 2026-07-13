package com.heichan.camera.test;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class DatabaseTestController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/database")
    public Map<String, Object> testDatabase() {
        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_user",
                Integer.class
        );

        Integer cameraCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM camera",
                Integer.class
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("databaseConnected", true);
        response.put("userCount", userCount);
        response.put("cameraCount", cameraCount);

        return response;
    }
}