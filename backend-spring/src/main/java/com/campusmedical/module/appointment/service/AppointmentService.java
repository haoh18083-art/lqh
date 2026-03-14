package com.campusmedical.module.appointment.service;

import com.campusmedical.common.api.PageResponse;
import com.campusmedical.common.exception.ConflictException;
import com.campusmedical.common.exception.NotFoundException;
import com.campusmedical.common.exception.ValidationException;
import com.campusmedical.infrastructure.persistence.mysql.entity.AppointmentEntity;
import com.campusmedical.infrastructure.persistence.mysql.entity.AppointmentStatusLogEntity;
import com.campusmedical.infrastructure.persistence.mysql.entity.DepartmentEntity;
import com.campusmedical.infrastructure.persistence.mysql.entity.DoctorEntity;
import com.campusmedical.infrastructure.persistence.mysql.entity.DoctorScheduleEntity;
import com.campusmedical.infrastructure.persistence.mysql.entity.DocumentEntity;
import com.campusmedical.infrastructure.persistence.mysql.entity.StudentEntity;
import com.campusmedical.infrastructure.persistence.mysql.repository.AppointmentRepository;
import com.campusmedical.infrastructure.persistence.mysql.repository.AppointmentStatusLogRepository;
import com.campusmedical.infrastructure.persistence.mysql.repository.DoctorScheduleRepository;
import com.campusmedical.module.appointment.dto.AppointmentDtos;
import com.campusmedical.module.doctor.service.DoctorService;
import com.campusmedical.module.student.service.StudentService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {

    private static final List<String> ACTIVE_STATUSES = Arrays.asList("confirmed", "in_progress", "completed");

    private final AppointmentRepository appointmentRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final AppointmentStatusLogRepository appointmentStatusLogRepository;
    private final DoctorService doctorService;
    private final StudentService studentService;
    private final AppointmentDocumentService appointmentDocumentService;

    public AppointmentService(
        AppointmentRepository appointmentRepository,
        DoctorScheduleRepository doctorScheduleRepository,
        AppointmentStatusLogRepository appointmentStatusLogRepository,
        DoctorService doctorService,
        StudentService studentService,
        AppointmentDocumentService appointmentDocumentService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.doctorScheduleRepository = doctorScheduleRepository;
        this.appointmentStatusLogRepository = appointmentStatusLogRepository;
        this.doctorService = doctorService;
        this.studentService = studentService;
        this.appointmentDocumentService = appointmentDocumentService;
    }

    @Transactional(readOnly = true)
    public List<AppointmentDtos.SlotResponse> getSlots(Long currentUserId, Long doctorId, LocalDate dateFrom, LocalDate dateTo) {
        StudentEntity student = studentService.findByUserId(currentUserId);

        Specification<DoctorScheduleEntity> specification = (root, query, builder) -> {
            Predicate predicate = builder.conjunction();
            predicate = builder.and(predicate, builder.equal(root.join("doctor").get("id"), doctorId));
            predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("scheduleDate"), dateFrom));
            predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("scheduleDate"), dateTo));
            return predicate;
        };

        List<DoctorScheduleEntity> schedules = doctorScheduleRepository.findAll(
            specification,
            Sort.by(Sort.Order.asc("scheduleDate"), Sort.Order.asc("timeSlot"))
        );

        Set<String> bookedSlots = new HashSet<String>();
        for (AppointmentEntity appointment : appointmentRepository.findByStudent_IdAndDoctor_IdAndVisitDateBetweenAndStatusIn(
            student.getId(),
            doctorId,
            dateFrom,
            dateTo,
            ACTIVE_STATUSES
        )) {
            bookedSlots.add(key(appointment.getVisitDate(), appointment.getTimeSlot()));
        }

        List<AppointmentDtos.SlotResponse> responses = new ArrayList<AppointmentDtos.SlotResponse>();
        for (DoctorScheduleEntity schedule : schedules) {
            AppointmentDtos.SlotResponse response = new AppointmentDtos.SlotResponse();
            response.setDate(schedule.getScheduleDate());
            response.setTime(schedule.getTimeSlot());
            response.setBookedCount(schedule.getBookedCount());
            response.setCapacity(schedule.getCapacity());
            if (!"open".equals(schedule.getStatus())) {
                response.setStatus("closed");
            } else if (schedule.getBookedCount() != null && schedule.getCapacity() != null && schedule.getBookedCount() >= schedule.getCapacity()) {
                response.setStatus("full");
            } else if (bookedSlots.contains(key(schedule.getScheduleDate(), schedule.getTimeSlot()))) {
                response.setStatus("booked");
            } else {
                response.setStatus("available");
            }
            responses.add(response);
        }
        return responses;
    }

    @Transactional
    public AppointmentDtos.Response create(AppointmentDtos.CreateRequest request, Long currentUserId) {
        StudentEntity student = studentService.findByUserId(currentUserId);
        DoctorEntity doctor = doctorService.findEntity(request.getDoctorId());
        validateDoctor(doctor);

        DoctorScheduleEntity schedule = getScheduleWithLock(doctor.getId(), request.getDate(), request.getTime());
        if (!"open".equals(schedule.getStatus())) {
            throw new ValidationException("该时段不可预约");
        }
        if (schedule.getBookedCount() != null && schedule.getCapacity() != null && schedule.getBookedCount() >= schedule.getCapacity()) {
            throw new ConflictException("该时段已满");
        }

        if (appointmentRepository.findFirstByStudent_IdAndVisitDateAndTimeSlotAndStatusIn(
            student.getId(),
            request.getDate(),
            request.getTime(),
            ACTIVE_STATUSES
        ).isPresent()) {
            throw new ConflictException("该时间段已有预约");
        }

        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setStudent(student);
        appointment.setDoctor(doctor);
        appointment.setDepartment(requireDepartment(doctor));
        appointment.setVisitDate(request.getDate());
        appointment.setTimeSlot(request.getTime());
        appointment.setStatus("confirmed");
        appointment.setSymptoms(trimToNull(request.getSymptoms()));
        appointment.setQueueNo(generateQueueNo(doctor.getId(), request.getDate(), request.getTime()));
        appointment.setCreatedAt(nowUtc());
        appointment.setConfirmedAt(nowUtc());

        schedule.setBookedCount(Integer.valueOf(schedule.getBookedCount() == null ? 1 : schedule.getBookedCount().intValue() + 1));
        AppointmentEntity saved = appointmentRepository.save(appointment);
        doctorScheduleRepository.save(schedule);
        logStatus(saved.getId(), "new", "confirmed", null);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<AppointmentDtos.Response> listMine(
        Long currentUserId,
        String status,
        LocalDate dateFrom,
        LocalDate dateTo,
        Integer page,
        Integer pageSize
    ) {
        Long studentId = studentService.findByUserId(currentUserId).getId();
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        Pageable pageable = PageRequest.of(normalizedPage - 1, normalizedPageSize, Sort.by(Sort.Order.desc("createdAt")));

        Specification<AppointmentEntity> specification = (root, query, builder) -> {
            query.distinct(true);
            Predicate predicate = builder.equal(root.join("student").get("id"), studentId);
            if (trimToNull(status) != null) {
                predicate = builder.and(predicate, builder.equal(root.get("status"), status.trim()));
            }
            if (dateFrom != null) {
                predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("visitDate"), dateFrom));
            }
            if (dateTo != null) {
                predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("visitDate"), dateTo));
            }
            return predicate;
        };

        Page<AppointmentEntity> result = appointmentRepository.findAll(specification, pageable);
        return new PageResponse<AppointmentDtos.Response>(
            result.getContent().stream().map(this::toResponse).collect(Collectors.toList()),
            result.getTotalElements(),
            normalizedPage,
            normalizedPageSize,
            result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public AppointmentDtos.Response detail(Long appointmentId, Long currentUserId) {
        StudentEntity student = studentService.findByUserId(currentUserId);
        return toResponse(findForStudent(appointmentId, student.getId()));
    }

    @Transactional
    public AppointmentDtos.Response cancel(Long appointmentId, String reason, Long currentUserId) {
        StudentEntity student = studentService.findByUserId(currentUserId);
        AppointmentEntity appointment = findForStudent(appointmentId, student.getId());
        if (!"confirmed".equals(appointment.getStatus())) {
            throw new ValidationException("当前状态不可取消");
        }

        DoctorScheduleEntity schedule = getScheduleWithLock(
            appointment.getDoctor().getId(),
            appointment.getVisitDate(),
            appointment.getTimeSlot()
        );
        appointment.setStatus("cancelled");
        appointment.setCancelledAt(nowUtc());
        appointment.setCancellationReason(reason);
        schedule.setBookedCount(Integer.valueOf(Math.max(0, schedule.getBookedCount() == null ? 0 : schedule.getBookedCount().intValue() - 1)));
        doctorScheduleRepository.save(schedule);
        AppointmentEntity saved = appointmentRepository.save(appointment);
        logStatus(saved.getId(), "confirmed", "cancelled", reason);
        return toResponse(saved);
    }

    @Transactional
    public AppointmentDtos.RescheduleResponse reschedule(
        Long appointmentId,
        AppointmentDtos.RescheduleRequest request,
        Long currentUserId
    ) {
        StudentEntity student = studentService.findByUserId(currentUserId);
        AppointmentEntity appointment = findForStudent(appointmentId, student.getId());
        if (!"confirmed".equals(appointment.getStatus())) {
            throw new ValidationException("当前状态不可改期");
        }

        DoctorScheduleEntity oldSchedule = getScheduleWithLock(
            appointment.getDoctor().getId(),
            appointment.getVisitDate(),
            appointment.getTimeSlot()
        );
        DoctorScheduleEntity newSchedule = getScheduleWithLock(
            appointment.getDoctor().getId(),
            request.getNewDate(),
            request.getNewTime()
        );

        if (!"open".equals(newSchedule.getStatus())) {
            throw new ValidationException("新时段不可预约");
        }
        if (newSchedule.getBookedCount() != null && newSchedule.getCapacity() != null && newSchedule.getBookedCount() >= newSchedule.getCapacity()) {
            throw new ConflictException("新时段已满");
        }

        appointment.setStatus("rescheduled");
        appointment.setRescheduleReason(request.getReason());
        appointmentRepository.save(appointment);
        logStatus(appointment.getId(), "confirmed", "rescheduled", request.getReason());

        AppointmentEntity newAppointment = new AppointmentEntity();
        newAppointment.setStudent(appointment.getStudent());
        newAppointment.setDoctor(appointment.getDoctor());
        newAppointment.setDepartment(appointment.getDepartment());
        newAppointment.setVisitDate(request.getNewDate());
        newAppointment.setTimeSlot(request.getNewTime());
        newAppointment.setStatus("confirmed");
        newAppointment.setSymptoms(appointment.getSymptoms());
        newAppointment.setQueueNo(generateQueueNo(appointment.getDoctor().getId(), request.getNewDate(), request.getNewTime()));
        newAppointment.setCreatedAt(nowUtc());
        newAppointment.setConfirmedAt(nowUtc());
        newAppointment.setNotes("改期自 " + appointment.getVisitDate() + " " + appointment.getTimeSlot());
        AppointmentEntity savedNewAppointment = appointmentRepository.save(newAppointment);
        logStatus(savedNewAppointment.getId(), "new", "confirmed", "reschedule");

        oldSchedule.setBookedCount(Integer.valueOf(Math.max(0, oldSchedule.getBookedCount() == null ? 0 : oldSchedule.getBookedCount().intValue() - 1)));
        newSchedule.setBookedCount(Integer.valueOf(newSchedule.getBookedCount() == null ? 1 : newSchedule.getBookedCount().intValue() + 1));
        doctorScheduleRepository.save(oldSchedule);
        doctorScheduleRepository.save(newSchedule);

        AppointmentDtos.RescheduleResponse response = new AppointmentDtos.RescheduleResponse();
        response.setOldAppointment(toResponse(appointment));
        response.setNewAppointment(toResponse(savedNewAppointment));
        return response;
    }

    @Transactional(readOnly = true)
    public AppointmentDtos.DocumentListResponse listDocuments(Long appointmentId, Long currentUserId, boolean admin) {
        ensureAccessible(appointmentId, currentUserId, admin);
        AppointmentDtos.DocumentListResponse response = new AppointmentDtos.DocumentListResponse();
        response.setItems(appointmentDocumentService.listDocuments(appointmentId));
        return response;
    }

    @Transactional(readOnly = true)
    public DocumentEntity getAccessibleDocument(Long appointmentId, String docType, Long currentUserId, boolean admin) {
        ensureAccessible(appointmentId, currentUserId, admin);
        return appointmentDocumentService.getDocumentEntity(appointmentId, docType);
    }

    @Transactional(readOnly = true)
    public AppointmentEntity findEntity(Long appointmentId) {
        return appointmentRepository.findById(appointmentId).orElseThrow(() -> new NotFoundException("预约不存在"));
    }

    public AppointmentDtos.Response toResponse(AppointmentEntity entity) {
        AppointmentDtos.Response response = new AppointmentDtos.Response();
        response.setId(entity.getId());
        response.setDate(entity.getVisitDate());
        response.setTime(entity.getTimeSlot());
        response.setStatus(entity.getStatus());
        response.setQueueNo(entity.getQueueNo());
        response.setSymptoms(entity.getSymptoms());
        response.setCreatedAt(entity.getCreatedAt());
        response.setConfirmedAt(entity.getConfirmedAt());
        response.setCancelledAt(entity.getCancelledAt());
        response.setCompletedAt(entity.getCompletedAt());

        AppointmentDtos.StudentInfo studentInfo = new AppointmentDtos.StudentInfo();
        studentInfo.setId(entity.getStudent().getId());
        studentInfo.setStudentId(entity.getStudent().getStudentId());
        studentInfo.setFullName(entity.getStudent().getUser() == null ? "" : entity.getStudent().getUser().getFullName());
        studentInfo.setGender(entity.getStudent().getGender());
        studentInfo.setDob(entity.getStudent().getDob());
        response.setStudent(studentInfo);

        AppointmentDtos.DoctorInfo doctorInfo = new AppointmentDtos.DoctorInfo();
        doctorInfo.setId(entity.getDoctor().getId());
        doctorInfo.setFullName(entity.getDoctor().getUser() == null ? "" : entity.getDoctor().getUser().getFullName());
        doctorInfo.setTitle(entity.getDoctor().getTitle());
        doctorInfo.setDepartmentId(resolveDepartmentId(entity));
        doctorInfo.setDepartmentName(resolveDepartmentName(entity));
        response.setDoctor(doctorInfo);

        AppointmentDtos.DepartmentInfo departmentInfo = new AppointmentDtos.DepartmentInfo();
        departmentInfo.setId(resolveDepartmentId(entity));
        departmentInfo.setName(resolveDepartmentName(entity));
        response.setDepartment(departmentInfo);

        return response;
    }

    private void ensureAccessible(Long appointmentId, Long currentUserId, boolean admin) {
        if (admin) {
            findEntity(appointmentId);
            return;
        }
        StudentEntity student = studentService.findByUserId(currentUserId);
        findForStudent(appointmentId, student.getId());
    }

    private AppointmentEntity findForStudent(Long appointmentId, Long studentId) {
        AppointmentEntity entity = findEntity(appointmentId);
        if (!entity.getStudent().getId().equals(studentId)) {
            throw new NotFoundException("预约不存在");
        }
        return entity;
    }

    private DoctorScheduleEntity getScheduleWithLock(Long doctorId, LocalDate date, String timeSlot) {
        return doctorScheduleRepository
            .findForUpdate(doctorId, date, timeSlot)
            .orElseThrow(() -> new NotFoundException("排班不存在"));
    }

    private void validateDoctor(DoctorEntity doctor) {
        if (doctor.getUser() == null || !Boolean.TRUE.equals(doctor.getUser().getIsActive())) {
            throw new NotFoundException("医生不存在");
        }
        requireDepartment(doctor);
    }

    private DepartmentEntity requireDepartment(DoctorEntity doctor) {
        if (doctor.getDepartmentRel() == null) {
            throw new ValidationException("医生未配置科室，无法预约");
        }
        return doctor.getDepartmentRel();
    }

    private String generateQueueNo(Long doctorId, LocalDate visitDate, String timeSlot) {
        long count = appointmentRepository.countByDoctor_IdAndVisitDateAndTimeSlotAndStatusIn(
            doctorId,
            visitDate,
            timeSlot,
            ACTIVE_STATUSES
        );
        return String.format("A%02d", Long.valueOf(count + 1));
    }

    private void logStatus(Long appointmentId, String fromStatus, String toStatus, String reason) {
        AppointmentStatusLogEntity log = new AppointmentStatusLogEntity();
        log.setAppointmentId(appointmentId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setReason(reason);
        appointmentStatusLogRepository.save(log);
    }

    private String key(LocalDate date, String timeSlot) {
        return date + "|" + timeSlot;
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private Long resolveDepartmentId(AppointmentEntity entity) {
        if (entity.getDepartment() != null) {
            return entity.getDepartment().getId();
        }
        if (entity.getDoctor() != null && entity.getDoctor().getDepartmentRel() != null) {
            return entity.getDoctor().getDepartmentRel().getId();
        }
        return null;
    }

    private String resolveDepartmentName(AppointmentEntity entity) {
        if (entity.getDepartment() != null) {
            return entity.getDepartment().getName();
        }
        if (entity.getDoctor() != null && entity.getDoctor().getDepartmentRel() != null) {
            return entity.getDoctor().getDepartmentRel().getName();
        }
        return entity.getDoctor() == null ? "未知" : entity.getDoctor().getDepartment();
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
