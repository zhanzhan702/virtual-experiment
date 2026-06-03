package com.example.experiment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("organization")
public class Organization {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String name;
    private String type;    //university/college/major/grade/class
    private String parentId;
    private String path;
    private int sort;
    private LocalDateTime createAt;
}
