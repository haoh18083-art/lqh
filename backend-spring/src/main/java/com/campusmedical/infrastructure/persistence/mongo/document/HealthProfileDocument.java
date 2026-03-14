package com.campusmedical.infrastructure.persistence.mongo.document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@Document(collection = "health_profiles")
public class HealthProfileDocument {

    @Id
    private String id;

    @Field("user_id")
    private Long userId;

    @Field("blood_type")
    private String bloodType;

    @Field("last_checkup_date")
    private LocalDateTime lastCheckupDate;

    @Field("allergies")
    private List<String> allergies = new ArrayList<String>();

    @Field("medical_history")
    private List<MedicalHistoryItem> medicalHistory = new ArrayList<MedicalHistoryItem>();

    @Field("emergency_contact")
    private EmergencyContact emergencyContact;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    public static class EmergencyContact {

        private String name;
        private String phone;
        private String relationship;
    }

    @Getter
    @Setter
    public static class MedicalHistoryItem {

        private String condition;
        private LocalDateTime date;
        private String notes;
    }
}
