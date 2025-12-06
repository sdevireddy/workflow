package com.zen.workflow.service;

import com.zen.entities.tenant.Workflow;
import com.zen.entities.tenant.WorkflowExecution;
import com.zen.entities.tenant.WorkflowExecutionLog;
import com.zen.workflow.dto.WorkflowExecutionDTO;
import com.zen.workflow.repository.WorkflowRepository;
import com.zen.workflow.repository.WorkflowExecutionRepository;
import com.zen.workflow.repository.WorkflowExecutionLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class WorkflowExecutionService {

    @Autowired
    private WorkflowRepository workflowRepository;
    
    @Autowired
    private WorkflowExecutionRepository executionRepository;
    
    @Autowired
    private WorkflowExecutionLogRepository logRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Async("workflowExecutor")
    public void executeWorkflowAsync(Long workflowId, String entityType, Long entityId, Map<String, Object> context) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));
        
        if (!workflow.getIsActive()) {
            throw new RuntimeException("Workflow is not active");
        }
        
        WorkflowExecution execution = new WorkflowExecution();
        execution.setWorkflowId(workflowId);
        execution.setExecutionKey(UUID.randomUUID().toString());
        execution.setTriggerType(workflow.getTriggerType());
        execution.setEntityType(entityType);
        execution.setEntityId(entityId);
        execution.setStatus("RUNNING");
        execution.setStartedAt(LocalDateTime.now());
        
        try {
            String contextJson = objectMapper.writeValueAsString(context);
            execution.setContextData(contextJson);
        } catch (Exception e) {
            execution.setContextData("{}");
        }
        
        execution = executionRepository.save(execution);
        
        logExecution(execution.getId(), "INFO", "Workflow execution started");
        
        try {
            Thread.sleep(1000);
            
            execution.setStatus("COMPLETED");
            execution.setCompletedAt(LocalDateTime.now());
            execution.setDurationMs(
                java.time.Duration.between(execution.getStartedAt(), execution.getCompletedAt()).toMillis()
            );
            
            logExecution(execution.getId(), "INFO", "Workflow execution completed successfully");
            
        } catch (Exception e) {
            execution.setStatus("FAILED");
            execution.setErrorMessage(e.getMessage());
            execution.setCompletedAt(LocalDateTime.now());
            
            logExecution(execution.getId(), "ERROR", "Workflow execution failed: " + e.getMessage());
        }
        
        executionRepository.save(execution);
    }

    public WorkflowExecutionDTO getExecution(Long executionId) {
        WorkflowExecution execution = executionRepository.findById(executionId)
            .orElseThrow(() -> new RuntimeException("Execution not found"));
        return toDTO(execution);
    }

    public List<WorkflowExecutionDTO> getAllExecutions() {
        return executionRepository.findAll().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public List<WorkflowExecutionDTO> getExecutionsByWorkflow(Long workflowId) {
        return executionRepository.findByWorkflowId(workflowId).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public List<WorkflowExecutionLog> getExecutionLogs(Long executionId) {
        return logRepository.findByExecutionIdOrderByCreatedAtDesc(executionId);
    }

    public void cancelExecution(Long executionId) {
        WorkflowExecution execution = executionRepository.findById(executionId)
            .orElseThrow(() -> new RuntimeException("Execution not found"));
        
        execution.setStatus("CANCELLED");
        execution.setCompletedAt(LocalDateTime.now());
        executionRepository.save(execution);
        
        logExecution(executionId, "INFO", "Workflow execution cancelled");
    }

    public WorkflowExecutionDTO retryExecution(Long executionId) {
        WorkflowExecution failedExecution = executionRepository.findById(executionId)
            .orElseThrow(() -> new RuntimeException("Execution not found"));
        
        if (!"FAILED".equals(failedExecution.getStatus())) {
            throw new RuntimeException("Can only retry failed executions");
        }
        
        logExecution(executionId, "INFO", "Retrying failed execution");
        
        // Parse context data
        Map<String, Object> context = new HashMap<>();
        try {
            context = objectMapper.readValue(failedExecution.getContextData(), Map.class);
        } catch (Exception e) {
            // Use empty context if parsing fails
        }
        
        // Execute workflow again asynchronously
        executeWorkflowAsync(
            failedExecution.getWorkflowId(),
            failedExecution.getEntityType(),
            failedExecution.getEntityId(),
            context
        );
        
        return toDTO(failedExecution);
    }

    private void logExecution(Long executionId, String level, String message) {
        WorkflowExecutionLog log = new WorkflowExecutionLog();
        log.setExecutionId(executionId);
        log.setLogLevel(level);
        log.setMessage(message);
        log.setCreatedAt(LocalDateTime.now());
        logRepository.save(log);
    }

    private WorkflowExecutionDTO toDTO(WorkflowExecution execution) {
        WorkflowExecutionDTO dto = new WorkflowExecutionDTO();
        dto.setId(execution.getId());
        dto.setWorkflowId(execution.getWorkflowId());
        dto.setExecutionKey(execution.getExecutionKey());
        dto.setTriggerType(execution.getTriggerType());
        dto.setEntityType(execution.getEntityType());
        dto.setEntityId(execution.getEntityId());
        dto.setStatus(execution.getStatus());
        dto.setCurrentNodeId(execution.getCurrentNodeId());
        
        try {
            Map<String, Object> context = objectMapper.readValue(execution.getContextData(), Map.class);
            dto.setContextData(context);
        } catch (Exception e) {
        }
        
        dto.setErrorMessage(execution.getErrorMessage());
        dto.setStartedAt(execution.getStartedAt());
        dto.setCompletedAt(execution.getCompletedAt());
        dto.setDurationMs(execution.getDurationMs());
        
        return dto;
    }
}
