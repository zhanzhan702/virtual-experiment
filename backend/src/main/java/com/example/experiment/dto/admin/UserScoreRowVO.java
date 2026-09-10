package com.example.experiment.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 分数原始行，不直接对外返回。成绩汇总与完成历史共用。
 *
 * <p>一个学生在一个类别下可能有多条已完成记录，SQL 把原始行全部取回， 由 Service 换算成百分制后按「取最高」的规则挑出最终展示值。 之所以不在 SQL 里做
 * MAX：需要同时知道「最高那条是不是人工改分」，在 Java 里判定更清晰。
 */
@Data
public class UserScoreRowVO {

  private String userId;
  private String experimentId;

  /** high_voltage / low_voltage */
  private String category;

  private String templateName;

  /** 0 进行中 / 1 完成 */
  private Integer status;

  /** 系统原始加权分 */
  private BigDecimal rawScore;

  /** 人工改分（百分制），未改分为 null */
  private BigDecimal manualScore;

  /** 该模板的步骤分合计，作为换算分母；为 0 或 null 时无法换算 */
  private BigDecimal stepTotal;

  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private Integer totalDuration;
  private LocalDateTime scoredAt;
}
