package com.campusmedical.module.doctorappointment.controller;

import com.campusmedical.common.api.ApiResponse;
import com.campusmedical.common.api.MessageResponse;
import com.campusmedical.module.appointment.dto.AppointmentDtos;
import com.campusmedical.module.doctorappointment.dto.DoctorAppointmentDtos;
import com.campusmedical.module.doctorappointment.service.DoctorAppointmentService;
import com.campusmedical.security.CurrentUserService;
import java.time.LocalDate;
import javax.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/doctor/appointments")
public class DoctorAppointmentController {

    private final DoctorAppointmentService doctorAppointmentService;
    private final CurrentUserService currentUserService;

    public DoctorAppointmentController(
        DoctorAppointmentService doctorAppointmentService,
        CurrentUserService currentUserService
    ) {
        this.doctorAppointmentService = doctorAppointmentService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<DoctorAppointmentDtos.ListResponse> list(
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "date_value", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateValue
    ) {
        Long currentUserId = currentUserService.requireRoles("doctor", "admin").getId();
        return ApiResponse.success(doctorAppointmentService.list(currentUserId, status, dateValue));
    }

    @PostMapping("/{appointmentId}/start")
    public ApiResponse<MessageResponse> start(@PathVariable Long appointmentId) {
        Long currentUserId = currentUserService.requireRoles("doctor", "admin").getId();
        return ApiResponse.success(doctorAppointmentService.startConsultation(appointmentId, currentUserId));
    }

    @PostMapping("/{appointmentId}/diagnosis")
    public ApiResponse<AppointmentDtos.ConsultationResponse> submitDiagnosis(
        @PathVariable Long appointmentId,
        @Valid @RequestBody AppointmentDtos.ConsultationCreateRequest request
    ) {
        Long currentUserId = currentUserService.requireRoles("doctor", "admin").getId();
        return ApiResponse.success(doctorAppointmentService.submitDiagnosis(appointmentId, request, currentUserId));
    }

    @GetMapping("/{appointmentId}/history")
    public ApiResponse<DoctorAppointmentDtos.StudentHistoryResponse> history(
        @PathVariable Long appointmentId,
        @RequestParam(value = "date_from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @RequestParam(value = "date_to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "page_size", required = false) Integer pageSize
    ) {
        Long currentUserId = currentUserService.requireRoles("doctor", "admin").getId();
        return ApiResponse.success(doctorAppointmentService.getStudentHistory(
            appointmentId,
            currentUserId,
            dateFrom,
            dateTo,
            page,
            pageSize
        ));
    }
}
