package com.zen.workflow.controller;

import com.zen.workflow.dto.NodeDefinitionDTO;
import com.zen.workflow.service.NodeDefinitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API endpoints for workflow node definitions
 */
@Slf4j
@RestController
@RequestMapping("/api/workflows/node-definitions")
@CrossOrigin(origins = "*")
public class NodeDefinitionController {

    @Autowired
    private NodeDefinitionService nodeDefinitionService;

    /**
     * Get all node definitions
     * GET /api/workflows/node-definitions
     */
    @GetMapping
    public ResponseEntity<List<NodeDefinitionDTO>> getAllNodeDefinitions(
            @RequestParam(required = false) String nodeType,
            @RequestParam(required = false) String category) {
        log.info("Getting node definitions - type: {}, category: {}", nodeType, category);
        try {
            List<NodeDefinitionDTO> definitions;
            
            if (nodeType != null) {
                definitions = nodeDefinitionService.getNodeDefinitionsByType(nodeType);
            } else if (category != null) {
                definitions = nodeDefinitionService.getNodeDefinitionsByCategory(category);
            } else {
                definitions = nodeDefinitionService.getAllNodeDefinitions();
            }
            
            return ResponseEntity.ok(definitions);
        } catch (Exception e) {
            log.error("Failed to get node definitions", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get node definition by type and subtype
     * GET /api/workflows/node-definitions/{type}/{subtype}
     */
    @GetMapping("/{type}/{subtype}")
    public ResponseEntity<NodeDefinitionDTO> getNodeDefinition(
            @PathVariable String type,
            @PathVariable String subtype) {
        log.info("Getting node definition: {} - {}", type, subtype);
        try {
            NodeDefinitionDTO definition = nodeDefinitionService.getNodeDefinition(type, subtype);
            return ResponseEntity.ok(definition);
        } catch (Exception e) {
            log.error("Failed to get node definition", e);
            return ResponseEntity.status(404).build();
        }
    }

    /**
     * Get node categories
     * GET /api/workflows/node-definitions/categories
     */
    @GetMapping("/categories")
    public ResponseEntity<List<Map<String, Object>>> getNodeCategories() {
        log.info("Getting node categories");
        try {
            List<Map<String, Object>> categories = nodeDefinitionService.getNodeCategories();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("Failed to get node categories", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get node types
     * GET /api/workflows/node-definitions/types
     */
    @GetMapping("/types")
    public ResponseEntity<List<String>> getNodeTypes() {
        log.info("Getting node types");
        try {
            List<String> types = nodeDefinitionService.getNodeTypes();
            return ResponseEntity.ok(types);
        } catch (Exception e) {
            log.error("Failed to get node types", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get configuration schema for a node type
     * GET /api/workflows/node-definitions/{type}/{subtype}/schema
     */
    @GetMapping("/{type}/{subtype}/schema")
    public ResponseEntity<Map<String, Object>> getNodeConfigSchema(
            @PathVariable String type,
            @PathVariable String subtype) {
        log.info("Getting config schema for: {} - {}", type, subtype);
        try {
            Map<String, Object> schema = nodeDefinitionService.getConfigSchema(type, subtype);
            return ResponseEntity.ok(schema);
        } catch (Exception e) {
            log.error("Failed to get config schema", e);
            return ResponseEntity.status(404).build();
        }
    }
}
