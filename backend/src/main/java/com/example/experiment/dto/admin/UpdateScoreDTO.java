package com.example.experiment.dto.admin;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 人工改分请求体。
 *
 * <p>分数为**百分制**，与界面展示、教师输入的单位一致。 传 {@code manualScore: null} 表示撤销人工改分，回到系统分。
 */
@Data
public class UpdateScoreDTO {

  @DecimalMin(value = "0", message = "分数不能小于 0")
  @DecimalMax(value = "100", message = "分数不能大于 100")
  private BigDecimal manualScore;
}
