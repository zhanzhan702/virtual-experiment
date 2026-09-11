package com.example.experiment.dto.admin;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 组织架构树节点。用于「查看学生成绩」与「专业班级管理」的左侧树。 */
@Data
public class OrgTreeNodeVO {

  private String id;
  private String name;

  /** university / college / major / grade / class */
  private String type;

  /** 挂靠在该节点下的用户数。用于删除前提示「该班级下还有 N 名学生」 */
  private Integer userCount = 0;

  /** 子节点；叶子节点为空数组而非 null，便于前端直接渲染 */
  private List<OrgTreeNodeVO> children = new ArrayList<>();
}
