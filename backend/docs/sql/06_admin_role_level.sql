-- 06. 角色层级：数值越大权限越高；用户可见范围 = level < 当前用户的 level（2026-09-10）
--
-- 注意：本脚本所有变更均已同步至 01 / 02，重建库无需执行本脚本；
--       本脚本仅为「不重建库」的存量环境提供增量迁移，后期会删除。
--       数据库可整体重建时，直接执行 01 → 02 → 03 → 04 即可。
--
-- 与 02 的差异说明：
--   02 面向全新建库，组织架构 ID 由脚本自己指定，故用固定 UUID 字面量；
--   06 面向已存在的库，其中的组织行是早先随机生成的 UUID，无从预知，
--   只能按 name 反查。此处用的是跨语句子查询（每个子查询在独立语句中
--   查已提交的数据），不使用会话变量，两种执行方式都安全。

ALTER TABLE roles ADD COLUMN level INT NOT NULL DEFAULT 0 COMMENT '角色层级，数值越大权限越高';

-- 先补齐既有角色的层级，再插入新角色
UPDATE roles SET level = 30 WHERE code = 'admin';
UPDATE roles SET level = 20 WHERE code = 'teacher';
UPDATE roles SET level = 10 WHERE code = 'student';

INSERT INTO roles (id, code, name, level) VALUES
(UUID_TO_BIN(UUID()), 'super_admin', '超级管理员', 40);

-- 超级管理员账号
-- 密码哈希与 admin 一致，即初始密码与 admin 相同
-- 自助改密入口在 Phase 4 提供，在此之前该账号密码只能由 DBA 直接改库
INSERT INTO users (id, username, password, name, org_id) VALUES
(UUID_TO_BIN(UUID()), 'superadmin',
 '$2a$10$CGMamDauMHck7ZD4d8wXMOj2I0ZthT9UCLAuwhatljrEKtmnC9Tme',
 '超级管理员', (SELECT id FROM organization WHERE name = '闽江大学'));

INSERT INTO user_roles (user_id, role_id) VALUES
((SELECT id FROM users WHERE username = 'superadmin'),
 (SELECT id FROM roles WHERE code = 'super_admin'));
