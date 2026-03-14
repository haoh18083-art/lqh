package com.campusmedical.infrastructure.persistence.mysql.repository;

import com.campusmedical.infrastructure.persistence.mysql.entity.DocumentEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

    List<DocumentEntity> findByAppointment_IdOrderByCreatedAtDesc(Long appointmentId);

    Optional<DocumentEntity> findFirstByAppointment_IdAndDocTypeOrderByCreatedAtDesc(Long appointmentId, String docType);
}
