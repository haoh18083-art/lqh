package com.campusmedical.module.system.controller;

import com.campusmedical.common.api.ApiResponse;
import com.campusmedical.module.system.dto.AdminDashboardDtos;
import com.campusmedical.module.system.service.AdminDashboardService;
import com.campusmedical.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final CurrentUserService currentUserService;

    public AdminDashboardController(
        AdminDashboardService adminDashboardService,
        CurrentUserService currentUserService
    ) {
        this.adminDashboardService = adminDashboardService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/operation-charts")
    public ApiResponse<AdminDashboardDtos.OperationChartsResponse> getOperationCharts() {
        currentUserService.requireAdmin();
        return ApiResponse.success(adminDashboardService.getOperationCharts());
    }
}
