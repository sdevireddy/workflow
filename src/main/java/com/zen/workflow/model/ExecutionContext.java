package com.zen.workflow.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ExecutionContext {
    private Long workflowId;
    private Long executionId;
    private String tenantId;
    private Map<String, Object> variables = new HashMap<>();
    private List<Map<String, Object>> executedNodes = new ArrayList<>();
    private Map<String, Object> metadata = new HashMap<>();
    
    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }
    
    public Object getVariable(String key) {
        return variables.get(key);
    }
    
    public boolean hasVariable(String key) {
        return variables.containsKey(key);
    }
}
