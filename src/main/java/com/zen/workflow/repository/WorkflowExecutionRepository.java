package com.zen.workflow.repository;

import com.zen.entities.tenant.WorkflowExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, Long> {
    
    List<WorkflowExecution> findByWorkflowId(Long workflowId);
    
    List<WorkflowExecution> findByTenantId(String tenantId);
    
    List<WorkflowExecution> findByExecutionStatus(String status);
    
    List<WorkflowExecution> findByWorkflowIdAndExecutionStatus(Long workflowId, String status);
}
