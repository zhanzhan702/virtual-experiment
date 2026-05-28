package com.example.experiment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private Integer status;  // 0进行中 1完成
    private BigDecimal score;
    private LocalDateTime createdAt;
}
