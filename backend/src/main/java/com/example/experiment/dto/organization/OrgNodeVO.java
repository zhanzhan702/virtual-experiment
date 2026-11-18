package com.example.experiment.dto.organization;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 组织架构树节点（注册页选择班级用） */
@Data
public class OrgNodeVO {
  private String id;
  private String name;
  private String type;
  private String parentId;
  private List<OrgNodeVO> children = new ArrayList<>();
}
