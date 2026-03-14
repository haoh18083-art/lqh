package com.campusmedical.module.system.controller;

import com.campusmedical.common.api.ApiResponse;
import com.campusmedical.module.audit.service.AuditService;
import com.campusmedical.module.system.dto.SystemSettingDtos;
import com.campusmedical.module.system.service.SystemSettingService;
import com.campusmedical.security.CurrentUserService;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/settings")
public class SystemSettingController {

    private final SystemSettingService systemSettingService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public SystemSettingController(
        SystemSettingService systemSettingService,
        CurrentUserService currentUserService,
        AuditService auditService
    ) {
        this.systemSettingService = systemSettingService;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    @GetMapping("/llm")
    public ApiResponse<SystemSettingDtos.LlmSettingsResponse> getLlmSettings() {
        currentUserService.requireAdmin();
        return ApiResponse.success(systemSettingService.getLlmSettings());
    }

    @PutMapping("/llm")
    public ApiResponse<SystemSettingDtos.LlmSettingsResponse> updateLlmSettings(
        @Valid @RequestBody SystemSettingDtos.UpdateRequest request,
        HttpServletRequest httpRequest
    ) {
        Long currentUserId = currentUserService.requireAdmin().getId();
        SystemSettingDtos.LlmSettingsResponse response = systemSettingService.updateLlmSettings(request, currentUserId);
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("base_url", request.getBaseUrl());
        details.put("model", request.getModel());
        details.put("api_key_updated", Boolean.valueOf(request.getApiKey() != null && !request.getApiKey().trim().isEmpty()));
        auditService.logAdminEvent(currentUserId, "update_llm_settings", details, httpRequest);
        return ApiResponse.success(response);
    }

    @PostMapping("/llm/test")
    public ApiResponse<SystemSettingDtos.TestResponse> testLlmSettings(
        @Valid @RequestBody SystemSettingDtos.TestRequest request,
        HttpServletRequest httpRequest
    ) {
        Long currentUserId = currentUserService.requireAdmin().getId();
        SystemSettingDtos.TestResponse response = systemSettingService.testLlmSettings(request, currentUserId);
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("base_url", request.getBaseUrl());
        details.put("model", request.getModel());
        details.put("result", response.getSuccess());
        details.put("message", response.getMessage());
        auditService.logAdminEvent(currentUserId, "test_llm_settings", details, httpRequest);
        return ApiResponse.success(response);
    }
}
