package com.zen.workflow.service;

import com.zen.entities.tenant.Workflow;
import com.zen.workflow.dto.WorkflowDTO;
import com.zen.workflow.repository.WorkflowRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class WorkflowService {

    @Autowired
    private WorkflowRepository workflowRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    public WorkflowDTO createWorkflow(WorkflowDTO dto) {
        Workflow workflow = new Workflow();
        workflow.setWorkflowName(dto.getWorkflowName());
        workflow.setWorkflowKey(dto.getWorkflowKey());
        workflow.setDescription(dto.getDescription());
        workflow.setModuleType(dto.getModuleType());
        workflow.setTriggerType(dto.getTriggerType());
        workflow.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : false);
        workflow.setVersion(1);
        
        try {
            String configJson = objectMapper.writeValueAsString(dto.getWorkflowConfig());
            workflow.setWorkflowConfig(configJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize workflow config", e);
        }
        
        workflow.setCreatedBy(dto.getCreatedBy());
        workflow.setCreatedAt(LocalDateTime.now());
        workflow.setUpdatedAt(LocalDateTime.now());
        
        Workflow saved = workflowRepository.save(workflow);
        return toDTO(saved);
    }

    public WorkflowDTO updateWorkflow(Long id, WorkflowDTO dto) {
        Workflow workflow = workflowRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));
        
        workflow.setWorkflowName(dto.getWorkflowName());
        workflow.setDescription(dto.getDescription());
        workflow.setModuleType(dto.getModuleType());
        workflow.setTriggerType(dto.getTriggerType());
        workflow.setIsActive(dto.getIsActive());
        
        try {
            String configJson = objectMapper.writeValueAsString(dto.getWorkflowConfig());
            workflow.setWorkflowConfig(configJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize workflow config", e);
        }
        
        workflow.setUpdatedBy(dto.getUpdatedBy());
        workflow.setUpdatedAt(LocalDateTime.now());
        workflow.setVersion(workflow.getVersion() + 1);
        
        Workflow updated = workflowRepository.save(workflow);
        return toDTO(updated);
    }

    public WorkflowDTO getWorkflow(Long id) {
        Workflow workflow = workflowRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));
        return toDTO(workflow);
    }

    public List<WorkflowDTO> getAllWorkflows() {
        return workflowRepository.findAll().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public List<WorkflowDTO> getActiveWorkflows() {
        return workflowRepository.findByIsActive(true).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public List<WorkflowDTO> getWorkflowsByModule(String moduleType) {
        return workflowRepository.findByModuleType(moduleType).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public void deleteWorkflow(Long id) {
        workflowRepository.deleteById(id);
    }

    public WorkflowDTO activateWorkflow(Long id) {
        Workflow workflow = workflowRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));
        workflow.setIsActive(true);
        workflow.setUpdatedAt(LocalDateTime.now());
        return toDTO(workflowRepository.save(workflow));
    }

    public WorkflowDTO deactivateWorkflow(Long id) {
        Workflow workflow = workflowRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));
        workflow.setIsActive(false);
        workflow.setUpdatedAt(LocalDateTime.now());
        return toDTO(workflowRepository.save(workflow));
    }

    private WorkflowDTO toDTO(Workflow workflow) {
        WorkflowDTO dto = new WorkflowDTO();
        dto.setId(workflow.getId());
        dto.setWorkflowName(workflow.getWorkflowName());
        dto.setWorkflowKey(workflow.getWorkflowKey());
        dto.setDescription(workflow.getDescription());
        dto.setModuleType(workflow.getModuleType());
        dto.setTriggerType(workflow.getTriggerType());
        dto.setIsActive(workflow.getIsActive());
        dto.setVersion(workflow.getVersion());
        
        try {
            Map<String, Object> config = objectMapper.readValue(workflow.getWorkflowConfig(), Map.class);
            dto.setWorkflowConfig(config);
        } catch (Exception e) {
            // Handle parsing error
        }
        
        dto.setCreatedBy(workflow.getCreatedBy());
        dto.setUpdatedBy(workflow.getUpdatedBy());
        dto.setCreatedAt(workflow.getCreatedAt());
        dto.setUpdatedAt(workflow.getUpdatedAt());
        
        return dto;
    }
}
