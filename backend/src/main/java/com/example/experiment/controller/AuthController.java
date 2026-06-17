package com.example.experiment.controller;

import com.example.experiment.dto.LoginDTO;
import com.example.experiment.dto.LoginVO;
import com.example.experiment.dto.RegisterDTO;
import com.example.experiment.service.UserService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
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
}
