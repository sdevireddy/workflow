package com.zen.workflow.repository;

import com.zen.entities.tenant.WorkflowApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorkflowApprovalRequestRepository extends JpaRepository<WorkflowApprovalRequest, Long> {
    List<WorkflowApprovalRequest> findByExecutionId(Long executionId);
    List<WorkflowApprovalRequest> findByApproverIdAndStatus(Long approverId, String status);
    
    @Query("SELECT ar FROM WorkflowApprovalRequest ar WHERE ar.approverId = ?1 AND ar.status = 'PENDING'")
    List<WorkflowApprovalRequest> findPendingApprovalsByApprover(Long approverId);
}
