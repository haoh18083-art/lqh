package com.campusmedical.module.department.service;

import com.campusmedical.common.api.PageResponse;
import com.campusmedical.common.exception.ConflictException;
import com.campusmedical.common.exception.NotFoundException;
import com.campusmedical.infrastructure.persistence.mysql.entity.DepartmentEntity;
import com.campusmedical.infrastructure.persistence.mysql.repository.DepartmentRepository;
import com.campusmedical.infrastructure.persistence.mysql.repository.DoctorRepository;
import com.campusmedical.module.department.dto.DepartmentDtos;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;

    public DepartmentService(DepartmentRepository departmentRepository, DoctorRepository doctorRepository) {
        this.departmentRepository = departmentRepository;
        this.doctorRepository = doctorRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<DepartmentDtos.Response> list(Integer page, Integer pageSize, Boolean isActive, String search) {
        Pageable pageable = PageRequest.of(
            normalizePage(page) - 1,
            normalizePageSize(pageSize),
            Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"))
        );

        Specification<DepartmentEntity> specification = (root, query, builder) -> {
            Predicate predicate = builder.conjunction();
            if (isActive != null) {
                predicate = builder.and(predicate, builder.equal(root.get("isActive"), isActive));
            }
            if (search != null && !search.trim().isEmpty()) {
                predicate = builder.and(predicate, builder.like(root.get("name"), "%" + search.trim() + "%"));
            }
            return predicate;
        };

        Page<DepartmentEntity> result = departmentRepository.findAll(specification, pageable);
        return new PageResponse<DepartmentDtos.Response>(
            result.getContent().stream().map(this::toResponse).collect(Collectors.toList()),
            result.getTotalElements(),
            normalizePage(page),
            normalizePageSize(pageSize),
            result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public List<DepartmentDtos.Response> listActive(Integer page, Integer pageSize) {
        return list(page, pageSize, Boolean.TRUE, null).getItems();
    }

    @Transactional(readOnly = true)
    public DepartmentDtos.Response getById(Long departmentId) {
        return toResponse(findEntity(departmentId));
    }

    @Transactional
    public DepartmentDtos.Response create(DepartmentDtos.CreateRequest request) {
        if (departmentRepository.existsByName(request.getName())) {
            throw new ConflictException("科室名称 '" + request.getName() + "' 已存在");
        }

        DepartmentEntity entity = new DepartmentEntity();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setIsActive(Boolean.TRUE);
        return toResponse(departmentRepository.save(entity));
    }

    @Transactional
    public DepartmentDtos.Response update(Long departmentId, DepartmentDtos.UpdateRequest request) {
        DepartmentEntity entity = findEntity(departmentId);
        if (request.getName() != null && !request.getName().equals(entity.getName()) && departmentRepository.existsByName(request.getName())) {
            throw new ConflictException("科室名称已存在");
        }

        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
        if (request.getSortOrder() != null) {
            entity.setSortOrder(request.getSortOrder());
        }
        return toResponse(departmentRepository.save(entity));
    }

    @Transactional
    public DepartmentDtos.Response updateStatus(Long departmentId, Boolean isActive) {
        DepartmentEntity entity = findEntity(departmentId);
        entity.setIsActive(isActive);
        return toResponse(departmentRepository.save(entity));
    }

    @Transactional
    public void delete(Long departmentId) {
        DepartmentEntity entity = findEntity(departmentId);
        long doctorCount = doctorRepository.countByDepartment(entity.getName());
        if (doctorCount > 0) {
            throw new ConflictException("无法删除科室 '" + entity.getName() + "'，该科室下还有 " + doctorCount + " 名医生");
        }
        departmentRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public DepartmentEntity findEntity(Long departmentId) {
        return departmentRepository.findById(departmentId)
            .orElseThrow(() -> new NotFoundException("科室不存在"));
    }

    public DepartmentDtos.Response toResponse(DepartmentEntity entity) {
        DepartmentDtos.Response response = new DepartmentDtos.Response();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setIsActive(entity.getIsActive());
        response.setSortOrder(entity.getSortOrder());
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
}
