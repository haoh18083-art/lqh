-- 测试数据脚本
-- 插入科室和医生测试数据

SET NAMES utf8mb4;

-- ========== 1. 插入更多科室数据 ==========
INSERT INTO `departments` (`name`, `description`, `is_active`, `sort_order`) VALUES
('儿科', '儿童疾病的诊断和治疗', TRUE, 7),
('妇产科', '女性生殖系统疾病和妊娠', TRUE, 8),
('皮肤科', '皮肤疾病诊断和治疗', TRUE, 9),
('检验科', '临床检验和诊断', TRUE, 10),
('放射科', '医学影像诊断', TRUE, 11),
('麻醉科', '临床麻醉和疼痛治疗', TRUE, 12),
('急诊科', '急危重症救治', TRUE, 13),
('病理科', '疾病病理诊断', TRUE, 14)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- ========== 2. 创建测试用户和医生数据 ==========
-- 注意：密码统一为 test123456 (bcrypt hash)

-- 获取科室ID
SET @dept_internal = (SELECT id FROM departments WHERE name = '内科' LIMIT 1);
SET @dept_surgery = (SELECT id FROM departments WHERE name = '外科' LIMIT 1);
SET @dept_dental = (SELECT id FROM departments WHERE name = '口腔科' LIMIT 1);
SET @dept_tcm = (SELECT id FROM departments WHERE name = '中医科' LIMIT 1);
SET @dept_ophthalmology = (SELECT id FROM departments WHERE name = '眼科' LIMIT 1);
SET @dept_ent = (SELECT id FROM departments WHERE name = '耳鼻喉科' LIMIT 1);
SET @dept_pediatrics = (SELECT id FROM departments WHERE name = '儿科' LIMIT 1);
SET @dept_obgyn = (SELECT id FROM departments WHERE name = '妇产科' LIMIT 1);
SET @dept_dermatology = (SELECT id FROM departments WHERE name = '皮肤科' LIMIT 1);

-- 插入测试用户和医生
INSERT INTO `users` (`username`, `email`, `hashed_password`, `full_name`, `phone`, `role`, `is_active`) VALUES
-- 内科医生
('doctor_li', 'li.yang@campus-medical.com', '$2b$12$R1Lyj4s/gz01wejADmvZL.OOxIMfdKrflI/CVE4m4prc93jXpUIt6', '李扬', '13800001001', 'doctor', TRUE),
('doctor_wang', 'wang.min@campus-medical.com', '$2b$12$R1Lyj4s/gz01wejADmvZL.OOxIMfdKrflI/CVE4m4prc93jXpUIt6', '王敏', '13800001002', 'doctor', TRUE),
('doctor_zhang', 'zhang.wei@campus-medical.com', '$2b$12$R1Lyj4s/gz01wejADmvZL.OOxIMfdKrflI/CVE4m4prc93jXpUIt6', '张伟', '13800001003', 'doctor', TRUE),

-- 外科医生
('doctor_chen', 'chen.hao@campus-medical.com', '$2b$12$R1Lyj4s/gz01wejADmvZL.OOxIMfdKrflI/CVE4m4prc93jXpUIt6', '陈浩', '13800002001', 'doctor', TRUE),
('doctor_liu', 'liu.jie@campus-medical.com', '$2b$12$R1Lyj4s/gz01wejADmvZL.OOxIMfdKrflI/CVE4m4prc93jXpUIt6', '刘杰', '13800002002', 'doctor', TRUE),

-- 口腔科医生
('doctor_zhao', 'zhao.xin@campus-medical.com', '$2b$12$R1Lyj4s/gz01wejADmvZL.OOxIMfdKrflI/CVE4m4prc93jXpUIt6', '赵欣', '13800003001', 'doctor', TRUE),

-- 中医科医生
('doctor_sun', 'sun.fang@campus-medical.com', '$2b$12$R1Lyj4s/gz01wejADmvZL.OOxIMfdKrflI/CVE4m4prc93jXpUIt6', '孙芳', '13800004001', 'doctor', TRUE),

-- 眼科医生
('doctor_wu', 'wu.qiang@campus-medical.com', '$2b$12$R1Lyj4s/gz01wejADmvZL.OOxIMfdKrflI/CVE4m4prc93jXpUIt6', '吴强', '13800005001', 'doctor', TRUE),

-- 儿科医生
('doctor_zhou', 'zhou.lei@campus-medical.com', '$2b$12$R1Lyj4s/gz01wejADmvZL.OOxIMfdKrflI/CVE4m4prc93jXpUIt6', '周磊', '13800007001', 'doctor', TRUE),
('doctor_xu', 'xu.na@campus-medical.com', '$2b$12$R1Lyj4s/gz01wejADmvZL.OOxIMfdKrflI/CVE4m4prc93jXpUIt6', '徐娜', '13800007002', 'doctor', TRUE),

