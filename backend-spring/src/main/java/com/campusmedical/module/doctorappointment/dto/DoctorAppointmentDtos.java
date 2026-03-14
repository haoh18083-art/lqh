package com.campusmedical.module.doctorappointment.dto;

import com.campusmedical.module.medicalrecord.dto.MedicalRecordDtos;
import com.campusmedical.module.student.dto.StudentDtos;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class DoctorAppointmentDtos {

    private DoctorAppointmentDtos() {
    }

    @Data
    @NoArgsConstructor
    public static class Item {

        private Long appointmentId;
        private String queueNo;
        private String studentName;
        private String gender;
        private String timeSlot;
        private String symptoms;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    public static class ListResponse {

        private List<Item> items = new ArrayList<Item>();
        private Integer total;
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
    public static class StudentHistoryResponse {

        private StudentInfo student;
        private List<StudentDtos.MedicalHistoryRecord> medicalHistory = new ArrayList<StudentDtos.MedicalHistoryRecord>();
        private List<MedicalRecordDtos.Item> medicalRecords = new ArrayList<MedicalRecordDtos.Item>();
        private long totalRecords;
        private int page;
        private int pageSize;
        private int totalPages;
    }
}
