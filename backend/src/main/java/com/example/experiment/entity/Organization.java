package com.example.experiment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("organization")
public class Organization {
  @TableId(type = IdType.ASSIGN_UUID)
  private String id;

  private String name;
  private String type;
  private String parentId;
  private String path;
  private int sort;

  @TableField("created_at")
  private LocalDateTime createdAt;
}
