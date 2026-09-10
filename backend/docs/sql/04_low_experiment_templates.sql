-- V4 插入低压场景实验模板和步骤
--
-- 注意：本脚本不使用会话变量（SET @var），模板 ID 用固定 UUID 字面量。
-- 会话变量是连接级的，逐条新开连接执行时会丢失，导致 template_id 写入 NULL
-- （experiment_steps.template_id 为 NOT NULL，会直接报错）。
--
-- 固定 UUID 分配表（与 02_create_data.sql 统一编号）：
--   32 低压训练场景V1（LV_TRAIN_V1）

INSERT INTO experiment_templates (id, code, name, category, mode, version, description) VALUES
(0x00000000000000000000000000000032, 'LV_TRAIN_V1', '低压训练场景V1', 'low_voltage', 'training', '1.0', '低压场景下的用电信息采集终端安装与调试训练模式');

-- 步骤 1：填写工作票
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), 0x00000000000000000000000000000032, 1, 'FILL_TICKET', '填写工作票', 300, 25.00);
