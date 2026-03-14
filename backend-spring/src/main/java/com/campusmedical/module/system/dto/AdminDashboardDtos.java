package com.campusmedical.module.system.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class AdminDashboardDtos {

    private AdminDashboardDtos() {
    }

    @Data
    @NoArgsConstructor
    public static class OperationChartsResponse {

        private HeatmapData medicineDiseaseHeatmap;
        private MonthlySeriesData medicineSeasonSeries;
        private MonthlySeriesData diseaseSeasonSeries;
        private Meta meta;
    }

    @Data
    @NoArgsConstructor
    public static class HeatmapData {

        private List<String> diseases = new ArrayList<String>();
        private List<String> medicines = new ArrayList<String>();
        private List<HeatmapPoint> points = new ArrayList<HeatmapPoint>();
    }

    @Data
    @NoArgsConstructor
    public static class HeatmapPoint {

        private Integer diseaseIndex;
        private Integer medicineIndex;
        private Integer quantity;
    }

    @Data
    @NoArgsConstructor
    public static class MonthlySeriesData {

        private List<String> months = new ArrayList<String>();
        private List<SeriesItem> series = new ArrayList<SeriesItem>();
    }

    @Data
    @NoArgsConstructor
    public static class SeriesItem {

        private String name;
        private List<Integer> data = new ArrayList<Integer>();
    }

    @Data
    @NoArgsConstructor
    public static class Meta {

        private Integer currentYear;
        private Integer currentMonth;
        private LocalDateTime generatedAt;
    }
}
