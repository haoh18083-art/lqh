package com.campusmedical.module.student.service;

import com.campusmedical.infrastructure.persistence.mongo.document.HealthProfileDocument;
import com.campusmedical.infrastructure.persistence.mongo.repository.HealthProfileRepository;
import com.campusmedical.module.student.dto.StudentDtos;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HealthProfileService {

    private final HealthProfileRepository healthProfileRepository;

    public HealthProfileService(HealthProfileRepository healthProfileRepository) {
        this.healthProfileRepository = healthProfileRepository;
    }

    @Transactional(readOnly = true)
    public StudentDtos.HealthProfileResponse getByUserId(Long userId) {
        return healthProfileRepository.findByUserId(userId)
            .map(this::toResponse)
            .orElseGet(() -> emptyProfile(userId));
    }

    @Transactional
    public StudentDtos.HealthProfileResponse createOrUpdate(Long userId, StudentDtos.HealthProfileUpdateRequest request) {
        HealthProfileDocument document = healthProfileRepository.findByUserId(userId).orElseGet(HealthProfileDocument::new);
        boolean isNew = document.getId() == null;
        if (isNew) {
            document.setUserId(userId);
            document.setCreatedAt(LocalDateTime.now());
        }

        if (request.getBloodType() != null) {
            document.setBloodType(request.getBloodType());
        }
        if (request.getLastCheckupDate() != null) {
            document.setLastCheckupDate(request.getLastCheckupDate());
        }
        if (request.getAllergies() != null) {
            document.setAllergies(new ArrayList<String>(request.getAllergies()));
        }
        if (request.getMedicalHistory() != null) {
            document.setMedicalHistory(toMedicalHistory(request.getMedicalHistory()));
        }
        if (request.getEmergencyContact() != null) {
            HealthProfileDocument.EmergencyContact contact = new HealthProfileDocument.EmergencyContact();
            contact.setName(request.getEmergencyContact().getName());
            contact.setPhone(request.getEmergencyContact().getPhone());
            contact.setRelationship(request.getEmergencyContact().getRelationship());
            document.setEmergencyContact(contact);
        }

        document.setUpdatedAt(LocalDateTime.now());
        return toResponse(healthProfileRepository.save(document));
    }

    private List<HealthProfileDocument.MedicalHistoryItem> toMedicalHistory(List<StudentDtos.MedicalHistoryRecord> items) {
        List<HealthProfileDocument.MedicalHistoryItem> results = new ArrayList<HealthProfileDocument.MedicalHistoryItem>();
        for (StudentDtos.MedicalHistoryRecord item : items) {
            HealthProfileDocument.MedicalHistoryItem target = new HealthProfileDocument.MedicalHistoryItem();
            target.setCondition(item.getCondition());
            target.setDate(item.getDate());
            target.setNotes(item.getNotes());
            results.add(target);
        }
        return results;
    }

    private StudentDtos.HealthProfileResponse emptyProfile(Long userId) {
        StudentDtos.HealthProfileResponse response = new StudentDtos.HealthProfileResponse();
        response.setUserId(userId);
        response.setAllergies(new ArrayList<String>());
        response.setMedicalHistory(new ArrayList<StudentDtos.MedicalHistoryRecord>());
        return response;
    }

    private StudentDtos.HealthProfileResponse toResponse(HealthProfileDocument document) {
        StudentDtos.HealthProfileResponse response = new StudentDtos.HealthProfileResponse();
        response.setUserId(document.getUserId());
        response.setBloodType(document.getBloodType());
        response.setLastCheckupDate(document.getLastCheckupDate());
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
        response.setAllergies(document.getAllergies() == null ? new ArrayList<String>() : new ArrayList<String>(document.getAllergies()));
        response.setMedicalHistory(new ArrayList<StudentDtos.MedicalHistoryRecord>());
        if (document.getMedicalHistory() != null) {
            for (HealthProfileDocument.MedicalHistoryItem item : document.getMedicalHistory()) {
                StudentDtos.MedicalHistoryRecord record = new StudentDtos.MedicalHistoryRecord();
                record.setCondition(item.getCondition());
                record.setDate(item.getDate());
                record.setNotes(item.getNotes());
                response.getMedicalHistory().add(record);
            }
        }
        if (document.getEmergencyContact() != null) {
            StudentDtos.EmergencyContact contact = new StudentDtos.EmergencyContact();
            contact.setName(document.getEmergencyContact().getName());
            contact.setPhone(document.getEmergencyContact().getPhone());
            contact.setRelationship(document.getEmergencyContact().getRelationship());
            response.setEmergencyContact(contact);
        }
        return response;
    }
}
