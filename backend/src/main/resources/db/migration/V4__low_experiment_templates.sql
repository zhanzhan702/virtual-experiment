-- V4 插入低压场景实验模板和步骤
INSERT INTO experiment_templates (id, code, name, category, mode, version, description) VALUES
(UUID_TO_BIN(UUID()), 'LV_TRAIN_V1', '低压训练场景V1', 'low_voltage', 'training', '1.0', '低压场景下的用电信息采集终端安装与调试训练模式');

-- 保存模板 ID 供后续步骤引用
SET @tpl_id = (SELECT id FROM experiment_templates WHERE code = 'LV_TRAIN_V1');

-- 步骤 1：填写工作票
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 1, 'FILL_TICKET', '填写工作票', 600, 25.00);
