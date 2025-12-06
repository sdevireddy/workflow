package com.zen.workflow.handler;

import com.zen.workflow.engine.VariableResolver;
import com.zen.workflow.model.ExecutionContext;
import com.zen.workflow.model.ExecutionResult;
import com.zen.workflow.model.NodeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles event-based triggers:
 * - button_click, form_submit, manual_enrollment
 * - email_opened, email_clicked, email_replied
 * - page_viewed, record_assigned, owner_changed
 * - added_to_list, removed_from_list, tag_added, tag_removed
 */
@Slf4j
@Component
public class EventTriggerHandler implements NodeHandler {

    @Autowired
    private VariableResolver variableResolver;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public ExecutionResult execute(NodeConfig config, ExecutionContext context) {
        String subtype = config.getSubtype();
        log.info("Executing Event Trigger: {}", subtype);

        try {
            switch (subtype) {
                // User interaction events
                case "button_click":
                    return handleButtonClick(config, context);
                case "form_submit":
                    return handleFormSubmit(config, context);
                case "manual_enrollment":
                    return handleManualEnrollment(config, context);
                
                // Email tracking events
                case "email_opened":
                case "email_clicked":
                case "email_replied":
                    return handleEmailEvent(config, context);
                
                // Analytics events
                case "page_viewed":
                    return handlePageViewed(config, context);
                
                // Record events
                case "record_assigned":
                case "owner_changed":
                    return handleRecordEvent(config, context);
                
                // List/Tag events
                case "added_to_list":
                case "removed_from_list":
                case "tag_added":
                case "tag_removed":
                    return handleListTagEvent(config, context);
                
                default:
                    return ExecutionResult.failed("Unknown event trigger: " + subtype);
            }
        } catch (Exception e) {
            log.error("Event trigger failed", e);
            return ExecutionResult.failed(e.getMessage());
        }
    }

    /**
     * Handle button click event
     */
    private ExecutionResult handleButtonClick(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String buttonId = (String) nodeConfig.get("buttonId");
        String recordId = variableResolver.resolve((String) nodeConfig.get("recordId"), context);
        String recordType = (String) nodeConfig.get("recordType");
        
        log.info("Button click event: {} on {} {}", buttonId, recordType, recordId);
        
        // Store event data in context
        context.setVariable("buttonId", buttonId);
        context.setVariable("clickedRecordId", recordId);
        context.setVariable("clickedRecordType", recordType);
        
        Map<String, Object> output = new HashMap<>();
        output.put("triggered", true);
        output.put("eventType", "button_click");
        output.put("buttonId", buttonId);
        output.put("recordId", recordId);
        
        return ExecutionResult.success(output);
    }

    /**
     * Handle form submission event
     */
    private ExecutionResult handleFormSubmit(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String formId = (String) nodeConfig.get("formId");
        Map<String, Object> formData = (Map<String, Object>) nodeConfig.get("formData");
        
        log.info("Form submit event: {}", formId);
        
        // Resolve form data variables
        if (formData != null) {
            formData = variableResolver.resolveMap(formData, context);
        }
        
        // Store in context
        context.setVariable("formId", formId);
        context.setVariable("formData", formData);
        
        Map<String, Object> output = new HashMap<>();
        output.put("triggered", true);
        output.put("eventType", "form_submit");
        output.put("formId", formId);
        output.put("formData", formData);
        
        return ExecutionResult.success(output);
    }

    /**
     * Handle manual enrollment
     */
    private ExecutionResult handleManualEnrollment(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String recordId = variableResolver.resolve((String) nodeConfig.get("recordId"), context);
        String recordType = (String) nodeConfig.get("recordType");
        String enrolledBy = variableResolver.resolve((String) nodeConfig.get("enrolledBy"), context);
        
        log.info("Manual enrollment: {} {} by user {}", recordType, recordId, enrolledBy);
        
        context.setVariable("enrolledRecordId", recordId);
        context.setVariable("enrolledRecordType", recordType);
        context.setVariable("enrolledBy", enrolledBy);
        
        Map<String, Object> output = new HashMap<>();
        output.put("triggered", true);
        output.put("eventType", "manual_enrollment");
        output.put("recordId", recordId);
        output.put("enrolledBy", enrolledBy);
        
        return ExecutionResult.success(output);
    }

