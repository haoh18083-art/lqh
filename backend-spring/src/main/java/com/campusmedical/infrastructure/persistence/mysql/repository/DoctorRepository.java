package com.campusmedical.infrastructure.persistence.mysql.repository;

import com.campusmedical.infrastructure.persistence.mysql.entity.DoctorEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DoctorRepository extends JpaRepository<DoctorEntity, Long>, JpaSpecificationExecutor<DoctorEntity> {

    boolean existsByDoctorId(String doctorId);

    Optional<DoctorEntity> findByUser_Id(Long userId);

    long countByDepartment(String department);
}
