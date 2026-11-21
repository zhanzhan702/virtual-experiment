-- V4 插入低压场景实验模板和步骤
--
-- 注意：本脚本不使用会话变量（SET @var），模板 ID 用固定 UUID 字面量。
-- 会话变量是连接级的，逐条新开连接执行时会丢失，导致 template_id 写入 NULL
-- （experiment_steps.template_id 为 NOT NULL，会直接报错）。
--
-- 固定 UUID 分配表（与 02_create_data.sql 统一编号）：
--   32 低压训练场景V1（LV_TRAIN_V1）：
--     1 填写工作票 / 2 工器具选择 / 3 架设围栏并悬挂标识牌 / 4 三步式验电 /
--     5 断电挂牌 / 6 挂表接线 / 7 上电封盖
--
-- 步骤 5~7 为居民表箱内部操作（V11 脚本内容已合并到此，旧环境仍可单独执行 11 升级）。

INSERT INTO experiment_templates (id, code, name, category, mode, version, description) VALUES
(0x00000000000000000000000000000032, 'LV_TRAIN_V1', '低压训练场景V1', 'low_voltage', 'training', '1.0', '低压场景下的用电信息采集终端安装与调试训练模式');

-- 步骤 1~7：低压完整流程
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), 0x00000000000000000000000000000032, 1, 'FILL_TICKET', '填写工作票', 300, 25.00),
(UUID_TO_BIN(UUID()), 0x00000000000000000000000000000032, 2, 'SELECT_TOOLS', '工器具选择', 180, 30.00),
(UUID_TO_BIN(UUID()), 0x00000000000000000000000000000032, 3, 'ERECT_FENCE', '架设围栏并悬挂标识牌', 120, 20.00),
(UUID_TO_BIN(UUID()), 0x00000000000000000000000000000032, 4, 'THREE_STEP_CHECK', '三步式验电', 90, 15.00),
(UUID_TO_BIN(UUID()), 0x00000000000000000000000000000032, 5, 'POWER_OFF_SIGN', '断电挂牌', 120, 15.00),
(UUID_TO_BIN(UUID()), 0x00000000000000000000000000000032, 6, 'MOUNT_METER_WIRING', '挂表接线', 300, 30.00),
(UUID_TO_BIN(UUID()), 0x00000000000000000000000000000032, 7, 'POWER_ON_SEAL', '上电封盖', 180, 20.00);
