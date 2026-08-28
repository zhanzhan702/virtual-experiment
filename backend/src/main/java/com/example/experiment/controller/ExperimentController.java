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

  /** 保存进度草稿 */
  @PostMapping("/step/draft")
  public ResponseEntity<?> saveDraft(
      @RequestBody ExperimentStepSubmitDTO dto,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    String userId = getUserIdFromAuth(auth);
    if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "未登录或 token 无效"));
    try {
      experimentService.saveDraft(dto);
      return ResponseEntity.ok(Map.of("message", "草稿已保存"));
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

  /** 查询未完成实验 */
  @GetMapping("/unfinished")
  public ResponseEntity<?> getUnfinished(
      @RequestHeader(value = "Authorization", required = false) String auth) {
    String userId = getUserIdFromAuth(auth);
    if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "未登录或 token 无效"));
    var list = experimentService.getUnfinishedExperiments(userId);
    return ResponseEntity.ok(list);
  }

  /** 删除实验（级联删除步骤） */
  @DeleteMapping("/{experimentId}")
  public ResponseEntity<?> deleteExperiment(
      @PathVariable String experimentId,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    String userId = getUserIdFromAuth(auth);
    if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "未登录或 token 无效"));
    try {
      experimentService.deleteExperiment(experimentId);
      return ResponseEntity.ok(Map.of("message", "已删除"));
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

  /** 获取步骤草稿数据（恢复用） */
  @GetMapping("/step/draft")
  public ResponseEntity<?> getStepDraft(
      @RequestParam String experimentId,
      @RequestParam String stepId,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    String userId = getUserIdFromAuth(auth);
    if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "未登录或 token 无效"));
    var data = experimentService.getStepDraftData(experimentId, stepId);
    return ResponseEntity.ok(data);
  }

  /** 获取实验步骤列表（恢复时重建步骤映射，防止 stepId 错位） */
  @GetMapping("/{experimentId}/steps")
  public ResponseEntity<?> getSteps(
      @PathVariable String experimentId,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    String userId = getUserIdFromAuth(auth);
    if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "未登录或 token 无效"));
    return ResponseEntity.ok(experimentService.getExperimentSteps(experimentId));
  }

  /** 完成实验：工作票终结后更新 status/结束时间/总时长/总分 */
  @PostMapping("/complete")
  public ResponseEntity<?> complete(
      @RequestBody Map<String, String> body,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    String userId = getUserIdFromAuth(auth);
    if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "未登录或 token 无效"));
    String experimentId = body.get("experimentId");
    if (experimentId == null || experimentId.isBlank())
      return ResponseEntity.badRequest().body(Map.of("message", "experimentId 不能为空"));
    try {
      experimentService.completeExperiment(experimentId);
      return ResponseEntity.ok(Map.of("message", "实验已完成"));
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

  /** 获取实验总耗时（已用秒数，含草稿） */
  @GetMapping("/{experimentId}/duration")
  public ResponseEntity<?> getTotalDuration(
      @PathVariable String experimentId,
      @RequestHeader(value = "Authorization", required = false) String auth) {
    String userId = getUserIdFromAuth(auth);
    if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "未登录或 token 无效"));
    Integer seconds = experimentService.getTotalDuration(experimentId);
    return ResponseEntity.ok(Map.of("durationSeconds", seconds));
  }
}
