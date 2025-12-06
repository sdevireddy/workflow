package com.zen.workflow.repository;

import com.zen.entities.tenant.WorkflowNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorkflowNodeRepository extends JpaRepository<WorkflowNode, Long> {
    List<WorkflowNode> findByWorkflowIdOrderBySortOrder(Long workflowId);
}
