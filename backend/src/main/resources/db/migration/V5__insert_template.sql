-- V5 插入测试模板（直接写入 UUID）
INSERT INTO experiment_templates (id, code, name, category, mode, version, description) VALUES
(UUID_TO_BIN('a1b2c3d4-e5f6-7890-abcd-ef1234567890'), 'high_voltage_1', '高压场景第一版', 'high_voltage', 'training', '1.0', '高压场景下的用电信息采集终端安装与调试');

INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN('b1b2c3d4-e5f6-7890-abcd-ef1234567891'), UUID_TO_BIN('a1b2c3d4-e5f6-7890-abcd-ef1234567890'), 1, 'step_01', '填写工作票', 600, 25.00);
