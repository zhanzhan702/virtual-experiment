package com.example.experiment.dto.admin;

import lombok.Data;

/** 用户列表行。字段与控制台表格列一一对应。 */
@Data
public class UserListVO {

  private String id;
  private String username;
  private String name;
  private String gender;

  /** 所属组织节点 ID。成绩页按班级归组时使用；用户管理页不展示 */
  private String orgId;

  /** 单位/班级，后端拼好的 organization.path 完整路径，前端不自行拼接 */
  private String orgName;

  private String phone;
}
