package com.zen.workflow.dto;

import java.util.Map;

public class ExecuteWorkflowRequest {
    private String entityType;
    private Long entityId;
    private Map<String, Object> context;
    
    public ExecuteWorkflowRequest() {}
    
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    
    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }
}
