package com.example.experiment.controller;

import com.example.experiment.dto.auth.ChangePasswordDTO;
import com.example.experiment.dto.auth.LoginDTO;
import com.example.experiment.dto.auth.LoginVO;
import com.example.experiment.dto.auth.RegisterDTO;
import com.example.experiment.dto.auth.UserVO;
import com.example.experiment.entity.Users;
import com.example.experiment.service.UserService;
import com.example.experiment.utils.UserContext;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

  private final UserService userService;

  /** 注册（仅限学生） */
  @PostMapping("/register")
  public ResponseEntity<?> register(@Valid @RequestBody RegisterDTO dto) {
    if (userService.existsByUsername(dto.getUsername())) {
      return ResponseEntity.badRequest().body(Map.of("message", "用户名已存在"));
    }

    var user = userService.register(dto);
    return ResponseEntity.ok(
        Map.of(
            "message", "注册成功",
            "userId", user.getId(),
            "username", user.getUsername()));
  }

  /** 登录 */
  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody LoginDTO dto) {
    try {
      LoginVO vo = userService.login(dto);
      return ResponseEntity.ok(vo);
    } catch (RuntimeException e) {
      return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
    }
  }

  /**
   * 当前登录用户信息。
   *
   * <p>前端刷新页面后 user/roles 内存态丢失，靠本接口恢复（否则路由守卫会把人踢回登录页）。
   *
   * <p>本接口<b>不在免鉴权白名单中</b>，AuthInterceptor 会先校验 token 并写入 UserContext， 这里直接取 userId 即可，无需业务参数。
   */
  @GetMapping("/me")
  public ResponseEntity<?> me() {
    String userId = UserContext.getUserId();
    Users user = userService.findById(userId);
    if (user == null) {
      return ResponseEntity.status(401).body(Map.of("message", "用户不存在"));
    }

    UserVO userVO = userService.toUserVO(user);
    userVO.setMaxLevel(userService.getMaxLevel(userId));

    return ResponseEntity.ok(
        Map.of(
            "user", userVO,
            "roles", userService.getUserRoleCodes(userId),
            "maxLevel", userVO.getMaxLevel()));
  }

  /**
   * 自助改密：修改当前登录用户自己的密码。
   *
   * <p>无 {@code @RequireRole} —— 任何登录用户都能改自己的密码。
   *
   * <p>改密成功后前端会清 token 跳回登录页；服务端不做会话失效处理（JWT 无状态，且无 token_version）。
   */
  @PutMapping("/password")
  public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
    userService.changePassword(UserContext.getUserId(), dto);
    return ResponseEntity.ok(Map.of("message", "密码修改成功"));
  }
}
