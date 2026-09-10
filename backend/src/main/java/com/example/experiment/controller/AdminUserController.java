package com.example.experiment.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.experiment.config.RequireRole;
import com.example.experiment.config.Role;
import com.example.experiment.dto.admin.UpdatePasswordDTO;
import com.example.experiment.dto.admin.UserDetailVO;
import com.example.experiment.dto.admin.UserListVO;
import com.example.experiment.dto.admin.UserQueryDTO;
import com.example.experiment.service.AdminUserService;
import com.example.experiment.utils.UserContext;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端用户接口。
 *
 * <p>最低门槛为教师：老师也需要查看学生。更细的可见范围（只看得到层级比自己低的用户）在 Service 层按 maxLevel 过滤， 注解只负责挡住未登录与学生的访问。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
@RequireRole(Role.TEACHER)
public class AdminUserController {

  private final AdminUserService adminUserService;

  /** 分页列表：搜索条件可选，返回总数与当前页数据 */
  @GetMapping
  public ResponseEntity<?> listUsers(UserQueryDTO query) {
    IPage<UserListVO> page = adminUserService.listUsers(query, UserContext.getMaxLevel());
    return ResponseEntity.ok(
        Map.of(
            "records", page.getRecords(),
            "total", page.getTotal(),
            "page", page.getCurrent(),
            "size", page.getSize()));
  }

  /** 用户详情（含高/低压实验记录）。越权 403、不存在 404，由 GlobalExceptionHandler 转换 */
  @GetMapping("/{id}")
  public ResponseEntity<?> getUserDetail(@PathVariable String id) {
    UserDetailVO detail = adminUserService.getUserDetail(id, UserContext.getMaxLevel());
    return ResponseEntity.ok(detail);
  }

  /** 重置他人密码。不改动对方已签发的 token，下次登录生效 */
  @PutMapping("/{id}/password")
  public ResponseEntity<?> resetPassword(
      @PathVariable String id, @Valid @RequestBody UpdatePasswordDTO dto) {
    adminUserService.resetPassword(id, dto, UserContext.getMaxLevel());
    return ResponseEntity.ok(Map.of("message", "密码修改成功"));
  }
}
