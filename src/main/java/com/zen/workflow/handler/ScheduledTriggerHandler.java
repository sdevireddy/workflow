package com.zen.workflow.handler;

import com.zen.workflow.engine.VariableResolver;
import com.zen.workflow.model.ExecutionContext;
import com.zen.workflow.model.ExecutionResult;
import com.zen.workflow.model.NodeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Handles scheduled triggers:
 * - scheduled: One-time execution at specific date/time
 * - date_based: Trigger based on date field (e.g., 7 days before due date)
 * - recurring: Recurring schedule (daily, weekly, monthly)
 */
@Slf4j
@Component
public class ScheduledTriggerHandler implements NodeHandler {

    @Autowired
    private VariableResolver variableResolver;

    @Override
    public ExecutionResult execute(NodeConfig config, ExecutionContext context) {
        String subtype = config.getSubtype();
        log.info("Executing Scheduled Trigger: {}", subtype);

        try {
            switch (subtype) {
                case "scheduled":
                    return handleScheduled(config, context);
                case "date_based":
                    return handleDateBased(config, context);
                case "recurring":
                    return handleRecurring(config, context);
                default:
                    return ExecutionResult.failed("Unknown scheduled trigger: " + subtype);
            }
        } catch (Exception e) {
            log.error("Scheduled trigger failed", e);
            return ExecutionResult.failed(e.getMessage());
        }
    }

    /**
     * Handle one-time scheduled execution
     */
    private ExecutionResult handleScheduled(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String scheduledDateStr = variableResolver.resolve((String) nodeConfig.get("scheduledDate"), context);
        String timezone = (String) nodeConfig.getOrDefault("timezone", "UTC");
        
        log.info("Processing scheduled trigger for date: {} ({})", scheduledDateStr, timezone);
        
        try {
            Date scheduledDate = parseDate(scheduledDateStr);
            Date now = new Date();
            
            // Check if scheduled time has arrived
            if (scheduledDate.after(now)) {
                long delayMs = scheduledDate.getTime() - now.getTime();
                log.info("Scheduled time not yet reached. Delay: {} ms", delayMs);
                
                Map<String, Object> output = new HashMap<>();
                output.put("scheduled", true);
                output.put("scheduledDate", scheduledDate);
                output.put("delayMs", delayMs);
                output.put("status", "pending");
                
                // In production, this would schedule the workflow execution
                // For now, we return the schedule information
                return ExecutionResult.success(output);
            } else {
                log.info("Scheduled time has arrived. Executing workflow.");
                
                Map<String, Object> output = new HashMap<>();
                output.put("scheduled", true);
                output.put("scheduledDate", scheduledDate);
                output.put("executedAt", now);
                output.put("status", "executed");
                
                return ExecutionResult.success(output);
            }
            
        } catch (Exception e) {
            log.error("Failed to parse scheduled date: {}", scheduledDateStr, e);
            return ExecutionResult.failed("Invalid scheduled date: " + scheduledDateStr);
        }
    }

