package com.campusmedical.module.appointment.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class AppointmentDtos {

    private AppointmentDtos() {
    }

    @Data
    @NoArgsConstructor
    public static class SlotResponse {

        private LocalDate date;
        private String time;
        private String status;
        private Integer bookedCount;
        private Integer capacity;
    }

    @Data
    @NoArgsConstructor
    public static class CreateRequest {

        @NotNull
        private Long doctorId;

        @NotNull
        private LocalDate date;

        @NotBlank
        private String time;

        private String symptoms;
    }

    @Data
    @NoArgsConstructor
    public static class CancelRequest {

        @NotBlank
        private String reason;
    }

    @Data
    @NoArgsConstructor
    public static class RescheduleRequest {

        @NotNull
        private LocalDate newDate;

        @NotBlank
        private String newTime;

        @NotBlank
        private String reason;
    }

    @Data
    @NoArgsConstructor
    public static class StudentInfo {

        private Long id;
        private String studentId;
        private String fullName;
        private String gender;
        private LocalDate dob;
    }

    @Data
    @NoArgsConstructor
    public static class DoctorInfo {

        private Long id;
        private String fullName;
        private String title;
        private Long departmentId;
        private String departmentName;
    }

    @Data
    @NoArgsConstructor
    public static class DepartmentInfo {

        private Long id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    public static class Response {

        private Long id;
        private LocalDate date;
        private String time;
        private String status;
        private String queueNo;
        private String symptoms;
        private LocalDateTime createdAt;
        private LocalDateTime confirmedAt;
        private LocalDateTime cancelledAt;
        private LocalDateTime completedAt;
        private StudentInfo student;
        private DoctorInfo doctor;
        private DepartmentInfo department;
    }

    @Data
    @NoArgsConstructor
    public static class RescheduleResponse {

        private Response oldAppointment;
        private Response newAppointment;
    }

    @Data
    @NoArgsConstructor
    public static class DocumentInfo {

        private Long id;
        private Long appointmentId;
        private String docType;
        private String fileName;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    public static class DocumentListResponse {

        private List<DocumentInfo> items = new ArrayList<DocumentInfo>();
    }

    @Data
    @NoArgsConstructor
    public static class PrescriptionItemInput {

        @NotNull
        private Long medicineId;

        private String dosage;

        @NotNull
        @Min(1)
        private Integer quantity = 1;
    }

    @Data
    @NoArgsConstructor
    public static class ConsultationCreateRequest {

        private String category;
        private String signs;

        @NotBlank
        private String conclusion;

        private String advice;

        private List<Long> drugIds = new ArrayList<Long>();

        @Valid
        private List<PrescriptionItemInput> items = new ArrayList<PrescriptionItemInput>();
    }

    @Data
    @NoArgsConstructor
    public static class ConsultationResponse {

        private Long id;
        private Long appointmentId;
        private Long doctorId;
        private Long studentId;
        private String category;
        private String signs;
        private String conclusion;
        private String advice;
        private LocalDateTime createdAt;
        private List<ConsultationPrescriptionItem> prescriptionItems = new ArrayList<ConsultationPrescriptionItem>();
    }

    @Data
    @NoArgsConstructor
    public static class ConsultationPrescriptionItem {

        private Long id;
        private Long medicineId;
        private String name;
        private String dosage;
        private Integer quantity;
        private String unit;
    }
}
