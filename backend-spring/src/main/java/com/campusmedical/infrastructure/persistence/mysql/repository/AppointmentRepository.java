package com.campusmedical.infrastructure.persistence.mysql.repository;

import com.campusmedical.infrastructure.persistence.mysql.entity.AppointmentEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AppointmentRepository extends JpaRepository<AppointmentEntity, Long>, JpaSpecificationExecutor<AppointmentEntity> {

    long countByDoctor_IdAndVisitDateAndTimeSlotAndStatusIn(Long doctorId, LocalDate visitDate, String timeSlot, List<String> statuses);

    Optional<AppointmentEntity> findFirstByStudent_IdAndVisitDateAndTimeSlotAndStatusIn(
        Long studentId,
        LocalDate visitDate,
        String timeSlot,
        List<String> statuses
    );

    List<AppointmentEntity> findByStudent_IdAndDoctor_IdAndVisitDateBetweenAndStatusIn(
        Long studentId,
        Long doctorId,
        LocalDate dateFrom,
        LocalDate dateTo,
        List<String> statuses
    );

    long countByStudent_IdAndVisitDateBetweenAndStatusIn(
        Long studentId,
        LocalDate dateFrom,
        LocalDate dateTo,
        List<String> statuses
    );
}
