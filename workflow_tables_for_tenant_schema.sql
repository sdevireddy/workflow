-- ============================================================================
-- WORKFLOW SERVICE TABLES - Add to Tenant Schema
-- ============================================================================
-- These tables should be added to V01__tenant_schema.sql in auth-service
-- Location: auth-service/src/main/resources/db/initialscript/core/V01__tenant_schema.sql
-- ============================================================================

-- Main Workflows Table
CREATE TABLE workflows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_name VARCHAR(255) NOT NULL,
    workflow_key VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    module_type VARCHAR(50) NOT NULL COMMENT 'LEAD, CONTACT, ACCOUNT, DEAL, CUSTOM',
    trigger_type VARCHAR(50) NOT NULL COMMENT 'FIELD_UPDATE, TIME_BASED, MANUAL, WEBHOOK',
    is_active BOOLEAN DEFAULT FALSE,
    version INT DEFAULT 1,
    workflow_config JSON COMMENT 'Complete workflow definition in JSON',
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (updated_by) REFERENCES users(id),
    INDEX idx_workflows_module (module_type),
    INDEX idx_workflows_active (is_active),
    INDEX idx_workflows_trigger (trigger_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Workflow definitions';

-- Workflow Triggers
CREATE TABLE workflow_triggers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    trigger_type VARCHAR(50) NOT NULL COMMENT 'FIELD_UPDATE, RECORD_CREATE, RECORD_UPDATE, TIME_BASED, SCHEDULED',
    trigger_config JSON COMMENT 'Trigger conditions and configuration',
    cron_expression VARCHAR(100) COMMENT 'For scheduled triggers',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE,
    INDEX idx_triggers_workflow (workflow_id),
    INDEX idx_triggers_type (trigger_type),
    INDEX idx_triggers_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Workflow trigger configurations';

-- Workflow Nodes (Steps)
CREATE TABLE workflow_nodes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    node_key VARCHAR(100) NOT NULL,
    node_type VARCHAR(50) NOT NULL COMMENT 'ACTION, CONDITION, WAIT, APPROVAL, LOOP, WEBHOOK',
    node_config JSON COMMENT 'Node-specific configuration',
    position_x INT COMMENT 'X coordinate for visual builder',
    position_y INT COMMENT 'Y coordinate for visual builder',
    next_node_id BIGINT COMMENT 'Next node for linear flow',
    true_node_id BIGINT COMMENT 'Next node if condition is true',
    false_node_id BIGINT COMMENT 'Next node if condition is false',
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE,
    UNIQUE KEY uk_workflow_node (workflow_id, node_key),
    INDEX idx_nodes_workflow (workflow_id),
    INDEX idx_nodes_type (node_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Individual workflow steps/nodes';

-- Workflow Actions
CREATE TABLE workflow_actions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_id BIGINT NOT NULL,
    action_type VARCHAR(50) NOT NULL COMMENT 'SEND_EMAIL, SEND_SMS, UPDATE_FIELD, CREATE_RECORD, WEBHOOK, CUSTOM_FUNCTION',
    action_config JSON COMMENT 'Action-specific configuration',
    template_id BIGINT COMMENT 'Reference to email/SMS template',
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    timeout_seconds INT DEFAULT 30,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (node_id) REFERENCES workflow_nodes(id) ON DELETE CASCADE,
    INDEX idx_actions_node (node_id),
    INDEX idx_actions_type (action_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Workflow action definitions';

-- Workflow Executions
CREATE TABLE workflow_executions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    execution_key VARCHAR(100) UNIQUE NOT NULL,
    trigger_type VARCHAR(50),
    entity_type VARCHAR(50) COMMENT 'LEAD, CONTACT, ACCOUNT, DEAL',
    entity_id BIGINT COMMENT 'ID of the record that triggered workflow',
    status VARCHAR(50) DEFAULT 'RUNNING' COMMENT 'RUNNING, COMPLETED, FAILED, PAUSED, CANCELLED',
    current_node_id BIGINT,
    context_data JSON COMMENT 'Runtime context and variables',
    error_message TEXT,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    duration_ms BIGINT COMMENT 'Execution duration in milliseconds',
    
    FOREIGN KEY (workflow_id) REFERENCES workflows(id),
    FOREIGN KEY (current_node_id) REFERENCES workflow_nodes(id),
    INDEX idx_executions_workflow (workflow_id),
    INDEX idx_executions_status (status),
    INDEX idx_executions_entity (entity_type, entity_id),
    INDEX idx_executions_started (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Workflow execution instances';

-- Workflow Execution Logs
CREATE TABLE workflow_execution_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id BIGINT NOT NULL,
    node_id BIGINT,
    log_level VARCHAR(20) DEFAULT 'INFO' COMMENT 'DEBUG, INFO, WARN, ERROR',
    message TEXT NOT NULL,
    details JSON COMMENT 'Additional log details',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (execution_id) REFERENCES workflow_executions(id) ON DELETE CASCADE,
    FOREIGN KEY (node_id) REFERENCES workflow_nodes(id),
    INDEX idx_logs_execution (execution_id),
    INDEX idx_logs_level (log_level),
    INDEX idx_logs_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Detailed execution logs';

-- Approval Processes
CREATE TABLE workflow_approval_processes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    approval_name VARCHAR(255) NOT NULL,
    approval_type VARCHAR(50) DEFAULT 'SEQUENTIAL' COMMENT 'SEQUENTIAL, PARALLEL, UNANIMOUS',
    approval_config JSON COMMENT 'Approval chain configuration',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE,
    INDEX idx_approval_processes_workflow (workflow_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Approval process definitions';

-- Approval Requests
CREATE TABLE workflow_approval_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id BIGINT NOT NULL,
    approval_process_id BIGINT NOT NULL,
    approver_id BIGINT NOT NULL,
    approval_level INT DEFAULT 1,
    status VARCHAR(50) DEFAULT 'PENDING' COMMENT 'PENDING, APPROVED, REJECTED, DELEGATED',
    comments TEXT,
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP NULL,
    delegated_to BIGINT,
    
    FOREIGN KEY (execution_id) REFERENCES workflow_executions(id) ON DELETE CASCADE,
    FOREIGN KEY (approval_process_id) REFERENCES workflow_approval_processes(id),
    FOREIGN KEY (approver_id) REFERENCES users(id),
    FOREIGN KEY (delegated_to) REFERENCES users(id),
    INDEX idx_approval_requests_execution (execution_id),
    INDEX idx_approval_requests_approver (approver_id),
    INDEX idx_approval_requests_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Individual approval requests';

-- Workflow Schedules
CREATE TABLE workflow_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    schedule_name VARCHAR(255) NOT NULL,
    cron_expression VARCHAR(100) NOT NULL,
    timezone VARCHAR(50) DEFAULT 'UTC',
    is_active BOOLEAN DEFAULT TRUE,
    last_run_at TIMESTAMP NULL,
    next_run_at TIMESTAMP NULL,
    run_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE,
    INDEX idx_schedules_workflow (workflow_id),
    INDEX idx_schedules_next_run (next_run_at),
    INDEX idx_schedules_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Scheduled workflow runs';

-- Workflow Variables
CREATE TABLE workflow_variables (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    variable_name VARCHAR(100) NOT NULL,
    variable_type VARCHAR(50) DEFAULT 'STRING' COMMENT 'STRING, NUMBER, BOOLEAN, DATE, JSON',
    default_value TEXT,
    description VARCHAR(500),
    is_required BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE,
    UNIQUE KEY uk_workflow_variable (workflow_id, variable_name),
    INDEX idx_variables_workflow (workflow_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Workflow-level variables';

-- Workflow Versions (for version control)
CREATE TABLE workflow_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    workflow_snapshot JSON COMMENT 'Complete workflow configuration snapshot',
    change_summary TEXT,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id),
    UNIQUE KEY uk_workflow_version (workflow_id, version_number),
    INDEX idx_versions_workflow (workflow_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Workflow version history';

-- Workflow Analytics
CREATE TABLE workflow_analytics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    date DATE NOT NULL,
    total_executions INT DEFAULT 0,
    successful_executions INT DEFAULT 0,
    failed_executions INT DEFAULT 0,
    avg_duration_ms BIGINT DEFAULT 0,
    max_duration_ms BIGINT DEFAULT 0,
    min_duration_ms BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE,
    UNIQUE KEY uk_workflow_analytics_date (workflow_id, date),
    INDEX idx_analytics_workflow (workflow_id),
    INDEX idx_analytics_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Daily workflow performance metrics';

-- Webhook Configurations
CREATE TABLE workflow_webhooks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    webhook_name VARCHAR(255) NOT NULL,
    webhook_url VARCHAR(1000) NOT NULL,
    http_method VARCHAR(10) DEFAULT 'POST',
    headers JSON COMMENT 'HTTP headers',
    auth_type VARCHAR(50) COMMENT 'NONE, BASIC, BEARER, API_KEY',
    auth_config JSON COMMENT 'Authentication configuration',
    timeout_seconds INT DEFAULT 30,
    retry_count INT DEFAULT 3,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE,
    INDEX idx_webhooks_workflow (workflow_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Webhook configurations for workflows';

-- Workflow Templates (Predefined workflows in common schema)
-- Note: This table should be in the 'common' schema for shared templates
CREATE TABLE workflow_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_name VARCHAR(255) NOT NULL,
    template_key VARCHAR(100) UNIQUE NOT NULL,
    category VARCHAR(100) COMMENT 'LEAD_NURTURING, SALES_AUTOMATION, CUSTOMER_ONBOARDING, etc.',
    description TEXT,
    module_type VARCHAR(50) NOT NULL,
    template_config JSON COMMENT 'Complete workflow template',
    preview_image_url VARCHAR(500),
    is_premium BOOLEAN DEFAULT FALSE,
    usage_count INT DEFAULT 0,
    rating DECIMAL(3,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_templates_category (category),
    INDEX idx_templates_module (module_type),
    INDEX idx_templates_premium (is_premium)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Predefined workflow templates';

-- ============================================================================
-- INDEXES FOR PERFORMANCE OPTIMIZATION
-- ============================================================================

-- Composite indexes for common queries
CREATE INDEX idx_executions_workflow_status ON workflow_executions(workflow_id, status);
CREATE INDEX idx_executions_entity_status ON workflow_executions(entity_type, entity_id, status);
CREATE INDEX idx_logs_execution_level ON workflow_execution_logs(execution_id, log_level);
CREATE INDEX idx_approval_requests_approver_status ON workflow_approval_requests(approver_id, status);

-- ============================================================================
-- SAMPLE DATA FOR TESTING
-- ============================================================================

-- Insert sample workflow template
INSERT INTO workflow_templates (template_name, template_key, category, description, module_type, template_config) VALUES
('Lead Follow-up Automation', 'lead_followup_auto', 'LEAD_NURTURING', 'Automatically follow up with new leads after 24 hours', 'LEAD', 
'{"nodes": [{"id": "N1", "type": "WAIT", "config": {"duration": 24, "unit": "HOURS"}}, {"id": "N2", "type": "ACTION", "actionType": "SEND_EMAIL", "config": {"templateId": "welcome_email"}}]}');

-- ============================================================================
-- END OF WORKFLOW TABLES
-- ============================================================================
