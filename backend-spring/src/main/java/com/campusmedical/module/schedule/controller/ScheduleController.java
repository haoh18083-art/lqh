package com.campusmedical.module.schedule.controller;

import com.campusmedical.common.api.ApiResponse;
import com.campusmedical.common.api.PageResponse;
import com.campusmedical.infrastructure.persistence.mysql.entity.UserEntity;
import com.campusmedical.module.doctor.service.DoctorService;
import com.campusmedical.module.schedule.dto.ScheduleDtos;
import com.campusmedical.module.schedule.service.ScheduleService;
import com.campusmedical.security.CurrentUserService;
import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/doctor-schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final DoctorService doctorService;
    private final CurrentUserService currentUserService;

    public ScheduleController(
        ScheduleService scheduleService,
        DoctorService doctorService,
        CurrentUserService currentUserService
    ) {
        this.scheduleService = scheduleService;
        this.doctorService = doctorService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ApiResponse<List<ScheduleDtos.Response>> create(@Valid @RequestBody ScheduleDtos.CreateRequest request) {
        currentUserService.requireAdmin();
        return ApiResponse.success(scheduleService.create(request));
    }

    @PostMapping("/bulk")
    public ApiResponse<ScheduleDtos.BulkResponse> bulkUpsert(@Valid @RequestBody ScheduleDtos.BulkUpsertRequest request) {
        currentUserService.requireAdmin();
        return ApiResponse.success(scheduleService.bulkUpsert(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<ScheduleDtos.Response>> list(
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "page_size", required = false) Integer pageSize,
        @RequestParam(value = "doctor_id", required = false) Long doctorId,
        @RequestParam(value = "date_value", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateValue,
        @RequestParam(value = "date_from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @RequestParam(value = "date_to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        currentUserService.requireAdmin();
        return ApiResponse.success(scheduleService.list(page, pageSize, doctorId, dateValue, dateFrom, dateTo));
    }

    @GetMapping("/mine")
    public ApiResponse<PageResponse<ScheduleDtos.Response>> mine(
        @RequestParam(value = "date_from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @RequestParam(value = "date_to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        UserEntity user = currentUserService.requireRoles("doctor");
        Long doctorId = doctorService.findByUserId(user.getId()).getId();
        return ApiResponse.success(scheduleService.listMine(doctorId, dateFrom, dateTo));
    }

    @PatchMapping("/{scheduleId}")
    public ApiResponse<ScheduleDtos.Response> update(
        @PathVariable Long scheduleId,
        @Valid @RequestBody ScheduleDtos.UpdateRequest request
    ) {
        currentUserService.requireAdmin();
        return ApiResponse.success(scheduleService.update(scheduleId, request));
    }
}
