package com.example.experiment.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 新增 / 重命名组织节点的请求体。 */
@Data
public class OrgNodeDTO {

  /** 新增时的父节点 ID；为空表示新增根节点（学校）。重命名时忽略此字段 */
  private String parentId;

  /**
   * 节点名称。
   *
   * <p>禁止包含 {@code /} —— path 以斜杠分段，名称里带斜杠会破坏分段结构且无法与分隔符区分。 见设计文档 3.3。
   */
  @NotBlank(message = "名称不能为空")
  @Pattern(regexp = "^[^/]+$", message = "名称不能包含斜杠 /")
  private String name;
}
