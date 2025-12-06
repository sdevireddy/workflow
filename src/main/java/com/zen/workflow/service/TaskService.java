package com.zen.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Service for managing tasks, activities, and events in workflows
 * Integrates with CRM task management system
 */
@Slf4j
@Service
public class TaskService {

    @Value("${crm.service.url:http://localhost:8080}")
    private String crmServiceUrl;

    @Value("${workflow.task.enabled:true}")
    private boolean taskEnabled;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Create a new task
     */
    public Map<String, Object> createTask(Map<String, Object> taskData) {
        if (!taskEnabled) {
            log.warn("Task service is disabled. Task not created.");
            return Map.of("created", false, "reason", "Task service disabled");
        }

        try {
            log.info("Creating task: {}", taskData.get("title"));
            
            // Validate required fields
            validateTaskData(taskData);
            
            // In production, this would call the CRM service
            // For now, we'll simulate task creation
            Map<String, Object> createdTask = new HashMap<>(taskData);
            createdTask.put("id", generateId());
            createdTask.put("createdAt", new Date());
            createdTask.put("status", taskData.getOrDefault("status", "Open"));
            
            log.info("Task created successfully with ID: {}", createdTask.get("id"));
            return createdTask;
            
        } catch (Exception e) {
            log.error("Failed to create task", e);
            throw new RuntimeException("Failed to create task: " + e.getMessage(), e);
        }
    }

    /**
     * Update an existing task
     */
    public Map<String, Object> updateTask(Long taskId, Map<String, Object> updates) {
        if (!taskEnabled) {
            log.warn("Task service is disabled. Task not updated.");
            return Map.of("updated", false, "reason", "Task service disabled");
        }

        try {
            log.info("Updating task ID: {}", taskId);
            
            // In production, this would call the CRM service
            Map<String, Object> updatedTask = new HashMap<>(updates);
            updatedTask.put("id", taskId);
            updatedTask.put("updatedAt", new Date());
            
            log.info("Task updated successfully: {}", taskId);
            return updatedTask;
            
        } catch (Exception e) {
            log.error("Failed to update task: {}", taskId, e);
            throw new RuntimeException("Failed to update task: " + e.getMessage(), e);
        }
    }

    /**
     * Complete a task
     */
    public Map<String, Object> completeTask(Long taskId, String completionNotes) {
        if (!taskEnabled) {
            log.warn("Task service is disabled. Task not completed.");
            return Map.of("completed", false, "reason", "Task service disabled");
        }

        try {
            log.info("Completing task ID: {}", taskId);
            
            Map<String, Object> completedTask = new HashMap<>();
            completedTask.put("id", taskId);
            completedTask.put("status", "Completed");
            completedTask.put("completedAt", new Date());
            completedTask.put("completionNotes", completionNotes);
            
            log.info("Task completed successfully: {}", taskId);
            return completedTask;
            
        } catch (Exception e) {
            log.error("Failed to complete task: {}", taskId, e);
            throw new RuntimeException("Failed to complete task: " + e.getMessage(), e);
        }
    }