    /**
     * Handle date-based trigger (e.g., X days before/after a date field)
     */
    private ExecutionResult handleDateBased(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String dateField = (String) nodeConfig.get("dateField");
        Integer offsetDays = (Integer) nodeConfig.getOrDefault("offsetDays", 0);
        String offsetType = (String) nodeConfig.getOrDefault("offsetType", "before"); // before or after
        String timezone = (String) nodeConfig.getOrDefault("timezone", "UTC");
        
        log.info("Processing date-based trigger: {} days {} {}", 
            Math.abs(offsetDays), offsetType, dateField);
        
        try {
            // Get the date field value from context
            Object dateValue = context.getVariable(dateField);
            if (dateValue == null) {
                // Try from trigger data
                if (context.getTriggerData() != null) {
                    dateValue = context.getTriggerData().get(dateField);
                }
            }
            
            if (dateValue == null) {
                return ExecutionResult.failed("Date field '" + dateField + "' not found in context");
            }
            
            Date baseDate = convertToDate(dateValue);
            
            // Calculate trigger date
            Calendar cal = Calendar.getInstance();
            cal.setTime(baseDate);
            
            int daysToAdd = "before".equals(offsetType) ? -offsetDays : offsetDays;
            cal.add(Calendar.DAY_OF_MONTH, daysToAdd);
            
            Date triggerDate = cal.getTime();
            Date now = new Date();
            
            log.info("Base date: {}, Trigger date: {}, Now: {}", baseDate, triggerDate, now);
            
            // Check if trigger date has arrived
            boolean shouldTrigger = !triggerDate.after(now);
            
            Map<String, Object> output = new HashMap<>();
            output.put("baseDate", baseDate);
            output.put("triggerDate", triggerDate);
            output.put("shouldTrigger", shouldTrigger);
            output.put("offsetDays", offsetDays);
            output.put("offsetType", offsetType);
            
            if (shouldTrigger) {
                log.info("Date-based trigger condition met. Executing workflow.");
                output.put("status", "triggered");
            } else {
                log.info("Date-based trigger condition not yet met.");
                output.put("status", "pending");
            }
            
            return ExecutionResult.success(output);
            
        } catch (Exception e) {
            log.error("Date-based trigger failed", e);
            return ExecutionResult.failed("Date-based trigger failed: " + e.getMessage());
        }
    }

    /**
     * Handle recurring schedule
     */
    private ExecutionResult handleRecurring(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String frequency = (String) nodeConfig.getOrDefault("frequency", "daily"); // daily, weekly, monthly, yearly
        Integer interval = (Integer) nodeConfig.getOrDefault("interval", 1);
        String startDateStr = (String) nodeConfig.get("startDate");
        String endDateStr = (String) nodeConfig.get("endDate");
        List<String> daysOfWeek = (List<String>) nodeConfig.get("daysOfWeek"); // For weekly
        Integer dayOfMonth = (Integer) nodeConfig.get("dayOfMonth"); // For monthly
        String timeOfDay = (String) nodeConfig.getOrDefault("timeOfDay", "00:00"); // HH:mm
        String timezone = (String) nodeConfig.getOrDefault("timezone", "UTC");
        
        log.info("Processing recurring trigger: {} every {} {}", frequency, interval, 
            frequency.endsWith("ly") ? frequency.substring(0, frequency.length() - 2) : frequency);
        
        try {
            Date startDate = startDateStr != null ? parseDate(startDateStr) : new Date();
            Date endDate = endDateStr != null ? parseDate(endDateStr) : null;
            Date now = new Date();
            
            // Check if we're within the date range
            if (now.before(startDate)) {
                log.info("Recurring schedule not yet started. Start date: {}", startDate);
                return ExecutionResult.success(Map.of(
                    "status", "not_started",
                    "startDate", startDate,
                    "nextRun", startDate
                ));
            }
            
            if (endDate != null && now.after(endDate)) {
                log.info("Recurring schedule has ended. End date: {}", endDate);
                return ExecutionResult.success(Map.of(
                    "status", "ended",
                    "endDate", endDate
                ));
            }
            
            // Calculate next run time
            Date nextRun = calculateNextRun(frequency, interval, startDate, now, 
                daysOfWeek, dayOfMonth, timeOfDay);
            
            // Check if it's time to run
            boolean shouldRun = !nextRun.after(now);
            
            Map<String, Object> output = new HashMap<>();
            output.put("frequency", frequency);
            output.put("interval", interval);
            output.put("nextRun", nextRun);
            output.put("shouldRun", shouldRun);
            output.put("startDate", startDate);
            output.put("endDate", endDate);
            
            if (shouldRun) {
                log.info("Recurring trigger condition met. Executing workflow.");
                output.put("status", "triggered");
                output.put("executedAt", now);
                
                // Calculate next occurrence
                Date nextOccurrence = calculateNextRun(frequency, interval, nextRun, nextRun,
                    daysOfWeek, dayOfMonth, timeOfDay);
                output.put("nextOccurrence", nextOccurrence);
            } else {
                log.info("Recurring trigger not yet due. Next run: {}", nextRun);
                output.put("status", "pending");
            }
            
            return ExecutionResult.success(output);
            
        } catch (Exception e) {
            log.error("Recurring trigger failed", e);
            return ExecutionResult.failed("Recurring trigger failed: " + e.getMessage());
        }
    }

