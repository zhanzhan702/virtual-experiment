package com.example.experiment.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 用户详情弹窗中的单条实验记录。 */
@Data
public class UserExperimentVO {

  private String experimentId;

  /** 模板名，如「高压训练场景V1」 */
  private String templateName;

  /** high_voltage / low_voltage，前端据此显示不同类型标签 */
  private String category;

  /** 0 进行中 / 1 完成 */
  private Integer status;

  private BigDecimal score;

  private LocalDateTime startTime;

  private LocalDateTime endTime;

  /** 总用时（秒） */
  private Integer totalDuration;
}
