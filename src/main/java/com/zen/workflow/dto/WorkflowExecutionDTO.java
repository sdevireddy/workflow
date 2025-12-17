package com.zen.workflow.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkflowExecutionDTO {
    private Long id;
    private Long workflowId;
    private String executionKey;
    private String triggerType;
    private String entityType;
    private Long entityId;
    private String status;
    private String executionStatus;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long durationMs;
    private String errorMessage;
    private String currentNodeId;
    private String tenantId;
    private java.util.Map<String, Object> contextData;
}
