package com.example.experiment.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.experiment.config.RequireRole;
import com.example.experiment.config.Role;
import com.example.experiment.dto.admin.StudentGradeVO;
import com.example.experiment.dto.admin.UpdateScoreDTO;
import com.example.experiment.service.AdminGradeService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端成绩接口。
 *
 * <p>最低门槛为教师。可见范围在 Service 层按 maxLevel 过滤 —— 老师只看得到学生。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@RequireRole(Role.TEACHER)
public class AdminGradeController {

  private final AdminGradeService adminGradeService;

  /** 某节点下所有班级的成绩汇总；orgId 留空表示全校 */
  @GetMapping("/grades/classes")
  public ResponseEntity<?> getClassSummaries(@RequestParam(required = false) String orgId) {
    return ResponseEntity.ok(adminGradeService.getClassSummaries(orgId, UserContext.getMaxLevel()));
  }

  /** 某节点下学生的成绩分页 */
  @GetMapping("/grades/students")
  public ResponseEntity<?> getStudentGrades(
      @RequestParam(required = false) String orgId,
      @RequestParam(required = false) String name,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size) {

    IPage<StudentGradeVO> result =
        adminGradeService.getStudentGrades(orgId, name, page, size, UserContext.getMaxLevel());

    return ResponseEntity.ok(
        Map.of(
            "records", result.getRecords(),
            "total", result.getTotal(),
            "page", result.getCurrent(),
            "size", result.getSize()));
  }

  /** 某学生的完成历史（含进行中的记录） */
  @GetMapping("/grades/students/{userId}/experiments")
  public ResponseEntity<?> getStudentExperiments(@PathVariable String userId) {
    return ResponseEntity.ok(
        adminGradeService.getStudentExperiments(userId, UserContext.getMaxLevel()));
  }

  /** 人工改分；manualScore 传 null 表示撤销改分 */
  @PutMapping("/grades/experiments/{experimentId}/score")
  public ResponseEntity<?> updateScore(
      @PathVariable String experimentId, @Valid @RequestBody UpdateScoreDTO dto) {

    adminGradeService.updateScore(
        experimentId, dto.getManualScore(), UserContext.getUserId(), UserContext.getMaxLevel());

    return ResponseEntity.ok(Map.of("message", "分数修改成功"));
  }
}
