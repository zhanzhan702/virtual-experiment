-- V3 插入高压场景实验模板和步骤
INSERT INTO experiment_templates (id, code, name, category, mode, version, description) VALUES
(UUID_TO_BIN(UUID()), 'HV_TRAIN_V1', '高压训练场景V1', 'high_voltage', 'training', '1.0', '高压场景下的用电信息采集终端安装与调试训练模式');

-- 保存模板 ID 供后续步骤引用
SET @tpl_id = (SELECT id FROM experiment_templates WHERE code = 'HV_TRAIN_V1');

-- 步骤 1：填写工作票
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 1, 'FILL_TICKET', '填写工作票', 120, 25.00);

-- 步骤 2：工器具选择
  INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
  (UUID_TO_BIN(UUID()), @tpl_id, 2, 'SELECT_TOOLS', '工器具选择', 180, 30.00);

-- 步骤 3：设置围栏
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 3, 'SET_FENCE', '设置围栏', 60, 5.00);

-- 步骤 4：悬挂标示牌
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 4, 'HANG_SIGN', '悬挂标示牌', 60, 5.00);

-- 步骤 5：三步验电（计量小室）
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 5, 'THREE_STEP_CHECK', '三步验电（计量小室）', 60, 10.00);

-- 步骤 6：挂表
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 6, 'HANG_METER', '挂表', 60, 5.00);

-- 步骤 7：调整接线盒（接线前准备）
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 7, 'ADJUST_JUNCTION_BOX_1', '调整接线盒', 30, 2.00);

-- 步骤 8：连接智能电表与接线盒（7根导线）
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 8, 'WIRE_METER_TO_JUNCTION', '接线：电表与接线盒', 300, 10.00);

-- 步骤 9：连接智能电表与排座（6芯信号线）
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 9, 'WIRE_METER_TO_TERMINAL', '接线：电表与排座', 180, 5.00);

-- 步骤 10：绑扎带指示牌
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 10, 'BIND_CABLE_TIE', '绑扎带指示牌', 60, 2.00);

-- 步骤 11：再次调整接线盒（接线后收尾）
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 11, 'ADJUST_JUNCTION_BOX_2', '再次调整接线盒', 30, 1.00);

-- 步骤 12：打铅封
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 12, 'SEAL', '打铅封', 60, 5.00);

-- 步骤 13：三步验电（终端小室）
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 13, 'THREE_STEP_CHECK', '三步验电（终端小室）', 60, 10.00);
-- TODO: 补充后续步骤 (step_order 从 14 开始)
-- INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
-- (UUID_TO_BIN(UUID()), @tpl_id, 14, '', '后续步骤', 300, 10.00);
