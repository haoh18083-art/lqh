package com.campusmedical.module.audit.service;

import com.campusmedical.common.util.ClientRequestUtil;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private static final String COLLECTION = "audit_logs";

    private final MongoTemplate mongoTemplate;

    public AuditService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void logAuthEvent(Long userId, String action, Map<String, Object> details, HttpServletRequest request) {
        log("auth", userId, action, details, request);
    }

    public void logAdminEvent(Long userId, String action, Map<String, Object> details, HttpServletRequest request) {
        log("admin", userId, action, details, request);
    }

    private void log(
        String resourceType,
        Long userId,
        String action,
        Map<String, Object> details,
        HttpServletRequest request
    ) {
        try {
            Document document = new Document();
            document.put("user_id", userId == null ? Long.valueOf(0L) : userId);
            document.put("action", action);
            document.put("resource_type", resourceType);
            document.put("details", details == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(details));
            document.put("ip_address", ClientRequestUtil.extractClientIp(request));
            document.put("user_agent", ClientRequestUtil.extractUserAgent(request));
            document.put("created_at", Date.from(Instant.now()));
            mongoTemplate.insert(document, COLLECTION);
        } catch (Exception ignored) {
            // 审计写入失败不影响主业务流程
        }
    }
}
