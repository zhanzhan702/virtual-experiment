package com.example.experiment.service;

import com.example.experiment.dto.admin.MoveNodeDTO;
import com.example.experiment.dto.admin.OrgNodeDTO;
import com.example.experiment.dto.admin.OrgTreeNodeVO;
import java.util.List;

/**
 * 组织架构服务。
 *
 * <p>层级由 {@code type} 表达，子节点类型由父节点推导（见设计文档第二节）。 path 是「查看学生成绩」按节点筛选的依据，重命名时必须级联更新整棵子树。
 */
public interface AdminOrgService {

  /** 完整组织架构树（5 级嵌套），每个节点带挂靠用户数 */
  List<OrgTreeNodeVO> getOrgTree();

  /** 新增节点；parentId 为空表示新增根节点（学校）。返回新节点 ID */
  String createNode(OrgNodeDTO dto);

  /** 重命名节点，并级联更新所有子孙的 path */
  void renameNode(String id, OrgNodeDTO dto);

  /** 删除节点；有子节点或有用户挂靠时拒绝 */
  void deleteNode(String id);

  /** 与相邻兄弟交换排序 */
  void moveNode(String id, MoveNodeDTO dto);
}
