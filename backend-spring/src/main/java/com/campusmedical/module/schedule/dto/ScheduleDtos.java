package com.campusmedical.module.schedule.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class ScheduleDtos {

    private ScheduleDtos() {
    }

    @Data
    @NoArgsConstructor
    public static class CreateRequest {

        @NotNull
        private Long doctorId;

        @NotNull
        private LocalDate date;

        @NotEmpty
        private List<String> timeSlots;

        @NotNull
        @Min(1)
        @Max(100)
        private Integer capacity = 10;
    }

    @Data
    @NoArgsConstructor
    public static class UpdateRequest {

        @Min(1)
        @Max(100)
        private Integer capacity;

        private String status;
    }

    @Data
    @NoArgsConstructor
    public static class BulkUpsertRequest {

        @NotNull
        private Long doctorId;

        @NotNull
        private LocalDate dateFrom;

        @NotNull
        private LocalDate dateTo;

        @NotEmpty
        private List<String> timeSlots;

        @NotNull
        @Min(1)
        @Max(100)
        private Integer capacity = 10;

        @NotNull
        private String status = "open";
    }

    @Data
    @NoArgsConstructor
    public static class BulkResponse {

        private Integer created;
        private Integer updated;
        private List<Response> items;
    }

    @Data
    @NoArgsConstructor
    public static class Response {

        private Long id;
        private Long doctorId;
        private LocalDate date;
        private String timeSlot;
        private Integer capacity;
        private Integer bookedCount;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
