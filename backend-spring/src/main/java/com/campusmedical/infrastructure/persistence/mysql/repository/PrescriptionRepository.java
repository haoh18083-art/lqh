package com.campusmedical.infrastructure.persistence.mysql.repository;

import com.campusmedical.infrastructure.persistence.mysql.entity.PrescriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionRepository extends JpaRepository<PrescriptionEntity, Long> {
}
