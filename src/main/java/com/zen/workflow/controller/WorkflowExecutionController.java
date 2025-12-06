package com.zen.workflow.controller;

import com.zen.workflow.dto.WorkflowExecutionDTO;
import com.zen.workflow.service.WorkflowExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API endpoints for workflow execution management
 */
@Slf4j
@RestController
@RequestMapping("/api/workflows/executions")
@CrossOrigin(origins = "*")
public class WorkflowExecutionController {

    @Autowired
    private WorkflowExecutionService executionService;

    /**
     * Get all executions
     * GET /api/workflows/executions
     */
    @GetMapping
    public ResponseEntity<List<WorkflowExecutionDTO>> getAllExecutions(
            @RequestParam(required = false) Long workflowId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String tenantId) {
        log.info("Getting executions - workflowId: {}, status: {}, tenantId: {}", 
            workflowId, status, tenantId);
        try {
            List<WorkflowExecutionDTO> executions;
            
            if (workflowId != null && status != null) {
                executions = executionService.getExecutionsByWorkflowAndStatus(workflowId, status);
            } else if (workflowId != null) {
                executions = executionService.getExecutionsByWorkflow(workflowId);
            } else if (tenantId != null) {
                executions = executionService.getExecutionsByTenant(tenantId);
            } else if (status != null) {
                executions = executionService.getExecutionsByStatus(status);
            } else {
                executions = executionService.getAllExecutions();
            }
            
            return ResponseEntity.ok(executions);
        } catch (Exception e) {
            log.error("Failed to get executions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get execution by ID
     * GET /api/workflows/executions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<WorkflowExecutionDTO> getExecution(@PathVariable Long id) {
        log.info("Getting execution: {}", id);
        try {
            WorkflowExecutionDTO execution = executionService.getExecution(id);
            return ResponseEntity.ok(execution);
        } catch (Exception e) {
            log.error("Failed to get execution", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Get execution logs
     * GET /api/workflows/executions/{id}/logs
     */
    @GetMapping("/{id}/logs")
    public ResponseEntity<List<Map<String, Object>>> getExecutionLogs(@PathVariable Long id) {
        log.info("Getting execution logs: {}", id);
        try {
            List<Map<String, Object>> logs = executionService.getExecutionLogs(id);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            log.error("Failed to get execution logs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Pause execution
     * POST /api/workflows/executions/{id}/pause
     */
    @PostMapping("/{id}/pause")
    public ResponseEntity<WorkflowExecutionDTO> pauseExecution(@PathVariable Long id) {
        log.info("Pausing execution: {}", id);
        try {
            WorkflowExecutionDTO paused = executionService.pauseExecution(id);
            return ResponseEntity.ok(paused);
        } catch (Exception e) {
            log.error("Failed to pause execution", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Resume execution
     * POST /api/workflows/executions/{id}/resume
     */
    @PostMapping("/{id}/resume")
    public ResponseEntity<WorkflowExecutionDTO> resumeExecution(@PathVariable Long id) {
        log.info("Resuming execution: {}", id);
        try {
            WorkflowExecutionDTO resumed = executionService.resumeExecution(id);
            return ResponseEntity.ok(resumed);
        } catch (Exception e) {
            log.error("Failed to resume execution", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Cancel execution
     * POST /api/workflows/executions/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<WorkflowExecutionDTO> cancelExecution(@PathVariable Long id) {
        log.info("Cancelling execution: {}", id);
        try {
            WorkflowExecutionDTO cancelled = executionService.cancelExecution(id);
            return ResponseEntity.ok(cancelled);
        } catch (Exception e) {
            log.error("Failed to cancel execution", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retry failed execution
     * POST /api/workflows/executions/{id}/retry
     */
    @PostMapping("/{id}/retry")
    public ResponseEntity<WorkflowExecutionDTO> retryExecution(@PathVariable Long id) {
        log.info("Retrying execution: {}", id);
        try {
            WorkflowExecutionDTO retried = executionService.retryExecution(id);
            return ResponseEntity.ok(retried);
        } catch (Exception e) {
            log.error("Failed to retry execution", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get execution history for a workflow
     * GET /api/workflows/{workflowId}/executions
     */
    @GetMapping("/{workflowId}/history")
    public ResponseEntity<List<WorkflowExecutionDTO>> getExecutionHistory(
            @PathVariable Long workflowId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Getting execution history for workflow: {}", workflowId);
        try {
            List<WorkflowExecutionDTO> history = executionService.getExecutionHistory(workflowId, page, size);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Failed to get execution history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get execution statistics
     * GET /api/workflows/executions/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getExecutionStats(
            @RequestParam(required = false) Long workflowId,
            @RequestParam(required = false) String tenantId) {
        log.info("Getting execution stats - workflowId: {}, tenantId: {}", workflowId, tenantId);
        try {
            Map<String, Object> stats = executionService.getExecutionStats(workflowId, tenantId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to get execution stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
