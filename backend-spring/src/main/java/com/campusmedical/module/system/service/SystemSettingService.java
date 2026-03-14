package com.campusmedical.module.system.service;

import com.campusmedical.common.exception.NotFoundException;
import com.campusmedical.common.exception.ValidationException;
import com.campusmedical.common.util.FernetCipher;
import com.campusmedical.infrastructure.persistence.mysql.entity.SystemSettingEntity;
import com.campusmedical.infrastructure.persistence.mysql.repository.SystemSettingRepository;
import com.campusmedical.module.system.dto.SystemSettingDtos;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemSettingService {

    private static final String CATEGORY = "llm";
    private static final String SETTING_KEY = "default_provider";

    private final SystemSettingRepository systemSettingRepository;
    private final String encryptionKey;

    public SystemSettingService(
        SystemSettingRepository systemSettingRepository,
        @Value("${app.settings.encryption-key:}") String encryptionKey
    ) {
        this.systemSettingRepository = systemSettingRepository;
        this.encryptionKey = encryptionKey;
    }

    @Transactional(readOnly = true)
    public SystemSettingDtos.LlmSettingsResponse getLlmSettings() {
        return toResponse(getOrCreate());
    }

    @Transactional
    public SystemSettingDtos.LlmSettingsResponse updateLlmSettings(SystemSettingDtos.UpdateRequest request, Long currentUserId) {
        SystemSettingEntity setting = getOrCreate();
        String incomingApiKey = trimToNull(request.getApiKey());
        if (incomingApiKey == null && trimToNull(setting.getApiKeyEncrypted()) == null) {
            throw new ValidationException("首次保存 LLM 配置时必须提供 API Key");
        }

        setting.setBaseUrl(normalizeBaseUrl(request.getBaseUrl()));
        setting.setModel(normalizeModel(request.getModel()));
        setting.setUpdatedBy(currentUserId);
        if (incomingApiKey != null) {
            setting.setApiKeyEncrypted(cipher().encrypt(incomingApiKey));
            setting.setApiKeyMasked(FernetCipher.mask(incomingApiKey));
        }
        setting.setIsConfigured(trimToNull(setting.getApiKeyEncrypted()) != null);
        return toResponse(systemSettingRepository.save(setting));
    }

    @Transactional
    public SystemSettingDtos.TestResponse testLlmSettings(SystemSettingDtos.TestRequest request, Long currentUserId) {
        SystemSettingEntity setting = getOrCreate();
        String baseUrl = normalizeBaseUrl(request.getBaseUrl());
        String model = normalizeModel(request.getModel());
        String apiKey = trimToNull(request.getApiKey());
        if (apiKey == null) {
            if (trimToNull(setting.getApiKeyEncrypted()) == null) {
                throw new ValidationException("请先配置 API Key，再测试连接");
            }
            apiKey = cipher().decrypt(setting.getApiKeyEncrypted());
        }

        long startedAt = System.nanoTime();
        boolean success = false;
        String message;
        try {
            performConnectionCheck(baseUrl, model, apiKey);
            success = true;
            message = "连接测试成功";
        } catch (IOException exception) {
            message = exception.getMessage();
        }

        int latencyMs = (int) ((System.nanoTime() - startedAt) / 1_000_000L);
        setting.setLastTestStatus(success ? "success" : "failed");
        setting.setLastTestMessage(message == null ? null : truncate(message, 500));
        setting.setLastTestedAt(LocalDateTime.now(ZoneOffset.UTC));
        setting.setUpdatedBy(currentUserId);
        systemSettingRepository.save(setting);

        SystemSettingDtos.ProviderEcho echo = new SystemSettingDtos.ProviderEcho();
        echo.setBaseUrl(baseUrl);
        echo.setModel(model);

        SystemSettingDtos.TestResponse response = new SystemSettingDtos.TestResponse();
        response.setSuccess(Boolean.valueOf(success));
        response.setMessage(message);
        response.setProviderEcho(echo);
        response.setLatencyMs(Integer.valueOf(latencyMs));
        return response;
    }

    private SystemSettingEntity getOrCreate() {
        return systemSettingRepository.findByCategoryAndSettingKey(CATEGORY, SETTING_KEY).orElseGet(() -> {
            SystemSettingEntity setting = new SystemSettingEntity();
            setting.setCategory(CATEGORY);
            setting.setSettingKey(SETTING_KEY);
            setting.setBaseUrl("");
            setting.setModel("");
            setting.setIsConfigured(Boolean.FALSE);
            setting.setLastTestStatus("unknown");
            return systemSettingRepository.save(setting);
        });
    }

    private SystemSettingDtos.LlmSettingsResponse toResponse(SystemSettingEntity setting) {
        SystemSettingDtos.LlmSettingsResponse response = new SystemSettingDtos.LlmSettingsResponse();
        response.setBaseUrl(setting.getBaseUrl());
        response.setModel(setting.getModel());
        response.setHasApiKey(Boolean.valueOf(trimToNull(setting.getApiKeyEncrypted()) != null));
        response.setApiKeyMasked(setting.getApiKeyMasked());
        response.setIsConfigured(Boolean.valueOf(Boolean.TRUE.equals(setting.getIsConfigured()) && trimToNull(setting.getApiKeyEncrypted()) != null));
        response.setLastTestStatus(trimToNull(setting.getLastTestStatus()) == null ? "unknown" : setting.getLastTestStatus());
        response.setLastTestMessage(setting.getLastTestMessage());
        response.setLastTestedAt(setting.getLastTestedAt());
        response.setUpdatedAt(setting.getUpdatedAt());
        response.setUpdatedBy(setting.getUpdatedBy());
        return response;
    }

    private void performConnectionCheck(String baseUrl, String model, String apiKey) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + "/chat/completions").openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(10_000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);

        String payload = "{\"model\":\"" + escapeJson(model) + "\",\"messages\":[{\"role\":\"user\",\"content\":\"ping\"}],\"max_tokens\":1}";
        OutputStream outputStream = connection.getOutputStream();
        try {
            outputStream.write(payload.getBytes(StandardCharsets.UTF_8));
        } finally {
            outputStream.close();
        }

        int statusCode = connection.getResponseCode();
        if (statusCode >= 400) {
            String detail = readResponseBody(connection.getErrorStream());
            throw new IOException("连接测试失败，服务返回状态码 " + statusCode + (detail.isEmpty() ? "" : ": " + detail));
        }

        InputStream inputStream = connection.getInputStream();
        if (inputStream != null) {
            inputStream.close();
        }
    }

    private String readResponseBody(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        try {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                if (builder.length() >= 200) {
                    break;
                }
            }
            return truncate(builder.toString(), 200);
        } finally {
            reader.close();
        }
    }

    private FernetCipher cipher() {
        return new FernetCipher(encryptionKey);
    }

    private String normalizeBaseUrl(String value) {
        String normalized = trimToNull(value);
        if (normalized == null || (!normalized.startsWith("http://") && !normalized.startsWith("https://"))) {
            throw new ValidationException("URL 必须以 http:// 或 https:// 开头");
        }
        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeModel(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new ValidationException("模型名称不能为空");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
