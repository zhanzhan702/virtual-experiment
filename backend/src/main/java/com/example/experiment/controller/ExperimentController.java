package com.example.experiment.controller;

import com.example.experiment.dto.experiment.ExperimentStartDTO;
import com.example.experiment.dto.experiment.ExperimentStepSubmitDTO;
import com.example.experiment.service.ExperimentService;
import com.example.experiment.utils.JwtUtils;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/experiment")
public class ExperimentController {

  private final ExperimentService experimentService;

  /** 从 Authorization 头中提取 userId */
  private String getUserIdFromAuth(String auth) {
    if (auth != null && auth.startsWith("Bearer ")) {
      String token = auth.substring(7);
      if (JwtUtils.validateToken(token)) {
        return JwtUtils.getUserId(token);
      }
    }
    return null;
  }

  /** 启动实验 */
  @PostMapping("/start")
  public ResponseEntity<?> start(
      @RequestBody ExperimentStartDTO dto,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    String userId = getUserIdFromAuth(auth);
    if (userId == null) {
      return ResponseEntity.status(401).body(Map.of("message", "未登录或 token 无效"));
    }
    try {
      var vo = experimentService.start(dto, userId);
      return ResponseEntity.ok(vo);
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

  /** 提交步骤 */
  @PostMapping("/step/submit")
  public ResponseEntity<?> submitStep(
      @RequestBody ExperimentStepSubmitDTO dto,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    String userId = getUserIdFromAuth(auth);
    if (userId == null) {
      return ResponseEntity.status(401).body(Map.of("message", "未登录或 token 无效"));
    }
    try {
      experimentService.submitStep(dto);
      return ResponseEntity.ok(Map.of("message", "提交成功"));
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }
}
