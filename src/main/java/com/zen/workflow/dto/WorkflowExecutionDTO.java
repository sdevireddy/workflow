package com.zen.workflow.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkflowExecutionDTO {
    private Long id;
    private Long workflowId;
    private String executionStatus;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long durationMs;
    private String errorMessage;
    private String currentNodeId;
}
