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
    private String resultData;   // JSON 字符串
    private String startedAt;    // ISO 时间字符串（步骤首次开始时传）
}
