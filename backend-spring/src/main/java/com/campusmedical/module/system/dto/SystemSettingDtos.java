package com.campusmedical.module.system.dto;

import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class SystemSettingDtos {

    private SystemSettingDtos() {
    }

    @Data
    @NoArgsConstructor
    public static class LlmSettingsResponse {

        private String baseUrl;
        private String model;
        private Boolean hasApiKey;
        private String apiKeyMasked;
        private Boolean isConfigured;
        private String lastTestStatus;
        private String lastTestMessage;
        private LocalDateTime lastTestedAt;
        private LocalDateTime updatedAt;
        private Long updatedBy;
    }

    @Data
    @NoArgsConstructor
    public static class UpdateRequest {

        @NotBlank
        @Size(max = 500)
        private String baseUrl;

        @NotBlank
        @Size(max = 100)
        private String model;

        @Size(max = 500)
        private String apiKey;
    }

    @Data
    @NoArgsConstructor
    public static class TestRequest {

        @NotBlank
        @Size(max = 500)
        private String baseUrl;

        @NotBlank
        @Size(max = 100)
        private String model;

        @Size(max = 500)
        private String apiKey;
    }

    @Data
    @NoArgsConstructor
    public static class ProviderEcho {

        private String baseUrl;
        private String model;
    }

    @Data
    @NoArgsConstructor
    public static class TestResponse {

        private Boolean success;
        private String message;
        private ProviderEcho providerEcho;
        private Integer latencyMs;
    }
}
