package com.campusmedical.module.medicalrecord.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class MedicalRecordDtos {

    private MedicalRecordDtos() {
    }

    @Data
    @NoArgsConstructor
    public static class DiagnosisSummary {

        private String category;
        private String signs;
        private String conclusion;
        private String advice;
    }

    @Data
    @NoArgsConstructor
    public static class PrescriptionSummaryItem {

        private String name;
        private String dosage;
        private Integer quantity;
        private String unit;
        private Double unitPrice;
        private Double totalPrice;
    }

    @Data
    @NoArgsConstructor
    public static class Item {

        private String id;
        private Long appointmentId;
        private Long studentId;
        private Long doctorId;
        private Long departmentId;
        private String visitDate;
        private String timeSlot;
        private String symptoms;
        private String doctorName;
        private String departmentName;
        private DiagnosisSummary diagnosisSummary;
        private List<PrescriptionSummaryItem> prescriptionSummary = new ArrayList<PrescriptionSummaryItem>();
        private Double feeTotal;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    public static class ListResponse {

        private List<Item> items = new ArrayList<Item>();
        private long total;
        private int page;
        private int pageSize;
        private int totalPages;
    }
}
