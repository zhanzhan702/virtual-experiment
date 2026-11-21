-- V11 低压居民表箱内部：步骤 5 改为「断电挂牌」，新增步骤 6「挂表接线」、7「上电封盖」
--
-- 幂等：步骤 5 重复执行只更新字段；6/7 已存在时不再插入。
-- 注意（Windows）：
--   mysql --default-character-set=utf8mb4 -u root virtual_experiment < 11_low_resident_box_inner_steps.sql

UPDATE experiment_steps
SET step_code = 'POWER_OFF_SIGN',
    step_name = '断电挂牌',
    required_seconds = 120,
    score = 15.00
WHERE template_id = (SELECT id FROM experiment_templates WHERE code = 'LV_TRAIN_V1')
  AND step_order = 5;

INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score)
SELECT UUID_TO_BIN(UUID()), (SELECT id FROM experiment_templates WHERE code = 'LV_TRAIN_V1'),
       6, 'MOUNT_METER_WIRING', '挂表接线', 300, 30.00
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM experiment_steps
  WHERE template_id = (SELECT id FROM experiment_templates WHERE code = 'LV_TRAIN_V1')
    AND step_order = 6
);

INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score)
SELECT UUID_TO_BIN(UUID()), (SELECT id FROM experiment_templates WHERE code = 'LV_TRAIN_V1'),
       7, 'POWER_ON_SEAL', '上电封盖', 180, 20.00
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM experiment_steps
  WHERE template_id = (SELECT id FROM experiment_templates WHERE code = 'LV_TRAIN_V1')
    AND step_order = 7
);
