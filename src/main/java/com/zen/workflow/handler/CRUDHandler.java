package com.zen.workflow.handler;

import com.zen.workflow.engine.VariableResolver;
import com.zen.workflow.model.ExecutionContext;
import com.zen.workflow.model.ExecutionResult;
import com.zen.workflow.model.NodeConfig;
import com.zen.workflow.service.DynamicEntityService;
import com.zen.workflow.service.LeadAssignmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles all data operations:
 * - get_records, query_database, search_records
 * - create_record, create_multiple, clone_record
 * - update_record, update_multiple, update_related
 * - delete_record, delete_multiple
 * - set_field, copy_field, clear_field, increment, decrement
 * - assign_record, rotate_owner, assign_team
 */
@Slf4j
@Component
public class CRUDHandler implements NodeHandler {

    @Autowired
    private VariableResolver variableResolver;
    
    @Autowired
    private LeadAssignmentService leadAssignmentService;
    
    @Autowired
    private DynamicEntityService dynamicEntityService;

    @Override
    public ExecutionResult execute(NodeConfig config, ExecutionContext context) {
        String subtype = config.getSubtype();
        log.info("Executing Data Operation: {}", subtype);

        try {
            switch (subtype) {
                // Query operations
                case "get_records":
                case "query_database":
                case "search_records":
                    return handleQuery(config, context);
                
                // Create operations
                case "create_record":
                case "create_multiple":
                case "clone_record":
                    return handleCreate(config, context);
                
                // Update operations
                case "update_record":
                case "update_multiple":
                case "update_related":
                    return handleUpdate(config, context);
                
                // Delete operations
                case "delete_record":
                case "delete_multiple":
                    return handleDelete(config, context);
                
                // Field operations
                case "set_field":
                    return handleSetField(config, context);
                case "copy_field":
                    return handleCopyField(config, context);
                case "clear_field":
                    return handleClearField(config, context);
                case "increment":
                    return handleIncrement(config, context);
                case "decrement":
                    return handleDecrement(config, context);
                
                // Assignment operations
                case "assign_record":
                case "rotate_owner":
                case "assign_team":
                    return handleAssignment(config, context);
                
                default:
                    return ExecutionResult.failed("Unknown data operation: " + subtype);
            }
        } catch (Exception e) {
            log.error("Data operation failed", e);
            return ExecutionResult.failed(e.getMessage());
        }
    }

    private ExecutionResult handleQuery(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        String entity = (String) nodeConfig.get("entity");
        String subtype = config.getSubtype();
        
        log.info("Querying records for entity: {} using {}", entity, subtype);
        
        try {
            List<Object> records;
            
            switch (subtype) {
                case "search_records":
                    String searchField = (String) nodeConfig.get("searchField");
                    String searchValue = variableResolver.resolve((String) nodeConfig.get("searchValue"), context);
                    Integer searchLimit = (Integer) nodeConfig.getOrDefault("limit", 100);
                    records = dynamicEntityService.searchRecords(entity, searchField, searchValue, searchLimit);
                    break;
                    
                case "query_database":
                case "get_records":
                default:
                    Map<String, Object> criteria = (Map<String, Object>) nodeConfig.get("criteria");
                    if (criteria != null) {
                        criteria = variableResolver.resolveMap(criteria, context);
                    }
                    Integer limit = (Integer) nodeConfig.getOrDefault("limit", 100);
                    records = dynamicEntityService.queryRecords(entity, criteria, limit);
                    break;
            }
            
            // Store results in context
            context.setVariable("queryResults", records);
            context.setVariable("recordCount", records.size());
            
            Map<String, Object> output = new HashMap<>();
            output.put("records", records);
            output.put("count", records.size());
            
            log.info("Query returned {} records for entity: {}", records.size(), entity);
            return ExecutionResult.success(output);
            
        } catch (Exception e) {
            log.error("Query failed for entity: {}", entity, e);
            return ExecutionResult.failed("Query failed: " + e.getMessage());
        }
    }

