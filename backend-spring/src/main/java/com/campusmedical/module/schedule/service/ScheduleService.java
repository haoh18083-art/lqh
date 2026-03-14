package com.campusmedical.module.schedule.service;

import com.campusmedical.common.api.PageResponse;
import com.campusmedical.common.exception.NotFoundException;
import com.campusmedical.common.exception.ValidationException;
import com.campusmedical.infrastructure.persistence.mysql.entity.DoctorEntity;
import com.campusmedical.infrastructure.persistence.mysql.entity.DoctorScheduleEntity;
import com.campusmedical.infrastructure.persistence.mysql.repository.DoctorScheduleRepository;
import com.campusmedical.module.doctor.service.DoctorService;
import com.campusmedical.module.schedule.dto.ScheduleDtos;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
public class ScheduleService {

    private final DoctorScheduleRepository scheduleRepository;
    private final DoctorService doctorService;

    public ScheduleService(DoctorScheduleRepository scheduleRepository, DoctorService doctorService) {
        this.scheduleRepository = scheduleRepository;
        this.doctorService = doctorService;
    }

    @Transactional
    public List<ScheduleDtos.Response> create(ScheduleDtos.CreateRequest request) {
        DoctorEntity doctor = doctorService.findEntity(request.getDoctorId());
        validateTimeSlots(request.getTimeSlots());
        List<ScheduleDtos.Response> responses = new ArrayList<ScheduleDtos.Response>();
        for (String slot : request.getTimeSlots()) {
            DoctorScheduleEntity entity = scheduleRepository
                .findByDoctor_IdAndScheduleDateAndTimeSlot(doctor.getId(), request.getDate(), slot)
                .orElseGet(DoctorScheduleEntity::new);
            entity.setDoctor(doctor);
            entity.setScheduleDate(request.getDate());
            entity.setTimeSlot(slot);
            entity.setCapacity(request.getCapacity());
            if (entity.getBookedCount() == null) {
                entity.setBookedCount(0);
            }
            entity.setStatus("open");
            responses.add(toResponse(scheduleRepository.save(entity)));
        }
        return responses;
    }

    @Transactional
    public ScheduleDtos.BulkResponse bulkUpsert(ScheduleDtos.BulkUpsertRequest request) {
        if (request.getDateFrom().isAfter(request.getDateTo())) {
            throw new ValidationException("起始日期不能晚于结束日期");
        }
        validateTimeSlots(request.getTimeSlots());
        DoctorEntity doctor = doctorService.findEntity(request.getDoctorId());

        int created = 0;
        int updated = 0;
        List<ScheduleDtos.Response> items = new ArrayList<ScheduleDtos.Response>();
        LocalDate current = request.getDateFrom();
        while (!current.isAfter(request.getDateTo())) {
            for (String slot : request.getTimeSlots()) {
                DoctorScheduleEntity entity = scheduleRepository
                    .findByDoctor_IdAndScheduleDateAndTimeSlot(doctor.getId(), current, slot)
                    .orElseGet(DoctorScheduleEntity::new);
                boolean existing = entity.getId() != null;
                if (existing && entity.getBookedCount() != null && request.getCapacity() < entity.getBookedCount()) {
                    throw new ValidationException("容量不能小于已预约人数");
                }
                entity.setDoctor(doctor);
                entity.setScheduleDate(current);
                entity.setTimeSlot(slot);
                entity.setCapacity(request.getCapacity());
                entity.setStatus(request.getStatus());
                if (entity.getBookedCount() == null) {
                    entity.setBookedCount(0);
                }
                scheduleRepository.save(entity);
                if (existing) {
                    updated += 1;
                } else {
                    created += 1;
                }
                items.add(toResponse(entity));
            }
            current = current.plusDays(1);
        }

        ScheduleDtos.BulkResponse response = new ScheduleDtos.BulkResponse();
        response.setCreated(created);
        response.setUpdated(updated);
        response.setItems(items);
        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<ScheduleDtos.Response> list(
        Integer page,
        Integer pageSize,
        Long doctorId,
        LocalDate dateValue,
        LocalDate dateFrom,
        LocalDate dateTo
    ) {
        Pageable pageable = PageRequest.of(
            normalizePage(page) - 1,
            normalizePageSize(pageSize),
            Sort.by(Sort.Order.desc("scheduleDate"), Sort.Order.desc("timeSlot"))
        );

        Specification<DoctorScheduleEntity> specification = (root, query, builder) -> {
            Predicate predicate = builder.conjunction();
            if (doctorId != null) {
                predicate = builder.and(predicate, builder.equal(root.join("doctor").get("id"), doctorId));
            }
            if (dateValue != null) {
                predicate = builder.and(predicate, builder.equal(root.get("scheduleDate"), dateValue));
            } else {
                if (dateFrom != null) {
                    predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("scheduleDate"), dateFrom));
                }
                if (dateTo != null) {
                    predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("scheduleDate"), dateTo));
                }
            }
            return predicate;
        };

        Page<DoctorScheduleEntity> result = scheduleRepository.findAll(specification, pageable);
        return new PageResponse<ScheduleDtos.Response>(
            result.getContent().stream().map(this::toResponse).collect(Collectors.toList()),
            result.getTotalElements(),
            normalizePage(page),
            normalizePageSize(pageSize),
            result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ScheduleDtos.Response> listMine(Long doctorId, LocalDate dateFrom, LocalDate dateTo) {
        LocalDate effectiveFrom = dateFrom == null ? LocalDate.now().minusYears(5) : dateFrom;
        LocalDate effectiveTo = dateTo == null ? LocalDate.now().plusYears(5) : dateTo;
        List<DoctorScheduleEntity> items = scheduleRepository
            .findByDoctor_IdAndScheduleDateBetweenOrderByScheduleDateDescTimeSlotDesc(doctorId, effectiveFrom, effectiveTo);
        List<ScheduleDtos.Response> responses = items.stream().map(this::toResponse).collect(Collectors.toList());
        return new PageResponse<ScheduleDtos.Response>(
            responses,
            responses.size(),
            1,
            responses.size(),
            responses.isEmpty() ? 0 : 1
        );
    }

    @Transactional
    public ScheduleDtos.Response update(Long scheduleId, ScheduleDtos.UpdateRequest request) {
        DoctorScheduleEntity entity = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new NotFoundException("排班不存在"));
        if (request.getCapacity() != null) {
            if (request.getCapacity() < entity.getBookedCount()) {
                throw new ValidationException("容量不能小于已预约人数");
            }
            entity.setCapacity(request.getCapacity());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        return toResponse(scheduleRepository.save(entity));
    }

    public ScheduleDtos.Response toResponse(DoctorScheduleEntity entity) {
        ScheduleDtos.Response response = new ScheduleDtos.Response();
        response.setId(entity.getId());
        response.setDoctorId(entity.getDoctor().getId());
        response.setDate(entity.getScheduleDate());
        response.setTimeSlot(entity.getTimeSlot());
        response.setCapacity(entity.getCapacity());
        response.setBookedCount(entity.getBookedCount());
        response.setStatus(entity.getStatus());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    private void validateTimeSlots(List<String> timeSlots) {
        if (timeSlots == null || timeSlots.isEmpty()) {
            throw new ValidationException("时间段不能为空");
        }
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
