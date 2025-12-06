package com.zen.workflow.dto;

import java.time.LocalDateTime;

public class ApprovalRequestDTO {
    private Long id;
    private Long executionId;
    private Long approvalProcessId;
    private Long approverId;
    private Integer approvalLevel;
    private String status;
    private String comments;
    private LocalDateTime requestedAt;
    private LocalDateTime respondedAt;
    
    public ApprovalRequestDTO() {}
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getExecutionId() { return executionId; }
    public void setExecutionId(Long executionId) { this.executionId = executionId; }
    
    public Long getApprovalProcessId() { return approvalProcessId; }
    public void setApprovalProcessId(Long approvalProcessId) { this.approvalProcessId = approvalProcessId; }
    
    public Long getApproverId() { return approverId; }
    public void setApproverId(Long approverId) { this.approverId = approverId; }
    
    public Integer getApprovalLevel() { return approvalLevel; }
    public void setApprovalLevel(Integer approvalLevel) { this.approvalLevel = approvalLevel; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
}
