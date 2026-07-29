package com.example.experiment.dto.experiment;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UnfinishedExperimentVO {
  private String experimentId;
  private String templateName;
  private String category;
  private LocalDateTime startTime;
  private int completedSteps;
  private int totalSteps;
  private String nextStepId;
  private int nextStepOrder;
  private String nextStepName;
}
