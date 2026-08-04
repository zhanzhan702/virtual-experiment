package com.example.experiment.service;

import com.example.experiment.dto.experiment.ExperimentStartDTO;
import com.example.experiment.dto.experiment.ExperimentStartVO;
import com.example.experiment.dto.experiment.ExperimentStepSubmitDTO;
import com.example.experiment.dto.experiment.UnfinishedExperimentVO;
import java.util.List;
import java.util.Map;

public interface ExperimentService {

  /** 开始实验：创建 user_experiments + 初始化所有步骤记录 */
  ExperimentStartVO start(ExperimentStartDTO dto, String userId);

  /** 提交步骤结果（最终提交，更新评分和完成状态） */
  void submitStep(ExperimentStepSubmitDTO dto);

  /** 保存步骤草稿（仅持久化当前数据，不评分、不设完成时间） */
  void saveDraft(ExperimentStepSubmitDTO dto);

  /** 查询用户所有未完成实验 */
  List<UnfinishedExperimentVO> getUnfinishedExperiments(String userId);

  /** 删除实验及其所有步骤记录（级联删除） */
  void deleteExperiment(String experimentId);

  /** 获取实验某步骤的草稿数据（用于恢复页面状态） */
  Map<String, Object> getStepDraftData(String experimentId, String stepId);

  /** 获取实验总耗时（所有步骤 duration_seconds 之和，含草稿，用于存档恢复后累加计时） */
  Integer getTotalDuration(String experimentId);

  /** 获取实验的步骤列表（含 stepId/stepOrder/stepName，恢复时重建前端步骤映射） */
  List<Map<String, Object>> getExperimentSteps(String experimentId);
}
