package com.example.experiment.service.impl;

import com.example.experiment.dto.admin.MoveNodeDTO;
import com.example.experiment.dto.admin.OrgNodeDTO;
import com.example.experiment.dto.admin.OrgTreeNodeVO;
import com.example.experiment.dto.admin.OrgUserCountVO;
import com.example.experiment.entity.Organization;
import com.example.experiment.exception.ApiException;
import com.example.experiment.mapper.OrganizationMapper;
import com.example.experiment.service.AdminOrgService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminOrgServiceImpl implements AdminOrgService {

  private final OrganizationMapper organizationMapper;

  /**
   * 层级约束：父节点 type → 允许的子节点 type。
   *
   * <p>用映射表而非 if 链，加层级时只改这一处。{@code null} 键代表根节点（无父级）。
   */
  private static final Map<String, String> CHILD_TYPE =
      Map.of(
          "university", "college",
          "college", "major",
          "major", "grade",
          "grade", "class");

  /** type → 中文名，用于错误提示 */
  private static final Map<String, String> TYPE_LABEL =
      Map.of(
          "university", "学校",
          "college", "学院",
          "major", "专业",
          "grade", "年级",
          "class", "班级");

  // ──────────────────────────── 建树 ────────────────────────────

  @Override
  public List<OrgTreeNodeVO> getOrgTree() {
    List<Organization> all = organizationMapper.selectAllOrdered();

    // 一次查出各节点挂靠用户数，避免逐节点查库。
    // 键统一转小写：SQL 的 HEX() 返回大写，而 UUIDTypeHandler 读出的 id 是小写
    Map<String, Integer> userCounts = new LinkedHashMap<>();
    for (OrgUserCountVO row : organizationMapper.countUsersGroupedByOrg()) {
      if (row.getOrgId() != null && row.getUserCount() != null) {
        userCounts.put(row.getOrgId().toLowerCase(), row.getUserCount());
      }
    }

    Map<String, OrgTreeNodeVO> byId = new LinkedHashMap<>();
    for (Organization org : all) {
      OrgTreeNodeVO node = new OrgTreeNodeVO();
      node.setId(org.getId());
      node.setName(org.getName());
      node.setType(org.getType());
      node.setUserCount(userCounts.getOrDefault(org.getId().toLowerCase(), 0));
      byId.put(org.getId(), node);
    }

    List<OrgTreeNodeVO> roots = new ArrayList<>();
    for (Organization org : all) {
      OrgTreeNodeVO node = byId.get(org.getId());
      OrgTreeNodeVO parent = org.getParentId() == null ? null : byId.get(org.getParentId());
      if (parent == null) {
        // 无父节点（或被引用的父节点不存在）视为根
        roots.add(node);
      } else {
        parent.getChildren().add(node);
      }
    }
    return roots;
  }

  // ──────────────────────────── 新增 ────────────────────────────

  @Override
  @Transactional
  public String createNode(OrgNodeDTO dto) {
    String name = normalizeName(dto.getName());
    String parentId = dto.getParentId();

    Organization parent = null;
    if (parentId != null && !parentId.isBlank()) {
      parent = organizationMapper.selectById(parentId);
      if (parent == null) {
        throw ApiException.notFound("父节点不存在");
      }
    }

    String type = resolveChildType(parent);
    requireNameAvailable(parentId, name, null);

    Organization node = new Organization();
    node.setName(name);
    node.setType(type);
    node.setParentId(parentId == null || parentId.isBlank() ? null : parentId);
    node.setPath(parent == null ? "/" + name + "/" : parent.getPath() + name + "/");
    // 排到同级末尾；selectMaxSiblingSort 在同级无节点时返回 null
    Integer maxSort = organizationMapper.selectMaxSiblingSort(node.getParentId());
    node.setSort(maxSort == null ? 1 : maxSort + 1);

    organizationMapper.insert(node);
    return node.getId();
  }

  /**
   * 由父节点推导子节点类型。
   *
   * <p>班级是叶子节点，不能再加子节点 —— 否则会出现「班级下的班级」这种无意义的层级。
   */
  private String resolveChildType(Organization parent) {
    if (parent == null) {
      return "university";
    }
    String childType = CHILD_TYPE.get(parent.getType());
    if (childType == null) {
      throw ApiException.badRequest(
          "「" + parent.getName() + "」是" + label(parent.getType()) + "，不能再添加子节点");
    }
    return childType;
  }

  // ──────────────────────────── 重命名 ────────────────────────────

  @Override
  @Transactional
  public void renameNode(String id, OrgNodeDTO dto) {
    Organization node = requireNode(id);
    String newName = normalizeName(dto.getName());

    if (newName.equals(node.getName())) {
      return; // 名称未变，无需处理（也避免无谓的 path 级联）
    }
    requireNameAvailable(node.getParentId(), newName, id);

    String oldPath = node.getPath();
    String newPath = buildPath(node.getParentId(), newName);

    Organization update = new Organization();
    update.setId(id);
    update.setName(newName);
    update.setPath(newPath);
    organizationMapper.updateById(update);

    // 级联：子孙的 path 仍以旧名字开头，必须整体替换前缀，否则成绩页按新名字筛选会漏掉它们
    if (oldPath != null && !oldPath.equals(newPath)) {
      organizationMapper.replacePathPrefix(oldPath, newPath, id);
    }
  }

  /** 由父节点 path 拼出新节点的 path；父节点为空表示根 */
  private String buildPath(String parentId, String name) {
    if (parentId == null || parentId.isBlank()) {
      return "/" + name + "/";
    }
    Organization parent = organizationMapper.selectById(parentId);
    if (parent == null || parent.getPath() == null) {
      throw ApiException.notFound("父节点不存在");
    }
    return parent.getPath() + name + "/";
  }

  // ──────────────────────────── 删除 ────────────────────────────

  @Override
  @Transactional
  public void deleteNode(String id) {
    Organization node = requireNode(id);

    // 两个外键（parent_id 自引用、users.org_id）会挡住删除，
    // 但数据库抛的是外键约束异常，前端只能看到一串英文。这里先校验并给出可读原因。
    int children = organizationMapper.countChildren(id);
    if (children > 0) {
      throw ApiException.badRequest("「" + node.getName() + "」下还有 " + children + " 个子节点，请先删除子节点");
    }

    int users = organizationMapper.countUsers(id);
    if (users > 0) {
      throw ApiException.badRequest("「" + node.getName() + "」下还有 " + users + " 名用户，请先调整归属");
    }

    organizationMapper.deleteById(id);
  }

  // ──────────────────────────── 排序 ────────────────────────────

  @Override
  @Transactional
  public void moveNode(String id, MoveNodeDTO dto) {
    Organization node = requireNode(id);
    boolean up = dto.getDirection() == MoveNodeDTO.Direction.UP;

    Organization sibling =
        organizationMapper.selectAdjacentSibling(node.getParentId(), id, node.getSort(), up);

    if (sibling == null) {
      throw ApiException.badRequest(up ? "已经是第一个，无法上移" : "已经是最后一个，无法下移");
    }

    // 交换两者的 sort；不改动其他兄弟，改动面最小
    int selfSort = node.getSort();
    int siblingSort = sibling.getSort();

    Organization selfUpdate = new Organization();
    selfUpdate.setId(id);
    selfUpdate.setSort(siblingSort);
    organizationMapper.updateById(selfUpdate);

    Organization siblingUpdate = new Organization();
    siblingUpdate.setId(sibling.getId());
    siblingUpdate.setSort(selfSort);
    organizationMapper.updateById(siblingUpdate);
  }

  // ──────────────────────────── 校验 ────────────────────────────

  private Organization requireNode(String id) {
    Organization node = id == null ? null : organizationMapper.selectById(id);
    if (node == null) {
      throw ApiException.notFound("组织节点不存在");
    }
    return node;
  }

  /** 去掉首尾空白；空串交由 @NotBlank 拦，此处再兜一层（重命名路径不走 Bean Validation 之外的分支） */
  private String normalizeName(String name) {
    String trimmed = name == null ? "" : name.trim();
    if (trimmed.isEmpty()) {
      throw ApiException.badRequest("名称不能为空");
    }
    return trimmed;
  }

  /** 同级不允许重名（跨级允许，不同年级下都可以有「1班」） */
  private void requireNameAvailable(String parentId, String name, String excludeId) {
    String normalizedParent = parentId == null || parentId.isBlank() ? null : parentId;
    if (organizationMapper.countSiblingsWithName(normalizedParent, name, excludeId) > 0) {
      throw ApiException.badRequest("同级下已存在名为「" + name + "」的节点");
    }
  }

  private String label(String type) {
    return TYPE_LABEL.getOrDefault(type, type);
  }
}
