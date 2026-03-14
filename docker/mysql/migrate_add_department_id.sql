-- 医生表优化：添加 department_id 外键
-- 将 department 字段从字符串改为外键引用 departments 表

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 步骤 1: 添加 department_id 字段
ALTER TABLE `doctors` ADD COLUMN `department_id` BIGINT UNSIGNED NULL COMMENT '科室ID（外键）' AFTER `user_id`;

-- 步骤 2: 创建外键约束
ALTER TABLE `doctors`
ADD CONSTRAINT `fk_doctors_department`
FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`)
ON DELETE RESTRICT
ON UPDATE CASCADE;

-- 步骤 3: 迁移现有数据（根据科室名称查找对应的 department_id）
UPDATE `doctors` d
INNER JOIN `departments` dep ON d.`department` = dep.`name`
SET d.`department_id` = dep.`id`;

-- 步骤 4: 设置 department_id 为 NOT NULL（数据迁移后）
-- 注意：如果有医生的科室名称不在 departments 表中，需要先处理这些数据
-- SET @invalid_doctors = (SELECT COUNT(*) FROM `doctors` WHERE `department_id` IS NULL);
-- 如果 @invalid_doctors > 0，需要先处理这些数据

-- 步骤 5: 添加索引
ALTER TABLE `doctors` ADD INDEX `idx_department_id` (`department_id`);

-- 步骤 6: 删除旧的 department 字段（可选，建议确认数据迁移无误后再删除）
-- ALTER TABLE `doctors` DROP COLUMN `department`;

SET FOREIGN_KEY_CHECKS = 1;

-- 验证迁移结果
-- SELECT d.id, d.doctor_id, d.department_id, dep.name as department_name, u.full_name
-- FROM `doctors` d
-- LEFT JOIN `departments` dep ON d.department_id = dep.id
-- LEFT JOIN `users` u ON d.user_id = u.id;
