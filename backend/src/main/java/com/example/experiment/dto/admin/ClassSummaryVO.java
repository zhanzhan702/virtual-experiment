package com.example.experiment.dto.admin;

import java.math.BigDecimal;
import lombok.Data;

/** 班级维度的成绩汇总。选中组织树任意节点时，列出其下所有班级的这一结构。 */
@Data
public class ClassSummaryVO {

  private String orgId;

  /**
   * 后端拼接的完整班级名，如「2025级电气工程及其自动化1班」。
   *
   * <p>库里班级名只有「1班」，脱离上下文无法辨识，故由后端向上回溯父级名称拼接。 参照原仿真后台的命名风格。前端不自行拼接。
   */
  private String className;

  /** 完整组织路径，供 tooltip 展示 */
  private String orgName;

  /** 班级学生人数 */
  private Integer studentCount;

  /** 高压已完成人数 */
  private Integer highDone;

  /** 低压已完成人数 */
  private Integer lowDone;

  /** 全班有效显示分的算术平均（百分制）；无有效分时为 null */
  private BigDecimal avgScore;
}
