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
 * Handles all task and activity operations:
 * - create_task, create_activity, create_event, create_meeting
 * - update_task, complete_task, assign_task
 * - add_note, add_comment, attach_file
 */
@Slf4j
@Component
public class TaskManagementHandler implements NodeHandler {

    @Autowired
    private VariableResolver variableResolver;
    
    @Autowired
    private com.zen.workflow.service.TaskService taskService;

    @Override
    public ExecutionResult execute(NodeConfig config, ExecutionContext context) {
        String subtype = config.getSubtype();
        log.info("Executing Task Operation: {}", subtype);

        try {
            switch (subtype) {
                case "create_task":
                    return handleCreateTask(config, context);
                case "create_activity":
                    return handleCreateActivity(config, context);
                case "create_event":
                    return handleCreateEvent(config, context);
                case "create_meeting":
                    return handleCreateMeeting(config, context);
                case "update_task":
                    return handleUpdateTask(config, context);
                case "complete_task":
                    return handleCompleteTask(config, context);
                case "assign_task":
                    return handleAssignTask(config, context);
                case "add_note":
                    return handleAddNote(config, context);
                case "add_comment":
                    return handleAddComment(config, context);
                case "attach_file":
                    return handleAttachFile(config, context);
                default:
                    return ExecutionResult.failed("Unknown task operation: " + subtype);
            }
        } catch (Exception e) {
            log.error("Task operation failed", e);
            return ExecutionResult.failed(e.getMessage());
        }
    }

    private ExecutionResult handleCreateTask(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String title = variableResolver.resolve((String) nodeConfig.get("title"), context);
        String description = variableResolver.resolve((String) nodeConfig.get("description"), context);
        String assignTo = variableResolver.resolve((String) nodeConfig.get("assignTo"), context);
        String dueDate = variableResolver.resolve((String) nodeConfig.get("dueDate"), context);
        String priority = (String) nodeConfig.getOrDefault("priority", "MEDIUM");
        String status = (String) nodeConfig.getOrDefault("status", "Open");
        
        log.info("Creating task: {} for user: {}", title, assignTo);
        
        try {
            // Check if task service is available
            if (!taskService.isTaskServiceAvailable()) {
                log.warn("Task service not available. Task will not be created.");
                Map<String, Object> output = new HashMap<>();
                output.put("created", false);
                output.put("reason", "Task service not configured");
                return ExecutionResult.success(output);
            }
            
            // Prepare task data
            Map<String, Object> taskData = new HashMap<>();
            taskData.put("title", title);
            taskData.put("description", description);
            taskData.put("assignedTo", assignTo != null ? Long.parseLong(assignTo) : null);
            taskData.put("dueDate", dueDate);
            taskData.put("priority", priority);
            taskData.put("status", status);
            
            // Add any additional fields from config
            if (nodeConfig.containsKey("relatedTo")) {
                taskData.put("relatedTo", variableResolver.resolve((String) nodeConfig.get("relatedTo"), context));
            }
            if (nodeConfig.containsKey("relatedType")) {
                taskData.put("relatedType", nodeConfig.get("relatedType"));
            }
            
            // Create task
            Map<String, Object> createdTask = taskService.createTask(taskData);
            
            // Store in context
            context.setVariable("createdTask", createdTask);
            context.setVariable("taskId", createdTask.get("id"));
            
            log.info("Task created successfully with ID: {}", createdTask.get("id"));
            return ExecutionResult.success(createdTask);
            
        } catch (Exception e) {
            log.error("Failed to create task", e);
            return ExecutionResult.failed("Task creation failed: " + e.getMessage());
        }
    }

    private ExecutionResult handleCreateActivity(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String activityType = (String) nodeConfig.get("activityType");
        String subject = variableResolver.resolve((String) nodeConfig.get("subject"), context);
        String description = variableResolver.resolve((String) nodeConfig.get("description"), context);
        String relatedTo = variableResolver.resolve((String) nodeConfig.get("relatedTo"), context);
        String relatedType = (String) nodeConfig.get("relatedType");
        
        log.info("Creating activity: {} of type: {}", subject, activityType);
        
        try {
            if (!taskService.isTaskServiceAvailable()) {
                log.warn("Task service not available. Activity will not be created.");
                return ExecutionResult.success(Map.of("created", false, "reason", "Task service not configured"));
            }
            
            // Prepare activity data
            Map<String, Object> activityData = new HashMap<>();
            activityData.put("type", activityType);
            activityData.put("subject", subject);
            activityData.put("description", description);
            activityData.put("relatedTo", relatedTo);
            activityData.put("relatedType", relatedType);
            
            // Create activity
            Map<String, Object> createdActivity = taskService.createActivity(activityData);
            
            // Store in context
            context.setVariable("createdActivity", createdActivity);
            context.setVariable("activityId", createdActivity.get("id"));
            
            log.info("Activity created successfully with ID: {}", createdActivity.get("id"));
            return ExecutionResult.success(createdActivity);
            
        } catch (Exception e) {
            log.error("Failed to create activity", e);
            return ExecutionResult.failed("Activity creation failed: " + e.getMessage());
        }
    }

