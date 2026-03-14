package com.campusmedical.module.student.controller;

import com.campusmedical.common.api.ApiResponse;
import com.campusmedical.common.api.PageResponse;
import com.campusmedical.module.audit.service.AuditService;
import com.campusmedical.module.student.dto.StudentDtos;
import com.campusmedical.module.student.service.HealthProfileService;
import com.campusmedical.module.student.service.StudentService;
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
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService studentService;
    private final HealthProfileService healthProfileService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public StudentController(
        StudentService studentService,
        HealthProfileService healthProfileService,
        CurrentUserService currentUserService,
        AuditService auditService
    ) {
        this.studentService = studentService;
        this.healthProfileService = healthProfileService;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    @GetMapping
    public ApiResponse<PageResponse<StudentDtos.Response>> list(
        HttpServletRequest httpRequest,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "page_size", required = false) Integer pageSize,
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "major", required = false) String major,
        @RequestParam(value = "grade", required = false) String grade,
        @RequestParam(value = "health_status", required = false) String healthStatus
    ) {
        Long currentUserId = currentUserService.requireAdmin().getId();
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("search", search);
        details.put("major", major);
        details.put("grade", grade);
        details.put("health_status", healthStatus);
        details.put("page", page == null ? Integer.valueOf(1) : page);
        details.put("page_size", pageSize == null ? Integer.valueOf(20) : pageSize);
        auditService.logAdminEvent(currentUserId, "list_students", details, httpRequest);
        return ApiResponse.success(studentService.list(page, pageSize, search, major, grade, healthStatus));
    }

    @GetMapping("/{studentId}")
    public ApiResponse<StudentDtos.Response> detail(@PathVariable Long studentId) {
        currentUserService.requireAdmin();
        return ApiResponse.success(studentService.getById(studentId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StudentDtos.Response>> create(
        @Valid @RequestBody StudentDtos.CreateRequest request,
        HttpServletRequest httpRequest
    ) {
        Long currentUserId = currentUserService.requireAdmin().getId();
        StudentDtos.Response response = studentService.create(request);
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("student_id", response.getStudentId());
        details.put("username", response.getUsername());
        details.put("full_name", response.getFullName());
        details.put("major", response.getMajor());
        details.put("grade", response.getGrade());
        auditService.logAdminEvent(currentUserId, "create_student", details, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{studentId}")
    public ApiResponse<StudentDtos.Response> update(
        @PathVariable Long studentId,
        @Valid @RequestBody StudentDtos.UpdateRequest request,
        HttpServletRequest httpRequest
    ) {
        Long currentUserId = currentUserService.requireAdmin().getId();
        StudentDtos.Response response = studentService.update(studentId, request);
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("student_id", response.getStudentId());
        details.put("updated_fields", buildStudentUpdateDetails(request));
        auditService.logAdminEvent(currentUserId, "update_student", details, httpRequest);
        return ApiResponse.success(response);
    }

    @PatchMapping("/{studentId}/status")
    public ApiResponse<StudentDtos.Response> updateStatus(
        @PathVariable Long studentId,
        @Valid @RequestBody StudentDtos.StatusUpdateRequest request,
        HttpServletRequest httpRequest
    ) {
        Long currentUserId = currentUserService.requireAdmin().getId();
        StudentDtos.Response response = studentService.updateStatus(studentId, request.getIsActive());
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("student_id", response.getStudentId());
        details.put("is_active", request.getIsActive());
        auditService.logAdminEvent(currentUserId, "update_student_status", details, httpRequest);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{studentId}")
    public ResponseEntity<Void> delete(@PathVariable Long studentId, HttpServletRequest httpRequest) {
        Long currentUserId = currentUserService.requireAdmin().getId();
        StudentDtos.Response response = studentService.getById(studentId);
        studentService.delete(studentId);
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("student_id", response.getStudentId());
        auditService.logAdminEvent(currentUserId, "delete_student", details, httpRequest);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{studentId}/health-profile")
    public ApiResponse<StudentDtos.HealthProfileResponse> getHealthProfile(@PathVariable Long studentId) {
        currentUserService.requireAdmin();
        Long userId = studentService.findEntity(studentId).getUser().getId();
        return ApiResponse.success(healthProfileService.getByUserId(userId));
    }

    @PutMapping("/{studentId}/health-profile")
    public ApiResponse<StudentDtos.HealthProfileResponse> updateHealthProfile(
        @PathVariable Long studentId,
        @Valid @RequestBody StudentDtos.HealthProfileUpdateRequest request,
        HttpServletRequest httpRequest
    ) {
        Long currentUserId = currentUserService.requireAdmin().getId();
        StudentDtos.Response student = studentService.getById(studentId);
        Long userId = studentService.findEntity(studentId).getUser().getId();
        StudentDtos.HealthProfileResponse profile = healthProfileService.createOrUpdate(userId, request);
        studentService.updateHealthStatus(studentId, profile);
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("student_id", student.getStudentId());
        auditService.logAdminEvent(currentUserId, "update_health_profile", details, httpRequest);
        return ApiResponse.success(profile);
    }

    private Map<String, Object> buildStudentUpdateDetails(StudentDtos.UpdateRequest request) {
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
        if (request.getMajor() != null) {
            details.put("major", request.getMajor());
        }
        if (request.getGrade() != null) {
            details.put("grade", request.getGrade());
        }
        if (request.getClassName() != null) {
            details.put("class_name", request.getClassName());
        }
        if (request.getHealthStatus() != null) {
            details.put("health_status", request.getHealthStatus());
        }
        if (request.getGender() != null) {
            details.put("gender", request.getGender());
        }
        if (request.getDob() != null) {
            details.put("dob", request.getDob());
        }
        return details;
    }
}
