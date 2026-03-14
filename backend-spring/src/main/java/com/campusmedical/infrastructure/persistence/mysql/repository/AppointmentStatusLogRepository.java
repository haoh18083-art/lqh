package com.campusmedical.infrastructure.persistence.mysql.repository;

import com.campusmedical.infrastructure.persistence.mysql.entity.AppointmentStatusLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentStatusLogRepository extends JpaRepository<AppointmentStatusLogEntity, Long> {
}