    /**
     * Assign task to user
     */
    public Map<String, Object> assignTask(Long taskId, Long userId) {
        if (!taskEnabled) {
            log.warn("Task service is disabled. Task not assigned.");
            return Map.of("assigned", false, "reason", "Task service disabled");
        }

        try {
            log.info("Assigning task {} to user {}", taskId, userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("assignedTo", userId);
            result.put("assignedAt", new Date());
            
            log.info("Task assigned successfully");
            return result;
            
        } catch (Exception e) {
            log.error("Failed to assign task", e);
            throw new RuntimeException("Failed to assign task: " + e.getMessage(), e);
        }
    }

    /**
     * Create an activity
     */
    public Map<String, Object> createActivity(Map<String, Object> activityData) {
        if (!taskEnabled) {
            log.warn("Task service is disabled. Activity not created.");
            return Map.of("created", false, "reason", "Task service disabled");
        }

        try {
            log.info("Creating activity: {}", activityData.get("type"));
            
            Map<String, Object> createdActivity = new HashMap<>(activityData);
            createdActivity.put("id", generateId());
            createdActivity.put("createdAt", new Date());
            
            log.info("Activity created successfully with ID: {}", createdActivity.get("id"));
            return createdActivity;
            
        } catch (Exception e) {
            log.error("Failed to create activity", e);
            throw new RuntimeException("Failed to create activity: " + e.getMessage(), e);
        }
    }

    /**
     * Create an event/meeting
     */
    public Map<String, Object> createEvent(Map<String, Object> eventData) {
        if (!taskEnabled) {
            log.warn("Task service is disabled. Event not created.");
            return Map.of("created", false, "reason", "Task service disabled");
        }

        try {
            log.info("Creating event: {}", eventData.get("title"));
            
            // Validate event data
            validateEventData(eventData);
            
            Map<String, Object> createdEvent = new HashMap<>(eventData);
            createdEvent.put("id", generateId());
            createdEvent.put("createdAt", new Date());
            createdEvent.put("status", "Scheduled");
            
            log.info("Event created successfully with ID: {}", createdEvent.get("id"));
            return createdEvent;
            
        } catch (Exception e) {
            log.error("Failed to create event", e);
            throw new RuntimeException("Failed to create event: " + e.getMessage(), e);
        }
    }

    /**
     * Create a meeting
     */
    public Map<String, Object> createMeeting(Map<String, Object> meetingData) {
        if (!taskEnabled) {
            log.warn("Task service is disabled. Meeting not created.");
            return Map.of("created", false, "reason", "Task service disabled");
        }

        try {
            log.info("Creating meeting: {}", meetingData.get("title"));
            
            // Validate meeting data
            validateMeetingData(meetingData);
            
            Map<String, Object> createdMeeting = new HashMap<>(meetingData);
            createdMeeting.put("id", generateId());
            createdMeeting.put("createdAt", new Date());
            createdMeeting.put("type", "Meeting");
            createdMeeting.put("status", "Scheduled");
            
            log.info("Meeting created successfully with ID: {}", createdMeeting.get("id"));
            return createdMeeting;
            
        } catch (Exception e) {
            log.error("Failed to create meeting", e);
            throw new RuntimeException("Failed to create meeting: " + e.getMessage(), e);
        }
    }

    /**
     * Add a note
     */
    public Map<String, Object> addNote(String entityType, Long entityId, String noteContent, Long createdBy) {
        if (!taskEnabled) {
            log.warn("Task service is disabled. Note not added.");
            return Map.of("added", false, "reason", "Task service disabled");
        }

        try {
            log.info("Adding note to {} ID: {}", entityType, entityId);
            
            Map<String, Object> note = new HashMap<>();
            note.put("id", generateId());
            note.put("entityType", entityType);
            note.put("entityId", entityId);
            note.put("content", noteContent);
            note.put("createdBy", createdBy);
            note.put("createdAt", new Date());
            
            log.info("Note added successfully with ID: {}", note.get("id"));
            return note;
            
        } catch (Exception e) {
            log.error("Failed to add note", e);
            throw new RuntimeException("Failed to add note: " + e.getMessage(), e);
        }
    }

    /**
     * Add a comment
     */
    public Map<String, Object> addComment(String entityType, Long entityId, String commentContent, Long createdBy) {
        if (!taskEnabled) {
            log.warn("Task service is disabled. Comment not added.");
            return Map.of("added", false, "reason", "Task service disabled");
        }

        try {
            log.info("Adding comment to {} ID: {}", entityType, entityId);
            
            Map<String, Object> comment = new HashMap<>();
            comment.put("id", generateId());
            comment.put("entityType", entityType);
            comment.put("entityId", entityId);
            comment.put("content", commentContent);
            comment.put("createdBy", createdBy);
            comment.put("createdAt", new Date());
            
            log.info("Comment added successfully with ID: {}", comment.get("id"));
            return comment;
            
        } catch (Exception e) {
            log.error("Failed to add comment", e);
            throw new RuntimeException("Failed to add comment: " + e.getMessage(), e);
        }
    }

    /**
     * Attach a file
     */
    public Map<String, Object> attachFile(String entityType, Long entityId, String fileName, 
                                          String fileUrl, Long uploadedBy) {
        if (!taskEnabled) {
            log.warn("Task service is disabled. File not attached.");
            return Map.of("attached", false, "reason", "Task service disabled");
        }

        try {
            log.info("Attaching file {} to {} ID: {}", fileName, entityType, entityId);
            
            Map<String, Object> attachment = new HashMap<>();
            attachment.put("id", generateId());
            attachment.put("entityType", entityType);
            attachment.put("entityId", entityId);
            attachment.put("fileName", fileName);
            attachment.put("fileUrl", fileUrl);
            attachment.put("uploadedBy", uploadedBy);
            attachment.put("uploadedAt", new Date());
            
            log.info("File attached successfully with ID: {}", attachment.get("id"));
            return attachment;
            
        } catch (Exception e) {
            log.error("Failed to attach file", e);
            throw new RuntimeException("Failed to attach file: " + e.getMessage(), e);
        }
    }

    // Validation methods

    private void validateTaskData(Map<String, Object> taskData) {
        if (taskData.get("title") == null || taskData.get("title").toString().trim().isEmpty()) {
            throw new IllegalArgumentException("Task title is required");
        }
    }

    private void validateEventData(Map<String, Object> eventData) {
        if (eventData.get("title") == null || eventData.get("title").toString().trim().isEmpty()) {
            throw new IllegalArgumentException("Event title is required");
        }
        if (eventData.get("startDate") == null) {
            throw new IllegalArgumentException("Event start date is required");
        }
    }

    private void validateMeetingData(Map<String, Object> meetingData) {
        if (meetingData.get("title") == null || meetingData.get("title").toString().trim().isEmpty()) {
            throw new IllegalArgumentException("Meeting title is required");
        }
        if (meetingData.get("startTime") == null) {
            throw new IllegalArgumentException("Meeting start time is required");
        }
        if (meetingData.get("attendees") == null) {
            throw new IllegalArgumentException("Meeting attendees are required");
        }
    }

    // Helper methods

    private Long generateId() {
        return System.currentTimeMillis();
    }

    public boolean isTaskServiceAvailable() {
        return taskEnabled;
    }
}
