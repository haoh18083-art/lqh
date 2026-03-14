package com.campusmedical.module.doctor.service;

import com.campusmedical.common.api.PageResponse;
import com.campusmedical.common.exception.ConflictException;
import com.campusmedical.common.exception.NotFoundException;
import com.campusmedical.infrastructure.persistence.mysql.entity.DepartmentEntity;
import com.campusmedical.infrastructure.persistence.mysql.entity.DoctorEntity;
import com.campusmedical.infrastructure.persistence.mysql.entity.UserEntity;
import com.campusmedical.infrastructure.persistence.mysql.repository.DoctorRepository;
import com.campusmedical.infrastructure.persistence.mysql.repository.UserRepository;
import com.campusmedical.module.department.service.DepartmentService;
import com.campusmedical.module.doctor.dto.DoctorDtos;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final DepartmentService departmentService;
    private final PasswordEncoder passwordEncoder;

    public DoctorService(
        DoctorRepository doctorRepository,
        UserRepository userRepository,
        DepartmentService departmentService,
        PasswordEncoder passwordEncoder
    ) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.departmentService = departmentService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<DoctorDtos.Response> list(Integer page, Integer pageSize, String search, String department, Long departmentId) {
        Pageable pageable = PageRequest.of(
            normalizePage(page) - 1,
            normalizePageSize(pageSize),
            Sort.by(Sort.Order.desc("id"))
        );

        Specification<DoctorEntity> specification = (root, query, builder) -> {
            query.distinct(true);
            Join<DoctorEntity, UserEntity> userJoin = root.join("user", JoinType.INNER);
            Predicate predicate = builder.conjunction();
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim() + "%";
                predicate = builder.and(
                    predicate,
                    builder.or(
                        builder.like(userJoin.get("fullName"), pattern),
                        builder.like(root.get("doctorId"), pattern)
                    )
                );
            }
            if (departmentId != null) {
                predicate = builder.and(predicate, builder.equal(root.join("departmentRel", JoinType.LEFT).get("id"), departmentId));
            } else if (department != null && !department.trim().isEmpty()) {
                predicate = builder.and(predicate, builder.equal(root.join("departmentRel", JoinType.LEFT).get("name"), department.trim()));
            }
            return predicate;
        };

        Page<DoctorEntity> result = doctorRepository.findAll(specification, pageable);
        return new PageResponse<DoctorDtos.Response>(
            result.getContent().stream().map(this::toResponse).collect(Collectors.toList()),
            result.getTotalElements(),
            normalizePage(page),
            normalizePageSize(pageSize),
            result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<DoctorDtos.Response> listActiveByDepartment(Long departmentId, Integer page, Integer pageSize) {
        Pageable pageable = PageRequest.of(
            normalizePage(page) - 1,
            normalizePageSize(pageSize),
            Sort.by(Sort.Order.desc("id"))
        );
        Specification<DoctorEntity> specification = (root, query, builder) -> {
            query.distinct(true);
            Join<DoctorEntity, UserEntity> userJoin = root.join("user", JoinType.INNER);
            Predicate predicate = builder.equal(root.join("departmentRel", JoinType.LEFT).get("id"), departmentId);
            predicate = builder.and(predicate, builder.isTrue(userJoin.get("isActive")));
            return predicate;
        };

        Page<DoctorEntity> result = doctorRepository.findAll(specification, pageable);
        return new PageResponse<DoctorDtos.Response>(
            result.getContent().stream().map(this::toResponse).collect(Collectors.toList()),
            result.getTotalElements(),
            normalizePage(page),
            normalizePageSize(pageSize),
            result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public DoctorDtos.Response getById(Long doctorId) {
        return toResponse(findEntity(doctorId));
    }

    @Transactional(readOnly = true)
    public DoctorEntity findEntity(Long doctorId) {
        return doctorRepository.findById(doctorId)
            .orElseThrow(() -> new NotFoundException("医生不存在"));
    }

    @Transactional(readOnly = true)
    public DoctorEntity findByUserId(Long userId) {
        return doctorRepository.findByUser_Id(userId)
            .orElseThrow(() -> new NotFoundException("医生不存在"));
    }

    @Transactional
    public DoctorDtos.Response create(DoctorDtos.CreateRequest request) {
        if (doctorRepository.existsByDoctorId(request.getDoctorId())) {
            throw new ConflictException("医生工号已存在");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("用户名已存在");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("邮箱已存在");
        }

        DepartmentEntity department = departmentService.findEntity(request.getDepartmentId());

        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setHashedPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setRole("doctor");
        user.setIsActive(Boolean.TRUE);
        user = userRepository.save(user);

        DoctorEntity entity = new DoctorEntity();
        entity.setUser(user);
        entity.setDoctorId(request.getDoctorId());
        entity.setDepartmentRel(department);
        entity.setDepartment(department.getName());
        entity.setTitle(request.getTitle());
        entity.setIntroduction(request.getIntroduction() == null ? "" : request.getIntroduction().trim());
        return toResponse(doctorRepository.save(entity));
    }

    @Transactional
    public DoctorDtos.Response update(Long doctorId, DoctorDtos.UpdateRequest request) {
        DoctorEntity entity = findEntity(doctorId);
        UserEntity user = entity.getUser();

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("邮箱已被使用");
        }

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        if (request.getDepartmentId() != null) {
            DepartmentEntity department = departmentService.findEntity(request.getDepartmentId());
            entity.setDepartmentRel(department);
            entity.setDepartment(department.getName());
        }
        if (request.getTitle() != null) {
            entity.setTitle(request.getTitle());
        }
        if (request.getIntroduction() != null) {
            entity.setIntroduction(request.getIntroduction().trim());
        }

        userRepository.save(user);
        return toResponse(doctorRepository.save(entity));
    }

    @Transactional
    public DoctorDtos.Response updateStatus(Long doctorId, Boolean isActive) {
        DoctorEntity entity = findEntity(doctorId);
        entity.getUser().setIsActive(isActive);
        userRepository.save(entity.getUser());
        return toResponse(entity);
    }

    @Transactional
    public void delete(Long doctorId) {
        DoctorEntity entity = findEntity(doctorId);
        doctorRepository.delete(entity);
    }

    public DoctorDtos.Response toResponse(DoctorEntity entity) {
        DoctorDtos.Response response = new DoctorDtos.Response();
        response.setId(entity.getId());
        response.setUserId(entity.getUser().getId());
        response.setDoctorId(entity.getDoctorId());
        response.setUsername(entity.getUser().getUsername());
        response.setEmail(entity.getUser().getEmail());
        response.setFullName(entity.getUser().getFullName());
        response.setDepartmentId(entity.getDepartmentRel() != null ? entity.getDepartmentRel().getId() : null);
        response.setDepartment(entity.getDepartmentRel() != null ? entity.getDepartmentRel().getName() : entity.getDepartment());
        response.setTitle(entity.getTitle());
        response.setIntroduction(entity.getIntroduction());
        response.setPhone(entity.getUser().getPhone());
        response.setIsActive(entity.getUser().getIsActive());
        response.setCreatedAt(entity.getUser().getCreatedAt());
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
