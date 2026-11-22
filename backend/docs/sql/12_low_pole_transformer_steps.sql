-- V12 低压杆上变外部场景：新增步骤 8「杆上变架设围栏并悬挂标识牌」、9「杆上变三步式验电」
--
-- 幂等：8/9 已存在时不再插入。
-- 注意（Windows）：
--   mysql --default-character-set=utf8mb4 -u root virtual_experiment < 12_low_pole_transformer_steps.sql

INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score)
SELECT UUID_TO_BIN(UUID()), (SELECT id FROM experiment_templates WHERE code = 'LV_TRAIN_V1'),
       8, 'POLE_ERECT_FENCE', '杆上变架设围栏并悬挂标识牌', 120, 20.00
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM experiment_steps
  WHERE template_id = (SELECT id FROM experiment_templates WHERE code = 'LV_TRAIN_V1')
    AND step_order = 8
);

INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score)
SELECT UUID_TO_BIN(UUID()), (SELECT id FROM experiment_templates WHERE code = 'LV_TRAIN_V1'),
       9, 'POLE_THREE_STEP_CHECK', '杆上变三步式验电', 90, 15.00
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM experiment_steps
  WHERE template_id = (SELECT id FROM experiment_templates WHERE code = 'LV_TRAIN_V1')
    AND step_order = 9
);
