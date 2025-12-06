package com.zen.workflow.handler;

import com.zen.workflow.engine.VariableResolver;
import com.zen.workflow.model.ExecutionContext;
import com.zen.workflow.model.ExecutionResult;
import com.zen.workflow.model.NodeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handles all record-based triggers:
 * - record_created
 * - record_updated
 * - record_deleted
 * - field_changed
 * - status_changed
 * - stage_changed
 */
@Slf4j
@Component
public class RecordTriggerHandler implements NodeHandler {

    @Autowired
    private VariableResolver variableResolver;

    @Override
    public ExecutionResult execute(NodeConfig config, ExecutionContext context) {
        String subtype = config.getSubtype();
        log.info("Executing RecordTrigger: {}", subtype);

        try {
            switch (subtype) {
                case "record_created":
                    return handleRecordCreated(config, context);
                case "record_updated":
                    return handleRecordUpdated(config, context);
                case "record_deleted":
                    return handleRecordDeleted(config, context);
                case "field_changed":
                    return handleFieldChanged(config, context);
                case "status_changed":
                    return handleStatusChanged(config, context);
                case "stage_changed":
                    return handleStageChanged(config, context);
                default:
                    return ExecutionResult.failed("Unknown trigger subtype: " + subtype);
            }
        } catch (Exception e) {
            log.error("RecordTrigger execution failed", e);
            return ExecutionResult.failed(e.getMessage());
        }
    }

    private ExecutionResult handleRecordCreated(NodeConfig config, ExecutionContext context) {
        // Trigger is already fired, just pass through
        log.info("Record created trigger - passing through");
        return ExecutionResult.success();
    }

    private ExecutionResult handleRecordUpdated(NodeConfig config, ExecutionContext context) {
        log.info("Record updated trigger - passing through");
        return ExecutionResult.success();
    }

    private ExecutionResult handleRecordDeleted(NodeConfig config, ExecutionContext context) {
        log.info("Record deleted trigger - passing through");
        return ExecutionResult.success();
    }

    private ExecutionResult handleFieldChanged(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        String field = (String) nodeConfig.get("field");
        
        log.info("Field changed trigger for field: {}", field);
        return ExecutionResult.success();
    }

    private ExecutionResult handleStatusChanged(NodeConfig config, ExecutionContext context) {
        log.info("Status changed trigger - passing through");
        return ExecutionResult.success();
    }

    private ExecutionResult handleStageChanged(NodeConfig config, ExecutionContext context) {
        log.info("Stage changed trigger - passing through");
        return ExecutionResult.success();
    }
}
