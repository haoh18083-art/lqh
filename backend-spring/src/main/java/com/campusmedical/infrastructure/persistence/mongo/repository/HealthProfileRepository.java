package com.campusmedical.infrastructure.persistence.mongo.repository;

import com.campusmedical.infrastructure.persistence.mongo.document.HealthProfileDocument;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HealthProfileRepository extends MongoRepository<HealthProfileDocument, String> {

    Optional<HealthProfileDocument> findByUserId(Long userId);
}
