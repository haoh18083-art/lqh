package com.campusmedical.module.doctor.dto;

import java.time.LocalDateTime;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class DoctorDtos {

    private DoctorDtos() {
    }

    @Data
    @NoArgsConstructor
    public static class CreateRequest {

        @NotBlank
        @Size(max = 20)
        private String doctorId;

        @NotBlank
        @Size(min = 3, max = 50)
        private String username;

        @NotBlank
        @Size(min = 8, max = 100)
        private String password;

        @Email
        @NotBlank
        private String email;

        @NotBlank
        @Size(max = 100)
        private String fullName;

        @NotNull
        private Long departmentId;

        @NotBlank
        @Size(max = 50)
        private String title;

        @NotBlank
        @Size(max = 500)
        private String introduction;

        @Size(max = 20)
        private String phone;
    }

    @Data
    @NoArgsConstructor
    public static class UpdateRequest {

        @Size(max = 100)
        private String fullName;

        @Email
        private String email;

        @Size(max = 20)
        private String phone;

        private Long departmentId;

        @Size(max = 50)
        private String title;

        @Size(max = 500)
        private String introduction;
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
        private Long userId;
        private String doctorId;
        private String username;
        private String email;
        private String fullName;
        private Long departmentId;
        private String department;
        private String title;
        private String introduction;
        private String phone;
        private Boolean isActive;
        private LocalDateTime createdAt;
    }
}
