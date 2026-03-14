package com.campusmedical.module.auth.dto;

import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class AuthDtos {

    private AuthDtos() {
    }

    @Data
    @NoArgsConstructor
    public static class LoginRequest {

        @NotBlank
        private String email;

        @NotBlank
        private String password;

        private String role;
    }

    @Data
    @NoArgsConstructor
    public static class RefreshTokenRequest {

        @NotBlank
        private String refreshToken;
    }

    @Data
    @NoArgsConstructor
    public static class LoginResponse {

        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private Long expiresIn;
        private UserInfo user;
    }

    @Data
    @NoArgsConstructor
    public static class RefreshTokenResponse {

        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private Long expiresIn;
    }

    @Data
    @NoArgsConstructor
    public static class UserInfo {

        private Long id;
        private String username;
        private String email;
        private String role;
        private String fullName;
        private String phone;
        private String studentId;
        private Boolean isActive;
        private LocalDateTime createdAt;
    }
}
