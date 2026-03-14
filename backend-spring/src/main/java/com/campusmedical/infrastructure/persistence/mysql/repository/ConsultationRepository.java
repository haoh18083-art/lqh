package com.campusmedical.infrastructure.persistence.mysql.repository;

import com.campusmedical.infrastructure.persistence.mysql.entity.ConsultationEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationRepository extends JpaRepository<ConsultationEntity, Long> {

    Optional<ConsultationEntity> findByAppointment_Id(Long appointmentId);
}
