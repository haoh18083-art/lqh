package com.campusmedical.infrastructure.persistence.mysql.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@Entity
@Table(name = "medicines")
public class MedicineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "spec", length = 100)
    private String specification;

    @Column(length = 20)
    private String unit;

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 50)
    private String code;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false)
    private Integer threshold;

    @Column(length = 100)
    private String supplier;

    @Column(length = 200)
    private String manufacturer;

    @Column(name = "approval_number", length = 100)
    private String approvalNumber;
}