    private ExecutionResult handleCreate(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        String entity = (String) nodeConfig.get("entity");
        String subtype = config.getSubtype();
        
        log.info("Creating record(s) for entity: {} using {}", entity, subtype);
        
        try {
            switch (subtype) {
                case "create_multiple":
                    List<Map<String, Object>> recordsList = (List<Map<String, Object>>) nodeConfig.get("records");
                    // Resolve variables in each record
                    for (int i = 0; i < recordsList.size(); i++) {
                        recordsList.set(i, variableResolver.resolveMap(recordsList.get(i), context));
                    }
                    List<Object> createdRecords = dynamicEntityService.createMultiple(entity, recordsList);
                    
                    Map<String, Object> bulkOutput = new HashMap<>();
                    bulkOutput.put("records", createdRecords);
                    bulkOutput.put("count", createdRecords.size());
                    bulkOutput.put("created", true);
                    
                    log.info("Created {} records for entity: {}", createdRecords.size(), entity);
                    return ExecutionResult.success(bulkOutput);
                    
                case "clone_record":
                    Long cloneId = Long.parseLong(variableResolver.resolve((String) nodeConfig.get("recordId"), context));
                    Map<String, Object> overrideFields = (Map<String, Object>) nodeConfig.get("overrideFields");
                    if (overrideFields != null) {
                        overrideFields = variableResolver.resolveMap(overrideFields, context);
                    }
                    Object clonedRecord = dynamicEntityService.cloneRecord(entity, cloneId, overrideFields);
                    
                    Map<String, Object> cloneOutput = new HashMap<>();
                    cloneOutput.put("record", clonedRecord);
                    cloneOutput.put("cloned", true);
                    
                    log.info("Cloned record {} for entity: {}", cloneId, entity);
                    return ExecutionResult.success(cloneOutput);
                    
                case "create_record":
                default:
                    Map<String, Object> fields = (Map<String, Object>) nodeConfig.get("fields");
                    // Resolve variables in fields
                    fields = variableResolver.resolveMap(fields, context);
                    
                    Object createdRecord = dynamicEntityService.createRecord(entity, fields);
                    
                    // Store created record in context
                    context.setVariable("createdRecord", createdRecord);
                    
                    Map<String, Object> output = new HashMap<>();
                    output.put("record", createdRecord);
                    output.put("created", true);
                    
                    log.info("Created record for entity: {}", entity);
                    return ExecutionResult.success(output);
            }
            
        } catch (Exception e) {
            log.error("Create failed for entity: {}", entity, e);
            return ExecutionResult.failed("Create failed: " + e.getMessage());
        }
    }

    private ExecutionResult handleUpdate(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        String entity = (String) nodeConfig.get("entity");
        String subtype = config.getSubtype();
        
        log.info("Updating record(s) for entity: {} using {}", entity, subtype);
        
        try {
            Map<String, Object> fields = (Map<String, Object>) nodeConfig.get("fields");
            // Resolve variables in fields
            fields = variableResolver.resolveMap(fields, context);
            
            switch (subtype) {
                case "update_multiple":
                    List<Long> ids = (List<Long>) nodeConfig.get("recordIds");
                    List<Object> updatedRecords = dynamicEntityService.updateMultiple(entity, ids, fields);
                    
                    Map<String, Object> bulkOutput = new HashMap<>();
                    bulkOutput.put("records", updatedRecords);
                    bulkOutput.put("count", updatedRecords.size());
                    bulkOutput.put("updated", true);
                    
                    log.info("Updated {} records for entity: {}", updatedRecords.size(), entity);
                    return ExecutionResult.success(bulkOutput);
                    
                case "update_related":
                    return handleUpdateRelated(nodeConfig, context);
                    
                case "update_record":
                default:
                    String recordId = variableResolver.resolve((String) nodeConfig.get("recordId"), context);
                    Long id = Long.parseLong(recordId);
                    
                    Object updatedRecord = dynamicEntityService.updateRecord(entity, id, fields);
                    
                    // Store updated record in context
                    context.setVariable("updatedRecord", updatedRecord);
                    
                    Map<String, Object> output = new HashMap<>();
                    output.put("record", updatedRecord);
                    output.put("recordId", id);
                    output.put("updated", true);
                    
                    log.info("Updated record {} for entity: {}", id, entity);
                    return ExecutionResult.success(output);
            }
            
        } catch (Exception e) {
            log.error("Update failed for entity: {}", entity, e);
            return ExecutionResult.failed("Update failed: " + e.getMessage());
        }
    }

