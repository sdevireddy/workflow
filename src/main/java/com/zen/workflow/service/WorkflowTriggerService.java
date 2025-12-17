package com.zen.workflow.service;

import com.zen.workflow.engine.WorkflowExecutionEngine;
import com.zen.workflow.model.ExecutionContext;
import com.zen.entities.tenant.Workflow;
import com.zen.workflow.repository.WorkflowRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Service for triggering workflows based on events from other services
 */
@Slf4j
@Service
public class WorkflowTriggerService {

    @Autowired
    private WorkflowRepository workflowRepository;
    
    @Autowired
    private WorkflowExecutionEngine executionEngine;

    /**
     * Find and execute all active workflows matching the trigger
     * This method runs asynchronously to not block the calling service
     */
    @Async
    @Transactional
    public int triggerWorkflows(String tenantId, String moduleType, String triggerType, Map<String, Object> recordData) {
        
        log.info("🔍 Finding workflows: module={}, trigger={}, tenant={}", moduleType, triggerType, tenantId);
        
        try {
            // Find all active workflows for this module and trigger
            List<Workflow> workflows = workflowRepository
                .findByModuleTypeAndTriggerTypeAndIsActiveTrue(moduleType, triggerType);
            
            log.info("📋 Found {} active workflow(s) for {}.{}", workflows.size(), moduleType, triggerType);
            
            if (workflows.isEmpty()) {
                log.info("ℹ️ No active workflows found for {}.{}", moduleType, triggerType);
                return 0;
            }
            
            int executedCount = 0;
            
            for (Workflow workflow : workflows) {
                try {
                    log.info("▶️ Executing workflow: {} (ID: {})", workflow.getWorkflowName(), workflow.getId());
                    
                    ExecutionContext context = new ExecutionContext();
                    context.setWorkflowId(workflow.getId());
                    context.setTenantId(tenantId);
                    context.setTriggerData(recordData);
                    
                    executionEngine.executeWorkflow(workflow.getId(), context);
                    executedCount++;
                    
                    log.info("✅ Workflow executed successfully: {}", workflow.getWorkflowName());
                    
                } catch (Exception e) {
                    log.error("❌ Failed to execute workflow {} ({}): {}", 
                        workflow.getWorkflowName(), workflow.getId(), e.getMessage(), e);
                    // Continue with other workflows even if one fails
                }
            }
            
            log.info("🎉 Workflow trigger complete: {}/{} workflows executed successfully", 
                executedCount, workflows.size());
            
            return executedCount;
            
        } catch (Exception e) {
            log.error("❌ Error in workflow trigger process: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to trigger workflows: " + e.getMessage(), e);
        }
    }

    /**
     * Trigger a specific workflow by ID
     */
    @Async
    @Transactional
    public void triggerWorkflowById(Long workflowId, String tenantId, Map<String, Object> recordData) {
        log.info("🎯 Triggering specific workflow: ID={}, tenant={}", workflowId, tenantId);
        
        try {
            ExecutionContext context = new ExecutionContext();
            context.setWorkflowId(workflowId);
            context.setTenantId(tenantId);
            context.setTriggerData(recordData);
            
            executionEngine.executeWorkflow(workflowId, context);
            
            log.info("✅ Workflow {} executed successfully", workflowId);
            
        } catch (Exception e) {
            log.error("❌ Failed to execute workflow {}: {}", workflowId, e.getMessage(), e);
            throw new RuntimeException("Failed to execute workflow: " + e.getMessage(), e);
        }
    }
}
