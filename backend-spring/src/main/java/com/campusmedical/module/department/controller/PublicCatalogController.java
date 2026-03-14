package com.campusmedical.module.department.controller;

import com.campusmedical.common.api.ApiResponse;
import com.campusmedical.common.api.PageResponse;
import com.campusmedical.module.department.dto.DepartmentDtos;
import com.campusmedical.module.department.service.DepartmentService;
import com.campusmedical.module.doctor.dto.DoctorDtos;
import com.campusmedical.module.doctor.service.DoctorService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
public class PublicCatalogController {

    private final DepartmentService departmentService;
    private final DoctorService doctorService;

    public PublicCatalogController(DepartmentService departmentService, DoctorService doctorService) {
        this.departmentService = departmentService;
        this.doctorService = doctorService;
    }

    @GetMapping("/departments")
    public ApiResponse<PageResponse<DepartmentDtos.Response>> listDepartments(
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "page_size", required = false) Integer pageSize
    ) {
        return ApiResponse.success(departmentService.list(page, pageSize, Boolean.TRUE, null));
    }

    @GetMapping("/doctors")
    public ApiResponse<PageResponse<DoctorDtos.Response>> listDoctors(
        @RequestParam("department_id") Long departmentId,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "page_size", required = false) Integer pageSize
    ) {
        PageResponse<DoctorDtos.Response> basePage = doctorService.list(page, pageSize, null, null, departmentId);
        int normalizedPage = page == null || page < 1 ? 1 : page;
        int normalizedPageSize = pageSize == null || pageSize < 1 || pageSize > 100 ? 20 : pageSize;

        List<DoctorDtos.Response> activeDoctors = new ArrayList<DoctorDtos.Response>();
        for (DoctorDtos.Response item : basePage.getItems()) {
            if (Boolean.TRUE.equals(item.getIsActive())) {
                activeDoctors.add(item);
            }
        }

        int totalActive = activeDoctors.size();
        int startIndex = (normalizedPage - 1) * normalizedPageSize;
        int endIndex = Math.min(startIndex + normalizedPageSize, totalActive);
        List<DoctorDtos.Response> items = startIndex >= totalActive
            ? new ArrayList<DoctorDtos.Response>()
            : new ArrayList<DoctorDtos.Response>(activeDoctors.subList(startIndex, endIndex));
        int totalPages = normalizedPageSize <= 0 ? 0 : (totalActive + normalizedPageSize - 1) / normalizedPageSize;

        return ApiResponse.success(new PageResponse<DoctorDtos.Response>(
            items,
            totalActive,
            normalizedPage,
            normalizedPageSize,
            totalPages
        ));
    }
}