    private ExecutionResult handleDelete(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        String entity = (String) nodeConfig.get("entity");
        String subtype = config.getSubtype();
        
        log.info("Deleting record(s) for entity: {} using {}", entity, subtype);
        
        try {
            switch (subtype) {
                case "delete_multiple":
                    List<Long> ids = (List<Long>) nodeConfig.get("recordIds");
                    dynamicEntityService.deleteMultiple(entity, ids);
                    
                    Map<String, Object> bulkOutput = new HashMap<>();
                    bulkOutput.put("count", ids.size());
                    bulkOutput.put("deleted", true);
                    
                    log.info("Deleted {} records for entity: {}", ids.size(), entity);
                    return ExecutionResult.success(bulkOutput);
                    
                case "delete_record":
                default:
                    String recordId = variableResolver.resolve((String) nodeConfig.get("recordId"), context);
                    Long id = Long.parseLong(recordId);
                    
                    dynamicEntityService.deleteRecord(entity, id);
                    
                    Map<String, Object> output = new HashMap<>();
                    output.put("recordId", id);
                    output.put("deleted", true);
                    
                    log.info("Deleted record {} for entity: {}", id, entity);
                    return ExecutionResult.success(output);
            }
            
        } catch (Exception e) {
            log.error("Delete failed for entity: {}", entity, e);
            return ExecutionResult.failed("Delete failed: " + e.getMessage());
        }
    }

    private ExecutionResult handleSetField(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        String field = (String) nodeConfig.get("field");
        String value = variableResolver.resolve((String) nodeConfig.get("value"), context);
        
        log.info("Setting field {} to value: {}", field, value);
        
        // Update context variable
        context.setVariable(field, value);
        
        return ExecutionResult.success(Map.of("field", field, "value", value));
    }

    private ExecutionResult handleCopyField(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        String sourceField = (String) nodeConfig.get("sourceField");
        String targetField = (String) nodeConfig.get("targetField");
        
        Object value = context.getVariable(sourceField);
        context.setVariable(targetField, value);
        
        log.info("Copied field {} to {}", sourceField, targetField);
        
        return ExecutionResult.success();
    }

    private ExecutionResult handleClearField(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        String field = (String) nodeConfig.get("field");
        
        context.setVariable(field, null);
        
        log.info("Cleared field {}", field);
        
        return ExecutionResult.success();
    }

    private ExecutionResult handleIncrement(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        String field = (String) nodeConfig.get("field");
        int amount = (int) nodeConfig.getOrDefault("amount", 1);
        
        Object currentValue = context.getVariable(field);
        int newValue = (currentValue != null ? Integer.parseInt(currentValue.toString()) : 0) + amount;
        context.setVariable(field, newValue);
        
        log.info("Incremented field {} by {} to {}", field, amount, newValue);
        
        return ExecutionResult.success(Map.of("field", field, "value", newValue));
    }

    private ExecutionResult handleDecrement(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        String field = (String) nodeConfig.get("field");
        int amount = (int) nodeConfig.getOrDefault("amount", 1);
        
        Object currentValue = context.getVariable(field);
        int newValue = (currentValue != null ? Integer.parseInt(currentValue.toString()) : 0) - amount;
        context.setVariable(field, newValue);
        
        log.info("Decremented field {} by {} to {}", field, amount, newValue);
        
        return ExecutionResult.success(Map.of("field", field, "value", newValue));
    }

    private ExecutionResult handleAssignment(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        String subtype = config.getSubtype();
        
        log.info("Executing assignment operation: {}", subtype);
        
        try {
            // Handle different assignment types
            switch (subtype) {
                case "rotate_owner":
                case "assign_record":
                    return handleLeadAssignment(nodeConfig, context);
                    
                case "assign_team":
                    return handleTeamAssignment(nodeConfig, context);
                    
                default:
                    // Simple assignment
                    String assignTo = variableResolver.resolve((String) nodeConfig.get("assignTo"), context);
                    log.info("Simple assignment to: {}", assignTo);
                    return ExecutionResult.success(Map.of("assignedTo", assignTo));
            }
        } catch (Exception e) {
            log.error("Assignment failed", e);
            return ExecutionResult.failed("Assignment failed: " + e.getMessage());
        }
    }
    
