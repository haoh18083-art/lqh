package com.campusmedical.module.appointment.controller;

import com.campusmedical.common.api.ApiResponse;
import com.campusmedical.common.api.PageResponse;
import com.campusmedical.infrastructure.persistence.mysql.entity.DocumentEntity;
import com.campusmedical.module.appointment.dto.AppointmentDtos;
import com.campusmedical.module.appointment.service.AppointmentDocumentService;
import com.campusmedical.module.appointment.service.AppointmentService;
import com.campusmedical.security.CurrentUserService;
import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentDocumentService appointmentDocumentService;
    private final CurrentUserService currentUserService;

    public AppointmentController(
        AppointmentService appointmentService,
        AppointmentDocumentService appointmentDocumentService,
        CurrentUserService currentUserService
    ) {
        this.appointmentService = appointmentService;
        this.appointmentDocumentService = appointmentDocumentService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/slots")
    public ApiResponse<List<AppointmentDtos.SlotResponse>> slots(
        @RequestParam("doctor_id") Long doctorId,
        @RequestParam("date_from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @RequestParam("date_to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        Long currentUserId = currentUserService.requireRoles("student").getId();
        return ApiResponse.success(appointmentService.getSlots(currentUserId, doctorId, dateFrom, dateTo));
    }

    @PostMapping
    public ApiResponse<AppointmentDtos.Response> create(@Valid @RequestBody AppointmentDtos.CreateRequest request) {
        Long currentUserId = currentUserService.requireRoles("student").getId();
        return ApiResponse.success(appointmentService.create(request, currentUserId));
    }

    @GetMapping("/mine")
    public ApiResponse<PageResponse<AppointmentDtos.Response>> mine(
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "date_from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @RequestParam(value = "date_to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "page_size", required = false) Integer pageSize
    ) {
        Long currentUserId = currentUserService.requireRoles("student").getId();
        return ApiResponse.success(appointmentService.listMine(currentUserId, status, dateFrom, dateTo, page, pageSize));
    }

    @GetMapping("/{appointmentId}")
    public ApiResponse<AppointmentDtos.Response> detail(@PathVariable Long appointmentId) {
        Long currentUserId = currentUserService.requireRoles("student").getId();
        return ApiResponse.success(appointmentService.detail(appointmentId, currentUserId));
    }

    @PostMapping("/{appointmentId}/cancel")
    public ApiResponse<AppointmentDtos.Response> cancel(
        @PathVariable Long appointmentId,
        @Valid @RequestBody AppointmentDtos.CancelRequest request
    ) {
        Long currentUserId = currentUserService.requireRoles("student").getId();
        return ApiResponse.success(appointmentService.cancel(appointmentId, request.getReason(), currentUserId));
    }

    @PostMapping("/{appointmentId}/reschedule")
    public ApiResponse<AppointmentDtos.RescheduleResponse> reschedule(
        @PathVariable Long appointmentId,
        @Valid @RequestBody AppointmentDtos.RescheduleRequest request
    ) {
        Long currentUserId = currentUserService.requireRoles("student").getId();
        return ApiResponse.success(appointmentService.reschedule(appointmentId, request, currentUserId));
    }

    @GetMapping("/{appointmentId}/documents")
    public ApiResponse<AppointmentDtos.DocumentListResponse> listDocuments(@PathVariable Long appointmentId) {
        Long currentUserId = currentUserService.requireRoles("student").getId();
        return ApiResponse.success(appointmentService.listDocuments(appointmentId, currentUserId, false));
    }

    @GetMapping("/{appointmentId}/documents/download")
    public ResponseEntity<Resource> download(
        @PathVariable Long appointmentId,
        @RequestParam("doc_type") String docType
    ) {
        com.campusmedical.infrastructure.persistence.mysql.entity.UserEntity currentUser = currentUserService.requireRoles("student", "admin");
        boolean admin = "admin".equals(currentUser.getRole());
        DocumentEntity document = appointmentService.getAccessibleDocument(appointmentId, docType, currentUser.getId(), admin);
        Resource resource = appointmentDocumentService.loadAsResource(document);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getFileName() + "\"")
            .body(resource);
    }
}
