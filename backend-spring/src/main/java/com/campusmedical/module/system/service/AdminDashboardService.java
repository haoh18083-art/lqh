package com.campusmedical.module.system.service;

import com.campusmedical.module.system.dto.AdminDashboardDtos;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {

    private static final int HEATMAP_LIMIT = 8;
    private static final int DISEASE_SERIES_LIMIT = 6;
    private static final int MEDICINE_SERIES_LIMIT = 5;

    private static final List<String> MONTH_LABELS = Arrays.asList(
        "1月",
        "2月",
        "3月",
        "4月",
        "5月",
        "6月",
        "7月",
        "8月",
        "9月",
        "10月",
        "11月",
        "12月"
    );

    private final JdbcTemplate jdbcTemplate;
    private final ZoneId appZoneId;

    public AdminDashboardService(
        JdbcTemplate jdbcTemplate,
        @Value("${app.timezone:Asia/Shanghai}") String timezone
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.appZoneId = ZoneId.of(timezone);
    }

    @Transactional(readOnly = true)
    public AdminDashboardDtos.OperationChartsResponse getOperationCharts() {
        LocalDateTime now = LocalDateTime.now(appZoneId);
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        AdminDashboardDtos.OperationChartsResponse response = new AdminDashboardDtos.OperationChartsResponse();
        response.setMedicineDiseaseHeatmap(buildMedicineDiseaseHeatmap(currentYear));
        response.setMedicineSeasonSeries(buildMedicineSeasonSeries(currentYear, currentMonth));
        response.setDiseaseSeasonSeries(buildDiseaseSeasonSeries(currentYear, currentMonth));

        AdminDashboardDtos.Meta meta = new AdminDashboardDtos.Meta();
        meta.setCurrentYear(Integer.valueOf(currentYear));
        meta.setCurrentMonth(Integer.valueOf(currentMonth));
        meta.setGeneratedAt(now);
        response.setMeta(meta);
        return response;
    }

    private AdminDashboardDtos.HeatmapData buildMedicineDiseaseHeatmap(int currentYear) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT COALESCE(NULLIF(TRIM(c.category), ''), '') AS disease_category, "
                + "COALESCE(NULLIF(TRIM(pi.name), ''), '未命名药品') AS medicine_name, "
                + "SUM(COALESCE(pi.quantity, 0)) AS total_quantity "
                + "FROM consultations c "
                + "JOIN prescriptions p ON p.consultation_id = c.id "
                + "JOIN prescription_items pi ON pi.prescription_id = p.id "
                + "WHERE YEAR(c.created_at) = ? "
                + "GROUP BY COALESCE(NULLIF(TRIM(c.category), ''), ''), COALESCE(NULLIF(TRIM(pi.name), ''), '未命名药品')",
            Integer.valueOf(currentYear)
        );

        Map<String, Integer> diseaseTotals = new LinkedHashMap<String, Integer>();
        Map<String, Integer> medicineTotals = new LinkedHashMap<String, Integer>();
        for (Map<String, Object> row : rows) {
            String disease = asString(row.get("disease_category"));
            String medicine = asString(row.get("medicine_name"));
            int quantity = asInt(row.get("total_quantity"));
            diseaseTotals.put(disease, Integer.valueOf(diseaseTotals.getOrDefault(disease, Integer.valueOf(0)).intValue() + quantity));
            medicineTotals.put(medicine, Integer.valueOf(medicineTotals.getOrDefault(medicine, Integer.valueOf(0)).intValue() + quantity));
        }

        List<String> diseases = diseaseTotals.entrySet().stream()
            .sorted((left, right) -> Integer.compare(right.getValue().intValue(), left.getValue().intValue()))
            .limit(HEATMAP_LIMIT)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        List<String> medicines = medicineTotals.entrySet().stream()
            .sorted((left, right) -> Integer.compare(right.getValue().intValue(), left.getValue().intValue()))
            .limit(HEATMAP_LIMIT)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        Map<String, Integer> diseaseIndexMap = buildIndexMap(diseases);
        Map<String, Integer> medicineIndexMap = buildIndexMap(medicines);

        AdminDashboardDtos.HeatmapData data = new AdminDashboardDtos.HeatmapData();
        data.setDiseases(diseases);
        data.setMedicines(medicines);

        for (Map<String, Object> row : rows) {
            String disease = asString(row.get("disease_category"));
            String medicine = asString(row.get("medicine_name"));
            Integer diseaseIndex = diseaseIndexMap.get(disease);
            Integer medicineIndex = medicineIndexMap.get(medicine);
            if (diseaseIndex == null || medicineIndex == null) {
                continue;
            }
            AdminDashboardDtos.HeatmapPoint point = new AdminDashboardDtos.HeatmapPoint();
            point.setDiseaseIndex(diseaseIndex);
            point.setMedicineIndex(medicineIndex);
            point.setQuantity(Integer.valueOf(asInt(row.get("total_quantity"))));
            data.getPoints().add(point);
        }
        return data;
    }

    private AdminDashboardDtos.MonthlySeriesData buildMedicineSeasonSeries(int currentYear, int currentMonth) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT month_no, medicine_name, SUM(quantity) AS total_quantity "
                + "FROM ("
                + "  SELECT MONTH(c.created_at) AS month_no, "
                + "         COALESCE(NULLIF(TRIM(pi.name), ''), '未命名药品') AS medicine_name, COALESCE(pi.quantity, 0) AS quantity "
                + "  FROM consultations c "
                + "  JOIN prescriptions p ON p.consultation_id = c.id "
                + "  JOIN prescription_items pi ON pi.prescription_id = p.id "
                + "  WHERE YEAR(c.created_at) = ? "
                + "  UNION ALL "
                + "  SELECT MONTH(mo.created_at) AS month_no, "
                + "         COALESCE(NULLIF(TRIM(moi.medicine_name_snapshot), ''), '未命名药品') AS medicine_name, COALESCE(moi.quantity, 0) AS quantity "
                + "  FROM medicine_orders mo "
                + "  JOIN medicine_order_items moi ON moi.order_id = mo.id "
                + "  WHERE YEAR(mo.created_at) = ?"
                + ") merged "
                + "GROUP BY month_no, medicine_name",
            Integer.valueOf(currentYear),
            Integer.valueOf(currentYear)
        );

        Map<String, Integer> yearlyTotals = new LinkedHashMap<String, Integer>();
        for (Map<String, Object> row : rows) {
            String medicine = asString(row.get("medicine_name"));
            int quantity = asInt(row.get("total_quantity"));
            yearlyTotals.put(medicine, Integer.valueOf(yearlyTotals.getOrDefault(medicine, Integer.valueOf(0)).intValue() + quantity));
        }

        List<String> topMedicines = yearlyTotals.entrySet().stream()
            .sorted((left, right) -> Integer.compare(right.getValue().intValue(), left.getValue().intValue()))
            .limit(MEDICINE_SERIES_LIMIT)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        Map<String, int[]> totals = new LinkedHashMap<String, int[]>();
        for (String medicine : topMedicines) {
            totals.put(medicine, new int[12]);
        }

        for (Map<String, Object> row : rows) {
            int month = asInt(row.get("month_no"));
            if (month < 1 || month > 12) {
                continue;
            }
            String medicine = asString(row.get("medicine_name"));
            int[] series = totals.get(medicine);
            if (series == null) {
                continue;
            }
            series[month - 1] += asInt(row.get("total_quantity"));
        }

        AdminDashboardDtos.MonthlySeriesData data = new AdminDashboardDtos.MonthlySeriesData();
        data.setMonths(new ArrayList<String>(MONTH_LABELS));
        for (String medicine : topMedicines) {
            AdminDashboardDtos.SeriesItem item = new AdminDashboardDtos.SeriesItem();
            item.setName(medicine);
            item.setData(maskFutureMonths(totals.getOrDefault(medicine, new int[12]), currentMonth));
            data.getSeries().add(item);
        }
        return data;
    }

    private AdminDashboardDtos.MonthlySeriesData buildDiseaseSeasonSeries(int currentYear, int currentMonth) {
        List<Map<String, Object>> yearlyRows = jdbcTemplate.queryForList(
            "SELECT COALESCE(NULLIF(TRIM(category), ''), '') AS disease_category, COUNT(*) AS total_count "
                + "FROM consultations "
                + "WHERE YEAR(created_at) = ? "
                + "GROUP BY COALESCE(NULLIF(TRIM(category), ''), '')",
            Integer.valueOf(currentYear)
        );

        List<String> topDiseases = yearlyRows.stream()
            .sorted((left, right) -> Integer.compare(asInt(right.get("total_count")), asInt(left.get("total_count"))))
            .limit(DISEASE_SERIES_LIMIT)
            .map(row -> asString(row.get("disease_category")))
            .collect(Collectors.toList());

        if (topDiseases.isEmpty()) {
            AdminDashboardDtos.MonthlySeriesData empty = new AdminDashboardDtos.MonthlySeriesData();
            empty.setMonths(new ArrayList<String>(MONTH_LABELS));
            return empty;
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT COALESCE(NULLIF(TRIM(category), ''), '') AS disease_category, MONTH(created_at) AS month_no, COUNT(*) AS total_count "
                + "FROM consultations "
                + "WHERE YEAR(created_at) = ? "
                + "GROUP BY COALESCE(NULLIF(TRIM(category), ''), ''), MONTH(created_at)",
            Integer.valueOf(currentYear)
        );

        Map<String, int[]> totals = new LinkedHashMap<String, int[]>();
        for (String disease : topDiseases) {
            totals.put(disease, new int[12]);
        }

        for (Map<String, Object> row : rows) {
            String disease = asString(row.get("disease_category"));
            int month = asInt(row.get("month_no"));
            if (!totals.containsKey(disease) || month < 1 || month > 12) {
                continue;
            }
            totals.get(disease)[month - 1] = asInt(row.get("total_count"));
        }

        AdminDashboardDtos.MonthlySeriesData data = new AdminDashboardDtos.MonthlySeriesData();
        data.setMonths(new ArrayList<String>(MONTH_LABELS));
        for (String disease : topDiseases) {
            AdminDashboardDtos.SeriesItem item = new AdminDashboardDtos.SeriesItem();
            item.setName(disease);
            item.setData(maskFutureMonths(totals.getOrDefault(disease, new int[12]), currentMonth));
            data.getSeries().add(item);
        }
        return data;
    }

    private Map<String, Integer> buildIndexMap(List<String> values) {
        Map<String, Integer> indexMap = new LinkedHashMap<String, Integer>();
        for (int index = 0; index < values.size(); index++) {
            indexMap.put(values.get(index), Integer.valueOf(index));
        }
        return indexMap;
    }

    private List<Integer> maskFutureMonths(int[] values, int currentMonth) {
        List<Integer> result = new ArrayList<Integer>();
        for (int index = 0; index < 12; index++) {
            result.add(Integer.valueOf(index < currentMonth ? values[index] : 0));
        }
        return result;
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int asInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return 0;
        }
        String stringValue = String.valueOf(value).trim();
        if (stringValue.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(stringValue);
    }
}
