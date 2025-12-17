package com.zen.workflow.controller;

import com.zen.workflow.service.WorkflowTriggerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for triggering workflows from external services (CRM, etc.)
 */
@Slf4j
@RestController
@RequestMapping("/api/workflows")
@CrossOrigin(origins = "*")
public class WorkflowTriggerController {

    @Autowired
    private WorkflowTriggerService triggerService;

    /**
     * Trigger workflows based on module and event type
     * POST /api/workflows/trigger
     * 
     * Request body:
     * {
     *   "moduleType": "LEAD",
     *   "triggerType": "ON_CREATE",
     *   "recordData": {
     *     "leadId": 123,
     *     "leadName": "John Doe",
     *     "email": "john@example.com"
     *   }
     * }
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerWorkflows(
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        String moduleType = (String) request.get("moduleType");
        String triggerType = (String) request.get("triggerType");
        @SuppressWarnings("unchecked")
        Map<String, Object> recordData = (Map<String, Object>) request.get("recordData");
        
        log.info("🎯 Workflow trigger request: module={}, trigger={}, tenant={}", 
            moduleType, triggerType, tenantId);

        try {
            int executedCount = triggerService.triggerWorkflows(
                tenantId, moduleType, triggerType, recordData
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("executedWorkflows", executedCount);
            response.put("message", executedCount + " workflow(s) triggered");
            
            log.info("✅ Successfully triggered {} workflow(s)", executedCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Failed to trigger workflows: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Health check for workflow trigger endpoint
     * GET /api/workflows/trigger/health
     */
    @GetMapping("/trigger/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "workflow-trigger");
        return ResponseEntity.ok(response);
    }
}