    private ExecutionResult handleCreateEvent(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String title = variableResolver.resolve((String) nodeConfig.get("title"), context);
        String startDate = variableResolver.resolve((String) nodeConfig.get("startDate"), context);
        String endDate = variableResolver.resolve((String) nodeConfig.get("endDate"), context);
        String location = variableResolver.resolve((String) nodeConfig.get("location"), context);
        String description = variableResolver.resolve((String) nodeConfig.get("description"), context);
        
        log.info("Creating event: {} from {} to {}", title, startDate, endDate);
        
        try {
            if (!taskService.isTaskServiceAvailable()) {
                return ExecutionResult.success(Map.of("created", false, "reason", "Task service not configured"));
            }
            
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("title", title);
            eventData.put("startDate", startDate);
            eventData.put("endDate", endDate);
            eventData.put("location", location);
            eventData.put("description", description);
            
            Map<String, Object> createdEvent = taskService.createEvent(eventData);
            context.setVariable("createdEvent", createdEvent);
            context.setVariable("eventId", createdEvent.get("id"));
            
            return ExecutionResult.success(createdEvent);
        } catch (Exception e) {
            log.error("Failed to create event", e);
            return ExecutionResult.failed("Event creation failed: " + e.getMessage());
        }
    }

    private ExecutionResult handleCreateMeeting(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String title = variableResolver.resolve((String) nodeConfig.get("title"), context);
        String startTime = variableResolver.resolve((String) nodeConfig.get("startTime"), context);
        String endTime = variableResolver.resolve((String) nodeConfig.get("endTime"), context);
        String location = variableResolver.resolve((String) nodeConfig.get("location"), context);
        
        log.info("Creating meeting: {} at {}", title, startTime);
        
        try {
            if (!taskService.isTaskServiceAvailable()) {
                return ExecutionResult.success(Map.of("created", false, "reason", "Task service not configured"));
            }
            
            Map<String, Object> meetingData = new HashMap<>();
            meetingData.put("title", title);
            meetingData.put("startTime", startTime);
            meetingData.put("endTime", endTime);
            meetingData.put("location", location);
            meetingData.put("attendees", nodeConfig.get("attendees"));
            
            Map<String, Object> createdMeeting = taskService.createMeeting(meetingData);
            context.setVariable("createdMeeting", createdMeeting);
            context.setVariable("meetingId", createdMeeting.get("id"));
            
            return ExecutionResult.success(createdMeeting);
        } catch (Exception e) {
            log.error("Failed to create meeting", e);
            return ExecutionResult.failed("Meeting creation failed: " + e.getMessage());
        }
    }

    private ExecutionResult handleUpdateTask(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String taskId = variableResolver.resolve((String) nodeConfig.get("taskId"), context);
        Map<String, Object> updates = (Map<String, Object>) nodeConfig.get("updates");
        
        log.info("Updating task: {}", taskId);
        
        try {
            if (!taskService.isTaskServiceAvailable()) {
                return ExecutionResult.success(Map.of("updated", false, "reason", "Task service not configured"));
            }
            
            // Resolve variables in updates
            updates = variableResolver.resolveMap(updates, context);
            
            Map<String, Object> updatedTask = taskService.updateTask(Long.parseLong(taskId), updates);
            context.setVariable("updatedTask", updatedTask);
            
            return ExecutionResult.success(updatedTask);
        } catch (Exception e) {
            log.error("Failed to update task", e);
            return ExecutionResult.failed("Task update failed: " + e.getMessage());
        }
    }

    private ExecutionResult handleCompleteTask(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String taskId = variableResolver.resolve((String) nodeConfig.get("taskId"), context);
        String completionNotes = variableResolver.resolve((String) nodeConfig.get("completionNotes"), context);
        
        log.info("Completing task: {}", taskId);
        
        try {
            if (!taskService.isTaskServiceAvailable()) {
                return ExecutionResult.success(Map.of("completed", false, "reason", "Task service not configured"));
            }
            
            Map<String, Object> completedTask = taskService.completeTask(Long.parseLong(taskId), completionNotes);
            context.setVariable("completedTask", completedTask);
            
            return ExecutionResult.success(completedTask);
        } catch (Exception e) {
            log.error("Failed to complete task", e);
            return ExecutionResult.failed("Task completion failed: " + e.getMessage());
        }
    }