-- 妇产科医生
('doctor_ma', 'ma.li@campus-medical.com', '$2b$12$R1Lyj4s/gz01wejADmvZL.OOxIMfdKrflI/CVE4m4prc93jXpUIt6', '马丽', '13800008001', 'doctor', TRUE),

-- 皮肤科医生
('doctor_lin', 'lin.yi@campus-medical.com', '$2b$12$R1Lyj4s/gz01wejADmvZL.OOxIMfdKrflI/CVE4m4prc93jXpUIt6', '林怡', '13800009001', 'doctor', TRUE)
ON DUPLICATE KEY UPDATE `username` = VALUES(`username`);

-- ========== 3. 插入医生档案信息 ==========
INSERT INTO `doctors` (`user_id`, `doctor_id`, `department_id`, `department`, `title`, `introduction`) VALUES
-- 内科医生
((SELECT id FROM users WHERE username = 'doctor_li'), 'D001', @dept_internal, '内科', '主任医师', '擅长内科常见病与慢病管理，注重循证诊疗与长期随访。'),
((SELECT id FROM users WHERE username = 'doctor_wang'), 'D002', @dept_internal, '内科', '副主任医师', '专注消化系统疾病诊治，具备丰富门诊和健康管理经验。'),
((SELECT id FROM users WHERE username = 'doctor_zhang'), 'D003', @dept_internal, '内科', '主治医师', '擅长呼吸系统与发热性疾病评估，沟通细致，重视个体化方案。'),

-- 外科医生
((SELECT id FROM users WHERE username = 'doctor_chen'), 'D004', @dept_surgery, '外科', '主任医师', '长期从事普通外科临床工作，擅长急腹症及微创手术评估。'),
((SELECT id FROM users WHERE username = 'doctor_liu'), 'D005', @dept_surgery, '外科', '副主任医师', '擅长创伤处理与围手术期管理，强调术后康复与风险宣教。'),

-- 口腔科医生
((SELECT id FROM users WHERE username = 'doctor_zhao'), 'D006', @dept_dental, '口腔科', '主治医师', '专注口腔常见疾病治疗与预防，擅长龋病和牙周问题处理。'),

-- 中医科医生
((SELECT id FROM users WHERE username = 'doctor_sun'), 'D007', @dept_tcm, '中医科', '副主任医师', '结合中医辨证施治与体质调理，关注亚健康和慢病干预。'),

-- 眼科医生
((SELECT id FROM users WHERE username = 'doctor_wu'), 'D008', @dept_ophthalmology, '眼科', '医师', '擅长常见眼表疾病筛查与基础诊疗，注重用眼健康指导。'),

-- 儿科医生
((SELECT id FROM users WHERE username = 'doctor_zhou'), 'D009', @dept_pediatrics, '儿科', '主任医师', '从事儿科多年，擅长儿童呼吸道与消化道常见病诊治。'),
((SELECT id FROM users WHERE username = 'doctor_xu'), 'D010', @dept_pediatrics, '儿科', '主治医师', '关注儿童生长发育与常见感染性疾病，沟通耐心细致。'),

-- 妇产科医生
((SELECT id FROM users WHERE username = 'doctor_ma'), 'D011', @dept_obgyn, '妇产科', '副主任医师', '擅长妇科常见病与孕期咨询管理，提供连续性健康指导。'),

-- 皮肤科医生
((SELECT id FROM users WHERE username = 'doctor_lin'), 'D012', @dept_dermatology, '皮肤科', '医师', '专注皮肤常见炎症与过敏性疾病诊疗，重视个体化护理建议。')
ON DUPLICATE KEY UPDATE
`doctor_id` = VALUES(`doctor_id`),
`department_id` = VALUES(`department_id`),
`department` = VALUES(`department`),
`title` = VALUES(`title`),
`introduction` = VALUES(`introduction`);

-- ========== 4. 验证插入结果 ==========
-- SELECT '=== 科室列表 ===' AS '';
-- SELECT id, name, description, is_active FROM departments ORDER BY sort_order;

-- SELECT '=== 医生列表 ===' AS '';
-- SELECT d.doctor_id, u.full_name, d.department_id, dep.name AS department_name, d.title
-- FROM doctors d
-- JOIN users u ON d.user_id = u.id
-- LEFT JOIN departments dep ON d.department_id = dep.id
-- ORDER BY d.doctor_id;
