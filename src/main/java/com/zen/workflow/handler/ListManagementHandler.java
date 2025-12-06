package com.zen.workflow.handler;

import com.zen.workflow.engine.VariableResolver;
import com.zen.workflow.model.ExecutionContext;
import com.zen.workflow.model.ExecutionResult;
import com.zen.workflow.model.NodeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles all list and tag operations:
 * - add_to_list, remove_from_list, add_tag, remove_tag
 */
@Slf4j
@Component
public class ListManagementHandler implements NodeHandler {

    @Autowired
    private VariableResolver variableResolver;
    
    @Autowired
    private com.zen.workflow.service.ListTagService listTagService;

    @Override
    public ExecutionResult execute(NodeConfig config, ExecutionContext context) {
        String subtype = config.getSubtype();
        log.info("Executing List Management: {}", subtype);

        try {
            switch (subtype) {
                case "add_to_list":
                    return handleAddToList(config, context);
                case "remove_from_list":
                    return handleRemoveFromList(config, context);
                case "add_tag":
                    return handleAddTag(config, context);
                case "remove_tag":
                    return handleRemoveTag(config, context);
                default:
                    return ExecutionResult.failed("Unknown list operation: " + subtype);
            }
        } catch (Exception e) {
            log.error("List management failed", e);
            return ExecutionResult.failed(e.getMessage());
        }
    }

    private ExecutionResult handleAddToList(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String recordId = variableResolver.resolve((String) nodeConfig.get("recordId"), context);
        String listId = variableResolver.resolve((String) nodeConfig.get("listId"), context);
        String recordType = (String) nodeConfig.getOrDefault("recordType", "Lead");
        
        log.info("Adding {} {} to list {}", recordType, recordId, listId);
        
        try {
            if (!listTagService.isListServiceAvailable()) {
                log.warn("List service not available.");
                return ExecutionResult.success(Map.of(
                    "addedToList", false,
                    "reason", "List service not configured"
                ));
            }
            
            Map<String, Object> result = listTagService.addToList(listId, recordId, recordType);
            
            context.setVariable("listResult", result);
            context.setVariable("addedToList", result.get("added"));
            
            return ExecutionResult.success(result);
            
        } catch (Exception e) {
            log.error("Failed to add to list", e);
            return ExecutionResult.failed("Add to list failed: " + e.getMessage());
        }
    }

    private ExecutionResult handleRemoveFromList(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String recordId = variableResolver.resolve((String) nodeConfig.get("recordId"), context);
        String listId = variableResolver.resolve((String) nodeConfig.get("listId"), context);
        String recordType = (String) nodeConfig.getOrDefault("recordType", "Lead");
        
        log.info("Removing {} {} from list {}", recordType, recordId, listId);
        
        try {
            if (!listTagService.isListServiceAvailable()) {
                return ExecutionResult.success(Map.of(
                    "removedFromList", false,
                    "reason", "List service not configured"
                ));
            }
            
            Map<String, Object> result = listTagService.removeFromList(listId, recordId, recordType);
            
            context.setVariable("listResult", result);
            context.setVariable("removedFromList", result.get("removed"));
            
            return ExecutionResult.success(result);
            
        } catch (Exception e) {
            log.error("Failed to remove from list", e);
            return ExecutionResult.failed("Remove from list failed: " + e.getMessage());
        }
    }

    private ExecutionResult handleAddTag(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String recordId = variableResolver.resolve((String) nodeConfig.get("recordId"), context);
        String tagName = variableResolver.resolve((String) nodeConfig.get("tag"), context);
        String recordType = (String) nodeConfig.getOrDefault("recordType", "Lead");
        Map<String, Object> tagData = (Map<String, Object>) nodeConfig.get("tagData");
        
        log.info("Adding tag '{}' to {} {}", tagName, recordType, recordId);
        
        try {
            if (!listTagService.isTagServiceAvailable()) {
                return ExecutionResult.success(Map.of(
                    "tagAdded", false,
                    "reason", "Tag service not configured"
                ));
            }
            
            if (tagData != null) {
                tagData = variableResolver.resolveMap(tagData, context);
            }
            
            Map<String, Object> result = listTagService.addTag(recordId, recordType, tagName, tagData);
            
            context.setVariable("tagResult", result);
            context.setVariable("tagId", result.get("tagId"));
            context.setVariable("tagAdded", result.get("added"));
            
            return ExecutionResult.success(result);
            
        } catch (Exception e) {
            log.error("Failed to add tag", e);
            return ExecutionResult.failed("Add tag failed: " + e.getMessage());
        }
    }

    private ExecutionResult handleRemoveTag(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String recordId = variableResolver.resolve((String) nodeConfig.get("recordId"), context);
        String tagName = variableResolver.resolve((String) nodeConfig.get("tag"), context);
        String recordType = (String) nodeConfig.getOrDefault("recordType", "Lead");
        
        log.info("Removing tag '{}' from {} {}", tagName, recordType, recordId);
        
        try {
            if (!listTagService.isTagServiceAvailable()) {
                return ExecutionResult.success(Map.of(
                    "tagRemoved", false,
                    "reason", "Tag service not configured"
                ));
            }
            
            Map<String, Object> result = listTagService.removeTag(recordId, recordType, tagName);
            
            context.setVariable("tagResult", result);
            context.setVariable("tagRemoved", result.get("removed"));
            
            return ExecutionResult.success(result);
            
        } catch (Exception e) {
            log.error("Failed to remove tag", e);
            return ExecutionResult.failed("Remove tag failed: " + e.getMessage());
        }
    }
}
