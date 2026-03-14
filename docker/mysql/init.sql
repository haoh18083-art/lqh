SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `ai_chat_messages_meta`;
DROP TABLE IF EXISTS `ai_chat_sessions`;
DROP TABLE IF EXISTS `medicine_order_items`;
DROP TABLE IF EXISTS `medicine_orders`;
DROP TABLE IF EXISTS `inventory_movements`;
DROP TABLE IF EXISTS `documents`;
DROP TABLE IF EXISTS `prescription_items`;
DROP TABLE IF EXISTS `prescriptions`;
DROP TABLE IF EXISTS `consultations`;
DROP TABLE IF EXISTS `appointment_status_logs`;
DROP TABLE IF EXISTS `appointments`;
DROP TABLE IF EXISTS `doctor_schedules`;
DROP TABLE IF EXISTS `medicines`;
DROP TABLE IF EXISTS `students`;
DROP TABLE IF EXISTS `doctors`;
DROP TABLE IF EXISTS `system_settings`;
DROP TABLE IF EXISTS `departments`;
DROP TABLE IF EXISTS `users`;

CREATE TABLE `users` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `email` VARCHAR(100) NOT NULL,
  `hashed_password` VARCHAR(255) NOT NULL,
  `full_name` VARCHAR(100) DEFAULT NULL,
  `phone` VARCHAR(20) DEFAULT NULL,
  `student_id` VARCHAR(20) DEFAULT NULL,
  `role` VARCHAR(20) NOT NULL DEFAULT 'student',
  `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_student_id` (`student_id`),
  KEY `idx_role` (`role`),
  KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `departments` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `description` TEXT DEFAULT NULL,
  `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
  `sort_order` INT UNSIGNED NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `doctors` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `department_id` BIGINT UNSIGNED DEFAULT NULL,
  `doctor_id` VARCHAR(20) NOT NULL,
  `department` VARCHAR(50) NOT NULL,
  `title` VARCHAR(50) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `introduction` VARCHAR(500) NOT NULL DEFAULT '暂无简介',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doctor_id` (`doctor_id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_department` (`department`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_department_id` (`department_id`),
  CONSTRAINT `fk_doctors_department` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_doctors_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `students` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `student_id` VARCHAR(20) NOT NULL,
  `major` VARCHAR(100) NOT NULL,
  `grade` VARCHAR(20) NOT NULL,
  `class_name` VARCHAR(50) DEFAULT NULL,
  `health_status` ENUM('良好', '异常') NOT NULL DEFAULT '良好',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `gender` ENUM('男', '女', '未知') DEFAULT NULL,
  `dob` DATE DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_id` (`student_id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_major` (`major`),
  KEY `idx_grade` (`grade`),
  KEY `idx_health_status` (`health_status`),
  CONSTRAINT `fk_students_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `medicines` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `spec` VARCHAR(100) DEFAULT NULL,
  `unit` VARCHAR(20) DEFAULT NULL,
  `stock` INT NOT NULL,
  `is_active` BOOLEAN NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `code` VARCHAR(50) DEFAULT NULL,
  `category` VARCHAR(50) NOT NULL DEFAULT '其他',
  `threshold` INT NOT NULL DEFAULT 0,
  `supplier` VARCHAR(100) DEFAULT NULL,
  `manufacturer` VARCHAR(200) DEFAULT NULL,
  `approval_number` VARCHAR(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ix_medicines_name` (`name`),
  KEY `idx_medicines_code` (`code`),
  KEY `idx_medicines_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `doctor_schedules` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `doctor_id` BIGINT UNSIGNED NOT NULL,
  `date` DATE NOT NULL,
  `time_slot` VARCHAR(20) NOT NULL,
  `capacity` INT NOT NULL,
  `booked_count` INT NOT NULL,
  `status` ENUM('open', 'closed') NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_doctor_date_time` (`doctor_id`, `date`, `time_slot`),
  KEY `ix_doctor_schedules_doctor_id` (`doctor_id`),
  KEY `ix_doctor_schedules_date` (`date`),
  KEY `ix_doctor_schedules_time_slot` (`time_slot`),
  CONSTRAINT `doctor_schedules_ibfk_1` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `appointments` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT UNSIGNED NOT NULL,
  `doctor_id` BIGINT UNSIGNED NOT NULL,
  `department_id` BIGINT UNSIGNED NOT NULL,
  `date` DATE NOT NULL,
  `time_slot` VARCHAR(20) NOT NULL,
  `status` ENUM('pending', 'confirmed', 'in_progress', 'completed', 'cancelled', 'missed', 'rescheduled') NOT NULL,
  `symptoms` TEXT DEFAULT NULL,
  `queue_no` VARCHAR(10) DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  `confirmed_at` DATETIME DEFAULT NULL,
  `cancelled_at` DATETIME DEFAULT NULL,
  `completed_at` DATETIME DEFAULT NULL,
  `reschedule_reason` TEXT DEFAULT NULL,
  `cancellation_reason` TEXT DEFAULT NULL,
  `notes` TEXT DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_appointments_time_slot` (`time_slot`),
  KEY `ix_appointments_doctor_id` (`doctor_id`),
  KEY `ix_appointments_student_id` (`student_id`),
  KEY `ix_appointments_department_id` (`department_id`),
  KEY `ix_appointments_date` (`date`),
  KEY `ix_appointments_status` (`status`),
  CONSTRAINT `appointments_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE,
  CONSTRAINT `appointments_ibfk_2` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `appointments_ibfk_3` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `appointment_status_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `appointment_id` BIGINT UNSIGNED NOT NULL,
  `from_status` VARCHAR(20) DEFAULT NULL,
  `to_status` VARCHAR(20) NOT NULL,
  `reason` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_appointment_status_logs_appointment_id` (`appointment_id`),
  CONSTRAINT `appointment_status_logs_ibfk_1` FOREIGN KEY (`appointment_id`) REFERENCES `appointments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `consultations` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `appointment_id` BIGINT UNSIGNED NOT NULL,
  `doctor_id` BIGINT UNSIGNED NOT NULL,
  `student_id` BIGINT UNSIGNED NOT NULL,
  `category` VARCHAR(50) DEFAULT NULL,
  `signs` VARCHAR(255) DEFAULT NULL,
  `conclusion` TEXT NOT NULL,
  `advice` TEXT DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ix_consultations_appointment_id` (`appointment_id`),
  KEY `ix_consultations_student_id` (`student_id`),
  KEY `ix_consultations_doctor_id` (`doctor_id`),
  CONSTRAINT `consultations_ibfk_1` FOREIGN KEY (`appointment_id`) REFERENCES `appointments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `consultations_ibfk_2` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `consultations_ibfk_3` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `prescriptions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `consultation_id` BIGINT UNSIGNED NOT NULL,
  `note` TEXT DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ix_prescriptions_consultation_id` (`consultation_id`),
  CONSTRAINT `prescriptions_ibfk_1` FOREIGN KEY (`consultation_id`) REFERENCES `consultations` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `prescription_items` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `prescription_id` BIGINT UNSIGNED NOT NULL,
  `medicine_id` BIGINT UNSIGNED DEFAULT NULL,
  `name` VARCHAR(100) NOT NULL,
  `dosage` VARCHAR(100) DEFAULT NULL,
  `quantity` INT NOT NULL,
  `unit` VARCHAR(20) DEFAULT NULL,
  `unit_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `total_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  PRIMARY KEY (`id`),
  KEY `ix_prescription_items_medicine_id` (`medicine_id`),
  KEY `ix_prescription_items_prescription_id` (`prescription_id`),
  CONSTRAINT `prescription_items_ibfk_1` FOREIGN KEY (`prescription_id`) REFERENCES `prescriptions` (`id`) ON DELETE CASCADE,
  CONSTRAINT `prescription_items_ibfk_2` FOREIGN KEY (`medicine_id`) REFERENCES `medicines` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `documents` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `appointment_id` BIGINT UNSIGNED NOT NULL,
  `doc_type` VARCHAR(20) NOT NULL,
  `file_path` VARCHAR(255) NOT NULL,
  `file_name` VARCHAR(100) NOT NULL,
  `created_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_documents_appointment_id` (`appointment_id`),
  CONSTRAINT `documents_ibfk_1` FOREIGN KEY (`appointment_id`) REFERENCES `appointments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `inventory_movements` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `medicine_id` BIGINT UNSIGNED NOT NULL,
  `delta` INT NOT NULL,
  `reason` VARCHAR(255) DEFAULT NULL,
  `ref_type` VARCHAR(50) DEFAULT NULL,
  `ref_id` BIGINT UNSIGNED DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_inventory_movements_medicine_id` (`medicine_id`),
  CONSTRAINT `inventory_movements_ibfk_1` FOREIGN KEY (`medicine_id`) REFERENCES `medicines` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `medicine_orders` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(32) NOT NULL,
  `student_id` BIGINT UNSIGNED NOT NULL,
  `status` ENUM('completed', 'pending', 'cancelled') NOT NULL,
  `total_amount` DECIMAL(10,2) NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ix_medicine_orders_order_no` (`order_no`),
  KEY `ix_medicine_orders_status` (`status`),
  KEY `ix_medicine_orders_student_id` (`student_id`),
  CONSTRAINT `medicine_orders_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `medicine_order_items` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT UNSIGNED NOT NULL,
  `medicine_id` BIGINT UNSIGNED DEFAULT NULL,
  `medicine_name_snapshot` VARCHAR(100) NOT NULL,
  `spec_snapshot` VARCHAR(100) DEFAULT NULL,
  `unit` VARCHAR(20) DEFAULT NULL,
  `unit_price` DECIMAL(10,2) NOT NULL,
  `quantity` INT NOT NULL,
  `total_price` DECIMAL(10,2) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_medicine_order_items_medicine_id` (`medicine_id`),
  KEY `ix_medicine_order_items_order_id` (`order_id`),
  CONSTRAINT `medicine_order_items_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `medicine_orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `medicine_order_items_ibfk_2` FOREIGN KEY (`medicine_id`) REFERENCES `medicines` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `system_settings` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `category` VARCHAR(50) NOT NULL,
  `setting_key` VARCHAR(100) NOT NULL,
  `base_url` VARCHAR(500) NOT NULL,
  `model` VARCHAR(100) NOT NULL,
  `api_key_encrypted` TEXT DEFAULT NULL,
  `api_key_masked` VARCHAR(255) DEFAULT NULL,
  `is_configured` BOOLEAN NOT NULL,
  `last_test_status` VARCHAR(20) NOT NULL,
  `last_test_message` VARCHAR(500) DEFAULT NULL,
  `last_tested_at` DATETIME DEFAULT NULL,
  `updated_by` BIGINT UNSIGNED DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_system_settings_category_key` (`category`, `setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `ai_chat_sessions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `public_session_id` VARCHAR(64) NOT NULL,
  `student_id` BIGINT UNSIGNED NOT NULL,
  `mongo_conversation_id` VARCHAR(24) NOT NULL,
  `title` VARCHAR(200) DEFAULT NULL,
  `status` ENUM('active', 'archived') NOT NULL,
  `last_message_at` DATETIME NOT NULL,
  `deleted_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ix_ai_chat_sessions_public_session_id` (`public_session_id`),
  KEY `ix_ai_chat_sessions_student_id` (`student_id`),
  KEY `ix_ai_chat_sessions_last_message_at` (`last_message_at`),
  KEY `ix_ai_chat_sessions_mongo_conversation_id` (`mongo_conversation_id`),
  KEY `ix_ai_chat_sessions_status` (`status`),
  CONSTRAINT `ai_chat_sessions_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `ai_chat_messages_meta` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `session_id` BIGINT UNSIGNED NOT NULL,
  `seq_no` INT NOT NULL,
  `role` ENUM('user', 'assistant', 'system', 'action') NOT NULL,
  `message_kind` ENUM('text', 'cards', 'action_result', 'error') NOT NULL,
  `mongo_message_id` VARCHAR(24) DEFAULT NULL,
  `storage_status` ENUM('pending', 'stored', 'failed') NOT NULL,
  `token_count` INT DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_chat_session_seq` (`session_id`, `seq_no`),
  KEY `ix_ai_chat_messages_meta_session_id` (`session_id`),
  KEY `ix_ai_chat_messages_meta_mongo_message_id` (`mongo_message_id`),
  KEY `ix_ai_chat_messages_meta_storage_status` (`storage_status`),
  CONSTRAINT `ai_chat_messages_meta_ibfk_1` FOREIGN KEY (`session_id`) REFERENCES `ai_chat_sessions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @admin_password = '$2b$12$W.g.oK7cE8YtQXC4ylGtLeCAknUCarghtzugzERNjyqKyJR/bpJqC';
SET @test_password = '$2b$12$R1Lyj4s/gz01wejADmvZL.OOxIMfdKrflI/CVE4m4prc93jXpUIt6';

INSERT INTO `departments` (`id`, `name`, `description`, `is_active`, `sort_order`, `created_at`, `updated_at`) VALUES
(1, '内科', '诊断和治疗呼吸、消化、发热等常见内科疾病', TRUE, 1, NOW(), NOW()),
(2, '外科', '处理外伤、急腹症及基础手术评估', TRUE, 2, NOW(), NOW()),
(3, '口腔科', '牙痛、龋齿、牙龈炎等口腔常见问题', TRUE, 3, NOW(), NOW()),
(4, '中医科', '中医辨证施治与体质调理', TRUE, 4, NOW(), NOW()),
(5, '皮肤科', '皮炎、湿疹、过敏等皮肤问题', TRUE, 5, NOW(), NOW()),
(6, '儿科', '儿童常见呼吸道及消化道疾病', TRUE, 6, NOW(), NOW());

INSERT INTO `users` (`id`, `username`, `email`, `hashed_password`, `full_name`, `phone`, `student_id`, `role`, `is_active`, `created_at`, `updated_at`) VALUES
(1, 'admin', 'admin@campus-medical.com', @admin_password, '系统管理员', '13800000000', NULL, 'admin', TRUE, NOW(), NOW()),
(2, 'student', 'student@campus-medical.com', @test_password, '测试学生', '13900000001', '20240001', 'student', TRUE, NOW(), NOW()),
(3, 'doctor', 'doctor@campus-medical.com', @test_password, '测试医生', '13900000002', NULL, 'doctor', TRUE, NOW(), NOW()),
(4, 'doctor_chen', 'chen.hao@campus-medical.com', @test_password, '陈浩', '13800002001', NULL, 'doctor', TRUE, NOW(), NOW()),
(5, 'reg_student', 'reg_student@campus-medical.com', @test_password, 'Regression Student', '13800009002', 'REGS2026001', 'student', TRUE, NOW(), NOW()),
(6, 'doctor_zhou', 'zhou.lei@campus-medical.com', @test_password, '周磊', '13800007001', NULL, 'doctor', TRUE, NOW(), NOW());

INSERT INTO `doctors` (`id`, `user_id`, `department_id`, `doctor_id`, `department`, `title`, `created_at`, `updated_at`, `introduction`) VALUES
(1, 3, 1, 'D001', '内科', '主治医师', NOW(), NOW(), '擅长呼吸系统感染、发热与校园常见病综合评估。'),
(2, 4, 2, 'D002', '外科', '副主任医师', NOW(), NOW(), '处理外伤与急腹症评估，提供简明清晰的术前指导。'),
(3, 6, 6, 'D003', '儿科', '主任医师', NOW(), NOW(), '擅长儿童感冒、咳嗽与胃肠问题的诊疗。');

INSERT INTO `students` (`id`, `user_id`, `student_id`, `major`, `grade`, `class_name`, `health_status`, `created_at`, `updated_at`, `gender`, `dob`) VALUES
(1, 2, 'S000002', '计算机学院', '大一', '计科2401', '良好', NOW(), NOW(), '未知', '2005-09-01'),
(2, 5, 'REGS2026001', 'Computer Science', '2026', 'Class 1', '良好', NOW(), NOW(), '女', '2006-01-01');

INSERT INTO `medicines` (`id`, `name`, `spec`, `unit`, `stock`, `is_active`, `created_at`, `updated_at`, `price`, `code`, `category`, `threshold`, `supplier`, `manufacturer`, `approval_number`) VALUES
(1, '对乙酰氨基酚片', '0.5g*20片', '盒', 118, TRUE, NOW(), NOW(), 28.50, 'MED001', '解热镇痛', 20, '国药供应链', '华夏制药', 'HN-PARACET-001'),
(2, '布洛芬缓释胶囊', '0.3g*24粒', '盒', 78, TRUE, NOW(), NOW(), 16.00, 'MED002', '解热镇痛', 20, '国药供应链', '华夏制药', 'HN-IBU-002'),
(3, '阿莫西林胶囊', '0.25g*24粒', '盒', 56, TRUE, NOW(), NOW(), 22.00, 'MED003', '抗感染', 20, '国药供应链', '康宁药业', 'HN-AMOX-003'),
(4, '蒙脱石散', '3g*10袋', '盒', 64, TRUE, NOW(), NOW(), 12.50, 'MED004', '肠胃用药', 15, '国药供应链', '康宁药业', 'HN-SMEC-004'),
(5, '盐酸西替利嗪片', '10mg*12片', '盒', 72, TRUE, NOW(), NOW(), 18.50, 'MED005', '抗过敏', 15, '国药供应链', '华康医药', 'HN-CETI-005'),
(6, '莫匹罗星软膏', '5g', '支', 30, TRUE, NOW(), NOW(), 19.90, 'MED006', '外用药', 10, '国药供应链', '华康医药', 'HN-MUPI-006');

INSERT INTO `system_settings` (`id`, `category`, `setting_key`, `base_url`, `model`, `api_key_encrypted`, `api_key_masked`, `is_configured`, `last_test_status`, `last_test_message`, `last_tested_at`, `updated_by`, `created_at`, `updated_at`) VALUES
(1, 'llm', 'default_provider', 'https://api.deepseek.com', 'deepseek-chat', NULL, NULL, FALSE, 'unknown', NULL, NULL, 1, NOW(), NOW());

SET @student_a_past_1 = DATE_SUB(CURDATE(), INTERVAL 60 DAY);
SET @student_a_past_2 = DATE_SUB(CURDATE(), INTERVAL 28 DAY);
SET @student_a_past_3 = DATE_SUB(CURDATE(), INTERVAL 8 DAY);
SET @student_b_past_1 = DATE_SUB(CURDATE(), INTERVAL 5 DAY);
SET @student_a_future_1 = DATE_ADD(CURDATE(), INTERVAL 1 DAY);
SET @student_b_future_1 = DATE_ADD(CURDATE(), INTERVAL 2 DAY);

INSERT INTO `doctor_schedules` (`doctor_id`, `date`, `time_slot`, `capacity`, `booked_count`, `status`, `created_at`, `updated_at`)
SELECT 1, DATE_ADD(CURDATE(), INTERVAL d.day_offset DAY), s.time_slot, 8, 0, 'open', NOW(), NOW()
FROM (
  SELECT 0 AS day_offset UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
) AS d
CROSS JOIN (
  SELECT '08:00' AS time_slot UNION ALL SELECT '10:00' UNION ALL SELECT '14:00' UNION ALL SELECT '16:00'
) AS s;

INSERT INTO `doctor_schedules` (`doctor_id`, `date`, `time_slot`, `capacity`, `booked_count`, `status`, `created_at`, `updated_at`)
SELECT 2, DATE_ADD(CURDATE(), INTERVAL d.day_offset DAY), s.time_slot, 6, 0, 'open', NOW(), NOW()
FROM (
  SELECT 0 AS day_offset UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
) AS d
CROSS JOIN (
  SELECT '09:00' AS time_slot UNION ALL SELECT '15:00'
) AS s;

INSERT INTO `doctor_schedules` (`doctor_id`, `date`, `time_slot`, `capacity`, `booked_count`, `status`, `created_at`, `updated_at`)
SELECT 3, DATE_ADD(CURDATE(), INTERVAL d.day_offset DAY), s.time_slot, 8, 0, 'open', NOW(), NOW()
FROM (
  SELECT 0 AS day_offset UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
) AS d
CROSS JOIN (
  SELECT '08:30' AS time_slot UNION ALL SELECT '13:30'
) AS s;

INSERT INTO `appointments` (`id`, `student_id`, `doctor_id`, `department_id`, `date`, `time_slot`, `status`, `symptoms`, `queue_no`, `created_at`, `confirmed_at`, `cancelled_at`, `completed_at`, `reschedule_reason`, `cancellation_reason`, `notes`) VALUES
(1, 1, 1, 1, @student_a_past_1, '09:00', 'completed', '发热、咳嗽两天', 'A001', TIMESTAMP(DATE_SUB(@student_a_past_1, INTERVAL 1 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(@student_a_past_1, INTERVAL 1 DAY), '10:05:00'), NULL, TIMESTAMP(@student_a_past_1, '09:40:00'), NULL, NULL, '门诊完成'),
(2, 1, 1, 1, @student_a_past_2, '14:00', 'completed', '腹痛、腹泻', 'A002', TIMESTAMP(DATE_SUB(@student_a_past_2, INTERVAL 1 DAY), '11:00:00'), TIMESTAMP(DATE_SUB(@student_a_past_2, INTERVAL 1 DAY), '11:06:00'), NULL, TIMESTAMP(@student_a_past_2, '14:35:00'), NULL, NULL, '门诊完成'),
(3, 1, 1, 1, @student_a_past_3, '10:00', 'completed', '鼻塞、打喷嚏、眼痒', 'A003', TIMESTAMP(DATE_SUB(@student_a_past_3, INTERVAL 1 DAY), '16:00:00'), TIMESTAMP(DATE_SUB(@student_a_past_3, INTERVAL 1 DAY), '16:08:00'), NULL, TIMESTAMP(@student_a_past_3, '10:25:00'), NULL, NULL, '门诊完成'),
(4, 1, 1, 1, @student_a_future_1, '08:00', 'confirmed', '咽痛、低热', 'A004', NOW(), NOW(), NULL, NULL, NULL, NULL, '待就诊'),
(5, 1, 2, 2, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '09:00', 'cancelled', '运动时扭伤脚踝', 'B001', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:05:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '08:00:00'), NULL, NULL, '学生行程冲突', '已取消'),
(6, 2, 3, 6, @student_b_past_1, '08:30', 'completed', '孩子咳嗽、轻度腹泻', 'C001', TIMESTAMP(DATE_SUB(@student_b_past_1, INTERVAL 1 DAY), '09:30:00'), TIMESTAMP(DATE_SUB(@student_b_past_1, INTERVAL 1 DAY), '09:35:00'), NULL, TIMESTAMP(@student_b_past_1, '09:05:00'), NULL, NULL, '门诊完成'),
(7, 2, 3, 6, @student_b_future_1, '13:30', 'confirmed', '复诊咨询', 'C002', NOW(), NOW(), NULL, NULL, NULL, NULL, '待就诊');

UPDATE `doctor_schedules`
SET `booked_count` = 1
WHERE (`doctor_id` = 1 AND `date` = @student_a_future_1 AND `time_slot` = '08:00')
   OR (`doctor_id` = 3 AND `date` = @student_b_future_1 AND `time_slot` = '13:30');

INSERT INTO `appointment_status_logs` (`id`, `appointment_id`, `from_status`, `to_status`, `reason`, `created_at`) VALUES
(1, 1, 'pending', 'confirmed', '自动确认', TIMESTAMP(DATE_SUB(@student_a_past_1, INTERVAL 1 DAY), '10:05:00')),
(2, 1, 'confirmed', 'in_progress', '开始接诊', TIMESTAMP(@student_a_past_1, '09:05:00')),
(3, 1, 'in_progress', 'completed', '完成接诊', TIMESTAMP(@student_a_past_1, '09:40:00')),
(4, 2, 'pending', 'confirmed', '自动确认', TIMESTAMP(DATE_SUB(@student_a_past_2, INTERVAL 1 DAY), '11:06:00')),
(5, 2, 'confirmed', 'in_progress', '开始接诊', TIMESTAMP(@student_a_past_2, '14:05:00')),
(6, 2, 'in_progress', 'completed', '完成接诊', TIMESTAMP(@student_a_past_2, '14:35:00')),
(7, 3, 'pending', 'confirmed', '自动确认', TIMESTAMP(DATE_SUB(@student_a_past_3, INTERVAL 1 DAY), '16:08:00')),
(8, 3, 'confirmed', 'completed', '医生提交诊断', TIMESTAMP(@student_a_past_3, '10:25:00')),
(9, 4, 'pending', 'confirmed', '自动确认', NOW()),
(10, 5, 'pending', 'confirmed', '自动确认', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:05:00')),
(11, 5, 'confirmed', 'cancelled', '学生取消', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '08:00:00')),
(12, 6, 'pending', 'confirmed', '自动确认', TIMESTAMP(DATE_SUB(@student_b_past_1, INTERVAL 1 DAY), '09:35:00')),
(13, 6, 'confirmed', 'in_progress', '开始接诊', TIMESTAMP(@student_b_past_1, '08:35:00')),
(14, 6, 'in_progress', 'completed', '完成接诊', TIMESTAMP(@student_b_past_1, '09:05:00')),
(15, 7, 'pending', 'confirmed', '自动确认', NOW());

INSERT INTO `consultations` (`id`, `appointment_id`, `doctor_id`, `student_id`, `category`, `signs`, `conclusion`, `advice`, `created_at`) VALUES
(1, 1, 1, 1, '上呼吸道感染', '38.2℃，咽部充血', '普通感冒伴低热', '多喝温水，注意休息，若持续高热请复诊。', TIMESTAMP(@student_a_past_1, '09:35:00')),
(2, 2, 1, 1, '急性胃肠炎', '腹部轻压痛，无脱水体征', '胃肠功能紊乱', '清淡饮食，避免辛辣生冷，若腹泻加重请复诊。', TIMESTAMP(@student_a_past_2, '14:30:00')),
(3, 3, 1, 1, '过敏性鼻炎', '鼻甲肿胀，结膜轻度充血', '季节性过敏性鼻炎', '减少接触过敏原，症状明显时规律服药。', TIMESTAMP(@student_a_past_3, '10:20:00')),
(4, 6, 3, 2, '小儿普通感冒', '轻度咽红，偶有腹泻', '儿童上呼吸道感染', '补充水分，观察精神状态和食欲变化。', TIMESTAMP(@student_b_past_1, '09:00:00'));

INSERT INTO `prescriptions` (`id`, `consultation_id`, `note`, `created_at`) VALUES
(1, 1, '按医嘱服药 3 天', TIMESTAMP(@student_a_past_1, '09:36:00')),
(2, 2, '清淡饮食为主，可暂不处方', TIMESTAMP(@student_a_past_2, '14:31:00')),
(3, 3, '过敏季连续服用 5 天', TIMESTAMP(@student_a_past_3, '10:21:00')),
(4, 4, '儿童按需服用', TIMESTAMP(@student_b_past_1, '09:01:00'));

INSERT INTO `prescription_items` (`id`, `prescription_id`, `medicine_id`, `name`, `dosage`, `quantity`, `unit`, `unit_price`, `total_price`) VALUES
(1, 1, 1, '对乙酰氨基酚片', '每次1片，每日3次', 2, '盒', 28.50, 57.00),
(2, 1, 2, '布洛芬缓释胶囊', '发热时每次1粒', 1, '盒', 16.00, 16.00),
(3, 3, 5, '盐酸西替利嗪片', '每晚1片', 1, '盒', 18.50, 18.50),
(4, 4, 1, '对乙酰氨基酚片', '体温超过38.5℃时每次半片', 1, '盒', 28.50, 28.50),
(5, 4, 4, '蒙脱石散', '每日3次，每次1袋', 1, '盒', 12.50, 12.50);

INSERT INTO `documents` (`id`, `appointment_id`, `doc_type`, `file_path`, `file_name`, `created_at`) VALUES
(1, 1, 'diagnosis', '/app/storage/docs/diagnosis_1_20260126152051.pdf', 'diagnosis_1_20260126152051.pdf', TIMESTAMP(@student_a_past_1, '09:37:00')),
(2, 1, 'prescription', '/app/storage/docs/prescription_1_20260126152051.pdf', 'prescription_1_20260126152051.pdf', TIMESTAMP(@student_a_past_1, '09:37:00')),
(3, 2, 'diagnosis', '/app/storage/docs/diagnosis_14_20260130165635.pdf', 'diagnosis_14_20260130165635.pdf', TIMESTAMP(@student_a_past_2, '14:32:00')),
(4, 2, 'prescription', '/app/storage/docs/prescription_14_20260130165635.pdf', 'prescription_14_20260130165635.pdf', TIMESTAMP(@student_a_past_2, '14:32:00')),
(5, 3, 'diagnosis', '/app/storage/docs/diagnosis_34_20260201092842.pdf', 'diagnosis_34_20260201092842.pdf', TIMESTAMP(@student_a_past_3, '10:22:00')),
(6, 3, 'prescription', '/app/storage/docs/prescription_34_20260201092842.pdf', 'prescription_34_20260201092842.pdf', TIMESTAMP(@student_a_past_3, '10:22:00')),
(7, 6, 'diagnosis', '/app/storage/docs/diagnosis_46_20260307162123.pdf', 'diagnosis_46_20260307162123.pdf', TIMESTAMP(@student_b_past_1, '09:02:00')),
(8, 6, 'prescription', '/app/storage/docs/prescription_46_20260307162123.pdf', 'prescription_46_20260307162123.pdf', TIMESTAMP(@student_b_past_1, '09:02:00'));

INSERT INTO `inventory_movements` (`id`, `medicine_id`, `delta`, `reason`, `ref_type`, `ref_id`, `created_at`) VALUES
(1, 1, 120, 'initial_seed', 'seed', NULL, NOW()),
(2, 2, 80, 'initial_seed', 'seed', NULL, NOW()),
(3, 3, 56, 'initial_seed', 'seed', NULL, NOW()),
(4, 4, 65, 'initial_seed', 'seed', NULL, NOW()),
(5, 5, 74, 'initial_seed', 'seed', NULL, NOW()),
(6, 6, 30, 'initial_seed', 'seed', NULL, NOW()),
(7, 1, -2, 'prescription', 'consultation', 1, TIMESTAMP(@student_a_past_1, '09:36:00')),
(8, 2, -1, 'prescription', 'consultation', 1, TIMESTAMP(@student_a_past_1, '09:36:00')),
(9, 5, -1, 'prescription', 'consultation', 3, TIMESTAMP(@student_a_past_3, '10:21:00')),
(10, 1, -1, 'prescription', 'consultation', 4, TIMESTAMP(@student_b_past_1, '09:01:00')),
(11, 4, -1, 'prescription', 'consultation', 4, TIMESTAMP(@student_b_past_1, '09:01:00')),
(12, 1, -1, 'student_purchase', 'medicine_order', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '19:00:00')),
(13, 2, -1, 'student_purchase', 'medicine_order', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '19:00:00')),
(14, 4, -1, 'student_purchase', 'medicine_order', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '18:30:00')),
(15, 5, -1, 'student_purchase', 'medicine_order', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '18:30:00'));

