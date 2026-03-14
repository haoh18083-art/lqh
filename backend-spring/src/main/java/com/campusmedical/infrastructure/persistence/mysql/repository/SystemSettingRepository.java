package com.campusmedical.infrastructure.persistence.mysql.repository;

import com.campusmedical.infrastructure.persistence.mysql.entity.SystemSettingEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingRepository extends JpaRepository<SystemSettingEntity, Long> {

    Optional<SystemSettingEntity> findByCategoryAndSettingKey(String category, String settingKey);
}
