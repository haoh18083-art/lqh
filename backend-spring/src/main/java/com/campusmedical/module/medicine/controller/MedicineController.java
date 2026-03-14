package com.campusmedical.module.medicine.controller;

import com.campusmedical.common.api.ApiResponse;
import com.campusmedical.common.api.PageResponse;
import com.campusmedical.module.medicine.dto.MedicineDtos;
import com.campusmedical.module.medicine.service.MedicineService;
import com.campusmedical.security.CurrentUserService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/medicines")
public class MedicineController {

    private final MedicineService medicineService;
    private final CurrentUserService currentUserService;

    public MedicineController(MedicineService medicineService, CurrentUserService currentUserService) {
        this.medicineService = medicineService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<PageResponse<MedicineDtos.Response>> list(
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "is_active", required = false) Boolean isActive,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "page_size", required = false) Integer pageSize
    ) {
        currentUserService.requireRoles("doctor", "admin");
        return ApiResponse.success(medicineService.list(search, isActive, page, pageSize));
    }

    @PostMapping
    public ApiResponse<MedicineDtos.Response> create(@Valid @RequestBody MedicineDtos.CreateRequest request) {
        currentUserService.requireAdmin();
        return ApiResponse.success(medicineService.create(request));
    }

    @PatchMapping("/{medicineId}")
    public ApiResponse<MedicineDtos.Response> update(
        @PathVariable Long medicineId,
        @Valid @RequestBody MedicineDtos.UpdateRequest request
    ) {
        currentUserService.requireAdmin();
        return ApiResponse.success(medicineService.update(medicineId, request));
    }

    @PatchMapping("/{medicineId}/stock")
    public ApiResponse<MedicineDtos.Response> updateStock(
        @PathVariable Long medicineId,
        @Valid @RequestBody MedicineDtos.StockUpdateRequest request
    ) {
        currentUserService.requireAdmin();
        return ApiResponse.success(medicineService.updateStock(medicineId, request.getDelta(), request.getReason()));
    }
}
