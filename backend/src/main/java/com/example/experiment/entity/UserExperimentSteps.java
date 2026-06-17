package com.example.experiment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user_experiment_steps")
public class UserExperimentSteps {
  @TableId(type = IdType.ASSIGN_UUID)
  private String id;

  private String experimentId;
  private String stepId;
  private Integer status;
  private Integer durationSeconds;
  private Integer operationCount;
  private Integer errorCount;
  private BigDecimal score;
  private String resultData; // JSON string
  private LocalDateTime startedAt;
  private LocalDateTime finishedAt;
  private LocalDateTime createdAt;
}
