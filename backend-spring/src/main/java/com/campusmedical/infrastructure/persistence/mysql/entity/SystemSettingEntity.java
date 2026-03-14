package com.campusmedical.infrastructure.persistence.mysql.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@Entity
@Table(
    name = "system_settings",
    uniqueConstraints = @UniqueConstraint(name = "uk_system_settings_category_key", columnNames = {"category", "setting_key"})
)
public class SystemSettingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "setting_key", nullable = false, length = 100)
    private String settingKey;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "api_key_encrypted", columnDefinition = "text")
    private String apiKeyEncrypted;

    @Column(name = "api_key_masked", length = 255)
    private String apiKeyMasked;

    @Column(name = "is_configured", nullable = false)
    private Boolean isConfigured;

    @Column(name = "last_test_status", nullable = false, length = 20)
    private String lastTestStatus;

    @Column(name = "last_test_message", length = 500)
    private String lastTestMessage;

    @Column(name = "last_tested_at")
    private LocalDateTime lastTestedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
