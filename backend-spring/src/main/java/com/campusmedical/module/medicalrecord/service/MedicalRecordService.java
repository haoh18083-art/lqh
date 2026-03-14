package com.campusmedical.module.medicalrecord.service;

import com.campusmedical.module.medicalrecord.dto.MedicalRecordDtos;
import com.campusmedical.module.student.service.StudentService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicalRecordService {

    private static final String COLLECTION = "medical_records";

    private final MongoTemplate mongoTemplate;
    private final StudentService studentService;

    public MedicalRecordService(MongoTemplate mongoTemplate, StudentService studentService) {
        this.mongoTemplate = mongoTemplate;
        this.studentService = studentService;
    }

    @Transactional(readOnly = true)
    public MedicalRecordDtos.ListResponse listMine(
        Long currentUserId,
        LocalDate dateFrom,
        LocalDate dateTo,
        Integer page,
        Integer pageSize
    ) {
        Long studentId = studentService.findByUserId(currentUserId).getId();
        return listForStudent(studentId, dateFrom, dateTo, page, pageSize);
    }

    @Transactional(readOnly = true)
    public MedicalRecordDtos.ListResponse listForStudent(
        Long studentId,
        LocalDate dateFrom,
        LocalDate dateTo,
        Integer page,
        Integer pageSize
    ) {
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        Query query = buildQuery(studentId, dateFrom, dateTo);

        long total = mongoTemplate.count(query, COLLECTION);
        query.with(Sort.by(Sort.Order.desc("visit_date"), Sort.Order.desc("time_slot")));
        query.skip((long) (normalizedPage - 1) * normalizedPageSize);
        query.limit(normalizedPageSize);

        List<Document> documents = mongoTemplate.find(query, Document.class, COLLECTION);
        List<MedicalRecordDtos.Item> items = new ArrayList<MedicalRecordDtos.Item>();
        for (Document document : documents) {
            items.add(toResponse(document));
        }

        MedicalRecordDtos.ListResponse response = new MedicalRecordDtos.ListResponse();
        response.setItems(items);
        response.setTotal(total);
        response.setPage(normalizedPage);
        response.setPageSize(normalizedPageSize);
        response.setTotalPages(total > 0 ? (int) Math.ceil((double) total / normalizedPageSize) : 0);
        return response;
    }

    public void createRecord(
        Long appointmentId,
        Long studentId,
        Long doctorId,
        Long departmentId,
        String visitDate,
        String timeSlot,
        String symptoms,
        String doctorName,
        String departmentName,
        String category,
        String signs,
        String conclusion,
        String advice,
        List<Map<String, Object>> prescriptionSummary,
        double feeTotal,
        LocalDateTime createdAt
    ) {
        Document document = new Document();
        document.put("appointment_id", appointmentId);
        document.put("student_id", studentId);
        document.put("doctor_id", doctorId);
        document.put("department_id", departmentId);
        document.put("visit_date", visitDate);
        document.put("time_slot", timeSlot);
        document.put("symptoms", symptoms);
        document.put("doctor_name", doctorName);
        document.put("department_name", departmentName);

        Document diagnosisSummary = new Document();
        diagnosisSummary.put("category", category);
        diagnosisSummary.put("signs", signs);
        diagnosisSummary.put("conclusion", conclusion);
        diagnosisSummary.put("advice", advice);
        document.put("diagnosis_summary", diagnosisSummary);

        document.put("prescription_summary", prescriptionSummary == null ? new ArrayList<Document>() : prescriptionSummary);
        document.put("fee_total", feeTotal);
        document.put("created_at", toDate(createdAt == null ? LocalDateTime.now(ZoneOffset.UTC) : createdAt));

        mongoTemplate.insert(document, COLLECTION);
    }

    private Query buildQuery(Long studentId, LocalDate dateFrom, LocalDate dateTo) {
        Query query = new Query();
        query.addCriteria(Criteria.where("student_id").is(studentId));
        if (dateFrom != null || dateTo != null) {
            Criteria visitDateCriteria = Criteria.where("visit_date");
            if (dateFrom != null) {
                visitDateCriteria = visitDateCriteria.gte(dateFrom.toString());
            }
            if (dateTo != null) {
                visitDateCriteria = visitDateCriteria.lte(dateTo.toString());
            }
            query.addCriteria(visitDateCriteria);
        }
        return query;
    }

    private MedicalRecordDtos.Item toResponse(Document document) {
        MedicalRecordDtos.Item item = new MedicalRecordDtos.Item();
        Object recordId = document.get("_id");
        item.setId(recordId == null ? "" : String.valueOf(recordId));
        item.setAppointmentId(toLong(document.get("appointment_id")));
        item.setStudentId(toLong(document.get("student_id")));
        item.setDoctorId(toLong(document.get("doctor_id")));
        item.setDepartmentId(defaultLong(toLong(document.get("department_id")), 0L));
        item.setVisitDate(normalizeVisitDate(document.get("visit_date"), document.get("created_at")));
        item.setTimeSlot(defaultString(document.get("time_slot"), ""));
        item.setSymptoms(nullableString(document.get("symptoms")));
        item.setDoctorName(defaultString(document.get("doctor_name"), ""));
        item.setDepartmentName(defaultString(document.get("department_name"), ""));
        item.setDiagnosisSummary(normalizeDiagnosis(document.get("diagnosis_summary")));
        item.setPrescriptionSummary(normalizePrescriptions(document.get("prescription_summary")));
        item.setFeeTotal(defaultDouble(toDouble(document.get("fee_total")), 0D));
        item.setCreatedAt(toLocalDateTime(document.get("created_at")));
        return item;
    }

    private MedicalRecordDtos.DiagnosisSummary normalizeDiagnosis(Object value) {
        MedicalRecordDtos.DiagnosisSummary summary = new MedicalRecordDtos.DiagnosisSummary();
        if (value instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) value;
            summary.setCategory(nullableString(map.get("category")));
            summary.setSigns(nullableString(map.get("signs")));
            summary.setConclusion(defaultString(map.get("conclusion"), ""));
            summary.setAdvice(nullableString(map.get("advice")));
        } else {
            summary.setConclusion("");
        }
        return summary;
    }

    private List<MedicalRecordDtos.PrescriptionSummaryItem> normalizePrescriptions(Object value) {
        List<MedicalRecordDtos.PrescriptionSummaryItem> items = new ArrayList<MedicalRecordDtos.PrescriptionSummaryItem>();
        if (!(value instanceof Iterable<?>)) {
            return items;
        }
        for (Object rawItem : (Iterable<?>) value) {
            if (!(rawItem instanceof Map<?, ?>)) {
                continue;
            }
            Map<?, ?> map = (Map<?, ?>) rawItem;
            MedicalRecordDtos.PrescriptionSummaryItem item = new MedicalRecordDtos.PrescriptionSummaryItem();
            item.setName(defaultString(map.get("name"), ""));
            item.setDosage(nullableString(map.get("dosage")));
            item.setQuantity(defaultInteger(toInteger(map.get("quantity")), 0));
            item.setUnit(nullableString(map.get("unit")));
            item.setUnitPrice(defaultDouble(toDouble(map.get("unit_price")), 0D));
            item.setTotalPrice(defaultDouble(toDouble(map.get("total_price")), 0D));
            items.add(item);
        }
        return items;
    }

    private String normalizeVisitDate(Object visitDate, Object createdAt) {
        if (visitDate instanceof String) {
            return (String) visitDate;
        }
        if (visitDate instanceof LocalDate) {
            return visitDate.toString();
        }
        if (visitDate instanceof LocalDateTime) {
            return ((LocalDateTime) visitDate).toLocalDate().toString();
        }
        if (visitDate instanceof Date) {
            return ((Date) visitDate).toInstant().atZone(ZoneOffset.UTC).toLocalDate().toString();
        }
        LocalDateTime createdAtTime = toLocalDateTime(createdAt);
        return createdAtTime == null ? "" : createdAtTime.toLocalDate().toString();
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof Date) {
            return LocalDateTime.ofInstant(((Date) value).toInstant(), ZoneOffset.UTC);
        }
        return null;
    }

    private Date toDate(LocalDateTime value) {
        return Date.from(value.toInstant(ZoneOffset.UTC));
    }

    private String defaultString(Object value, String defaultValue) {
        String result = nullableString(value);
        return result == null ? defaultValue : result;
    }

    private String nullableString(Object value) {
        if (value == null) {
            return null;
        }
        String result = String.valueOf(value);
        return result.trim().isEmpty() ? null : result;
    }

    private Long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String && !((String) value).trim().isEmpty()) {
            return Long.valueOf(((String) value).trim());
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String && !((String) value).trim().isEmpty()) {
            return Integer.valueOf(((String) value).trim());
        }
        return null;
    }

    private Double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String && !((String) value).trim().isEmpty()) {
            return Double.valueOf(((String) value).trim());
        }
        return null;
    }

    private long defaultLong(Long value, long defaultValue) {
        return value == null ? defaultValue : value.longValue();
    }

    private int defaultInteger(Integer value, int defaultValue) {
        return value == null ? defaultValue : value.intValue();
    }

    private double defaultDouble(Double value, double defaultValue) {
        return value == null ? defaultValue : value.doubleValue();
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1 || pageSize > 100) {
            return 20;
        }
        return pageSize;
    }
}
