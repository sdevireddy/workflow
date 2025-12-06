package com.zen.workflow.handler;

import com.zen.workflow.model.ExecutionContext;
import com.zen.workflow.model.ExecutionResult;
import com.zen.workflow.model.NodeConfig;

/**
 * Base interface for all node handlers
 */
public interface NodeHandler {
    
    /**
     * Execute the node logic
     * 
     * @param config Node configuration
     * @param context Execution context with variables
     * @return Execution result
     */
    ExecutionResult execute(NodeConfig config, ExecutionContext context);
    
    /**
     * Validate node configuration
     * 
     * @param config Node configuration
     * @return true if valid
     */
    default boolean validate(NodeConfig config) {
        return config != null && config.getType() != null;
    }
}
