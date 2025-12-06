package com.zen.workflow.handler;

import com.zen.workflow.engine.VariableResolver;
import com.zen.workflow.model.ExecutionContext;
import com.zen.workflow.model.ExecutionResult;
import com.zen.workflow.model.NodeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles all approval operations:
 * - approval_step, multi_step_approval, parallel_approval, review_process
 */
@Slf4j
@Component
public class ApprovalHandler implements NodeHandler {

    @Autowired
    private VariableResolver variableResolver;
    
    @Autowired
    private com.zen.workflow.service.ApprovalService approvalService;
    
    @Autowired
    private com.zen.workflow.service.NotificationService notificationService;

    @Override
    public ExecutionResult execute(NodeConfig config, ExecutionContext context) {
        String subtype = config.getSubtype();
        log.info("Executing Approval: {}", subtype);

        try {
            switch (subtype) {
                case "approval_step":
                case "multi_step_approval":
                case "parallel_approval":
                case "review_process":
                    return handleApproval(config, context);
                default:
                    return ExecutionResult.failed("Unknown approval type: " + subtype);
            }
        } catch (Exception e) {
            log.error("Approval failed", e);
            return ExecutionResult.failed(e.getMessage());
        }
    }

    private ExecutionResult handleApproval(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        String subtype = config.getSubtype();
        
        try {
            if (!approvalService.isApprovalServiceAvailable()) {
                log.warn("Approval service not available.");
                return ExecutionResult.success(Map.of(
                    "approved", false,
                    "reason", "Approval service not configured"
                ));
            }
            
            switch (subtype) {
                case "approval_step":
                    return handleSingleApproval(nodeConfig, context);
                    
                case "multi_step_approval":
                    return handleMultiStepApproval(nodeConfig, context);
                    
                case "parallel_approval":
                    return handleParallelApproval(nodeConfig, context);
                    
                case "review_process":
                    return handleReviewProcess(nodeConfig, context);
                    
                default:
                    return ExecutionResult.failed("Unknown approval type: " + subtype);
            }
            
        } catch (Exception e) {
            log.error("Approval handling failed", e);
            return ExecutionResult.failed("Approval failed: " + e.getMessage());
        }
    }
    
    private ExecutionResult handleSingleApproval(Map<String, Object> nodeConfig, ExecutionContext context) {
        List<Long> approverIds = (List<Long>) nodeConfig.get("approvers");
        String title = variableResolver.resolve((String) nodeConfig.get("title"), context);
        String message = variableResolver.resolve((String) nodeConfig.get("message"), context);
        Map<String, Object> requestData = (Map<String, Object>) nodeConfig.get("requestData");
        
        if (requestData != null) {
            requestData = variableResolver.resolveMap(requestData, context);
        }
        
        log.info("Creating single approval request for {} approvers", approverIds.size());
        
        // Create approval request
        Map<String, Object> approval = approvalService.createApprovalRequest(
            context.getExecutionId(),
            nodeConfig.get("nodeId").toString(),
            approverIds,
            "SINGLE",
            requestData
        );
        
        // Send notifications
        approvalService.sendApprovalNotifications(
            (Long) approval.get("id"),
            approverIds,
            title,
            message
        );
        
        // Store in context
        context.setVariable("approvalId", approval.get("id"));
        context.setVariable("approvalStatus", approval.get("status"));
        
        // Return paused status - workflow will resume when approved
        Map<String, Object> output = new HashMap<>();
        output.put("approvalId", approval.get("id"));
        output.put("status", "PENDING");
        output.put("approvers", approverIds);
        output.put("paused", true);
        
        log.info("Approval request created. Workflow paused pending approval.");
        return ExecutionResult.paused("Waiting for approval");
    }
    
