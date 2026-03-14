package com.campusmedical.module.student.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class StudentDtos {

    private StudentDtos() {
    }

    @Data
    @NoArgsConstructor
    public static class CreateRequest {

        @NotBlank
        @Size(max = 20)
        private String studentId;

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

        @NotBlank
        @Size(max = 100)
        private String major;

        @NotBlank
        @Size(max = 20)
        private String grade;

        @Size(max = 50)
        private String className;

        @Size(max = 20)
        private String phone;

        @Size(max = 10)
        private String gender;

        private LocalDate dob;
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

        @Size(max = 100)
        private String major;

        @Size(max = 20)
        private String grade;

        @Size(max = 50)
        private String className;

        private String healthStatus;

        @Size(max = 10)
        private String gender;

        private LocalDate dob;
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
        private String studentId;
        private String username;
        private String email;
        private String fullName;
        private String major;
        private String grade;
        private String className;
        private String gender;
        private LocalDate dob;
        private String healthStatus;
        private String phone;
        private Boolean isActive;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    public static class HealthProfileUpdateRequest {

        private String bloodType;
        private LocalDateTime lastCheckupDate;
        private List<String> allergies;

        @Valid
        private List<MedicalHistoryRecord> medicalHistory;

        @Valid
        private EmergencyContact emergencyContact;
    }

    @Data
    @NoArgsConstructor
    public static class HealthProfileResponse {

        private Long userId;
        private String bloodType;
        private LocalDateTime lastCheckupDate;
        private List<String> allergies = new ArrayList<String>();
        private List<MedicalHistoryRecord> medicalHistory = new ArrayList<MedicalHistoryRecord>();
        private EmergencyContact emergencyContact;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @NoArgsConstructor
    public static class MedicalHistoryRecord {

        private String condition;
        private LocalDateTime date;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    public static class EmergencyContact {

        private String name;
        private String phone;
        private String relationship;
    }
}