INSERT INTO `medicine_orders` (`id`, `order_no`, `student_id`, `status`, `total_amount`, `created_at`, `updated_at`) VALUES
(1, 'ORD-DEMO-0001', 1, 'completed', 44.50, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '19:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '19:00:00')),
(2, 'ORD-DEMO-0002', 2, 'completed', 31.00, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '18:30:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '18:30:00'));

INSERT INTO `medicine_order_items` (`id`, `order_id`, `medicine_id`, `medicine_name_snapshot`, `spec_snapshot`, `unit`, `unit_price`, `quantity`, `total_price`) VALUES
(1, 1, 1, '对乙酰氨基酚片', '0.5g*20片', '盒', 28.50, 1, 28.50),
(2, 1, 2, '布洛芬缓释胶囊', '0.3g*24粒', '盒', 16.00, 1, 16.00),
(3, 2, 4, '蒙脱石散', '3g*10袋', '盒', 12.50, 1, 12.50),
(4, 2, 5, '盐酸西替利嗪片', '10mg*12片', '盒', 18.50, 1, 18.50);

INSERT INTO `ai_chat_sessions` (`id`, `public_session_id`, `student_id`, `mongo_conversation_id`, `title`, `status`, `last_message_at`, `deleted_at`, `created_at`, `updated_at`) VALUES
(1, '11111111-2222-3333-4444-555555555555', 1, '65f0a0000000000000000001', '头痛应该挂什么科', 'active', NOW(), NULL, NOW(), NOW());

INSERT INTO `ai_chat_messages_meta` (`id`, `session_id`, `seq_no`, `role`, `message_kind`, `mongo_message_id`, `storage_status`, `token_count`, `created_at`) VALUES
(1, 1, 1, 'user', 'text', '65f0b0000000000000000001', 'stored', NULL, NOW()),
(2, 1, 2, 'assistant', 'cards', '65f0b0000000000000000002', 'stored', NULL, NOW());

SET FOREIGN_KEY_CHECKS = 1;