    /**
     * Handle lead assignment using LeadAssignmentService with 9 strategies
     */
    private ExecutionResult handleLeadAssignment(Map<String, Object> nodeConfig, ExecutionContext context) {
        // Get assignment strategy
        String strategy = (String) nodeConfig.getOrDefault("strategy", "ROUND_ROBIN");
        Map<String, Object> strategyConfig = (Map<String, Object>) nodeConfig.get("strategyConfig");
        
        if (strategyConfig == null) {
            strategyConfig = new HashMap<>();
        }
        
        // Get lead data from context
        Map<String, Object> leadData = new HashMap<>();
        
        // Try to get lead data from trigger
        if (context.getTriggerData() != null) {
            leadData.putAll(context.getTriggerData());
        }
        
        // Override with specific fields from config if provided
        if (nodeConfig.containsKey("leadData")) {
            Map<String, Object> configLeadData = (Map<String, Object>) nodeConfig.get("leadData");
            for (Map.Entry<String, Object> entry : configLeadData.entrySet()) {
                String resolvedValue = variableResolver.resolve(entry.getValue().toString(), context);
                leadData.put(entry.getKey(), resolvedValue);
            }
        }
        
        // Add context variables to lead data
        leadData.putAll(context.getVariables());
        
        log.info("Assigning lead using strategy: {} with config: {}", strategy, strategyConfig);
        log.debug("Lead data: {}", leadData);
        
        try {
            // Call LeadAssignmentService
            Long assignedUserId = leadAssignmentService.assignLead(leadData, strategy, strategyConfig);
            
            if (assignedUserId == null) {
                log.warn("Lead assignment returned null - no user assigned");
                return ExecutionResult.failed("No user available for assignment");
            }
            
            // Update context with assigned user
            context.setVariable("assignedUserId", assignedUserId);
            context.setVariable("assignmentStrategy", strategy);
            
            log.info("Lead successfully assigned to user: {} using strategy: {}", assignedUserId, strategy);
            
            Map<String, Object> result = new HashMap<>();
            result.put("assignedTo", assignedUserId);
            result.put("strategy", strategy);
            result.put("success", true);
            
            return ExecutionResult.success(result);
            
        } catch (Exception e) {
            log.error("Lead assignment failed with strategy: {}", strategy, e);
            return ExecutionResult.failed("Lead assignment failed: " + e.getMessage());
        }
    }
    
    /**
     * Handle team assignment
     */
    private ExecutionResult handleTeamAssignment(Map<String, Object> nodeConfig, ExecutionContext context) {
        String teamId = variableResolver.resolve((String) nodeConfig.get("teamId"), context);
        
        log.info("Assigning record to team: {}", teamId);
        
        // TODO: Implement team assignment logic
        // For now, return success with team ID
        
        context.setVariable("assignedTeamId", teamId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("assignedTeam", teamId);
        result.put("success", true);
        
        return ExecutionResult.success(result);
    }


    
    /**
     * Handle updating related records
     */
    private ExecutionResult handleUpdateRelated(Map<String, Object> nodeConfig, ExecutionContext context) {
        String entity = (String) nodeConfig.get("entity");
        String recordId = variableResolver.resolve((String) nodeConfig.get("recordId"), context);
        String relationshipField = (String) nodeConfig.get("relationshipField");
        String relatedEntity = (String) nodeConfig.get("relatedEntity");
        Map<String, Object> fields = (Map<String, Object>) nodeConfig.get("fields");
        
        log.info("Updating related {} records for {} {}", relatedEntity, entity, recordId);
        
        try {
            // Resolve variables in fields
            fields = variableResolver.resolveMap(fields, context);
            
            // In production, query related records and update them
            // For now, simulate the operation
            List<Object> updatedRecords = new ArrayList<>();
            
            // Simulate finding and updating related records
            // relatedRecords = dynamicEntityService.queryRecords(relatedEntity, 
            //     Map.of(relationshipField, recordId), null);
            // for (Object record : relatedRecords) {
            //     dynamicEntityService.updateRecord(relatedEntity, record.getId(), fields);
            // }
            
            Map<String, Object> output = new HashMap<>();
            output.put("entity", entity);
            output.put("recordId", recordId);
            output.put("relatedEntity", relatedEntity);
            output.put("relationshipField", relationshipField);
            output.put("updatedCount", updatedRecords.size());
            output.put("updated", true);
            
            log.info("Updated {} related {} records", updatedRecords.size(), relatedEntity);
            return ExecutionResult.success(output);
            
        } catch (Exception e) {
            log.error("Update related failed", e);
            return ExecutionResult.failed("Update related failed: " + e.getMessage());
        }
    }
}

