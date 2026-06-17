package com.example.experiment.service;

import com.example.experiment.dto.experiment.ExperimentStartDTO;
import com.example.experiment.dto.experiment.ExperimentStartVO;
import com.example.experiment.dto.experiment.ExperimentStepSubmitDTO;

public interface ExperimentService {

  /** 开始实验：创建 user_experiments + 初始化所有步骤记录 */
  ExperimentStartVO start(ExperimentStartDTO dto, String userId);

  /** 提交步骤结果 */
  void submitStep(ExperimentStepSubmitDTO dto);
}
