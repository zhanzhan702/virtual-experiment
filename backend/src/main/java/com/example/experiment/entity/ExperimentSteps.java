package com.example.experiment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("experiment_steps")
public class ExperimentSteps {
  @TableId(type = IdType.ASSIGN_UUID)
  private String id;

  private String templateId;
  private Integer stepOrder;
  private String stepCode;
  private String stepName;
  private Integer requiredSeconds;
  private BigDecimal score;
  private LocalDateTime createdAt;
}
