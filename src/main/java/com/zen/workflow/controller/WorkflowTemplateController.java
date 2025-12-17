package com.zen.workflow.controller;

import com.zen.workflow.service.WorkflowTemplateProvisioningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for workflow template management
 * Allows tenants to browse and use workflow templates
 */
@RestController
@RequestMapping("/api/workflows/workflow-templates")
public class WorkflowTemplateController {

    private static final Logger log = LoggerFactory.getLogger(WorkflowTemplateController.class);

    private final WorkflowTemplateProvisioningService provisioningService;

    public WorkflowTemplateController(WorkflowTemplateProvisioningService provisioningService) {
        this.provisioningService = provisioningService;
    }

    /**
     * Get all available workflow templates for current tenant
     * GET /api/workflow-templates
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAvailableTemplates(
            @RequestHeader("X-Tenant-ID") String tenantSchema) {
        
        log.info("📋 GET /api/workflow-templates - Tenant: {}", tenantSchema);

        try {
            List<Map<String, Object>> templates = provisioningService.getAvailableTemplates(tenantSchema);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("templates", templates);
            response.put("count", templates.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Failed to get workflow templates: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get workflow templates by category
     * GET /api/workflow-templates/category/{category}
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<Map<String, Object>> getTemplatesByCategory(
            @RequestHeader("X-Tenant-ID") String tenantSchema,
            @PathVariable String category) {
        
        log.info("📋 GET /api/workflow-templates/category/{} - Tenant: {}", category, tenantSchema);

        try {
            List<Map<String, Object>> templates = provisioningService.getTemplatesByCategory(
                    tenantSchema, category.toUpperCase());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("category", category.toUpperCase());
            response.put("templates", templates);
            response.put("count", templates.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Failed to get workflow templates by category: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Create a new workflow from a template
     * POST /api/workflow-templates/{templateKey}/create
     * 
     * Request body:
     * {
     *   "name": "My Custom Workflow",
     *   "userId": 123
     * }
     */
    @PostMapping("/{templateKey}/create")
    public ResponseEntity<Map<String, Object>> createWorkflowFromTemplate(
            @RequestHeader("X-Tenant-ID") String tenantSchema,
            @PathVariable String templateKey,
            @RequestBody Map<String, Object> request) {
        
        log.info("🎨 POST /api/workflow-templates/{}/create - Tenant: {}", templateKey, tenantSchema);

        try {
            String workflowName = (String) request.get("name");
            Long userId = request.get("userId") != null ? 
                    Long.valueOf(request.get("userId").toString()) : null;

            Long workflowId = provisioningService.createWorkflowFromTemplate(
                    tenantSchema, templateKey, workflowName, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("workflowId", workflowId);
            response.put("message", "Workflow created successfully from template");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("❌ Template not found: {}", e.getMessage());
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Template not found: " + templateKey);
            
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            log.error("❌ Failed to create workflow from template: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Copy a specific template from common schema to tenant
     * POST /api/workflow-templates/{templateKey}/copy
     * 
     * This is useful if a template was added to common schema after tenant creation
     */
    @PostMapping("/{templateKey}/copy")
    public ResponseEntity<Map<String, Object>> copyTemplateToTenant(
            @RequestHeader("X-Tenant-ID") String tenantSchema,
            @PathVariable String templateKey) {
        
        log.info("📥 POST /api/workflow-templates/{}/copy - Tenant: {}", templateKey, tenantSchema);

        try {
            Long templateId = provisioningService.copyTemplateToTenant(tenantSchema, templateKey);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("templateId", templateId);
            response.put("message", "Template copied successfully");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("❌ Template not found: {}", e.getMessage());
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Template not found in common schema: " + templateKey);
            
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            log.error("❌ Failed to copy template: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Provision all workflow templates for a tenant
     * POST /api/workflow-templates/provision
     * 
     * This is called during tenant setup when workflow module is enabled
     * Admin only endpoint
     */
    @PostMapping("/provision")
    public ResponseEntity<Map<String, Object>> provisionWorkflowTemplates(
            @RequestHeader("X-Tenant-ID") String tenantSchema) {
        
        log.info("🔧 POST /api/workflow-templates/provision - Tenant: {}", tenantSchema);

        try {
            provisioningService.provisionWorkflowTemplates(tenantSchema);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Workflow templates provisioned successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Failed to provision workflow templates: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
