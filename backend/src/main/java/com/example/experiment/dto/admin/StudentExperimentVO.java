package com.example.experiment.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 学生完成历史中的单条实验记录。 */
@Data
public class StudentExperimentVO {

  private String experimentId;
  private String templateName;

  /** high_voltage / low_voltage */
  private String category;

  /** 0 进行中 / 1 完成 */
  private Integer status;

  /** 系统原始加权分（量纲 = 模板步骤分合计），仅供参考 */
  private BigDecimal rawScore;

  /** 换算后的百分制分；未完成或模板无步骤分时为 null */
  private BigDecimal percentScore;

  /** 人工改分（百分制）；未改分时为 null */
  private BigDecimal manualScore;

  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private Integer totalDuration;

  /** 改分时间；未改分时为 null */
  private LocalDateTime scoredAt;
}
