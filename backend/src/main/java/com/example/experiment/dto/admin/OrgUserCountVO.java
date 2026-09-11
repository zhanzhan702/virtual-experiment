package com.example.experiment.dto.admin;

import lombok.Data;

/**
 * 按组织节点统计的用户数。
 *
 * <p>刻意用 DTO 而非 {@code Map<String, Object>} 接收：Map 的键取自 JDBC 驱动的列标签， 不同驱动/配置下大小写不一致，取错键会静默拿到 0。
 */
@Data
public class OrgUserCountVO {

  /** 组织节点 ID（32 位十六进制） */
  private String orgId;

  private Integer userCount;
}
