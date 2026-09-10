package com.example.experiment.dto.admin;

import lombok.Data;

/** 用户列表查询条件。全部字段可选，为空则不参与过滤。 */
@Data
public class UserQueryDTO {

  /** 页码，从 1 开始 */
  private Integer page = 1;

  /** 每页条数 */
  private Integer size = 10;

  /** 姓名，模糊匹配 */
  private String name;

  /** 用户名，模糊匹配 */
  private String username;

  /** 联系电话，模糊匹配 */
  private String phone;

  /**
   * 组织节点 ID。本期前端不传，预留：传了则按 organization.path 前缀过滤该节点及其子节点下的用户。
   *
   * <p>用于后期实现「老师只能看本院系学生」的院系隔离。
   */
  private String orgId;

  /** 页码兜底，防止前端传 0 或负数导致 SQL 报错 */
  public int safePage() {
    return page == null || page < 1 ? 1 : page;
  }

  /** 每页条数兜底；上限由分页插件的 maxLimit 再兜一层 */
  public int safeSize() {
    if (size == null || size < 1) {
      return 10;
    }
    return Math.min(size, 200);
  }
}
