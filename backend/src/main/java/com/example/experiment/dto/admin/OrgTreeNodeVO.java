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

  /** 子节点；叶子节点为空数组而非 null，便于前端直接渲染 */
  private List<OrgTreeNodeVO> children = new ArrayList<>();
}
