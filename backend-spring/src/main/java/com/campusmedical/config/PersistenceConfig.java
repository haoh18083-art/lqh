package com.campusmedical.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EntityScan(basePackages = "com.campusmedical.infrastructure.persistence.mysql.entity")
@EnableJpaRepositories(basePackages = "com.campusmedical.infrastructure.persistence.mysql.repository")
@EnableMongoRepositories(basePackages = "com.campusmedical.infrastructure.persistence.mongo.repository")
public class PersistenceConfig {
}
