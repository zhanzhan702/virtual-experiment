package com.example.experiment.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 实验启动响应 */
@Data
public class ExperimentStartVO {
  private String experimentId;
  private String templateName;
  private LocalDateTime startTime;
  private List<StepBrief> steps;

  @Data
  public static class StepBrief {
    private String stepId;
    private String stepName;
    private Integer stepOrder;
  }
}
