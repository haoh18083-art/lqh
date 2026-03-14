package com.campusmedical.infrastructure.persistence.mysql.repository;

import com.campusmedical.infrastructure.persistence.mysql.entity.MedicineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MedicineRepository extends JpaRepository<MedicineEntity, Long>, JpaSpecificationExecutor<MedicineEntity> {

    boolean existsByName(String name);
}