    private ExecutionResult handleAssignTask(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String taskId = variableResolver.resolve((String) nodeConfig.get("taskId"), context);
        String assignTo = variableResolver.resolve((String) nodeConfig.get("assignTo"), context);
        
        log.info("Assigning task {} to user: {}", taskId, assignTo);
        
        try {
            if (!taskService.isTaskServiceAvailable()) {
                return ExecutionResult.success(Map.of("assigned", false, "reason", "Task service not configured"));
            }
            
            Map<String, Object> result = taskService.assignTask(Long.parseLong(taskId), Long.parseLong(assignTo));
            context.setVariable("taskAssignment", result);
            
            return ExecutionResult.success(result);
        } catch (Exception e) {
            log.error("Failed to assign task", e);
            return ExecutionResult.failed("Task assignment failed: " + e.getMessage());
        }
    }

    private ExecutionResult handleAddNote(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String entityType = (String) nodeConfig.getOrDefault("entityType", "Lead");
        String entityId = variableResolver.resolve((String) nodeConfig.get("entityId"), context);
        String noteContent = variableResolver.resolve((String) nodeConfig.get("note"), context);
        String createdBy = variableResolver.resolve((String) nodeConfig.get("createdBy"), context);
        
        log.info("Adding note to {} ID: {}", entityType, entityId);
        
        try {
            if (!taskService.isTaskServiceAvailable()) {
                return ExecutionResult.success(Map.of("added", false, "reason", "Task service not configured"));
            }
            
            Map<String, Object> note = taskService.addNote(
                entityType, 
                Long.parseLong(entityId), 
                noteContent, 
                createdBy != null ? Long.parseLong(createdBy) : null
            );
            
            context.setVariable("createdNote", note);
            context.setVariable("noteId", note.get("id"));
            
            return ExecutionResult.success(note);
        } catch (Exception e) {
            log.error("Failed to add note", e);
            return ExecutionResult.failed("Note creation failed: " + e.getMessage());
        }
    }

    private ExecutionResult handleAddComment(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String entityType = (String) nodeConfig.getOrDefault("entityType", "Lead");
        String entityId = variableResolver.resolve((String) nodeConfig.get("entityId"), context);
        String commentContent = variableResolver.resolve((String) nodeConfig.get("comment"), context);
        String createdBy = variableResolver.resolve((String) nodeConfig.get("createdBy"), context);
        
        log.info("Adding comment to {} ID: {}", entityType, entityId);
        
        try {
            if (!taskService.isTaskServiceAvailable()) {
                return ExecutionResult.success(Map.of("added", false, "reason", "Task service not configured"));
            }
            
            Map<String, Object> comment = taskService.addComment(
                entityType, 
                Long.parseLong(entityId), 
                commentContent, 
                createdBy != null ? Long.parseLong(createdBy) : null
            );
            
            context.setVariable("createdComment", comment);
            context.setVariable("commentId", comment.get("id"));
            
            return ExecutionResult.success(comment);
        } catch (Exception e) {
            log.error("Failed to add comment", e);
            return ExecutionResult.failed("Comment creation failed: " + e.getMessage());
        }
    }

    private ExecutionResult handleAttachFile(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String entityType = (String) nodeConfig.getOrDefault("entityType", "Lead");
        String entityId = variableResolver.resolve((String) nodeConfig.get("entityId"), context);
        String fileName = variableResolver.resolve((String) nodeConfig.get("fileName"), context);
        String fileUrl = variableResolver.resolve((String) nodeConfig.get("fileUrl"), context);
        String uploadedBy = variableResolver.resolve((String) nodeConfig.get("uploadedBy"), context);
        
        log.info("Attaching file {} to {} ID: {}", fileName, entityType, entityId);
        
        try {
            if (!taskService.isTaskServiceAvailable()) {
                return ExecutionResult.success(Map.of("attached", false, "reason", "Task service not configured"));
            }
            
            Map<String, Object> attachment = taskService.attachFile(
                entityType, 
                Long.parseLong(entityId), 
                fileName, 
                fileUrl, 
                uploadedBy != null ? Long.parseLong(uploadedBy) : null
            );
            
            context.setVariable("createdAttachment", attachment);
            context.setVariable("attachmentId", attachment.get("id"));
            
            return ExecutionResult.success(attachment);
        } catch (Exception e) {
            log.error("Failed to attach file", e);
            return ExecutionResult.failed("File attachment failed: " + e.getMessage());
        }
    }
}
