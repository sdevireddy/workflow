package com.zen.workflow.controller;

import com.zen.workflow.dto.WorkflowDTO;
import com.zen.workflow.dto.WorkflowExecutionDTO;
import com.zen.workflow.service.WorkflowService;
import com.zen.workflow.engine.WorkflowExecutionEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API endpoints for workflow management
 */
@Slf4j
@RestController
@RequestMapping("/api/workflows")
@CrossOrigin(origins = "*")
public class WorkflowController {

    @Autowired
    private WorkflowService workflowService;
    
    @Autowired
    private WorkflowExecutionEngine executionEngine;

    /**
     * Create a new workflow
     * POST /api/workflows
     */
    @PostMapping
    public ResponseEntity<WorkflowDTO> createWorkflow(@RequestBody WorkflowDTO dto) {
        log.info("Creating workflow: {}", dto.getWorkflowName());
        try {
            WorkflowDTO created = workflowService.createWorkflow(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            log.error("Failed to create workflow", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all workflows
     * GET /api/workflows
     */
    @GetMapping
    public ResponseEntity<List<WorkflowDTO>> getAllWorkflows(
            @RequestParam(required = false) String moduleType,
            @RequestParam(required = false) Boolean isActive) {
        log.info("Getting workflows - moduleType: {}, isActive: {}", moduleType, isActive);
        try {
            List<WorkflowDTO> workflows;
            
            if (moduleType != null) {
                workflows = workflowService.getWorkflowsByModule(moduleType);
            } else if (Boolean.TRUE.equals(isActive)) {
                workflows = workflowService.getActiveWorkflows();
            } else {
                workflows = workflowService.getAllWorkflows();
            }
            
            return ResponseEntity.ok(workflows);
        } catch (Exception e) {
            log.error("Failed to get workflows", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get workflow by ID
     * GET /api/workflows/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<WorkflowDTO> getWorkflow(@PathVariable Long id) {
        log.info("Getting workflow: {}", id);
        try {
            WorkflowDTO workflow = workflowService.getWorkflow(id);
            return ResponseEntity.ok(workflow);
        } catch (Exception e) {
            log.error("Failed to get workflow", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Update workflow
     * PUT /api/workflows/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<WorkflowDTO> updateWorkflow(
            @PathVariable Long id,
            @RequestBody WorkflowDTO dto) {
        log.info("Updating workflow: {}", id);
        try {
            WorkflowDTO updated = workflowService.updateWorkflow(id, dto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Failed to update workflow", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete workflow
     * DELETE /api/workflows/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable Long id) {
        log.info("Deleting workflow: {}", id);
        try {
            workflowService.deleteWorkflow(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Failed to delete workflow", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Activate workflow
     * POST /api/workflows/{id}/activate
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<WorkflowDTO> activateWorkflow(@PathVariable Long id) {
        log.info("Activating workflow: {}", id);
        try {
            WorkflowDTO activated = workflowService.activateWorkflow(id);
            return ResponseEntity.ok(activated);
        } catch (Exception e) {
            log.error("Failed to activate workflow", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Deactivate workflow
     * POST /api/workflows/{id}/deactivate
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<WorkflowDTO> deactivateWorkflow(@PathVariable Long id) {
        log.info("Deactivating workflow: {}", id);
        try {
            WorkflowDTO deactivated = workflowService.deactivateWorkflow(id);
            return ResponseEntity.ok(deactivated);
        } catch (Exception e) {
            log.error("Failed to deactivate workflow", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Execute workflow manually
     * POST /api/workflows/{id}/execute
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<WorkflowExecutionDTO> executeWorkflow(
            @PathVariable Long id,
            @RequestBody Map<String, Object> triggerData,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantId) {
        log.info("Executing workflow: {} for tenant: {}", id, tenantId);
        try {
            WorkflowExecutionDTO execution = executionEngine.executeWorkflow(id, triggerData, tenantId);
            return ResponseEntity.ok(execution);
        } catch (Exception e) {
            log.error("Failed to execute workflow", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Test workflow (dry run)
     * POST /api/workflows/{id}/test
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> testWorkflow(
            @PathVariable Long id,
            @RequestBody Map<String, Object> testData) {
        log.info("Testing workflow: {}", id);
        try {
            // TODO: Implement test mode (dry run without side effects)
            Map<String, Object> result = Map.of(
                "status", "success",
                "message", "Workflow test completed",
                "nodesExecuted", 5,
                "duration", "1.2s"
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to test workflow", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Validate workflow configuration
     * POST /api/workflows/{id}/validate
     */
    @PostMapping("/{id}/validate")
    public ResponseEntity<Map<String, Object>> validateWorkflow(@PathVariable Long id) {
        log.info("Validating workflow: {}", id);
        try {
            // TODO: Implement validation logic
            Map<String, Object> result = Map.of(
                "valid", true,
                "errors", List.of(),
                "warnings", List.of()
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to validate workflow", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Clone workflow
     * POST /api/workflows/{id}/clone
     */
    @PostMapping("/{id}/clone")
    public ResponseEntity<WorkflowDTO> cloneWorkflow(
            @PathVariable Long id,
            @RequestBody Map<String, String> cloneData) {
        log.info("Cloning workflow: {}", id);
        try {
            String newName = cloneData.getOrDefault("name", "Copy of Workflow");
            WorkflowDTO cloned = workflowService.cloneWorkflow(id, newName);
            return ResponseEntity.status(HttpStatus.CREATED).body(cloned);
        } catch (Exception e) {
            log.error("Failed to clone workflow", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get workflow statistics
     * GET /api/workflows/{id}/stats
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<Map<String, Object>> getWorkflowStats(@PathVariable Long id) {
        log.info("Getting workflow stats: {}", id);
        try {
            Map<String, Object> stats = workflowService.getWorkflowStats(id);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to get workflow stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
