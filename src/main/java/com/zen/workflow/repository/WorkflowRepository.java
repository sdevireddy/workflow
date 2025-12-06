package com.zen.workflow.repository;

import com.zen.entities.tenant.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, Long> {
    Optional<Workflow> findByWorkflowKey(String workflowKey);
    List<Workflow> findByIsActive(Boolean isActive);
    List<Workflow> findByModuleType(String moduleType);
    
    @Query("SELECT w FROM Workflow w WHERE w.moduleType = ?1 AND w.isActive = true")
    List<Workflow> findActiveWorkflowsByModule(String moduleType);
}