    private ExecutionResult handleMultiStepApproval(Map<String, Object> nodeConfig, ExecutionContext context) {
        List<List<Long>> approvalSteps = (List<List<Long>>) nodeConfig.get("steps");
        String title = variableResolver.resolve((String) nodeConfig.get("title"), context);
        String message = variableResolver.resolve((String) nodeConfig.get("message"), context);
        Map<String, Object> requestData = (Map<String, Object>) nodeConfig.get("requestData");
        
        if (requestData != null) {
            requestData = variableResolver.resolveMap(requestData, context);
        }
        
        log.info("Creating multi-step approval with {} steps", approvalSteps.size());
        
        // Create multi-step approval
        Map<String, Object> approval = approvalService.createMultiStepApproval(
            context.getExecutionId(),
            nodeConfig.get("nodeId").toString(),
            approvalSteps,
            requestData
        );
        
        // Send notifications to first step approvers
        approvalService.sendApprovalNotifications(
            (Long) approval.get("id"),
            approvalSteps.get(0),
            title + " - Step 1",
            message
        );
        
        context.setVariable("approvalId", approval.get("id"));
        context.setVariable("currentStep", 0);
        
        Map<String, Object> output = new HashMap<>();
        output.put("approvalId", approval.get("id"));
        output.put("status", "PENDING");
        output.put("totalSteps", approvalSteps.size());
        output.put("currentStep", 0);
        output.put("paused", true);
        
        return ExecutionResult.paused("Waiting for multi-step approval");
    }
    
    private ExecutionResult handleParallelApproval(Map<String, Object> nodeConfig, ExecutionContext context) {
        List<Long> approverIds = (List<Long>) nodeConfig.get("approvers");
        Integer requiredApprovals = (Integer) nodeConfig.getOrDefault("requiredApprovals", approverIds.size());
        String title = variableResolver.resolve((String) nodeConfig.get("title"), context);
        String message = variableResolver.resolve((String) nodeConfig.get("message"), context);
        Map<String, Object> requestData = (Map<String, Object>) nodeConfig.get("requestData");
        
        if (requestData != null) {
            requestData = variableResolver.resolveMap(requestData, context);
        }
        
        log.info("Creating parallel approval requiring {} out of {} approvals",
            requiredApprovals, approverIds.size());
        
        // Create parallel approval
        Map<String, Object> approval = approvalService.createParallelApproval(
            context.getExecutionId(),
            nodeConfig.get("nodeId").toString(),
            approverIds,
            requiredApprovals,
            requestData
        );
        
        // Send notifications to all approvers
        approvalService.sendApprovalNotifications(
            (Long) approval.get("id"),
            approverIds,
            title,
            message
        );
        
        context.setVariable("approvalId", approval.get("id"));
        context.setVariable("requiredApprovals", requiredApprovals);
        
        Map<String, Object> output = new HashMap<>();
        output.put("approvalId", approval.get("id"));
        output.put("status", "PENDING");
        output.put("approvers", approverIds);
        output.put("requiredApprovals", requiredApprovals);
        output.put("paused", true);
        
        return ExecutionResult.paused("Waiting for parallel approval");
    }
    
    private ExecutionResult handleReviewProcess(Map<String, Object> nodeConfig, ExecutionContext context) {
        List<Long> reviewers = (List<Long>) nodeConfig.get("reviewers");
        String title = variableResolver.resolve((String) nodeConfig.get("title"), context);
        String message = variableResolver.resolve((String) nodeConfig.get("message"), context);
        Map<String, Object> reviewData = (Map<String, Object>) nodeConfig.get("reviewData");
        
        if (reviewData != null) {
            reviewData = variableResolver.resolveMap(reviewData, context);
        }
        
        log.info("Creating review process for {} reviewers", reviewers.size());
        
        // Create review request (similar to approval)
        Map<String, Object> review = approvalService.createApprovalRequest(
            context.getExecutionId(),
            nodeConfig.get("nodeId").toString(),
            reviewers,
            "REVIEW",
            reviewData
        );
        
        // Send notifications
        approvalService.sendApprovalNotifications(
            (Long) review.get("id"),
            reviewers,
            title,
            message
        );
        
        context.setVariable("reviewId", review.get("id"));
        
        Map<String, Object> output = new HashMap<>();
        output.put("reviewId", review.get("id"));
        output.put("status", "PENDING");
        output.put("reviewers", reviewers);
        output.put("paused", true);
        
        return ExecutionResult.paused("Waiting for review");
    }
}
