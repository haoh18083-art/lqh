package com.campusmedical.config;

import com.campusmedical.common.api.ApiResponse;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private static final DateTimeFormatter ISO_MICROS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

    private final JdbcTemplate jdbcTemplate;
    private final MongoTemplate mongoTemplate;
    private final String projectName;
    private final String apiVersion;
    private final String environment;

    public HealthController(
        JdbcTemplate jdbcTemplate,
        MongoTemplate mongoTemplate,
        @Value("${app.project-name}") String projectName,
        @Value("${app.api-version}") String apiVersion,
        @Value("${app.environment:development}") String environment
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.mongoTemplate = mongoTemplate;
        this.projectName = projectName;
        this.apiVersion = apiVersion;
        this.environment = environment;
    }

    @GetMapping("/")
    public ApiResponse<Map<String, Object>> root() {
        return health();
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        Map<String, Object> checks = new LinkedHashMap<String, Object>();
        response.put("status", "healthy");
        response.put("timestamp", timestampNow());
        response.put("service", projectName);
        response.put("version", "1.0.0");
        response.put("environment", environment);
        response.put("checks", checks);

        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            Map<String, Object> database = new LinkedHashMap<String, Object>();
            database.put("status", result != null && result == 1 ? "healthy" : "unhealthy");
            database.put("message", "MySQL connection successful");
            database.put("type", "mysql");
            checks.put("database", database);
        } catch (Exception exception) {
            Map<String, Object> database = new LinkedHashMap<String, Object>();
            database.put("status", "unhealthy");
            database.put("message", "MySQL connection failed: " + exception.getMessage());
            checks.put("database", database);
            response.put("status", "unhealthy");
        }

        try {
            mongoTemplate.executeCommand(new Document("ping", 1));
            Map<String, Object> mongo = new LinkedHashMap<String, Object>();
            mongo.put("status", "healthy");
            mongo.put("message", "MongoDB connection successful");
            checks.put("mongodb", mongo);
        } catch (Exception exception) {
            Map<String, Object> mongo = new LinkedHashMap<String, Object>();
            mongo.put("status", "unhealthy");
            mongo.put("message", "MongoDB connection failed: " + exception.getMessage());
            checks.put("mongodb", mongo);
            if ("healthy".equals(response.get("status"))) {
                response.put("status", "degraded");
            }
        }

        return ApiResponse.success(response);
    }

    @GetMapping("/health/ready")
    public ApiResponse<Map<String, Object>> ready() {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("status", "ready");
        response.put("timestamp", timestampNow());
        return ApiResponse.success(response);
    }

    @GetMapping("/health/live")
    public ApiResponse<Map<String, Object>> live() {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("status", "alive");
        response.put("timestamp", timestampNow());
        return ApiResponse.success(response);
    }

    @GetMapping("/health/version")
    public ApiResponse<Map<String, Object>> version() {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("name", projectName);
        response.put("version", "1.0.0");
        response.put("environment", environment);
        response.put("api_version", apiVersion);
        return ApiResponse.success(response);
    }

    private String timestampNow() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        int micros = (now.getNano() / 1000) * 1000;
        return now.withNano(micros).format(ISO_MICROS);
    }
}
