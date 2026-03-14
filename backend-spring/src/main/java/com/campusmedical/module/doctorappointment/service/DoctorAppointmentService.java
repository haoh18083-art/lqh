package com.campusmedical.module.doctorappointment.service;

import com.campusmedical.common.api.MessageResponse;
import com.campusmedical.common.exception.ConflictException;
import com.campusmedical.common.exception.NotFoundException;
import com.campusmedical.common.exception.ValidationException;
import com.campusmedical.infrastructure.persistence.mysql.entity.AppointmentEntity;
import com.campusmedical.infrastructure.persistence.mysql.entity.AppointmentStatusLogEntity;
import com.campusmedical.infrastructure.persistence.mysql.entity.ConsultationEntity;
import com.campusmedical.infrastructure.persistence.mysql.entity.DoctorEntity;
import com.campusmedical.infrastructure.persistence.mysql.entity.InventoryMovementEntity;
import com.campusmedical.infrastructure.persistence.mysql.entity.MedicineEntity;
import com.campusmedical.infrastructure.persistence.mysql.entity.PrescriptionEntity;
import com.campusmedical.infrastructure.persistence.mysql.entity.PrescriptionItemEntity;
import com.campusmedical.infrastructure.persistence.mysql.entity.StudentEntity;
import com.campusmedical.infrastructure.persistence.mysql.repository.AppointmentRepository;
import com.campusmedical.infrastructure.persistence.mysql.repository.AppointmentStatusLogRepository;
import com.campusmedical.infrastructure.persistence.mysql.repository.ConsultationRepository;
import com.campusmedical.infrastructure.persistence.mysql.repository.InventoryMovementRepository;
import com.campusmedical.infrastructure.persistence.mysql.repository.MedicineRepository;
import com.campusmedical.infrastructure.persistence.mysql.repository.PrescriptionItemRepository;
import com.campusmedical.infrastructure.persistence.mysql.repository.PrescriptionRepository;
import com.campusmedical.module.appointment.dto.AppointmentDtos;
import com.campusmedical.module.appointment.service.AppointmentDocumentService;
import com.campusmedical.module.appointment.service.AppointmentService;
import com.campusmedical.module.doctor.service.DoctorService;
import com.campusmedical.module.doctorappointment.dto.DoctorAppointmentDtos;
import com.campusmedical.module.medicalrecord.dto.MedicalRecordDtos;
import com.campusmedical.module.medicalrecord.service.MedicalRecordService;
import com.campusmedical.module.student.dto.StudentDtos;
import com.campusmedical.module.student.service.HealthProfileService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorAppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentStatusLogRepository appointmentStatusLogRepository;
    private final ConsultationRepository consultationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final MedicineRepository medicineRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final AppointmentService appointmentService;
    private final AppointmentDocumentService appointmentDocumentService;
    private final DoctorService doctorService;
    private final HealthProfileService healthProfileService;
    private final MedicalRecordService medicalRecordService;
    private final ZoneId appZoneId;

    public DoctorAppointmentService(
        AppointmentRepository appointmentRepository,
        AppointmentStatusLogRepository appointmentStatusLogRepository,
        ConsultationRepository consultationRepository,
        PrescriptionRepository prescriptionRepository,
        PrescriptionItemRepository prescriptionItemRepository,
        MedicineRepository medicineRepository,
        InventoryMovementRepository inventoryMovementRepository,
        AppointmentService appointmentService,
        AppointmentDocumentService appointmentDocumentService,
        DoctorService doctorService,
        HealthProfileService healthProfileService,
        MedicalRecordService medicalRecordService,
        @Value("${app.timezone:Asia/Shanghai}") String timezone
    ) {
        this.appointmentRepository = appointmentRepository;
        this.appointmentStatusLogRepository = appointmentStatusLogRepository;
        this.consultationRepository = consultationRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.prescriptionItemRepository = prescriptionItemRepository;
        this.medicineRepository = medicineRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.appointmentService = appointmentService;
        this.appointmentDocumentService = appointmentDocumentService;
        this.doctorService = doctorService;
        this.healthProfileService = healthProfileService;
        this.medicalRecordService = medicalRecordService;
        this.appZoneId = ZoneId.of(timezone);
    }

    @Transactional(readOnly = true)
    public DoctorAppointmentDtos.ListResponse list(Long currentUserId, String status, LocalDate dateValue) {
        DoctorEntity doctor = doctorService.findByUserId(currentUserId);
        Specification<AppointmentEntity> specification = (root, query, builder) -> {
            query.distinct(true);
            Predicate predicate = builder.equal(root.join("doctor").get("id"), doctor.getId());
            if (status != null && !status.trim().isEmpty()) {
                predicate = builder.and(predicate, builder.equal(root.get("status"), status.trim()));
            }
            if (dateValue != null) {
                predicate = builder.and(predicate, builder.equal(root.get("visitDate"), dateValue));
            }
            return predicate;
        };

        List<AppointmentEntity> appointments = appointmentRepository.findAll(
            specification,
            Sort.by(Sort.Order.asc("createdAt"))
        );

        DoctorAppointmentDtos.ListResponse response = new DoctorAppointmentDtos.ListResponse();
        response.setItems(new ArrayList<DoctorAppointmentDtos.Item>());
        for (AppointmentEntity appointment : appointments) {
            DoctorAppointmentDtos.Item item = new DoctorAppointmentDtos.Item();
            item.setAppointmentId(appointment.getId());
            item.setQueueNo(appointment.getQueueNo());
            item.setStudentName(appointment.getStudent().getUser() == null ? "" : appointment.getStudent().getUser().getFullName());
            item.setGender(appointment.getStudent().getGender());
            item.setTimeSlot(appointment.getTimeSlot());
            item.setSymptoms(appointment.getSymptoms());
            item.setCreatedAt(appointment.getCreatedAt());
            response.getItems().add(item);
        }
        response.setTotal(Integer.valueOf(response.getItems().size()));
        return response;
    }

    @Transactional
    public MessageResponse startConsultation(Long appointmentId, Long currentUserId) {
        DoctorEntity doctor = doctorService.findByUserId(currentUserId);
        AppointmentEntity appointment = findForDoctor(appointmentId, doctor.getId());
        if (!"confirmed".equals(appointment.getStatus())) {
            throw new ValidationException("当前预约无法开始接诊");
        }
        if (LocalDateTime.now(appZoneId).isBefore(resolveAppointmentStartAt(appointment))) {
            throw new ValidationException("未到就诊时间");
        }

        String previousStatus = appointment.getStatus();
        appointment.setStatus("in_progress");
        appointmentRepository.save(appointment);
        logStatus(appointment.getId(), previousStatus, "in_progress", null);
        return new MessageResponse("开始接诊");
    }

    @Transactional
    public AppointmentDtos.ConsultationResponse submitDiagnosis(
        Long appointmentId,
        AppointmentDtos.ConsultationCreateRequest request,
        Long currentUserId
    ) {
        DoctorEntity doctor = doctorService.findByUserId(currentUserId);
        AppointmentEntity appointment = findForDoctor(appointmentId, doctor.getId());
        if (!"confirmed".equals(appointment.getStatus()) && !"in_progress".equals(appointment.getStatus())) {
            throw new ValidationException("当前预约状态不可诊断");
        }
        if (consultationRepository.findByAppointment_Id(appointmentId).isPresent()) {
            throw new ConflictException("该预约已存在诊断记录");
        }

        ConsultationEntity consultation = new ConsultationEntity();
        consultation.setAppointment(appointment);
        consultation.setDoctorId(appointment.getDoctor().getId());
        consultation.setStudentId(appointment.getStudent().getId());
        consultation.setCategory(trimToNull(request.getCategory()));
        consultation.setSigns(trimToNull(request.getSigns()));
        consultation.setConclusion(request.getConclusion().trim());
        consultation.setAdvice(trimToNull(request.getAdvice()));
        consultation = consultationRepository.save(consultation);

        PrescriptionEntity prescription = new PrescriptionEntity();
        prescription.setConsultation(consultation);
        prescription.setNote(null);
        prescription = prescriptionRepository.save(prescription);

        List<AppointmentDtos.PrescriptionItemInput> itemInputs = new ArrayList<AppointmentDtos.PrescriptionItemInput>();
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            itemInputs.addAll(request.getItems());
        } else if (request.getDrugIds() != null) {
            for (Long drugId : request.getDrugIds()) {
                AppointmentDtos.PrescriptionItemInput input = new AppointmentDtos.PrescriptionItemInput();
                input.setMedicineId(drugId);
                input.setQuantity(Integer.valueOf(1));
                itemInputs.add(input);
            }
        }

        List<PrescriptionItemEntity> createdItems = new ArrayList<PrescriptionItemEntity>();
        for (AppointmentDtos.PrescriptionItemInput input : itemInputs) {
            MedicineEntity medicine = medicineRepository.findById(input.getMedicineId())
                .orElseThrow(() -> new NotFoundException("药品不存在"));
            if (!Boolean.TRUE.equals(medicine.getIsActive())) {
                throw new NotFoundException("药品不存在");
            }
            int quantity = input.getQuantity() == null ? 1 : input.getQuantity().intValue();
            if (medicine.getStock() == null || medicine.getStock().intValue() < quantity) {
                throw new ValidationException("药品库存不足: " + medicine.getName());
            }

            BigDecimal unitPrice = medicine.getPrice() == null ? BigDecimal.ZERO : medicine.getPrice();
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));

            medicine.setStock(Integer.valueOf(medicine.getStock().intValue() - quantity));
            medicineRepository.save(medicine);

            InventoryMovementEntity movement = new InventoryMovementEntity();
            movement.setMedicine(medicine);
            movement.setDelta(Integer.valueOf(-quantity));
            movement.setReason("prescription");
            movement.setRefType("consultation");
            movement.setRefId(consultation.getId());
            inventoryMovementRepository.save(movement);

            PrescriptionItemEntity item = new PrescriptionItemEntity();
            item.setPrescription(prescription);
            item.setMedicine(medicine);
            item.setName(medicine.getName());
            item.setDosage(trimToNull(input.getDosage()));
            item.setQuantity(Integer.valueOf(quantity));
            item.setUnit(medicine.getUnit());
            item.setUnitPrice(unitPrice);
            item.setTotalPrice(totalPrice);
            createdItems.add(prescriptionItemRepository.save(item));
        }

        String previousStatus = appointment.getStatus();
        appointment.setStatus("completed");
        appointment.setCompletedAt(LocalDateTime.now(ZoneOffset.UTC));
        appointmentRepository.save(appointment);
        logStatus(appointment.getId(), previousStatus, "completed", null);

        appointmentDocumentService.createDiagnosisDocument(appointment, consultation);
        appointmentDocumentService.createPrescriptionDocument(appointment, createdItems);

        try {
            medicalRecordService.createRecord(
                appointment.getId(),
                appointment.getStudent().getId(),
                appointment.getDoctor().getId(),
                appointment.getDepartment() == null ? 0L : appointment.getDepartment().getId(),
                appointment.getVisitDate() == null ? "" : appointment.getVisitDate().toString(),
                appointment.getTimeSlot(),
                appointment.getSymptoms(),
                appointment.getDoctor().getUser() == null ? "" : appointment.getDoctor().getUser().getFullName(),
                appointment.getDepartment() == null ? "" : appointment.getDepartment().getName(),
                consultation.getCategory(),
                consultation.getSigns(),
                consultation.getConclusion(),
                consultation.getAdvice(),
                buildPrescriptionSummary(createdItems),
                calculateFeeTotal(createdItems),
                LocalDateTime.now(ZoneOffset.UTC)
            );
        } catch (Exception ignored) {
        }

        AppointmentDtos.ConsultationResponse response = new AppointmentDtos.ConsultationResponse();
        response.setId(consultation.getId());
        response.setAppointmentId(appointment.getId());
        response.setDoctorId(consultation.getDoctorId());
        response.setStudentId(consultation.getStudentId());
        response.setCategory(consultation.getCategory());
        response.setSigns(consultation.getSigns());
        response.setConclusion(consultation.getConclusion());
        response.setAdvice(consultation.getAdvice());
        response.setCreatedAt(consultation.getCreatedAt());
        response.setPrescriptionItems(createdItems.stream().map(this::toConsultationItem).collect(Collectors.toList()));
        return response;
    }

    @Transactional(readOnly = true)
    public DoctorAppointmentDtos.StudentHistoryResponse getStudentHistory(
        Long appointmentId,
        Long currentUserId,
        LocalDate dateFrom,
        LocalDate dateTo,
        Integer page,
        Integer pageSize
    ) {
        DoctorEntity doctor = doctorService.findByUserId(currentUserId);
        AppointmentEntity appointment = findForDoctor(appointmentId, doctor.getId());
        StudentEntity student = appointment.getStudent();

        StudentDtos.HealthProfileResponse healthProfile = healthProfileService.getByUserId(student.getUser().getId());
        MedicalRecordDtos.ListResponse medicalRecords = medicalRecordService.listForStudent(student.getId(), dateFrom, dateTo, page, pageSize);

        DoctorAppointmentDtos.StudentInfo studentInfo = new DoctorAppointmentDtos.StudentInfo();
        studentInfo.setId(student.getId());
        studentInfo.setStudentId(student.getStudentId());
        studentInfo.setFullName(student.getUser() == null ? "" : student.getUser().getFullName());
        studentInfo.setGender(student.getGender());
        studentInfo.setDob(student.getDob());

        DoctorAppointmentDtos.StudentHistoryResponse response = new DoctorAppointmentDtos.StudentHistoryResponse();
        response.setStudent(studentInfo);
        response.setMedicalHistory(healthProfile.getMedicalHistory());
        response.setMedicalRecords(medicalRecords.getItems());
        response.setTotalRecords(medicalRecords.getTotal());
        response.setPage(medicalRecords.getPage());
        response.setPageSize(medicalRecords.getPageSize());
        response.setTotalPages(medicalRecords.getTotalPages());
        return response;
    }

    private AppointmentEntity findForDoctor(Long appointmentId, Long doctorId) {
        AppointmentEntity appointment = appointmentService.findEntity(appointmentId);
        if (!appointment.getDoctor().getId().equals(doctorId)) {
            throw new NotFoundException("预约不存在");
        }
        return appointment;
    }

    private LocalDateTime resolveAppointmentStartAt(AppointmentEntity appointment) {
        String slot = appointment.getTimeSlot() == null ? "" : appointment.getTimeSlot();
        String startValue = slot.split("-")[0].split("~")[0].trim();
        try {
            LocalTime startTime = LocalTime.parse(startValue);
            return LocalDateTime.of(appointment.getVisitDate(), startTime);
        } catch (DateTimeParseException exception) {
            throw new ValidationException("无法解析就诊时间段");
        }
    }

    private void logStatus(Long appointmentId, String fromStatus, String toStatus, String reason) {
        AppointmentStatusLogEntity log = new AppointmentStatusLogEntity();
        log.setAppointmentId(appointmentId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setReason(reason);
        appointmentStatusLogRepository.save(log);
    }

    private AppointmentDtos.ConsultationPrescriptionItem toConsultationItem(PrescriptionItemEntity entity) {
        AppointmentDtos.ConsultationPrescriptionItem item = new AppointmentDtos.ConsultationPrescriptionItem();
        item.setId(entity.getId());
        item.setMedicineId(entity.getMedicine() == null ? null : entity.getMedicine().getId());
        item.setName(entity.getName());
        item.setDosage(entity.getDosage());
        item.setQuantity(entity.getQuantity());
        item.setUnit(entity.getUnit());
        return item;
    }

    private List<Map<String, Object>> buildPrescriptionSummary(List<PrescriptionItemEntity> items) {
        List<Map<String, Object>> summaries = new ArrayList<Map<String, Object>>();
        for (PrescriptionItemEntity item : items) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("name", item.getName());
            map.put("dosage", item.getDosage());
            map.put("quantity", item.getQuantity());
            map.put("unit", item.getUnit());
            map.put("unit_price", item.getUnitPrice() == null ? 0D : item.getUnitPrice().doubleValue());
            map.put("total_price", item.getTotalPrice() == null ? 0D : item.getTotalPrice().doubleValue());
            summaries.add(map);
        }
        return summaries;
    }

    private double calculateFeeTotal(List<PrescriptionItemEntity> items) {
        double total = 0D;
        for (PrescriptionItemEntity item : items) {
            total += item.getTotalPrice() == null ? 0D : item.getTotalPrice().doubleValue();
        }
        return total;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
