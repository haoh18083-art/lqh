package com.campusmedical.infrastructure.persistence.mysql.repository;

import com.campusmedical.infrastructure.persistence.mysql.entity.PrescriptionItemEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItemEntity, Long> {

    List<PrescriptionItemEntity> findByPrescription_IdOrderByIdAsc(Long prescriptionId);
}