    // Helper methods

    /**
     * Parse date string in various formats
     */
    private Date parseDate(String dateStr) throws ParseException {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new ParseException("Date string is null or empty", 0);
        }
        
        // Try ISO format first
        try {
            return Date.from(LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME)
                .atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            // Ignore and try other formats
        }
        
        // Try common formats
        String[] formats = {
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
            "MM/dd/yyyy HH:mm:ss",
            "MM/dd/yyyy",
            "dd-MM-yyyy HH:mm:ss",
            "dd-MM-yyyy"
        };
        
        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                return sdf.parse(dateStr);
            } catch (ParseException e) {
                // Try next format
            }
        }
        
        throw new ParseException("Unable to parse date: " + dateStr, 0);
    }

    /**
     * Convert various date types to Date
     */
    private Date convertToDate(Object dateValue) {
        if (dateValue instanceof Date) {
            return (Date) dateValue;
        } else if (dateValue instanceof Long) {
            return new Date((Long) dateValue);
        } else if (dateValue instanceof String) {
            try {
                return parseDate((String) dateValue);
            } catch (ParseException e) {
                throw new RuntimeException("Invalid date format: " + dateValue, e);
            }
        } else if (dateValue instanceof LocalDateTime) {
            return Date.from(((LocalDateTime) dateValue).atZone(ZoneId.systemDefault()).toInstant());
        } else {
            throw new RuntimeException("Unsupported date type: " + dateValue.getClass().getName());
        }
    }

    /**
     * Calculate next run time for recurring schedule
     */
    private Date calculateNextRun(String frequency, int interval, Date startDate, Date fromDate,
                                   List<String> daysOfWeek, Integer dayOfMonth, String timeOfDay) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(fromDate);
        
        // Parse time of day
        String[] timeParts = timeOfDay.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = timeParts.length > 1 ? Integer.parseInt(timeParts[1]) : 0;
        
        switch (frequency.toLowerCase()) {
            case "daily":
                cal.add(Calendar.DAY_OF_MONTH, interval);
                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, minute);
                cal.set(Calendar.SECOND, 0);
                break;
                
            case "weekly":
                cal.add(Calendar.WEEK_OF_YEAR, interval);
                if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
                    // Set to first day of week in the list
                    int targetDay = getDayOfWeek(daysOfWeek.get(0));
                    cal.set(Calendar.DAY_OF_WEEK, targetDay);
                }
                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, minute);
                cal.set(Calendar.SECOND, 0);
                break;
                
            case "monthly":
                cal.add(Calendar.MONTH, interval);
                if (dayOfMonth != null) {
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                }
                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, minute);
                cal.set(Calendar.SECOND, 0);
                break;
                
            case "yearly":
                cal.add(Calendar.YEAR, interval);
                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, minute);
                cal.set(Calendar.SECOND, 0);
                break;
                
            default:
                log.warn("Unknown frequency: {}", frequency);
                cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        return cal.getTime();
    }

    /**
     * Convert day name to Calendar constant
     */
    private int getDayOfWeek(String dayName) {
        switch (dayName.toLowerCase()) {
            case "sunday": return Calendar.SUNDAY;
            case "monday": return Calendar.MONDAY;
            case "tuesday": return Calendar.TUESDAY;
            case "wednesday": return Calendar.WEDNESDAY;
            case "thursday": return Calendar.THURSDAY;
            case "friday": return Calendar.FRIDAY;
            case "saturday": return Calendar.SATURDAY;
            default: return Calendar.MONDAY;
        }
    }
}
