-- V2 初始化数据
--
-- 注意：本脚本不使用会话变量（SET @var）。
-- 会话变量是连接级的，逐条新开连接执行时会丢失并静默写入 NULL
-- （organization.parent_id 与 users.org_id 均可空，不会报错），
-- 导致组织架构树断裂、用户没有班级。
-- 固定 UUID 字面量保证「手动逐句执行」与「整文件一次性执行」结果完全一致。
--
-- 固定 UUID 分配表（后续脚本引用这些字面量）：
--   组织架构：01 闽江大学 / 02 物理与电子信息工程学院 / 03 电气工程及其自动化
--             04 2024级   / 05 1班 / 06 2班
--   角色：    11 学生 / 12 教师 / 13 管理员 / 14 超级管理员
--   用户：    21 admin / 22 teacher1 / 23 student1 / 24 superadmin

-- 1. 组织架构
ALTER TABLE organization
ADD CONSTRAINT chk_organization_type
CHECK (type IN ('university', 'college', 'major', 'grade', 'class'));

INSERT INTO organization (id, name, type, parent_id, path, sort) VALUES
(0x00000000000000000000000000000001, '闽江大学', 'university', NULL,
 '/闽江大学/', 1);

INSERT INTO organization (id, name, type, parent_id, path, sort) VALUES
(0x00000000000000000000000000000002, '物理与电子信息工程学院', 'college',
 0x00000000000000000000000000000001,
 '/闽江大学/物理与电子信息工程学院/', 1);

INSERT INTO organization (id, name, type, parent_id, path, sort) VALUES
(0x00000000000000000000000000000003, '电气工程及其自动化', 'major',
 0x00000000000000000000000000000002,
 '/闽江大学/物理与电子信息工程学院/电气工程及其自动化/', 1);

INSERT INTO organization (id, name, type, parent_id, path, sort) VALUES
(0x00000000000000000000000000000004, '2024级', 'grade',
 0x00000000000000000000000000000003,
 '/闽江大学/物理与电子信息工程学院/电气工程及其自动化/2024级/', 1);

INSERT INTO organization (id, name, type, parent_id, path, sort) VALUES
(0x00000000000000000000000000000005, '1班', 'class',
 0x00000000000000000000000000000004,
 '/闽江大学/物理与电子信息工程学院/电气工程及其自动化/2024级/1班/', 1),
(0x00000000000000000000000000000006, '2班', 'class',
 0x00000000000000000000000000000004,
 '/闽江大学/物理与电子信息工程学院/电气工程及其自动化/2024级/2班/', 2);

-- 2. 角色（先插入，供 user_roles 引用）
INSERT INTO roles (id, code, name, level) VALUES
(0x00000000000000000000000000000011, 'student',     '学生',       10),
(0x00000000000000000000000000000012, 'teacher',     '教师',       20),
(0x00000000000000000000000000000013, 'admin',       '管理员',     30),
(0x00000000000000000000000000000014, 'super_admin', '超级管理员', 40);

-- 3. 用户（密码为 BCrypt 哈希值）
-- superadmin 的哈希与 admin 相同，即初始密码一致
INSERT INTO users (id, username, password, name, org_id) VALUES
(0x00000000000000000000000000000021, 'admin',
 '$2a$10$CGMamDauMHck7ZD4d8wXMOj2I0ZthT9UCLAuwhatljrEKtmnC9Tme', '管理员',
 0x00000000000000000000000000000001),
(0x00000000000000000000000000000022, 'teacher1',
 '$2a$10$mKchFrQcW7J/adF5kluUH.98xHoppnW85/OEsJx96GfLMP1SD/TDi', '教师A',
 0x00000000000000000000000000000002),
(0x00000000000000000000000000000023, 'student1',
 '$2a$10$Z9LfnXd9GhoS3JGW8sB7pOa6fu/KnoBTYAK.9MK52kDCUsbnDUST6', '学生A',
 0x00000000000000000000000000000005),
(0x00000000000000000000000000000024, 'superadmin',
 '$2a$10$CGMamDauMHck7ZD4d8wXMOj2I0ZthT9UCLAuwhatljrEKtmnC9Tme', '超级管理员',
 0x00000000000000000000000000000001);

-- 4. 用户角色关联
INSERT INTO user_roles (user_id, role_id) VALUES
(0x00000000000000000000000000000021, 0x00000000000000000000000000000013),
(0x00000000000000000000000000000022, 0x00000000000000000000000000000012),
(0x00000000000000000000000000000023, 0x00000000000000000000000000000011),
(0x00000000000000000000000000000024, 0x00000000000000000000000000000014);
