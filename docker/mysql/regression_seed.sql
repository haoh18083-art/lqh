-- Idempotent regression seed for Spring backend smoke and full-chain tests.
-- Accounts:
--   reg_admin   / test123456
--   reg_doctor  / test123456
--   reg_student / test123456

SET @password_hash = '$2b$12$R1Lyj4s/gz01wejADmvZL.OOxIMfdKrflI/CVE4m4prc93jXpUIt6';

INSERT INTO departments (`name`, `description`, `is_active`, `sort_order`)
VALUES (
  'Regression Dept',
  'Dedicated department for Docker regression tests',
  TRUE,
  999
)
ON DUPLICATE KEY UPDATE
  `description` = VALUES(`description`),
  `is_active` = TRUE,
  `sort_order` = VALUES(`sort_order`);

SET @reg_department_id = (
  SELECT `id`
  FROM departments
  WHERE `name` = 'Regression Dept'
  LIMIT 1
);

INSERT INTO users (
  `username`,
  `email`,
  `hashed_password`,
  `full_name`,
  `phone`,
  `role`,
  `is_active`
)
VALUES (
  'reg_admin',
  'reg_admin@campus-medical.com',
  @password_hash,
  'Regression Admin',
  '13800009000',
  'admin',
  TRUE
)
ON DUPLICATE KEY UPDATE
  `email` = VALUES(`email`),
  `hashed_password` = VALUES(`hashed_password`),
  `full_name` = VALUES(`full_name`),
  `phone` = VALUES(`phone`),
  `role` = 'admin',
  `is_active` = TRUE;

SET @reg_admin_user_id = (
  SELECT `id`
  FROM users
  WHERE `username` = 'reg_admin'
  LIMIT 1
);

INSERT INTO users (
  `username`,
  `email`,
  `hashed_password`,
  `full_name`,
  `phone`,
  `role`,
  `is_active`
)
VALUES (
  'reg_doctor',
  'reg_doctor@campus-medical.com',
  @password_hash,
  'Regression Doctor',
  '13800009001',
  'doctor',
  TRUE
)
ON DUPLICATE KEY UPDATE
  `email` = VALUES(`email`),
  `hashed_password` = VALUES(`hashed_password`),
  `full_name` = VALUES(`full_name`),
  `phone` = VALUES(`phone`),
  `role` = 'doctor',
  `is_active` = TRUE;

SET @reg_doctor_user_id = (
  SELECT `id`
  FROM users
  WHERE `username` = 'reg_doctor'
  LIMIT 1
);

INSERT INTO doctors (
  `user_id`,
  `doctor_id`,
  `department_id`,
  `department`,
  `title`,
  `introduction`,
  `created_at`,
  `updated_at`
)
VALUES (
  @reg_doctor_user_id,
  'REGD001',
  @reg_department_id,
  'Regression Dept',
  'Attending Physician',
  'Regression doctor for Docker end-to-end tests.',
  UTC_TIMESTAMP(),
  UTC_TIMESTAMP()
)
ON DUPLICATE KEY UPDATE
  `department_id` = VALUES(`department_id`),
  `department` = VALUES(`department`),
  `title` = VALUES(`title`),
  `introduction` = VALUES(`introduction`),
  `updated_at` = UTC_TIMESTAMP();

SET @reg_doctor_id = (
  SELECT `id`
  FROM doctors
  WHERE `user_id` = @reg_doctor_user_id
     OR `doctor_id` = 'REGD001'
  LIMIT 1
);

INSERT INTO users (
  `username`,
  `email`,
  `hashed_password`,
  `full_name`,
  `phone`,
  `student_id`,
  `role`,
  `is_active`
)
VALUES (
  'reg_student',
  'reg_student@campus-medical.com',
  @password_hash,
  'Regression Student',
  '13800009002',
  'REGS2026001',
  'student',
  TRUE
)
ON DUPLICATE KEY UPDATE
  `email` = VALUES(`email`),
  `hashed_password` = VALUES(`hashed_password`),
  `full_name` = VALUES(`full_name`),
  `phone` = VALUES(`phone`),
  `student_id` = VALUES(`student_id`),
  `role` = 'student',
  `is_active` = TRUE;

SET @reg_student_user_id = (
  SELECT `id`
  FROM users
  WHERE `username` = 'reg_student'
  LIMIT 1
);

INSERT INTO students (
  `user_id`,
  `student_id`,
  `major`,
  `grade`,
  `class_name`,
  `dob`,
  `created_at`,
  `updated_at`
)
VALUES (
  @reg_student_user_id,
  'REGS2026001',
  'Computer Science',
  '2026',
  'Class 1',
  '2006-01-01',
  UTC_TIMESTAMP(),
  UTC_TIMESTAMP()
)
ON DUPLICATE KEY UPDATE
  `major` = VALUES(`major`),
  `grade` = VALUES(`grade`),
  `class_name` = VALUES(`class_name`),
  `dob` = VALUES(`dob`),
  `updated_at` = UTC_TIMESTAMP();

SET @reg_student_id = (
  SELECT `id`
  FROM students
  WHERE `user_id` = @reg_student_user_id
     OR `student_id` = 'REGS2026001'
  LIMIT 1
);

INSERT IGNORE INTO doctor_schedules (
  `doctor_id`,
  `date`,
  `time_slot`,
  `capacity`,
  `booked_count`,
  `status`,
  `created_at`,
  `updated_at`
)
SELECT
  @reg_doctor_id AS doctor_id,
  DATE_ADD(CURDATE(), INTERVAL day_offsets.day_offset DAY) AS visit_date,
  time_slots.time_slot,
  8,
  0,
  'open',
  UTC_TIMESTAMP(),
  UTC_TIMESTAMP()
FROM (
  SELECT 0 AS day_offset
  UNION ALL SELECT 1
  UNION ALL SELECT 2
  UNION ALL SELECT 3
  UNION ALL SELECT 4
  UNION ALL SELECT 5
  UNION ALL SELECT 6
  UNION ALL SELECT 7
  UNION ALL SELECT 8
  UNION ALL SELECT 9
  UNION ALL SELECT 10
  UNION ALL SELECT 11
  UNION ALL SELECT 12
  UNION ALL SELECT 13
) AS day_offsets
CROSS JOIN (
  SELECT '00:00' AS time_slot
  UNION ALL SELECT '08:00'
  UNION ALL SELECT '10:00'
  UNION ALL SELECT '14:00'
  UNION ALL SELECT '16:00'
) AS time_slots
;

UPDATE doctor_schedules
SET
  `capacity` = GREATEST(COALESCE(`booked_count`, 0), 8),
  `status` = 'open',
  `updated_at` = UTC_TIMESTAMP()
WHERE `doctor_id` = @reg_doctor_id
  AND `date` BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 13 DAY)
  AND `time_slot` IN ('00:00', '08:00', '10:00', '14:00', '16:00');

SELECT
  u.id,
  u.username,
  u.email,
  u.role,
  u.is_active
FROM users u
WHERE u.username IN ('reg_admin', 'reg_doctor', 'reg_student')
ORDER BY u.role, u.username;
