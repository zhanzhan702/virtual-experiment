package com.example.experiment.dto;

import lombok.Data;

@Data
public class ExperimentStartDTO {
  private String templateId; // 按 ID 启动
  private String templateCode; // 按编码启动（如 HV_TRAIN_V1）
}
