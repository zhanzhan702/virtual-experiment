-- V2 初始化数据

-- 1. 组织架构

-- 修改type: university/college/major/grade/class
ALTER TABLE organization
ADD CONSTRAINT chk_organization_type
CHECK (type IN ('university', 'college', 'major', 'grade', 'class'));

INSERT INTO organization (id, name, type, parent_id, path, sort) VALUES
(UUID_TO_BIN(UUID()), '闽江大学', 'university', NULL, '/闽江大学/', 1);

-- 用变量存上级 ID，避免同表子查询报错
SET @univ_id = (SELECT id FROM organization WHERE name = '闽江大学');
INSERT INTO organization (id, name, type, parent_id, path, sort) VALUES
(UUID_TO_BIN(UUID()), '物理与电子信息工程学院', 'college', @univ_id, '/闽江大学/物理与电子信息工程学院/', 1);

SET @college_id = (SELECT id FROM organization WHERE name = '物理与电子信息工程学院');
INSERT INTO organization (id, name, type, parent_id, path, sort) VALUES
(UUID_TO_BIN(UUID()), '电气工程及其自动化', 'major', @college_id, '/闽江大学/物理与电子信息工程学院/电气工程及其自动化/', 1);

SET @major_id = (SELECT id FROM organization WHERE name = '电气工程及其自动化');
INSERT INTO organization (id, name, type, parent_id, path, sort) VALUES
(UUID_TO_BIN(UUID()), '2024级', 'grade', @major_id, '/闽江大学/物理与电子信息工程学院/电气工程及其自动化/2024级/', 1);

SET @grade_id = (SELECT id FROM organization WHERE name = '2024级');
INSERT INTO organization (id, name, type, parent_id, path, sort) VALUES
(UUID_TO_BIN(UUID()), '1班', 'class', @grade_id, '/闽江大学/物理与电子信息工程学院/电气工程及其自动化/2024级/1班/', 1),
(UUID_TO_BIN(UUID()), '2班', 'class', @grade_id, '/闽江大学/物理与电子信息工程学院/电气工程及其自动化/2024级/2班/', 2);

-- 2. 角色（先插入，供 user_roles 引用）
INSERT INTO roles (id, code, name) VALUES
(UUID_TO_BIN(UUID()), 'admin', '管理员'),
(UUID_TO_BIN(UUID()), 'teacher', '教师'),
(UUID_TO_BIN(UUID()), 'student', '学生');

-- 3. 用户（密码为 MD5 哈希值）
INSERT INTO users (id, username, password, name, org_id) VALUES
(UUID_TO_BIN(UUID()), 'admin', '$2a$10$CGMamDauMHck7ZD4d8wXMOj2I0ZthT9UCLAuwhatljrEKtmnC9Tme', '管理员', @univ_id),
(UUID_TO_BIN(UUID()), 'teacher1', '$2a$10$mKchFrQcW7J/adF5kluUH.98xHoppnW85/OEsJx96GfLMP1SD/TDi', '教师A', @college_id),
(UUID_TO_BIN(UUID()), 'student1', '$2a$10$Z9LfnXd9GhoS3JGW8sB7pOa6fu/KnoBTYAK.9MK52kDCUsbnDUST6', '学生A',
    (SELECT id FROM organization WHERE name = '1班'));

-- 4. 用户角色关联
INSERT INTO user_roles (user_id, role_id) VALUES
((SELECT id FROM users WHERE username = 'admin'), (SELECT id FROM roles WHERE code = 'admin')),
((SELECT id FROM users WHERE username = 'teacher1'), (SELECT id FROM roles WHERE code = 'teacher')),
((SELECT id FROM users WHERE username = 'student1'), (SELECT id FROM roles WHERE code = 'student'));
