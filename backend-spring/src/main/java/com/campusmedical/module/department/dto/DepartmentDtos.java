package com.campusmedical.module.department.dto;

import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class DepartmentDtos {

    private DepartmentDtos() {
    }

    @Data
    @NoArgsConstructor
    public static class CreateRequest {

        @NotBlank
        @Size(max = 50)
        private String name;

        private String description;

        @NotNull
        private Integer sortOrder = 0;
    }

    @Data
    @NoArgsConstructor
    public static class UpdateRequest {

        @Size(max = 50)
        private String name;

        private String description;
        private Boolean isActive;
        private Integer sortOrder;
    }

    @Data
    @NoArgsConstructor
    public static class StatusUpdateRequest {

        @NotNull
        private Boolean isActive;
    }

    @Data
    @NoArgsConstructor
    public static class Response {

        private Long id;
        private String name;
        private String description;
        private Boolean isActive;
        private Integer sortOrder;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
