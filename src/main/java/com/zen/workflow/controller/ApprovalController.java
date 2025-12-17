package com.zen.workflow.controller;

import com.zen.workflow.dto.ApprovalDTO;
import com.zen.workflow.service.ApprovalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API endpoints for workflow approvals
 */
@Slf4j
@RestController
@RequestMapping("/api/workflows/approvals")
@CrossOrigin(origins = "*")
public class ApprovalController {

    @Autowired
    private ApprovalService approvalService;

    /**
     * Get all pending approvals for a user
     * GET /api/workflows/approvals/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<List<Map<String, Object>>> getPendingApprovals(
            @RequestParam Long userId) {
        log.info("Getting pending approvals for user: {}", userId);
        try {
            List<Map<String, Object>> approvals = approvalService.getPendingApprovals(userId);
            return ResponseEntity.ok(approvals);
        } catch (Exception e) {
            log.error("Failed to get pending approvals", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get approval by ID
     * GET /api/workflows/approvals/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getApproval(@PathVariable Long id) {
        log.info("Getting approval: {}", id);
        try {
            Map<String, Object> approval = approvalService.getApproval(id);
            return ResponseEntity.ok(approval);
        } catch (Exception e) {
            log.error("Failed to get approval", e);
            return ResponseEntity.status(404).build();
        }
    }

    /**
     * Approve a workflow
     * POST /api/workflows/approvals/{id}/approve
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveWorkflow(
            @PathVariable Long id,
            @RequestBody Map<String, Object> approvalData) {
        log.info("Approving workflow approval: {}", id);
        try {
            Long userId = ((Number) approvalData.get("userId")).longValue();
            String comments = (String) approvalData.get("comments");
            
            Map<String, Object> approved = approvalService.approve(id, userId, comments);
            return ResponseEntity.ok(approved);
        } catch (Exception e) {
            log.error("Failed to approve workflow", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Reject a workflow
     * POST /api/workflows/approvals/{id}/reject
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectWorkflow(
            @PathVariable Long id,
            @RequestBody Map<String, Object> rejectionData) {
        log.info("Rejecting workflow approval: {}", id);
        try {
            Long userId = ((Number) rejectionData.get("userId")).longValue();
            String reason = (String) rejectionData.get("reason");
            
            Map<String, Object> rejected = approvalService.reject(id, userId, reason);
            return ResponseEntity.ok(rejected);
        } catch (Exception e) {
            log.error("Failed to reject workflow", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get approval history for an execution
     * GET /api/workflows/executions/{executionId}/approvals
     */
    @GetMapping("/executions/{executionId}")
    public ResponseEntity<List<Map<String, Object>>> getExecutionApprovals(
            @PathVariable Long executionId) {
        log.info("Getting approvals for execution: {}", executionId);
        try {
            List<Map<String, Object>> approvals = approvalService.getExecutionApprovals(executionId);
            return ResponseEntity.ok(approvals);
        } catch (Exception e) {
            log.error("Failed to get execution approvals", e);
            return ResponseEntity.status(500).build();
        }
    }
}
