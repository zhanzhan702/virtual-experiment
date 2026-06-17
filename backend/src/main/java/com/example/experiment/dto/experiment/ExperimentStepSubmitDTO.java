package com.example.experiment.dto.experiment;

import lombok.Data;

@Data
public class ExperimentStepSubmitDTO {
    private String experimentId;
    private String stepId;
    private Integer status;
    private Integer durationSeconds;
    private Integer operationCount;
    private Integer errorCount;
    private Double score;
    private String resultData;
    private String startedAt;
}
