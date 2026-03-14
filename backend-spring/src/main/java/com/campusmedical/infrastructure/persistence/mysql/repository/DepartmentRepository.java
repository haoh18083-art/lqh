package com.campusmedical.infrastructure.persistence.mysql.repository;

import com.campusmedical.infrastructure.persistence.mysql.entity.DepartmentEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Long>, JpaSpecificationExecutor<DepartmentEntity> {

    boolean existsByName(String name);

    Optional<DepartmentEntity> findByName(String name);
}
