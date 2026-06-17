package com.example.experiment.controller;

import com.example.experiment.dto.ExperimentStartDTO;
import com.example.experiment.dto.ExperimentStepSubmitDTO;
import com.example.experiment.service.ExperimentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/experiment")
public class ExperimentController {

    private final ExperimentService experimentService;

    /** 启动实验 */
    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestBody ExperimentStartDTO dto,
                                   @RequestHeader(value = "Authorization", required = false) String auth) {
        try {
            // TODO: 从 JWT token 中解析 userId，目前先用固定值测试
            String userId = "test_user_id";
            var vo = experimentService.start(dto, userId);
            return ResponseEntity.ok(vo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 提交步骤 */
    @PostMapping("/step/submit")
    public ResponseEntity<?> submitStep(@RequestBody ExperimentStepSubmitDTO dto) {
        try {
            experimentService.submitStep(dto);
            return ResponseEntity.ok(Map.of("message", "提交成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
