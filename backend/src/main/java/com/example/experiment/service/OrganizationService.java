package com.example.experiment.service;

import com.example.experiment.dto.organization.OrgNodeVO;
import com.example.experiment.entity.Organization;
import java.util.List;

public interface OrganizationService {

  /** 获取完整的组织架构树（大学→学院→专业→年级→班级） */
  List<OrgNodeVO> getTree();

  /** 按 ID 查询组织节点 */
  Organization findById(String id);
}
