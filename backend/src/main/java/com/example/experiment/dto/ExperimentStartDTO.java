package com.example.experiment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExperimentStartDTO {
    @NotBlank(message = "模板ID不能为空")
    private String templateId;
}
