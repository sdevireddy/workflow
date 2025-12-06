package com.zen.workflow.model;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class ExecutionResult {
    private String status; // SUCCESS, FAILED, PAUSED, WAITING
    private boolean success;
    private String errorMessage;
    private Map<String, Object> output = new HashMap<>();
    
    public static ExecutionResult success() {
        ExecutionResult result = new ExecutionResult();
        result.setStatus("SUCCESS");
        result.setSuccess(true);
        return result;
    }
    
    public static ExecutionResult success(Map<String, Object> output) {
        ExecutionResult result = success();
        result.setOutput(output);
        return result;
    }
    
    public static ExecutionResult failed(String errorMessage) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus("FAILED");
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    public static ExecutionResult waiting() {
        ExecutionResult result = new ExecutionResult();
        result.setStatus("WAITING");
        result.setSuccess(true);
        return result;
    }
}
