package com.campusmedical.infrastructure.persistence.mysql.repository;

import com.campusmedical.infrastructure.persistence.mysql.entity.DoctorScheduleEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DoctorScheduleRepository extends JpaRepository<DoctorScheduleEntity, Long>, JpaSpecificationExecutor<DoctorScheduleEntity> {

    Optional<DoctorScheduleEntity> findByDoctor_IdAndScheduleDateAndTimeSlot(Long doctorId, LocalDate scheduleDate, String timeSlot);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "select schedule from DoctorScheduleEntity schedule " +
        "where schedule.doctor.id = :doctorId and schedule.scheduleDate = :scheduleDate and schedule.timeSlot = :timeSlot"
    )
    Optional<DoctorScheduleEntity> findForUpdate(
        @Param("doctorId") Long doctorId,
        @Param("scheduleDate") LocalDate scheduleDate,
        @Param("timeSlot") String timeSlot
    );

    List<DoctorScheduleEntity> findByDoctor_IdAndScheduleDateBetweenOrderByScheduleDateDescTimeSlotDesc(
        Long doctorId,
        LocalDate dateFrom,
        LocalDate dateTo
    );
}