    /**
     * Handle email tracking events
     */
    private ExecutionResult handleEmailEvent(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        String subtype = config.getSubtype();
        
        String emailId = variableResolver.resolve((String) nodeConfig.get("emailId"), context);
        String recipientEmail = variableResolver.resolve((String) nodeConfig.get("recipientEmail"), context);
        String linkUrl = variableResolver.resolve((String) nodeConfig.get("linkUrl"), context);
        
        log.info("Email event: {} for email {} by {}", subtype, emailId, recipientEmail);
        
        context.setVariable("emailId", emailId);
        context.setVariable("recipientEmail", recipientEmail);
        context.setVariable("emailEventType", subtype);
        
        if (linkUrl != null) {
            context.setVariable("clickedLink", linkUrl);
        }
        
        Map<String, Object> output = new HashMap<>();
        output.put("triggered", true);
        output.put("eventType", subtype);
        output.put("emailId", emailId);
        output.put("recipientEmail", recipientEmail);
        
        return ExecutionResult.success(output);
    }

    /**
     * Handle page viewed event
     */
    private ExecutionResult handlePageViewed(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String pageUrl = variableResolver.resolve((String) nodeConfig.get("pageUrl"), context);
        String visitorId = variableResolver.resolve((String) nodeConfig.get("visitorId"), context);
        String referrer = variableResolver.resolve((String) nodeConfig.get("referrer"), context);
        
        log.info("Page viewed: {} by visitor {}", pageUrl, visitorId);
        
        context.setVariable("viewedPageUrl", pageUrl);
        context.setVariable("visitorId", visitorId);
        context.setVariable("referrer", referrer);
        
        Map<String, Object> output = new HashMap<>();
        output.put("triggered", true);
        output.put("eventType", "page_viewed");
        output.put("pageUrl", pageUrl);
        output.put("visitorId", visitorId);
        
        return ExecutionResult.success(output);
    }

    /**
     * Handle record assignment/owner change events
     */
    private ExecutionResult handleRecordEvent(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        String subtype = config.getSubtype();
        
        String recordId = variableResolver.resolve((String) nodeConfig.get("recordId"), context);
        String recordType = (String) nodeConfig.get("recordType");
        String newOwner = variableResolver.resolve((String) nodeConfig.get("newOwner"), context);
        String previousOwner = variableResolver.resolve((String) nodeConfig.get("previousOwner"), context);
        
        log.info("Record event: {} for {} {} (new owner: {})", subtype, recordType, recordId, newOwner);
        
        context.setVariable("recordId", recordId);
        context.setVariable("recordType", recordType);
        context.setVariable("newOwner", newOwner);
        context.setVariable("previousOwner", previousOwner);
        
        Map<String, Object> output = new HashMap<>();
        output.put("triggered", true);
        output.put("eventType", subtype);
        output.put("recordId", recordId);
        output.put("newOwner", newOwner);
        
        return ExecutionResult.success(output);
    }

    /**
     * Handle list/tag events
     */
    private ExecutionResult handleListTagEvent(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        String subtype = config.getSubtype();
        
        String recordId = variableResolver.resolve((String) nodeConfig.get("recordId"), context);
        String recordType = (String) nodeConfig.get("recordType");
        String listId = variableResolver.resolve((String) nodeConfig.get("listId"), context);
        String tagName = variableResolver.resolve((String) nodeConfig.get("tagName"), context);
        
        log.info("List/Tag event: {} for {} {}", subtype, recordType, recordId);
        
        context.setVariable("recordId", recordId);
        context.setVariable("recordType", recordType);
        
        if (listId != null) {
            context.setVariable("listId", listId);
        }
        if (tagName != null) {
            context.setVariable("tagName", tagName);
        }
        
        Map<String, Object> output = new HashMap<>();
        output.put("triggered", true);
        output.put("eventType", subtype);
        output.put("recordId", recordId);
        
        if (listId != null) {
            output.put("listId", listId);
        }
        if (tagName != null) {
            output.put("tagName", tagName);
        }
        
        return ExecutionResult.success(output);
    }
}
