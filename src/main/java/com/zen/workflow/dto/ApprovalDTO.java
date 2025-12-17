package com.zen.workflow.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApprovalDTO {
    private Long id;
    private Long workflowExecutionId;
    private String approverUserId;
    private String status; // PENDING, APPROVED, REJECTED
    private String comments;
    private LocalDateTime requestedAt;
    private LocalDateTime respondedAt;
}
