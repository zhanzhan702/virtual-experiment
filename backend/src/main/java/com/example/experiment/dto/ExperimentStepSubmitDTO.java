package com.example.experiment.dto;

import lombok.Data;

/** 步骤提交请求 */
@Data
public class ExperimentStepSubmitDTO {
    private String experimentId;
    private String stepId;
    private Integer status;
    private Integer durationSeconds;
    private Integer operationCount;
    private Integer errorCount;
    private Double score;
    private String resultData;  // JSON 字符串
}
