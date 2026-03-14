package com.campusmedical.module.medicine.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class MedicineDtos {

    private MedicineDtos() {
    }

    @Data
    @NoArgsConstructor
    public static class CreateRequest {

        @NotBlank
        @Size(max = 100)
        private String name;

        private String spec;
        private String unit;

        @NotNull
        @Min(0)
        private Integer stock = 0;

        @NotNull
        private Boolean isActive = Boolean.TRUE;

        @NotNull
        @DecimalMin("0")
        private BigDecimal price = BigDecimal.ZERO;
    }

    @Data
    @NoArgsConstructor
    public static class UpdateRequest {

        @Size(max = 100)
        private String name;

        private String spec;
        private String unit;

        @Min(0)
        private Integer stock;

        private Boolean isActive;

        @DecimalMin("0")
        private BigDecimal price;
    }

    @Data
    @NoArgsConstructor
    public static class StockUpdateRequest {

        @NotNull
        private Integer delta;

        private String reason;
    }

    @Data
    @NoArgsConstructor
    public static class Response {

        private Long id;
        private String name;
        private String spec;
        private String unit;
        private Integer stock;
        private Boolean isActive;
        private BigDecimal price;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
