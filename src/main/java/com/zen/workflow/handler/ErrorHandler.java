package com.zen.workflow.handler;

import com.zen.workflow.engine.VariableResolver;
import com.zen.workflow.model.ExecutionContext;
import com.zen.workflow.model.ExecutionResult;
import com.zen.workflow.model.NodeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles all error handling operations:
 * - error_handler, retry_on_failure, stop_workflow
 */
@Slf4j
@Component
public class ErrorHandler implements NodeHandler {

    @Autowired
    private VariableResolver variableResolver;

    @Override
    public ExecutionResult execute(NodeConfig config, ExecutionContext context) {
        String subtype = config.getSubtype();
        log.info("Executing Error Handler: {}", subtype);

        try {
            switch (subtype) {
                case "error_handler":
                    return handleError(config, context);
                case "retry_on_failure":
                    return handleRetry(config, context);
                case "stop_workflow":
                    return handleStop(config, context);
                default:
                    return ExecutionResult.failed("Unknown error handler type: " + subtype);
            }
        } catch (Exception e) {
            log.error("Error handler failed", e);
            return ExecutionResult.failed(e.getMessage());
        }
    }

    private ExecutionResult handleError(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String errorMessage = (String) context.getVariable("lastError");
        String action = (String) nodeConfig.getOrDefault("action", "LOG");
        
        log.info("Handling error: {} with action: {}", errorMessage, action);
        
        // TODO: Implement error handling logic
        // - LOG: Log the error
        // - NOTIFY: Send notification to admin
        // - CONTINUE: Continue workflow
        // - STOP: Stop workflow
        
        Map<String, Object> output = new HashMap<>();
        output.put("errorHandled", true);
        output.put("action", action);
        
        return ExecutionResult.success(output);
    }

    private ExecutionResult handleRetry(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        int maxRetries = (int) nodeConfig.getOrDefault("maxRetries", 3);
        int retryDelay = (int) nodeConfig.getOrDefault("retryDelay", 60); // seconds
        
        Integer currentRetry = (Integer) context.getVariable("retryCount");
        if (currentRetry == null) {
            currentRetry = 0;
        }
        
        log.info("Retry attempt {} of {}", currentRetry + 1, maxRetries);
        
        if (currentRetry < maxRetries) {
            context.setVariable("retryCount", currentRetry + 1);
            
            Map<String, Object> output = new HashMap<>();
            output.put("retrying", true);
            output.put("retryCount", currentRetry + 1);
            output.put("retryDelay", retryDelay);
            
            return ExecutionResult.success(output);
        } else {
            log.error("Max retries exceeded");
            return ExecutionResult.failed("Max retries exceeded");
        }
    }

    private ExecutionResult handleStop(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String reason = variableResolver.resolve((String) nodeConfig.get("reason"), context);
        
        log.info("Stopping workflow: {}", reason);
        
        Map<String, Object> output = new HashMap<>();
        output.put("workflowStopped", true);
        output.put("reason", reason);
        
        return ExecutionResult.failed("Workflow stopped: " + reason);
    }
}
