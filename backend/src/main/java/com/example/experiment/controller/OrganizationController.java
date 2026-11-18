package com.example.experiment.controller;

import com.example.experiment.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/organization")
public class OrganizationController {

  private final OrganizationService organizationService;

  /** 组织架构树（注册页选择班级用，公开接口） */
  @GetMapping("/tree")
  public ResponseEntity<?> tree() {
    return ResponseEntity.ok(organizationService.getTree());
  }
}
