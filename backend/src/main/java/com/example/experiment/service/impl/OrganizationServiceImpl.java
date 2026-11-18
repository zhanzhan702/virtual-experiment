package com.example.experiment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.experiment.dto.organization.OrgNodeVO;
import com.example.experiment.entity.Organization;
import com.example.experiment.mapper.OrganizationMapper;
import com.example.experiment.service.OrganizationService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

  private final OrganizationMapper organizationMapper;

  @Override
  public List<OrgNodeVO> getTree() {
    List<Organization> all =
        organizationMapper.selectList(
            new LambdaQueryWrapper<Organization>().orderByAsc(Organization::getSort));

    // 先建节点，再按 parentId 组装为树
    Map<String, OrgNodeVO> nodeMap = new LinkedHashMap<>();
    for (Organization org : all) {
      OrgNodeVO node = new OrgNodeVO();
      node.setId(org.getId());
      node.setName(org.getName());
      node.setType(org.getType());
      node.setParentId(org.getParentId());
      nodeMap.put(org.getId(), node);
    }

    List<OrgNodeVO> roots = new ArrayList<>();
    for (OrgNodeVO node : nodeMap.values()) {
      String parentId = node.getParentId();
      if (parentId == null || parentId.isBlank() || !nodeMap.containsKey(parentId)) {
        roots.add(node);
      } else {
        nodeMap.get(parentId).getChildren().add(node);
      }
    }
    return roots;
  }

  @Override
  public Organization findById(String id) {
    return organizationMapper.selectById(id);
  }
}
