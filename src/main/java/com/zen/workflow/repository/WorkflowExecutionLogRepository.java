package com.zen.workflow.repository;

import com.zen.entities.tenant.WorkflowExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorkflowExecutionLogRepository extends JpaRepository<WorkflowExecutionLog, Long> {
    List<WorkflowExecutionLog> findByExecutionIdOrderByCreatedAtDesc(Long executionId);
}
