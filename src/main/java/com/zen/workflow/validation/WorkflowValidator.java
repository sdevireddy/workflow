package com.zen.workflow.validation;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Comprehensive Workflow Validator
 * Validates all node types, required fields, and business rules
 * Inspired by: Salesforce Flow Validation, HubSpot Workflow Validation
 */
@Slf4j
@Component
public class WorkflowValidator {

    /**
     * Validate entire workflow
     */
    public ValidationResult validateWorkflow(Map<String, Object> workflowConfig) {
        ValidationResult result = new ValidationResult();
        
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) workflowConfig.get("nodes");
        
        if (nodes == null || nodes.isEmpty()) {
            result.addError("WORKFLOW", "Workflow must have at least one node");
            return result;
        }
        
        // Validate each node
        for (Map<String, Object> node : nodes) {
            validateNode(node, result);
        }
        
        // Validate workflow structure
        validateWorkflowStructure(nodes, result);
        
        // Validate connections
        validateConnections(nodes, result);
        
        // Check for infinite loops
        validateNoInfiniteLoops(nodes, result);
        
        return result;
    }

    /**
     * Validate individual node
     */
    private void validateNode(Map<String, Object> node, ValidationResult result) {
        String nodeId = (String) node.get("id");
        String nodeType = (String) node.get("type");
        String nodeSubtype = (String) node.get("subtype");
        Map<String, Object> config = (Map<String, Object>) node.get("config");
        
        if (nodeId == null || nodeId.isEmpty()) {
            result.addError("NODE", "Node must have an ID");
            return;
        }
        
        if (nodeType == null || nodeType.isEmpty()) {
            result.addError(nodeId, "Node must have a type");
            return;
        }
        
        // Validate based on node type
        switch (nodeType) {
            case "trigger":
                validateTriggerNode(nodeId, nodeSubtype, config, result);
                break;
            case "condition":
                validateConditionNode(nodeId, nodeSubtype, config, result);
                break;
            case "data":
                validateDataNode(nodeId, nodeSubtype, config, result);
                break;
            case "communication":
                validateCommunicationNode(nodeId, nodeSubtype, config, result);
                break;
            case "task":
                validateTaskNode(nodeId, nodeSubtype, config, result);
                break;
            case "approval":
                validateApprovalNode(nodeId, nodeSubtype, config, result);
                break;
            case "delay":
                validateDelayNode(nodeId, nodeSubtype, config, result);
                break;
            case "integration":
                validateIntegrationNode(nodeId, nodeSubtype, config, result);
                break;
            case "list":
                validateListNode(nodeId, nodeSubtype, config, result);
                break;
            case "error":
                validateErrorNode(nodeId, nodeSubtype, config, result);
                break;
            default:
                result.addError(nodeId, "Unknown node type: " + nodeType);
        }
    }

    /**
     * Validate Trigger Nodes
     */
    private void validateTriggerNode(String nodeId, String subtype, Map<String, Object> config, ValidationResult result) {
        if (config == null) {
            config = new HashMap<>();
        }
        
        switch (subtype) {
            case "record_created":
            case "record_updated":
            case "record_deleted":
                // Required: entity
                if (!config.containsKey("entity")) {
                    result.addError(nodeId, "Trigger must specify entity (LEAD, CONTACT, DEAL, etc.)");
                }
                break;
                
            case "field_changed":
            case "status_changed":
            case "stage_changed":
                // Required: entity, field
                if (!config.containsKey("entity")) {
                    result.addError(nodeId, "Trigger must specify entity");
                }
                if (!config.containsKey("field")) {
                    result.addError(nodeId, "Trigger must specify which field changed");
                }
                break;
                
            case "scheduled":
                // Required: schedule (cron expression)
                if (!config.containsKey("schedule")) {
                    result.addError(nodeId, "Scheduled trigger must have cron expression");
                } else {
                    String schedule = (String) config.get("schedule");
                    if (!isValidCronExpression(schedule)) {
                        result.addError(nodeId, "Invalid cron expression: " + schedule);
                    }
                }
                break;
                
            case "date_based":
                // Required: dateField, offset
                if (!config.containsKey("dateField")) {
                    result.addError(nodeId, "Date-based trigger must specify date field");
                }
                if (!config.containsKey("offset")) {
                    result.addWarning(nodeId, "Date-based trigger should specify offset (e.g., -7 days)");
                }
                break;
                
            case "email_opened":
            case "email_clicked":
            case "email_replied":
                // Required: emailId or campaignId
                if (!config.containsKey("emailId") && !config.containsKey("campaignId")) {
                    result.addError(nodeId, "Email trigger must specify emailId or campaignId");
                }
                break;
                
            case "form_submit":
                // Required: formId
                if (!config.containsKey("formId")) {
                    result.addError(nodeId, "Form submission trigger must specify formId");
                }
                break;
                
            default:
                result.addWarning(nodeId, "Unknown trigger subtype: " + subtype);
        }
    }

    /**
     * Validate Condition Nodes
     */
    private void validateConditionNode(String nodeId, String subtype, Map<String, Object> config, ValidationResult result) {
        if (config == null) {
            result.addError(nodeId, "Condition node must have configuration");
            return;
        }
        
        switch (subtype) {
            case "if_else":
            case "field_check":
                // Required: field, operator, value
                if (!config.containsKey("field")) {
                    result.addError(nodeId, "Condition must specify field to check");
                }
                if (!config.containsKey("operator")) {
                    result.addError(nodeId, "Condition must specify operator (equals, greater_than, etc.)");
                } else {
                    String operator = (String) config.get("operator");
                    if (!isValidOperator(operator)) {
                        result.addError(nodeId, "Invalid operator: " + operator);
                    }
                }
                if (!config.containsKey("value") && !isNullCheckOperator((String) config.get("operator"))) {
                    result.addError(nodeId, "Condition must specify value to compare");
                }
                break;
                
            case "compare_fields":
                // Required: field1, field2, operator
                if (!config.containsKey("field1")) {
                    result.addError(nodeId, "Must specify first field");
                }
                if (!config.containsKey("field2")) {
                    result.addError(nodeId, "Must specify second field");
                }
                if (!config.containsKey("operator")) {
                    result.addError(nodeId, "Must specify comparison operator");
                }
                break;
                
            case "loop":
                // Required: collection
                if (!config.containsKey("collection")) {
                    result.addError(nodeId, "Loop must specify collection to iterate");
                }
                if (!config.containsKey("maxIterations")) {
                    result.addWarning(nodeId, "Loop should have maxIterations to prevent infinite loops");
                } else {
                    int maxIterations = (int) config.get("maxIterations");
                    if (maxIterations > 1000) {
                        result.addWarning(nodeId, "Loop maxIterations is very high (" + maxIterations + "), may cause performance issues");
                    }
                }
                break;
                
            case "formula":
                // Required: formula
                if (!config.containsKey("formula")) {
                    result.addError(nodeId, "Formula condition must specify formula");
                } else {
                    String formula = (String) config.get("formula");
                    if (!isValidFormula(formula)) {
                        result.addError(nodeId, "Invalid formula syntax");
                    }
                }
                break;
                
            default:
                result.addWarning(nodeId, "Unknown condition subtype: " + subtype);
        }
    }

    /**
     * Validate Data Operation Nodes
     */
    private void validateDataNode(String nodeId, String subtype, Map<String, Object> config, ValidationResult result) {
        if (config == null) {
            result.addError(nodeId, "Data operation must have configuration");
            return;
        }
        
        switch (subtype) {
            case "get_records":
            case "query_database":
            case "search_records":
                // Required: entity
                if (!config.containsKey("entity")) {
                    result.addError(nodeId, "Query must specify entity");
                }
                if (!config.containsKey("criteria") && !config.containsKey("query")) {
                    result.addWarning(nodeId, "Query should have criteria or query string");
                }
                if (config.containsKey("limit")) {
                    int limit = (int) config.get("limit");
                    if (limit > 10000) {
                        result.addWarning(nodeId, "Query limit is very high (" + limit + "), may cause performance issues");
                    }
                }
                break;
                
            case "create_record":
            case "create_multiple":
                // Required: entity, fields
                if (!config.containsKey("entity")) {
                    result.addError(nodeId, "Create operation must specify entity");
                }
                if (!config.containsKey("fields")) {
                    result.addError(nodeId, "Create operation must specify fields");
                } else {
                    Map<String, Object> fields = (Map<String, Object>) config.get("fields");
                    validateRequiredFields(nodeId, (String) config.get("entity"), fields, result);
                }
                break;
                
            case "update_record":
            case "update_multiple":
                // Required: entity, recordId (or criteria), fields
                if (!config.containsKey("entity")) {
                    result.addError(nodeId, "Update operation must specify entity");
                }
                if (!config.containsKey("recordId") && !config.containsKey("criteria")) {
                    result.addError(nodeId, "Update operation must specify recordId or criteria");
                }
                if (!config.containsKey("fields")) {
                    result.addError(nodeId, "Update operation must specify fields to update");
                }
                break;
                
            case "delete_record":
            case "delete_multiple":
                // Required: entity, recordId (or criteria)
                if (!config.containsKey("entity")) {
                    result.addError(nodeId, "Delete operation must specify entity");
                }
                if (!config.containsKey("recordId") && !config.containsKey("criteria")) {
                    result.addError(nodeId, "Delete operation must specify recordId or criteria");
                }
                result.addWarning(nodeId, "Delete operation is irreversible - ensure proper safeguards");
                break;
                
            case "set_field":
            case "copy_field":
            case "clear_field":
                // Required: field
                if (!config.containsKey("field")) {
                    result.addError(nodeId, "Field operation must specify field");
                }
                if ("set_field".equals(subtype) && !config.containsKey("value")) {
                    result.addError(nodeId, "Set field operation must specify value");
                }
                if ("copy_field".equals(subtype) && !config.containsKey("sourceField")) {
                    result.addError(nodeId, "Copy field operation must specify sourceField");
                }
                break;
                
            case "increment":
            case "decrement":
                // Required: field
                if (!config.containsKey("field")) {
                    result.addError(nodeId, "Increment/Decrement must specify field");
                }
                if (config.containsKey("amount")) {
                    Object amount = config.get("amount");
                    if (!(amount instanceof Number)) {
                        result.addError(nodeId, "Amount must be a number");
                    }
                }
                break;
                
            case "assign_record":
            case "rotate_owner":
            case "assign_team":
                // Required: entity, recordId, assignTo (or team)
                if (!config.containsKey("entity")) {
                    result.addError(nodeId, "Assignment must specify entity");
                }
                if (!config.containsKey("recordId")) {
                    result.addError(nodeId, "Assignment must specify recordId");
                }
                if ("assign_record".equals(subtype) && !config.containsKey("assignTo")) {
                    result.addError(nodeId, "Assignment must specify assignTo user");
                }
                if ("assign_team".equals(subtype) && !config.containsKey("team")) {
                    result.addError(nodeId, "Team assignment must specify team");
                }
                break;
                
            default:
                result.addWarning(nodeId, "Unknown data operation subtype: " + subtype);
        }
    }

    /**
     * Validate Communication Nodes
     */
    private void validateCommunicationNode(String nodeId, String subtype, Map<String, Object> config, ValidationResult result) {
        if (config == null) {
            result.addError(nodeId, "Communication node must have configuration");
            return;
        }
        
        switch (subtype) {
            case "send_email":
                // Required: to, subject, body (or templateId)
                if (!config.containsKey("to")) {
                    result.addError(nodeId, "Email must specify recipient (to)");
                } else {
                    String to = (String) config.get("to");
                    if (!isValidEmailOrVariable(to)) {
                        result.addError(nodeId, "Invalid email address: " + to);
                    }
                }
                if (!config.containsKey("subject")) {
                    result.addError(nodeId, "Email must have subject");
                }
                if (!config.containsKey("body") && !config.containsKey("templateId")) {
                    result.addError(nodeId, "Email must have body or templateId");
                }
                break;
                
            case "send_template_email":
                // Required: to, templateId
                if (!config.containsKey("to")) {
                    result.addError(nodeId, "Email must specify recipient");
                }
                if (!config.containsKey("templateId")) {
                    result.addError(nodeId, "Template email must specify templateId");
                }
                break;
                
            case "send_bulk_email":
                // Required: recipients, templateId
                if (!config.containsKey("recipients")) {
                    result.addError(nodeId, "Bulk email must specify recipients list");
                } else {
                    Object recipients = config.get("recipients");
                    if (recipients instanceof List) {
                        List<?> recipientList = (List<?>) recipients;
                        if (recipientList.size() > 1000) {
                            result.addWarning(nodeId, "Bulk email has " + recipientList.size() + " recipients, may hit rate limits");
                        }
                    }
                }
                if (!config.containsKey("templateId")) {
                    result.addError(nodeId, "Bulk email must specify templateId");
                }
                break;
                
            case "send_sms":
            case "send_whatsapp":
                // Required: phoneNumber, message
                if (!config.containsKey("phoneNumber")) {
                    result.addError(nodeId, "SMS/WhatsApp must specify phoneNumber");
                } else {
                    String phone = (String) config.get("phoneNumber");
                    if (!isValidPhoneOrVariable(phone)) {
                        result.addWarning(nodeId, "Phone number format may be invalid: " + phone);
                    }
                }
                if (!config.containsKey("message") && !config.containsKey("templateId")) {
                    result.addError(nodeId, "SMS/WhatsApp must have message or templateId");
                }
                break;
                
            case "send_notification":
            case "internal_notification":
                // Required: userId, title, message
                if (!config.containsKey("userId") && !config.containsKey("userIds")) {
                    result.addError(nodeId, "Notification must specify userId or userIds");
                }
                if (!config.containsKey("title")) {
                    result.addError(nodeId, "Notification must have title");
                }
                if (!config.containsKey("message")) {
                    result.addError(nodeId, "Notification must have message");
                }
                break;
                
            case "post_to_chat":
            case "slack_message":
                // Required: channel, message
                if (!config.containsKey("channel")) {
                    result.addError(nodeId, "Chat message must specify channel");
                }
                if (!config.containsKey("message")) {
                    result.addError(nodeId, "Chat message must have message");
                }
                break;
                
            default:
                result.addWarning(nodeId, "Unknown communication subtype: " + subtype);
        }
    }

    /**
     * Validate Task Nodes
     */
    private void validateTaskNode(String nodeId, String subtype, Map<String, Object> config, ValidationResult result) {
        if (config == null) {
            result.addError(nodeId, "Task node must have configuration");
            return;
        }
        
        switch (subtype) {
            case "create_task":
            case "create_activity":
                // Required: title, assignTo
                if (!config.containsKey("title")) {
                    result.addError(nodeId, "Task must have title");
                }
                if (!config.containsKey("assignTo")) {
                    result.addError(nodeId, "Task must specify assignTo user");
                }
                if (!config.containsKey("dueDate")) {
                    result.addWarning(nodeId, "Task should have dueDate");
                }
                break;
                
            case "create_event":
            case "create_meeting":
                // Required: title, startDate, endDate
                if (!config.containsKey("title")) {
                    result.addError(nodeId, "Event must have title");
                }
                if (!config.containsKey("startDate")) {
                    result.addError(nodeId, "Event must have startDate");
                }
                if (!config.containsKey("endDate")) {
                    result.addError(nodeId, "Event must have endDate");
                }
                if ("create_meeting".equals(subtype) && !config.containsKey("attendees")) {
                    result.addWarning(nodeId, "Meeting should have attendees");
                }
                break;
                
            case "update_task":
            case "complete_task":
            case "assign_task":
                // Required: taskId
                if (!config.containsKey("taskId")) {
                    result.addError(nodeId, "Task operation must specify taskId");
                }
                if ("assign_task".equals(subtype) && !config.containsKey("assignTo")) {
                    result.addError(nodeId, "Task assignment must specify assignTo user");
                }
                break;
                
            case "add_note":
            case "add_comment":
                // Required: recordId, note/comment
                if (!config.containsKey("recordId")) {
                    result.addError(nodeId, "Note/Comment must specify recordId");
                }
                if (!config.containsKey("note") && !config.containsKey("comment")) {
                    result.addError(nodeId, "Must specify note or comment text");
                }
                break;
                
            case "attach_file":
                // Required: recordId, fileUrl or fileId
                if (!config.containsKey("recordId")) {
                    result.addError(nodeId, "File attachment must specify recordId");
                }
                if (!config.containsKey("fileUrl") && !config.containsKey("fileId")) {
                    result.addError(nodeId, "File attachment must specify fileUrl or fileId");
                }
                break;
                
            default:
                result.addWarning(nodeId, "Unknown task subtype: " + subtype);
        }
    }

    /**
     * Validate Approval Nodes
     */
    private void validateApprovalNode(String nodeId, String subtype, Map<String, Object> config, ValidationResult result) {
        if (config == null) {
            result.addError(nodeId, "Approval node must have configuration");
            return;
        }
        
        // Required: approvers, message
        if (!config.containsKey("approvers")) {
            result.addError(nodeId, "Approval must specify approvers");
        } else {
            Object approvers = config.get("approvers");
            if (approvers instanceof List) {
                List<?> approverList = (List<?>) approvers;
                if (approverList.isEmpty()) {
                    result.addError(nodeId, "Approval must have at least one approver");
                }
                if (approverList.size() > 10) {
                    result.addWarning(nodeId, "Approval has many approvers (" + approverList.size() + "), may cause delays");
                }
            }
        }
        
        if (!config.containsKey("message")) {
            result.addWarning(nodeId, "Approval should have message for approvers");
        }
        
        if (config.containsKey("expiresIn")) {
            int expiresIn = (int) config.get("expiresIn");
            if (expiresIn < 1) {
                result.addError(nodeId, "Approval expiration must be at least 1 hour");
            }
        }
    }

    /**
     * Validate Delay Nodes
     */
    private void validateDelayNode(String nodeId, String subtype, Map<String, Object> config, ValidationResult result) {
        if (config == null) {
            result.addError(nodeId, "Delay node must have configuration");
            return;
        }
        
        switch (subtype) {
            case "wait_duration":
                // Required: duration, unit
                if (!config.containsKey("duration")) {
                    result.addError(nodeId, "Wait duration must specify duration");
                } else {
                    int duration = (int) config.get("duration");
                    if (duration < 1) {
                        result.addError(nodeId, "Duration must be at least 1");
                    }
                    if (duration > 365 && "DAYS".equals(config.get("unit"))) {
                        result.addWarning(nodeId, "Wait duration is very long (" + duration + " days)");
                    }
                }
                if (!config.containsKey("unit")) {
                    result.addError(nodeId, "Wait duration must specify unit (MINUTES, HOURS, DAYS, WEEKS)");
                }
                break;
                
            case "wait_until_date":
                // Required: targetDate
                if (!config.containsKey("targetDate")) {
                    result.addError(nodeId, "Wait until date must specify targetDate");
                }
                break;
                
            case "wait_for_event":
                // Required: eventType
                if (!config.containsKey("eventType")) {
                    result.addError(nodeId, "Wait for event must specify eventType");
                }
                if (!config.containsKey("timeout")) {
                    result.addWarning(nodeId, "Wait for event should have timeout to prevent indefinite waiting");
                }
                break;
                
            case "schedule_action":
                // Required: scheduleTime
                if (!config.containsKey("scheduleTime")) {
                    result.addError(nodeId, "Schedule action must specify scheduleTime");
                }
                break;
                
            default:
                result.addWarning(nodeId, "Unknown delay subtype: " + subtype);
        }
    }

    /**
     * Validate Integration Nodes
     */
    private void validateIntegrationNode(String nodeId, String subtype, Map<String, Object> config, ValidationResult result) {
        if (config == null) {
            result.addError(nodeId, "Integration node must have configuration");
            return;
        }
        
        switch (subtype) {
            case "webhook":
            case "api_call":
                // Required: url, method
                if (!config.containsKey("url")) {
                    result.addError(nodeId, "Webhook/API call must specify url");
                } else {
                    String url = (String) config.get("url");
                    if (!isValidUrlOrVariable(url)) {
                        result.addError(nodeId, "Invalid URL: " + url);
                    }
                }
                if (!config.containsKey("method")) {
                    result.addWarning(nodeId, "HTTP method not specified, will default to POST");
                } else {
                    String method = (String) config.get("method");
                    if (!isValidHttpMethod(method)) {
                        result.addError(nodeId, "Invalid HTTP method: " + method);
                    }
                }
                if (!config.containsKey("timeout")) {
                    result.addWarning(nodeId, "No timeout specified, may cause workflow to hang");
                }
                break;
                
            case "custom_function":
                // Required: functionName
                if (!config.containsKey("functionName")) {
                    result.addError(nodeId, "Custom function must specify functionName");
                }
                break;
                
            case "call_subflow":
                // Required: subflowId
                if (!config.containsKey("subflowId")) {
                    result.addError(nodeId, "Sub-workflow call must specify subflowId");
                }
                break;
                
            case "external_service":
                // Required: serviceName, action
                if (!config.containsKey("serviceName")) {
                    result.addError(nodeId, "External service must specify serviceName");
                }
                if (!config.containsKey("action")) {
                    result.addError(nodeId, "External service must specify action");
                }
                break;
                
            default:
                result.addWarning(nodeId, "Unknown integration subtype: " + subtype);
        }
    }

    /**
     * Validate List Management Nodes
     */
    private void validateListNode(String nodeId, String subtype, Map<String, Object> config, ValidationResult result) {
        if (config == null) {
            result.addError(nodeId, "List node must have configuration");
            return;
        }
        
        // Required: recordId, listId (or tag)
        if (!config.containsKey("recordId")) {
            result.addError(nodeId, "List operation must specify recordId");
        }
        
        if (subtype.contains("list") && !config.containsKey("listId")) {
            result.addError(nodeId, "List operation must specify listId");
        }
        
        if (subtype.contains("tag") && !config.containsKey("tag")) {
            result.addError(nodeId, "Tag operation must specify tag");
        }
    }

    /**
     * Validate Error Handler Nodes
     */
    private void validateErrorNode(String nodeId, String subtype, Map<String, Object> config, ValidationResult result) {
        if (config == null) {
            config = new HashMap<>();
        }
        
        switch (subtype) {
            case "retry_on_failure":
                if (!config.containsKey("maxRetries")) {
                    result.addWarning(nodeId, "Retry should specify maxRetries");
                } else {
                    int maxRetries = (int) config.get("maxRetries");
                    if (maxRetries > 10) {
                        result.addWarning(nodeId, "Max retries is very high (" + maxRetries + ")");
                    }
                }
                break;
                
            case "stop_workflow":
                if (!config.containsKey("reason")) {
                    result.addWarning(nodeId, "Stop workflow should specify reason");
                }
                break;
        }
    }

    // Helper methods continue in next part...

    /**
     * Validate workflow structure
     */
    private void validateWorkflowStructure(List<Map<String, Object>> nodes, ValidationResult result) {
        // Must have at least one trigger
        boolean hasTrigger = nodes.stream()
            .anyMatch(node -> "trigger".equals(node.get("type")));
        
        if (!hasTrigger) {
            result.addError("WORKFLOW", "Workflow must have at least one trigger node");
        }
        
        // Check for orphaned nodes (nodes with no incoming connections)
        Set<String> nodeIds = new HashSet<>();
        Set<String> connectedNodes = new HashSet<>();
        
        for (Map<String, Object> node : nodes) {
            String nodeId = (String) node.get("id");
            nodeIds.add(nodeId);
            
            Map<String, Object> connections = (Map<String, Object>) node.get("connections");
            if (connections != null) {
                connections.values().forEach(target -> {
                    if (target != null) {
                        connectedNodes.add(target.toString());
                    }
                });
            }
        }
        
        // Trigger nodes don't need incoming connections
        for (Map<String, Object> node : nodes) {
            String nodeId = (String) node.get("id");
            String nodeType = (String) node.get("type");
            
            if (!"trigger".equals(nodeType) && !connectedNodes.contains(nodeId)) {
                result.addWarning(nodeId, "Node is not connected to any other node (orphaned)");
            }
        }
    }

    /**
     * Validate connections
     */
    private void validateConnections(List<Map<String, Object>> nodes, ValidationResult result) {
        Set<String> nodeIds = nodes.stream()
            .map(node -> (String) node.get("id"))
            .collect(java.util.stream.Collectors.toSet());
        
        for (Map<String, Object> node : nodes) {
            String nodeId = (String) node.get("id");
            Map<String, Object> connections = (Map<String, Object>) node.get("connections");
            
            if (connections != null) {
                for (Map.Entry<String, Object> entry : connections.entrySet()) {
                    Object targetId = entry.getValue();
                    if (targetId != null && !nodeIds.contains(targetId.toString())) {
                        result.addError(nodeId, "Connection points to non-existent node: " + targetId);
                    }
                }
            }
        }
    }

    /**
     * Check for infinite loops
     */
    private void validateNoInfiniteLoops(List<Map<String, Object>> nodes, ValidationResult result) {
        Map<String, List<String>> graph = new HashMap<>();
        
        // Build graph
        for (Map<String, Object> node : nodes) {
            String nodeId = (String) node.get("id");
            List<String> targets = new ArrayList<>();
            
            Map<String, Object> connections = (Map<String, Object>) node.get("connections");
            if (connections != null) {
                connections.values().forEach(target -> {
                    if (target != null) {
                        targets.add(target.toString());
                    }
                });
            }
            
            graph.put(nodeId, targets);
        }
        
        // Detect cycles using DFS
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        for (String nodeId : graph.keySet()) {
            if (hasCycle(nodeId, graph, visited, recursionStack)) {
                result.addError("WORKFLOW", "Workflow contains infinite loop involving node: " + nodeId);
                break;
            }
        }
    }

    private boolean hasCycle(String nodeId, Map<String, List<String>> graph, 
                            Set<String> visited, Set<String> recursionStack) {
        visited.add(nodeId);
        recursionStack.add(nodeId);
        
        List<String> neighbors = graph.get(nodeId);
        if (neighbors != null) {
            for (String neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    if (hasCycle(neighbor, graph, visited, recursionStack)) {
                        return true;
                    }
                } else if (recursionStack.contains(neighbor)) {
                    return true;
                }
            }
        }
        
        recursionStack.remove(nodeId);
        return false;
    }

    /**
     * Validate required fields for entity
     */
    private void validateRequiredFields(String nodeId, String entity, 
                                       Map<String, Object> fields, ValidationResult result) {
        if (fields == null || fields.isEmpty()) {
            result.addError(nodeId, "No fields specified for " + entity);
            return;
        }
        
        List<String> requiredFields = getRequiredFieldsForEntity(entity);
        
        for (String requiredField : requiredFields) {
            if (!fields.containsKey(requiredField)) {
                result.addError(nodeId, "Missing required field for " + entity + ": " + requiredField);
            }
        }
    }

    /**
     * Get required fields for entity
     */
    private List<String> getRequiredFieldsForEntity(String entity) {
        switch (entity) {
            case "LEAD":
                return Arrays.asList("firstName", "lastName", "email");
            case "CONTACT":
                return Arrays.asList("firstName", "lastName", "email");
            case "DEAL":
                return Arrays.asList("name", "amount", "stage");
            case "ACCOUNT":
                return Arrays.asList("name");
            case "TASK":
                return Arrays.asList("title", "assignTo");
            default:
                return Collections.emptyList();
        }
    }

    // Validation helper methods
    
    private boolean isValidCronExpression(String cron) {
        // Basic cron validation (5 or 6 fields)
        String[] parts = cron.split("\\s+");
        return parts.length == 5 || parts.length == 6;
    }

    private boolean isValidOperator(String operator) {
        return Arrays.asList(
            "equals", "not_equals", "contains", "not_contains",
            "starts_with", "ends_with", "greater_than", "less_than",
            "greater_than_or_equal", "less_than_or_equal",
            "is_null", "is_not_null", "is_empty", "is_not_empty",
            "in", "not_in"
        ).contains(operator);
    }

    private boolean isNullCheckOperator(String operator) {
        return "is_null".equals(operator) || "is_not_null".equals(operator) ||
               "is_empty".equals(operator) || "is_not_empty".equals(operator);
    }

    private boolean isValidFormula(String formula) {
        // Basic formula validation
        return formula != null && !formula.trim().isEmpty();
    }

    private boolean isValidEmailOrVariable(String email) {
        // Check if it's a variable or valid email
        if (email.contains("{{") && email.contains("}}")) {
            return true; // Variable
        }
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private boolean isValidPhoneOrVariable(String phone) {
        // Check if it's a variable or valid phone
        if (phone.contains("{{") && phone.contains("}}")) {
            return true; // Variable
        }
        return phone.matches("^\\+?[1-9]\\d{1,14}$");
    }

    private boolean isValidUrlOrVariable(String url) {
        // Check if it's a variable or valid URL
        if (url.contains("{{") && url.contains("}}")) {
            return true; // Variable
        }
        return url.matches("^https?://.*");
    }

    private boolean isValidHttpMethod(String method) {
        return Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS")
            .contains(method.toUpperCase());
    }

    /**
     * Validation Result class
     */
    @Data
    public static class ValidationResult {
        private List<ValidationError> errors = new ArrayList<>();
        private List<ValidationError> warnings = new ArrayList<>();
        
        public void addError(String nodeId, String message) {
            errors.add(new ValidationError("ERROR", nodeId, message));
        }
        
        public void addWarning(String nodeId, String message) {
            warnings.add(new ValidationError("WARNING", nodeId, message));
        }
        
        public boolean isValid() {
            return errors.isEmpty();
        }
        
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
        
        public int getErrorCount() {
            return errors.size();
        }
        
        public int getWarningCount() {
            return warnings.size();
        }
    }

    /**
     * Validation Error class
     */
    @Data
    public static class ValidationError {
        private final String severity;
        private final String nodeId;
        private final String message;
        
        public ValidationError(String severity, String nodeId, String message) {
            this.severity = severity;
            this.nodeId = nodeId;
            this.message = message;
        }
    }
}
