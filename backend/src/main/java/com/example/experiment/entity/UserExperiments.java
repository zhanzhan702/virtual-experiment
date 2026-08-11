package com.example.experiment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user_experiments")
public class UserExperiments {
  @TableId(type = IdType.ASSIGN_UUID)
  private String id;

  private String userId;
  private String templateId;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private Integer totalDuration;
  private Integer status; // 0进行中 1完成
  private BigDecimal score;
  private String draftData; // 当前存档步骤草稿 JSON（saveDraft 写入，submitStep 清空）
  private String ticketData; // 工作票提交数据 JSON（ticketNo + member1，提交时写入保留）
  private LocalDateTime createdAt;
}
