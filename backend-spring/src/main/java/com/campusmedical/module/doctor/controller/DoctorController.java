package com.campusmedical.module.doctor.controller;

import com.campusmedical.common.api.ApiResponse;
import com.campusmedical.common.api.PageResponse;
import com.campusmedical.module.audit.service.AuditService;
import com.campusmedical.module.doctor.dto.DoctorDtos;
import com.campusmedical.module.doctor.service.DoctorService;
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
@RequestMapping("/api/v1/doctors")
public class DoctorController {

    private final DoctorService doctorService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public DoctorController(
        DoctorService doctorService,
        CurrentUserService currentUserService,
        AuditService auditService
    ) {
        this.doctorService = doctorService;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    @GetMapping
    public ApiResponse<PageResponse<DoctorDtos.Response>> list(
        HttpServletRequest httpRequest,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "page_size", required = false) Integer pageSize,
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "department", required = false) String department,
        @RequestParam(value = "department_id", required = false) Long departmentId
    ) {
        Long currentUserId = currentUserService.requireAdmin().getId();
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("search", search);
        details.put("department", department);
        details.put("department_id", departmentId);
        details.put("page", page == null ? Integer.valueOf(1) : page);
        details.put("page_size", pageSize == null ? Integer.valueOf(20) : pageSize);
        auditService.logAdminEvent(currentUserId, "list_doctors", details, httpRequest);
        return ApiResponse.success(doctorService.list(page, pageSize, search, department, departmentId));
    }

    @GetMapping("/{doctorId}")
    public ApiResponse<DoctorDtos.Response> detail(@PathVariable Long doctorId) {
        currentUserService.requireAdmin();
        return ApiResponse.success(doctorService.getById(doctorId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DoctorDtos.Response>> create(
        @Valid @RequestBody DoctorDtos.CreateRequest request,
        HttpServletRequest httpRequest
    ) {
        Long currentUserId = currentUserService.requireAdmin().getId();
        DoctorDtos.Response response = doctorService.create(request);
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("doctor_id", response.getDoctorId());
        details.put("username", response.getUsername());
        details.put("full_name", response.getFullName());
        details.put("department_id", response.getDepartmentId());
        details.put("title", response.getTitle());
        auditService.logAdminEvent(currentUserId, "create_doctor", details, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{doctorId}")
    public ApiResponse<DoctorDtos.Response> update(
        @PathVariable Long doctorId,
        @Valid @RequestBody DoctorDtos.UpdateRequest request,
        HttpServletRequest httpRequest
    ) {
        Long currentUserId = currentUserService.requireAdmin().getId();
        DoctorDtos.Response response = doctorService.update(doctorId, request);
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("doctor_id", response.getDoctorId());
        details.put("updated_fields", buildDoctorUpdateDetails(request));
        auditService.logAdminEvent(currentUserId, "update_doctor", details, httpRequest);
        return ApiResponse.success(response);
    }

    @PatchMapping("/{doctorId}/status")
    public ApiResponse<DoctorDtos.Response> updateStatus(
        @PathVariable Long doctorId,
        @Valid @RequestBody DoctorDtos.StatusUpdateRequest request,
        HttpServletRequest httpRequest
    ) {
        Long currentUserId = currentUserService.requireAdmin().getId();
        DoctorDtos.Response response = doctorService.updateStatus(doctorId, request.getIsActive());
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("doctor_id", response.getDoctorId());
        details.put("is_active", request.getIsActive());
        auditService.logAdminEvent(currentUserId, "update_doctor_status", details, httpRequest);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{doctorId}")
    public ResponseEntity<Void> delete(@PathVariable Long doctorId, HttpServletRequest httpRequest) {
        Long currentUserId = currentUserService.requireAdmin().getId();
        DoctorDtos.Response response = doctorService.getById(doctorId);
        doctorService.delete(doctorId);
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("doctor_id", response.getDoctorId());
        auditService.logAdminEvent(currentUserId, "delete_doctor", details, httpRequest);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> buildDoctorUpdateDetails(DoctorDtos.UpdateRequest request) {
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        if (request.getFullName() != null) {
            details.put("full_name", request.getFullName());
        }
        if (request.getEmail() != null) {
            details.put("email", request.getEmail());
        }
        if (request.getPhone() != null) {
            details.put("phone", request.getPhone());
        }
        if (request.getDepartmentId() != null) {
            details.put("department_id", request.getDepartmentId());
        }
        if (request.getTitle() != null) {
            details.put("title", request.getTitle());
        }
        if (request.getIntroduction() != null) {
            details.put("introduction", request.getIntroduction());
        }
        return details;
    }
}
