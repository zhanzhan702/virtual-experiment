package com.example.experiment.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 同级排序调整请求体。 */
@Data
public class MoveNodeDTO {

  /** UP = 与前一个兄弟交换；DOWN = 与后一个兄弟交换 */
  @NotNull(message = "请指定移动方向")
  private Direction direction;

  public enum Direction {
    UP,
    DOWN
  }
}
