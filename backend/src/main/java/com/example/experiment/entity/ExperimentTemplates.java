package com.example.experiment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("experiment_templates")
public class ExperimentTemplates {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String code;
    private String name;
    private String experimentLevel;
    private String description;
    private LocalDateTime createdAt;
}
