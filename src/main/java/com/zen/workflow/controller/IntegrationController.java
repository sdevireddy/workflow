package com.zen.workflow.controller;

import com.zen.workflow.dto.IntegrationDTO;
import com.zen.workflow.service.IntegrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API endpoints for workflow integrations
 */
@Slf4j
@RestController
@RequestMapping("/api/workflows/integrations")
@CrossOrigin(origins = "*")
public class IntegrationController {

    @Autowired
    private IntegrationService integrationService;

    /**
     * Get all integrations
     * GET /api/workflows/integrations
     */
    @GetMapping
    public ResponseEntity<List<IntegrationDTO>> getAllIntegrations(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String tenantId) {
        log.info("Getting integrations - type: {}, tenantId: {}", type, tenantId);
        try {
            List<IntegrationDTO> integrations;
            
            if (type != null) {
                integrations = integrationService.getIntegrationsByType(type);
            } else if (tenantId != null) {
                integrations = integrationService.getIntegrationsByTenant(tenantId);
            } else {
                integrations = integrationService.getAllIntegrations();
            }
            
            return ResponseEntity.ok(integrations);
        } catch (Exception e) {
            log.error("Failed to get integrations", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get integration by ID
     * GET /api/workflows/integrations/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<IntegrationDTO> getIntegration(@PathVariable Long id) {
        log.info("Getting integration: {}", id);
        try {
            IntegrationDTO integration = integrationService.getIntegration(id);
            return ResponseEntity.ok(integration);
        } catch (Exception e) {
            log.error("Failed to get integration", e);
            return ResponseEntity.status(404).build();
        }
    }

    /**
     * Create integration
     * POST /api/workflows/integrations
     */
    @PostMapping
    public ResponseEntity<IntegrationDTO> createIntegration(@RequestBody IntegrationDTO dto) {
        log.info("Creating integration: {}", dto.getIntegrationName());
        try {
            IntegrationDTO created = integrationService.createIntegration(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            log.error("Failed to create integration", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Update integration
     * PUT /api/workflows/integrations/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<IntegrationDTO> updateIntegration(
            @PathVariable Long id,
            @RequestBody IntegrationDTO dto) {
        log.info("Updating integration: {}", id);
        try {
            IntegrationDTO updated = integrationService.updateIntegration(id, dto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Failed to update integration", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Delete integration
     * DELETE /api/workflows/integrations/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIntegration(@PathVariable Long id) {
        log.info("Deleting integration: {}", id);
        try {
            integrationService.deleteIntegration(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Failed to delete integration", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Test integration
     * POST /api/workflows/integrations/{id}/test
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> testIntegration(
            @PathVariable Long id,
            @RequestBody Map<String, Object> testData) {
        log.info("Testing integration: {}", id);
        try {
            Map<String, Object> result = integrationService.testIntegration(id, testData);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to test integration", e);
            return ResponseEntity.status(500).build();
        }
    }
}
