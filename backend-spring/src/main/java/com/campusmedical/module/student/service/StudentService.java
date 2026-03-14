package com.campusmedical.module.student.service;

import com.campusmedical.common.api.PageResponse;
import com.campusmedical.common.exception.ConflictException;
import com.campusmedical.common.exception.NotFoundException;
import com.campusmedical.infrastructure.persistence.mysql.entity.StudentEntity;
import com.campusmedical.infrastructure.persistence.mysql.entity.UserEntity;
import com.campusmedical.infrastructure.persistence.mysql.repository.StudentRepository;
import com.campusmedical.infrastructure.persistence.mysql.repository.UserRepository;
import com.campusmedical.module.student.dto.StudentDtos;
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
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentService(
        StudentRepository studentRepository,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentDtos.Response> list(
        Integer page,
        Integer pageSize,
        String search,
        String major,
        String grade,
        String healthStatus
    ) {
        Pageable pageable = PageRequest.of(
            normalizePage(page) - 1,
            normalizePageSize(pageSize),
            Sort.by(Sort.Order.desc("id"))
        );

        Specification<StudentEntity> specification = (root, query, builder) -> {
            query.distinct(true);
            Join<StudentEntity, UserEntity> userJoin = root.join("user", JoinType.INNER);
            Predicate predicate = builder.conjunction();
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim() + "%";
                predicate = builder.and(
                    predicate,
                    builder.or(
                        builder.like(userJoin.get("fullName"), pattern),
                        builder.like(root.get("studentId"), pattern)
                    )
                );
            }
            if (major != null && !major.trim().isEmpty()) {
                predicate = builder.and(predicate, builder.equal(root.get("major"), major.trim()));
            }
            if (grade != null && !grade.trim().isEmpty()) {
                predicate = builder.and(predicate, builder.equal(root.get("grade"), grade.trim()));
            }
            if (healthStatus != null && !healthStatus.trim().isEmpty()) {
                predicate = builder.and(predicate, builder.equal(root.get("healthStatus"), healthStatus.trim()));
            }
            return predicate;
        };

        Page<StudentEntity> result = studentRepository.findAll(specification, pageable);
        return new PageResponse<StudentDtos.Response>(
            result.getContent().stream().map(this::toResponse).collect(java.util.stream.Collectors.toList()),
            result.getTotalElements(),
            normalizePage(page),
            normalizePageSize(pageSize),
            result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public StudentDtos.Response getById(Long studentId) {
        return toResponse(findEntity(studentId));
    }

    @Transactional(readOnly = true)
    public StudentEntity findEntity(Long studentId) {
        return studentRepository.findById(studentId)
            .orElseThrow(() -> new NotFoundException("学生不存在"));
    }

    @Transactional(readOnly = true)
    public StudentEntity findByUserId(Long userId) {
        return studentRepository.findByUser_Id(userId)
            .orElseThrow(() -> new NotFoundException("学生不存在"));
    }

    @Transactional
    public StudentDtos.Response create(StudentDtos.CreateRequest request) {
        if (studentRepository.existsByStudentId(request.getStudentId())) {
            throw new ConflictException("学号已存在");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("用户名已存在");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("邮箱已存在");
        }

        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setHashedPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setRole("student");
        user.setIsActive(Boolean.TRUE);
        user = userRepository.save(user);

        StudentEntity entity = new StudentEntity();
        entity.setUser(user);
        entity.setStudentId(request.getStudentId());
        entity.setMajor(request.getMajor());
        entity.setGrade(request.getGrade());
        entity.setClassName(request.getClassName());
        entity.setGender(request.getGender());
        entity.setDob(request.getDob());
        entity.setHealthStatus("良好");
        return toResponse(studentRepository.save(entity));
    }

    @Transactional
    public StudentDtos.Response update(Long studentId, StudentDtos.UpdateRequest request) {
        StudentEntity entity = findEntity(studentId);
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
        if (request.getMajor() != null) {
            entity.setMajor(request.getMajor());
        }
        if (request.getGrade() != null) {
            entity.setGrade(request.getGrade());
        }
        if (request.getClassName() != null) {
            entity.setClassName(request.getClassName());
        }
        if (request.getHealthStatus() != null) {
            entity.setHealthStatus(request.getHealthStatus());
        }
        if (request.getGender() != null) {
            entity.setGender(request.getGender());
        }
        if (request.getDob() != null) {
            entity.setDob(request.getDob());
        }

        userRepository.save(user);
        return toResponse(studentRepository.save(entity));
    }

    @Transactional
    public StudentDtos.Response updateStatus(Long studentId, Boolean isActive) {
        StudentEntity entity = findEntity(studentId);
        entity.getUser().setIsActive(isActive);
        userRepository.save(entity.getUser());
        return toResponse(entity);
    }

    @Transactional
    public void delete(Long studentId) {
        StudentEntity entity = findEntity(studentId);
        studentRepository.delete(entity);
    }

    @Transactional
    public void updateHealthStatus(Long studentId, StudentDtos.HealthProfileResponse profile) {
        StudentEntity entity = findEntity(studentId);
        boolean hasIssues = (profile.getAllergies() != null && !profile.getAllergies().isEmpty())
            || (profile.getMedicalHistory() != null && !profile.getMedicalHistory().isEmpty());
        entity.setHealthStatus(hasIssues ? "异常" : "良好");
        studentRepository.save(entity);
    }

    public StudentDtos.Response toResponse(StudentEntity entity) {
        StudentDtos.Response response = new StudentDtos.Response();
        response.setId(entity.getId());
        response.setUserId(entity.getUser().getId());
        response.setStudentId(entity.getStudentId());
        response.setUsername(entity.getUser().getUsername());
        response.setEmail(entity.getUser().getEmail());
        response.setFullName(entity.getUser().getFullName());
        response.setMajor(entity.getMajor());
        response.setGrade(entity.getGrade());
        response.setClassName(entity.getClassName());
        response.setGender(entity.getGender());
        response.setDob(entity.getDob());
        response.setHealthStatus(entity.getHealthStatus());
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
