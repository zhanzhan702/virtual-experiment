package com.example.experiment.controller;

import com.example.experiment.config.RequireRole;
import com.example.experiment.config.Role;
import com.example.experiment.dto.admin.MoveNodeDTO;
import com.example.experiment.dto.admin.OrgNodeDTO;
import com.example.experiment.service.AdminOrgService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 组织架构接口。
 *
 * <p>类级门槛为管理员 —— 改动组织架构影响全局。但读树要放行老师（「查看学生成绩」页需要）， 因此 {@code getOrgTree}
 * 上单独标了方法级注解；拦截器按「方法级优先于类级」处理。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/org")
@RequireRole(Role.ADMIN)
public class AdminOrgController {

  private final AdminOrgService adminOrgService;

  /** 完整组织架构树；老师也可读 */
  @GetMapping("/tree")
  @RequireRole(Role.TEACHER)
  public ResponseEntity<?> getOrgTree() {
    return ResponseEntity.ok(adminOrgService.getOrgTree());
  }

  /** 新增节点；parentId 为空表示新增根节点（学校） */
  @PostMapping("/nodes")
  public ResponseEntity<?> createNode(@Valid @RequestBody OrgNodeDTO dto) {
    String id = adminOrgService.createNode(dto);
    return ResponseEntity.ok(Map.of("message", "新增成功", "id", id));
  }

  /** 重命名节点，子孙 path 会级联更新 */
  @PutMapping("/nodes/{id}")
  public ResponseEntity<?> renameNode(@PathVariable String id, @Valid @RequestBody OrgNodeDTO dto) {
    adminOrgService.renameNode(id, dto);
    return ResponseEntity.ok(Map.of("message", "保存成功"));
  }

  /** 删除节点；有子节点或有用户挂靠时拒绝 */
  @DeleteMapping("/nodes/{id}")
  public ResponseEntity<?> deleteNode(@PathVariable String id) {
    adminOrgService.deleteNode(id);
    return ResponseEntity.ok(Map.of("message", "删除成功"));
  }

  /** 与相邻兄弟交换排序 */
  @PostMapping("/nodes/{id}/move")
  public ResponseEntity<?> moveNode(@PathVariable String id, @Valid @RequestBody MoveNodeDTO dto) {
    adminOrgService.moveNode(id, dto);
    return ResponseEntity.ok(Map.of("message", "排序已调整"));
  }
}
