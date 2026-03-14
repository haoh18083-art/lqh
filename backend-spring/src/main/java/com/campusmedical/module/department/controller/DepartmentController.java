package com.campusmedical.module.department.controller;

import com.campusmedical.common.api.ApiResponse;
import com.campusmedical.common.api.PageResponse;
import com.campusmedical.module.audit.service.AuditService;
import com.campusmedical.module.department.dto.DepartmentDtos;
import com.campusmedical.module.department.service.DepartmentService;
import com.campusmedical.security.CurrentUserService;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {

    private final DepartmentService departmentService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public DepartmentController(
        DepartmentService departmentService,
        CurrentUserService currentUserService,
        AuditService auditService
    ) {
        this.departmentService = departmentService;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    @GetMapping
    public ApiResponse<PageResponse<DepartmentDtos.Response>> list(
        HttpServletRequest httpRequest,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "page_size", required = false) Integer pageSize,
        @RequestParam(value = "is_active", required = false) Boolean isActive,
        @RequestParam(value = "search", required = false) String search
    ) {
        Long currentUserId = currentUserService.requireAdmin().getId();
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("is_active", isActive);
        details.put("search", search);
        details.put("page", page == null ? Integer.valueOf(1) : page);
        details.put("page_size", pageSize == null ? Integer.valueOf(20) : pageSize);
        auditService.logAdminEvent(currentUserId, "list_departments", details, httpRequest);
        return ApiResponse.success(departmentService.list(page, pageSize, isActive, search));
    }

    @GetMapping("/{departmentId}")
    public ApiResponse<DepartmentDtos.Response> detail(@PathVariable Long departmentId) {
        currentUserService.requireAdmin();
        return ApiResponse.success(departmentService.getById(departmentId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DepartmentDtos.Response>> create(
        @Valid @RequestBody DepartmentDtos.CreateRequest request,
        HttpServletRequest httpRequest
    ) {
        Long currentUserId = currentUserService.requireAdmin().getId();
        DepartmentDtos.Response response = departmentService.create(request);
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("name", request.getName());
        details.put("description", request.getDescription());
        details.put("sort_order", request.getSortOrder());
        auditService.logAdminEvent(currentUserId, "create_department", details, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{departmentId}")
    public ApiResponse<DepartmentDtos.Response> update(
        @PathVariable Long departmentId,
        @Valid @RequestBody DepartmentDtos.UpdateRequest request,
        HttpServletRequest httpRequest
    ) {
        Long currentUserId = currentUserService.requireAdmin().getId();
        DepartmentDtos.Response response = departmentService.update(departmentId, request);
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("department_id", departmentId);
        details.put("updated_fields", buildDepartmentUpdateDetails(request));
        auditService.logAdminEvent(currentUserId, "update_department", details, httpRequest);
        return ApiResponse.success(response);
    }

    @PatchMapping("/{departmentId}/status")
    public ApiResponse<DepartmentDtos.Response> updateStatus(
        @PathVariable Long departmentId,
        @Valid @RequestBody DepartmentDtos.StatusUpdateRequest request,
        HttpServletRequest httpRequest
    ) {
        Long currentUserId = currentUserService.requireAdmin().getId();
        DepartmentDtos.Response response = departmentService.updateStatus(departmentId, request.getIsActive());
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("department_id", departmentId);
        details.put("is_active", request.getIsActive());
        auditService.logAdminEvent(currentUserId, "update_department_status", details, httpRequest);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{departmentId}")
    public ResponseEntity<Void> delete(@PathVariable Long departmentId, HttpServletRequest httpRequest) {
        Long currentUserId = currentUserService.requireAdmin().getId();
        departmentService.delete(departmentId);
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("department_id", departmentId);
        auditService.logAdminEvent(currentUserId, "delete_department", details, httpRequest);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> buildDepartmentUpdateDetails(DepartmentDtos.UpdateRequest request) {
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        if (request.getName() != null) {
            details.put("name", request.getName());
        }
        if (request.getDescription() != null) {
            details.put("description", request.getDescription());
        }
        if (request.getSortOrder() != null) {
            details.put("sort_order", request.getSortOrder());
        }
        if (request.getIsActive() != null) {
            details.put("is_active", request.getIsActive());
        }
        return details;
    }
}
