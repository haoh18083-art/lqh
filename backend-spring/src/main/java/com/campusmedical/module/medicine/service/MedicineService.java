package com.campusmedical.module.medicine.service;

import com.campusmedical.common.api.PageResponse;
import com.campusmedical.common.exception.ConflictException;
import com.campusmedical.common.exception.NotFoundException;
import com.campusmedical.common.exception.ValidationException;
import com.campusmedical.infrastructure.persistence.mysql.entity.InventoryMovementEntity;
import com.campusmedical.infrastructure.persistence.mysql.entity.MedicineEntity;
import com.campusmedical.infrastructure.persistence.mysql.repository.InventoryMovementRepository;
import com.campusmedical.infrastructure.persistence.mysql.repository.MedicineRepository;
import com.campusmedical.module.medicine.dto.MedicineDtos;
import javax.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final InventoryMovementRepository inventoryMovementRepository;

    public MedicineService(
        MedicineRepository medicineRepository,
        InventoryMovementRepository inventoryMovementRepository
    ) {
        this.medicineRepository = medicineRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<MedicineDtos.Response> list(String search, Boolean isActive, Integer page, Integer pageSize) {
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        Pageable pageable = PageRequest.of(
            normalizedPage - 1,
            normalizedPageSize,
            Sort.by(Sort.Order.desc("id"))
        );

        Specification<MedicineEntity> specification = (root, query, builder) -> {
            Predicate predicate = builder.conjunction();
            if (search != null && !search.trim().isEmpty()) {
                predicate = builder.and(predicate, builder.like(root.get("name"), "%" + search.trim() + "%"));
            }
            if (isActive != null) {
                predicate = builder.and(predicate, builder.equal(root.get("isActive"), isActive));
            }
            return predicate;
        };

        Page<MedicineEntity> result = medicineRepository.findAll(specification, pageable);
        return new PageResponse<MedicineDtos.Response>(
            result.getContent().stream().map(this::toResponse).collect(java.util.stream.Collectors.toList()),
            result.getTotalElements(),
            normalizedPage,
            normalizedPageSize,
            result.getTotalPages()
        );
    }

    @Transactional
    public MedicineDtos.Response create(MedicineDtos.CreateRequest request) {
        if (medicineRepository.existsByName(request.getName().trim())) {
            throw new ConflictException("药品名称已存在");
        }

        MedicineEntity entity = new MedicineEntity();
        entity.setName(request.getName().trim());
        entity.setSpecification(trimToNull(request.getSpec()));
        entity.setUnit(trimToNull(request.getUnit()));
        entity.setStock(request.getStock());
        entity.setIsActive(request.getIsActive());
        entity.setPrice(request.getPrice());
        if (entity.getCode() == null) {
            entity.setCode(null);
        }
        if (entity.getCategory() == null) {
            entity.setCategory("其他");
        }
        if (entity.getThreshold() == null) {
            entity.setThreshold(0);
        }
        return toResponse(medicineRepository.save(entity));
    }

    @Transactional
    public MedicineDtos.Response update(Long medicineId, MedicineDtos.UpdateRequest request) {
        MedicineEntity entity = findEntity(medicineId);
        if (request.getName() != null) {
            String newName = request.getName().trim();
            if (!newName.equals(entity.getName()) && medicineRepository.existsByName(newName)) {
                throw new ConflictException("药品名称已存在");
            }
            entity.setName(newName);
        }
        if (request.getSpec() != null) {
            entity.setSpecification(trimToNull(request.getSpec()));
        }
        if (request.getUnit() != null) {
            entity.setUnit(trimToNull(request.getUnit()));
        }
        if (request.getStock() != null) {
            entity.setStock(request.getStock());
        }
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
        if (request.getPrice() != null) {
            entity.setPrice(request.getPrice());
        }
        return toResponse(medicineRepository.save(entity));
    }

    @Transactional
    public MedicineDtos.Response updateStock(Long medicineId, Integer delta, String reason) {
        MedicineEntity entity = findEntity(medicineId);
        int newStock = entity.getStock().intValue() + delta.intValue();
        if (newStock < 0) {
            throw new ValidationException("库存不足");
        }
        entity.setStock(Integer.valueOf(newStock));

        InventoryMovementEntity movement = new InventoryMovementEntity();
        movement.setMedicine(entity);
        movement.setDelta(delta);
        movement.setReason(reason == null || reason.trim().isEmpty() ? "manual_adjust" : reason.trim());
        movement.setRefType("manual");
        movement.setRefId(null);
        inventoryMovementRepository.save(movement);

        return toResponse(medicineRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public MedicineEntity findEntity(Long medicineId) {
        return medicineRepository.findById(medicineId).orElseThrow(() -> new NotFoundException("药品不存在"));
    }

    public MedicineDtos.Response toResponse(MedicineEntity entity) {
        MedicineDtos.Response response = new MedicineDtos.Response();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setSpec(entity.getSpecification());
        response.setUnit(entity.getUnit());
        response.setStock(entity.getStock());
        response.setIsActive(entity.getIsActive());
        response.setPrice(entity.getPrice());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1 || pageSize > 100) {
            return 20;
        }
        return pageSize;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
