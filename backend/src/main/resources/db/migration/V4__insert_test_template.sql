-- V4 插入测试实验模板和步骤

INSERT INTO experiment_templates (id, code, name, category, mode, version, description) VALUES
(UUID_TO_BIN(UUID()), 'high_voltage_1', '高压场景第一版', 'high_voltage', 'training', '1.0', '高压场景下的用电信息采集终端安装与调试');

SET @tpl_id = (SELECT id FROM experiment_templates WHERE code = 'high_voltage_1');

INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 1, 'step_01', '填写工作票', 600, 25.00),
(UUID_TO_BIN(UUID()), @tpl_id, 2, 'step_02', '工作负责人布置工作任务', 300, 10.00),
(UUID_TO_BIN(UUID()), @tpl_id, 3, 'step_03', '选择工器具', 600, 20.00),
(UUID_TO_BIN(UUID()), @tpl_id, 4, 'step_04', '计量柜操作', 900, 25.00),
(UUID_TO_BIN(UUID()), @tpl_id, 5, 'step_05', '计量小室接线操作', 1200, 20.00);
