package com.zen.workflow.handler;

import com.zen.workflow.engine.VariableResolver;
import com.zen.workflow.model.ExecutionContext;
import com.zen.workflow.model.ExecutionResult;
import com.zen.workflow.model.NodeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles all delay operations:
 * - wait_duration, wait_until_date, wait_for_event, schedule_action
 */
@Slf4j
@Component
public class DelayHandler implements NodeHandler {

    @Autowired
    private VariableResolver variableResolver;

    @Override
    public ExecutionResult execute(NodeConfig config, ExecutionContext context) {
        String subtype = config.getSubtype();
        log.info("Executing Delay: {}", subtype);

        try {
            switch (subtype) {
                case "wait_duration":
                    return handleWaitDuration(config, context);
                case "wait_until_date":
                    return handleWaitUntilDate(config, context);
                case "wait_for_event":
                    return handleWaitForEvent(config, context);
                case "schedule_action":
                    return handleScheduleAction(config, context);
                default:
                    return ExecutionResult.failed("Unknown delay type: " + subtype);
            }
        } catch (Exception e) {
            log.error("Delay failed", e);
            return ExecutionResult.failed(e.getMessage());
        }
    }

    private ExecutionResult handleWaitDuration(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        int duration = (int) nodeConfig.get("duration");
        String unit = (String) nodeConfig.getOrDefault("unit", "MINUTES");
        
        log.info("Waiting for {} {}", duration, unit);
        
        // Calculate resume time
        String resumeTime = calculateResumeTime(duration, unit);
        LocalDateTime resumeDateTime = LocalDateTime.parse(resumeTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        // Store in context
        context.setVariable("resumeAt", resumeTime);
        context.setVariable("waitDuration", duration);
        context.setVariable("waitUnit", unit);
        
        // In production, schedule workflow resumption with Quartz
        // For now, check if we should wait or continue
        LocalDateTime now = LocalDateTime.now();
        if (resumeDateTime.isAfter(now)) {
            log.info("Workflow paused. Will resume at: {}", resumeTime);
            
            Map<String, Object> output = new HashMap<>();
            output.put("paused", true);
            output.put("waitDuration", duration);
            output.put("waitUnit", unit);
            output.put("resumeAt", resumeTime);
            output.put("delayMs", calculateDelayMs(duration, unit));
            
            return ExecutionResult.paused("Waiting for " + duration + " " + unit);
        } else {
            // Time has passed, continue
            Map<String, Object> output = new HashMap<>();
            output.put("waited", true);
            output.put("resumedAt", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return ExecutionResult.success(output);
        }
    }

    private ExecutionResult handleWaitUntilDate(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String targetDate = variableResolver.resolve((String) nodeConfig.get("targetDate"), context);
        String timezone = (String) nodeConfig.getOrDefault("timezone", "UTC");
        
        log.info("Waiting until date: {} ({})", targetDate, timezone);
        
        try {
            LocalDateTime targetDateTime = LocalDateTime.parse(targetDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime now = LocalDateTime.now();
            
            context.setVariable("targetDate", targetDate);
            context.setVariable("timezone", timezone);
            
            if (targetDateTime.isAfter(now)) {
                log.info("Workflow paused. Will resume at: {}", targetDate);
                
                Map<String, Object> output = new HashMap<>();
                output.put("paused", true);
                output.put("targetDate", targetDate);
                output.put("timezone", timezone);
                output.put("currentTime", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                
                return ExecutionResult.paused("Waiting until " + targetDate);
            } else {
                // Target date has passed, continue
                Map<String, Object> output = new HashMap<>();
                output.put("waited", true);
                output.put("targetDate", targetDate);
                output.put("resumedAt", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                
                return ExecutionResult.success(output);
            }
            
        } catch (Exception e) {
            log.error("Failed to parse target date: {}", targetDate, e);
            return ExecutionResult.failed("Invalid target date format: " + targetDate);
        }
    }

    private ExecutionResult handleWaitForEvent(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String eventType = (String) nodeConfig.get("eventType");
        String eventCondition = variableResolver.resolve((String) nodeConfig.get("eventCondition"), context);
        Integer timeoutMinutes = (Integer) nodeConfig.get("timeoutMinutes");
        
        log.info("Waiting for event: {} with condition: {}", eventType, eventCondition);
        
        // Store event listener info in context
        context.setVariable("waitingForEvent", eventType);
        context.setVariable("eventCondition", eventCondition);
        
        if (timeoutMinutes != null) {
            String timeoutAt = LocalDateTime.now().plusMinutes(timeoutMinutes)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            context.setVariable("eventTimeout", timeoutAt);
        }
        
        // In production, register event listener and pause workflow
        Map<String, Object> output = new HashMap<>();
        output.put("paused", true);
        output.put("eventType", eventType);
        output.put("eventCondition", eventCondition);
        output.put("waiting", true);
        
        if (timeoutMinutes != null) {
            output.put("timeoutMinutes", timeoutMinutes);
        }
        
        log.info("Workflow paused. Waiting for event: {}", eventType);
        return ExecutionResult.paused("Waiting for event: " + eventType);
    }

    private ExecutionResult handleScheduleAction(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String scheduleTime = variableResolver.resolve((String) nodeConfig.get("scheduleTime"), context);
        String actionType = (String) nodeConfig.get("actionType");
        Map<String, Object> actionConfig = (Map<String, Object>) nodeConfig.get("actionConfig");
        
        log.info("Scheduling {} action for: {}", actionType, scheduleTime);
        
        if (actionConfig != null) {
            actionConfig = variableResolver.resolveMap(actionConfig, context);
        }
        
        // Store schedule info in context
        context.setVariable("scheduledFor", scheduleTime);
        context.setVariable("scheduledAction", actionType);
        context.setVariable("scheduledActionConfig", actionConfig);
        
        // In production, create scheduled job with Quartz
        Map<String, Object> output = new HashMap<>();
        output.put("scheduled", true);
        output.put("scheduledFor", scheduleTime);
        output.put("actionType", actionType);
        output.put("scheduleId", "schedule_" + System.currentTimeMillis());
        
        log.info("Action scheduled successfully for: {}", scheduleTime);
        return ExecutionResult.success(output);
    }
    
    private long calculateDelayMs(int duration, String unit) {
        switch (unit.toUpperCase()) {
            case "MINUTES":
                return duration * 60 * 1000L;
            case "HOURS":
                return duration * 60 * 60 * 1000L;
            case "DAYS":
                return duration * 24 * 60 * 60 * 1000L;
            case "WEEKS":
                return duration * 7 * 24 * 60 * 60 * 1000L;
            default:
                return 0;
        }
    }

    private String calculateResumeTime(int duration, String unit) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime resumeTime;
        
        switch (unit.toUpperCase()) {
            case "MINUTES":
                resumeTime = now.plusMinutes(duration);
                break;
            case "HOURS":
                resumeTime = now.plusHours(duration);
                break;
            case "DAYS":
                resumeTime = now.plusDays(duration);
                break;
            case "WEEKS":
                resumeTime = now.plusWeeks(duration);
                break;
            default:
                resumeTime = now;
        }
        
        return resumeTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
