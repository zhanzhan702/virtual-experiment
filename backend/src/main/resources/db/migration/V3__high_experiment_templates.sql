-- V3 插入高压场景实验模板和步骤
INSERT INTO experiment_templates (id, code, name, category, mode, version, description) VALUES
(UUID_TO_BIN(UUID()), 'HV_TRAIN_V1', '高压训练场景V1', 'high_voltage', 'training', '1.0', '高压场景下的用电信息采集终端安装与调试训练模式');

-- 保存模板 ID 供后续步骤引用
SET @tpl_id = (SELECT id FROM experiment_templates WHERE code = 'HV_TRAIN_V1');

-- 步骤 1：填写工作票
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 1, 'FILL_TICKET', '填写工作票', 120, 20.00);

-- 步骤 2：工器具选择
  INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
  (UUID_TO_BIN(UUID()), @tpl_id, 2, 'SELECT_TOOLS', '工器具选择', 180, 30.00);

-- 步骤 3：架设围栏并悬挂标示牌
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 3, 'SET_FENCE_AND_HANG_SIGN', '架设围栏并悬挂标示牌', 60, 5.00);

-- 步骤 4：三步验电（计量小室）
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 4, 'THREE_STEP_CHECK', '三步验电（计量小室）', 60, 3.00);

-- 步骤 5：挂电表（计量小室）
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 5, 'HANG_METER_METERING', '挂电表（计量小室）', 30, 1.00);

-- 步骤 6：接线盒处理（计量小室第一次）
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 6, 'ADJUST_JUNCTION_BOX_METERING', '接线盒处理（计量小室第一次）', 120, 10.00);


-- 步骤 7：接电压、电流进出线（计量小室，7根导线）
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 7, 'WIRE_VOLTAGE_CURRENT_METERING', '接电压电流进出线（计量小室）', 180, 15.00);

-- 步骤 8：6芯信号线连接（计量小室）
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 8, 'WIRE_SIGNAL_6CORE_METERING', '6芯信号线连接（计量小室）', 240, 30.00);

-- 步骤 9：添加扎带标识牌（计量小室）
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 9, 'BIND_CABLE_TIE_METERING', '添加扎带标识牌（计量小室）', 30, 1.00);

-- 步骤 10：接线盒处理（计量小室第二次）
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 10, 'ADJUST_JUNCTION_BOX_METERING_2', '接线盒处理（计量小室第二次）', 120, 10.00);

-- 步骤 11：计量小室加铅封
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 11, 'SEAL_METERING_ROOM', '计量小室加铅封', 90, 5.00);

-- 步骤 12：三步验电（终端小室）
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 12, 'THREE_STEP_CHECK_TERMINAL', '三步验电（终端小室）', 60, 5.00);

-- 步骤 13：挂表（终端小室）
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 13, 'HANG_TERMINAL_TERMINAL', '挂表（终端小室）', 30, 1.00);

-- 步骤 14：接线盒处理（终端小室）
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 14, 'ADJUST_JUNCTION_BOX_TERMINAL', '接线盒处理（终端小室）', 90, 5.00);

-- 步骤 15：接电压、电流进出线（终端小室，7根导线）
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 15, 'WIRE_VOLTAGE_CURRENT_TERMINAL', '接电压电流进出线（终端小室）', 120, 10.00);

-- 步骤 16：遥控压板处理
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 16, 'ADJUST_REMOTE_CONTROL', '遥控压板处理', 90, 4.00);

-- 步骤 17：终端侧信号线连接（2芯遥控、2芯遥信、8芯信号线）
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 17, 'WIRE_SIGNAL_TERMINAL', '终端侧信号线连接', 300, 40.00);

-- 步骤 18：安装通信模块、SIM卡、天线
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 18, 'INSTALL_COMM_MODULE', '安装通信模块/SIM/天线', 90, 5.00);

-- 步骤 19：绑扎带指示牌（终端小室）
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 19, 'BIND_CABLE_TIE_TERMINAL', '绑扎带指示牌（终端小室）', 60, 3.00);

-- 步骤 20：上电（合闸）
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 20, 'POWER_ON', '上电（合闸）', 30, 1.00);

-- 步骤 21：终端小室加铅封【新增】
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 21, 'SEAL_TERMINAL_ROOM', '终端小室加铅封', 90, 5.00);

-- 步骤 22：柜门门把加铅封
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 22, 'SEAL_CABINET', '柜门门把加铅封', 60, 2.00);

