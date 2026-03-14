package com.campusmedical.module.auth.controller;

import com.campusmedical.common.api.ApiResponse;
import com.campusmedical.infrastructure.persistence.mysql.entity.UserEntity;
import com.campusmedical.module.audit.service.AuditService;
import com.campusmedical.module.auth.dto.AuthDtos;
import com.campusmedical.module.auth.service.AuthService;
import com.campusmedical.security.CurrentUserService;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public AuthController(
        AuthService authService,
        CurrentUserService currentUserService,
        AuditService auditService
    ) {
        this.authService = authService;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthDtos.LoginResponse> login(
        @Valid @RequestBody AuthDtos.LoginRequest request,
        HttpServletRequest httpRequest
    ) {
        try {
            AuthDtos.LoginResponse response = authService.login(request);
            Map<String, Object> details = new LinkedHashMap<String, Object>();
            details.put("role", response.getUser() == null ? null : response.getUser().getRole());
            details.put("email", request.getEmail());
            auditService.logAuthEvent(
                response.getUser() == null ? Long.valueOf(0L) : response.getUser().getId(),
                "login",
                details,
                httpRequest
            );
            return ApiResponse.success(response);
        } catch (RuntimeException exception) {
            Map<String, Object> details = new LinkedHashMap<String, Object>();
            details.put("email", request.getEmail());
            details.put("role", request.getRole());
            details.put("error", exception.getMessage());
            auditService.logAuthEvent(Long.valueOf(0L), "failed_login", details, httpRequest);
            throw exception;
        }
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthDtos.RefreshTokenResponse> refresh(
        @Valid @RequestBody AuthDtos.RefreshTokenRequest request,
        HttpServletRequest httpRequest
    ) {
        AuthDtos.RefreshTokenResponse response = authService.refresh(request.getRefreshToken());
        Long currentUserId = authService.parseRefreshTokenUserId(request.getRefreshToken());
        auditService.logAuthEvent(currentUserId, "refresh_token", new LinkedHashMap<String, Object>(), httpRequest);
        return ApiResponse.success(response);
    }

    @PostMapping("/logout")
    public ApiResponse<Map<String, Object>> logout(
        @Valid @RequestBody AuthDtos.RefreshTokenRequest request,
        HttpServletRequest httpRequest
    ) {
        UserEntity currentUser = currentUserService.requireCurrentUser();
        auditService.logAuthEvent(currentUser.getId(), "logout", new LinkedHashMap<String, Object>(), httpRequest);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("success", Boolean.TRUE);
        payload.put("message", "登出成功");
        return ApiResponse.success(payload);
    }

    @GetMapping("/me")
    public ApiResponse<AuthDtos.UserInfo> me() {
        return ApiResponse.success(authService.toUserInfo(currentUserService.requireCurrentUser()));
    }
}
