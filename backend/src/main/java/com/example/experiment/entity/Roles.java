package com.example.experiment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("roles")
public class Roles {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String code;
    private String name;
    private String description;
}
