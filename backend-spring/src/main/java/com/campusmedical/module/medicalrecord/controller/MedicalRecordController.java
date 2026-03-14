package com.campusmedical.module.medicalrecord.controller;

import com.campusmedical.common.api.ApiResponse;
import com.campusmedical.module.medicalrecord.dto.MedicalRecordDtos;
import com.campusmedical.module.medicalrecord.service.MedicalRecordService;
import com.campusmedical.security.CurrentUserService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/medical-records")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;
    private final CurrentUserService currentUserService;

    public MedicalRecordController(MedicalRecordService medicalRecordService, CurrentUserService currentUserService) {
        this.medicalRecordService = medicalRecordService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/mine")
    public ApiResponse<MedicalRecordDtos.ListResponse> mine(
        @RequestParam(value = "date_from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @RequestParam(value = "date_to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "page_size", required = false) Integer pageSize
    ) {
        Long currentUserId = currentUserService.requireRoles("student").getId();
        return ApiResponse.success(medicalRecordService.listMine(currentUserId, dateFrom, dateTo, page, pageSize));
    }
}