-- 步骤 23：清理现场并办理工作票终结
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 23, 'CLEAN_SITE_AND_FINALIZE_TICKET', '清理现场并办理工作票终结', 60, 2.00);

-- 完整重建（请先确认模板 ID）
SET @tpl_id = (SELECT id FROM experiment_templates WHERE code = 'HV_TRAIN_V1');

-- 清空该模板下原有步骤（谨慎！）
DELETE FROM experiment_steps WHERE template_id = @tpl_id;


-- 一次性全部插入
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score) VALUES
(UUID_TO_BIN(UUID()), @tpl_id, 1, 'FILL_TICKET', '填写工作票', 120, 20.00),
(UUID_TO_BIN(UUID()), @tpl_id, 2, 'SELECT_TOOLS', '工器具选择', 180, 30.00),
(UUID_TO_BIN(UUID()), @tpl_id, 3, 'SET_FENCE_AND_HANG_SIGN', '架设围栏并悬挂标示牌', 60, 5.00),
(UUID_TO_BIN(UUID()), @tpl_id, 4, 'THREE_STEP_CHECK', '三步验电（计量小室）', 60, 5.00),
(UUID_TO_BIN(UUID()), @tpl_id, 5, 'HANG_METER_METERING', '挂电表（计量小室）', 30, 1.00),
(UUID_TO_BIN(UUID()), @tpl_id, 6, 'ADJUST_JUNCTION_BOX_METERING', '接线盒处理（计量小室第一次）', 120, 10.00),
(UUID_TO_BIN(UUID()), @tpl_id, 7, 'WIRE_VOLTAGE_CURRENT_METERING', '接电压电流进出线（计量小室）', 180, 15.00),
(UUID_TO_BIN(UUID()), @tpl_id, 8, 'WIRE_SIGNAL_6CORE_METERING', '6芯信号线连接（计量小室）', 240, 30.00),
(UUID_TO_BIN(UUID()), @tpl_id, 9, 'BIND_CABLE_TIE_METERING', '添加扎带标识牌（计量小室）', 30, 1.00),
(UUID_TO_BIN(UUID()), @tpl_id, 10, 'ADJUST_JUNCTION_BOX_METERING_2', '接线盒处理（计量小室第二次）', 120, 10.00),
(UUID_TO_BIN(UUID()), @tpl_id, 11, 'SEAL_METERING_ROOM', '计量小室加铅封', 90, 5.00),
(UUID_TO_BIN(UUID()), @tpl_id, 12, 'THREE_STEP_CHECK_TERMINAL', '三步验电（终端小室）', 60, 5.00),
(UUID_TO_BIN(UUID()), @tpl_id, 13, 'HANG_TERMINAL_TERMINAL', '挂表（终端小室）', 30, 1.00),
(UUID_TO_BIN(UUID()), @tpl_id, 14, 'ADJUST_JUNCTION_BOX_TERMINAL', '接线盒处理（终端小室）', 90, 5.00),
(UUID_TO_BIN(UUID()), @tpl_id, 15, 'WIRE_VOLTAGE_CURRENT_TERMINAL', '接电压电流进出线（终端小室）', 120, 10.00),
(UUID_TO_BIN(UUID()), @tpl_id, 16, 'ADJUST_REMOTE_CONTROL', '遥控压板处理', 90, 4.00),
(UUID_TO_BIN(UUID()), @tpl_id, 17, 'WIRE_SIGNAL_TERMINAL', '终端侧信号线连接', 300, 40.00),
(UUID_TO_BIN(UUID()), @tpl_id, 18, 'INSTALL_COMM_MODULE', '安装通信模块/SIM/天线', 90, 5.00),
(UUID_TO_BIN(UUID()), @tpl_id, 19, 'BIND_CABLE_TIE_TERMINAL', '绑扎带指示牌（终端小室）', 60, 3.00),
(UUID_TO_BIN(UUID()), @tpl_id, 20, 'POWER_ON', '上电（合闸）', 30, 1.00),
(UUID_TO_BIN(UUID()), @tpl_id, 21, 'SEAL_TERMINAL_ROOM', '终端小室加铅封', 90, 5.00),
(UUID_TO_BIN(UUID()), @tpl_id, 22, 'SEAL_CABINET', '柜门门把加铅封', 60, 2.00),
(UUID_TO_BIN(UUID()), @tpl_id, 23, 'CLEAN_SITE_AND_FINALIZE_TICKET', '清理现场并办理工作票终结', 60, 2.00);

